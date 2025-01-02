package codechicken.chunkloader.tile;

import codechicken.chunkloader.api.IChunkLoader;
import codechicken.chunkloader.api.IChunkLoaderHandler;
import codechicken.chunkloader.network.ChunkLoaderSPH;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public abstract class TileChunkLoaderBase extends BlockEntity implements IChunkLoader {

    @Nullable
    public UUID owner;
    @Nullable
    public Component ownerName;
    protected boolean loaded = false;
    protected boolean unloaded = false;
    protected boolean powered = false;
    public boolean active = false;

    @Nullable
    public RenderInfo renderInfo;

    public TileChunkLoaderBase(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("powered", powered);
        if (owner != null) {
            assert ownerName != null;

            tag.putUUID("owner", owner);
            tag.putString("owner_name", Component.Serializer.toJson(ownerName, registries));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("owner")) {
            owner = tag.getUUID("owner");
            ownerName = Component.Serializer.fromJson(tag.getString("owner_name"), registries);
        }
        if (tag.contains("powered")) {
            powered = tag.getBoolean("powered");
        }
        loaded = true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        assert level != null;
        if (!level.isClientSide && loaded && !powered) {
            activate();
        }
        unloaded = false;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();

        assert level != null;
        if (level.isClientSide) {
            renderInfo = new RenderInfo();
        }
        unloaded = false;
    }

    public boolean isPowered() {
        for (Direction face : Direction.BY_3D_DATA) {
            assert level != null;
            boolean isPowered = isPoweringTo(level, getBlockPos().relative(face), face);
            if (isPowered) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPoweringTo(Level world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);
        return state.getSignal(world, pos, side) > 0;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        assert level != null;
        if (!level.isClientSide && !unloaded) {
            deactivate();
        }
    }

    public ChunkPos getChunkPosition() {
        return new ChunkPos(getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
    }

    public void onBlockPlacedBy(LivingEntity entityliving) {
        if (entityliving instanceof Player) {
            owner = entityliving.getUUID();
            ownerName = entityliving.getName();
        }
        activate();
    }

    @Override
    @Nullable
    public UUID getOwner() {
        return owner;
    }

    @Override
    public Level world() {
        return requireNonNull(level);
    }

    @Override
    public BlockPos pos() {
        return getBlockPos();
    }

    @Override
    public void deactivate() {
        if (owner == null) {
            return;
        }
        loaded = true;
        active = false;
        IChunkLoaderHandler.instance().removeChunkLoader(this);
        ChunkLoaderSPH.sendStateUpdate(this);
    }

    public void activate() {
        if (owner == null) {
            return;
        }
        loaded = true;
        active = true;
        IChunkLoaderHandler.instance().addChunkLoader(this);
        ChunkLoaderSPH.sendStateUpdate(this);
    }

    @Override
    public boolean isValid() {
        return !remove;
    }

    public void tickServer() {
        boolean nowPowered = isPowered();
        if (powered != nowPowered) {
            powered = nowPowered;
            if (powered) {
                deactivate();
            } else {
                activate();
            }
        }
    }

    public void tickClient() {
        assert renderInfo != null;
        renderInfo.update(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putBoolean("active", active);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        active = tag.getBoolean("active");
    }

    public void writeToPacket(MCDataOutput packet) {
        packet.writeBoolean(active);
        packet.writeBoolean(owner != null);
        if (owner != null) {
            packet.writeUUID(owner);
            assert ownerName != null;
            packet.writeTextComponent(ownerName);
        }
    }

    public void readFromPacket(MCDataInput packet) {
        active = packet.readBoolean();
        if (packet.readBoolean()) {
            owner = packet.readUUID();
            ownerName = packet.readTextComponent();
        }
    }

    public static class RenderInfo {

        public int activationCounter;
        public boolean showLasers;

        public void update(TileChunkLoaderBase chunkLoader) {
            if (activationCounter < 20 && chunkLoader.active) {
                activationCounter++;
            } else if (activationCounter > 0 && !chunkLoader.active) {
                activationCounter--;
            }
        }
    }
}
