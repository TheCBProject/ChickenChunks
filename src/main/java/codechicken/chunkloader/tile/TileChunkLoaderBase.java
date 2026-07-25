package codechicken.chunkloader.tile;

import codechicken.chunkloader.api.IChunkLoader;
import codechicken.chunkloader.api.IChunkLoaderHandler;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("powered", powered);
        if (owner != null) {
            assert ownerName != null;

            output.store("owner", UUIDUtil.CODEC, owner);
            output.store("owner_name", ComponentSerialization.CODEC, ownerName);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        powered = input.getBooleanOr("powered", false);
        input.read("owner", UUIDUtil.CODEC).ifPresent(e -> owner = e);
        input.read("owner_name", ComponentSerialization.CODEC).ifPresent(e -> ownerName = e);
        loaded = true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        assert level != null;
        if (!level.isClientSide() && loaded && !powered) {
            activate();
        }
        unloaded = false;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();

        assert level != null;
        if (level.isClientSide()) {
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
        if (!level.isClientSide() && !unloaded) {
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
        ChickenChunksNetwork.sendTileUpdate(this);
    }

    public void activate() {
        if (owner == null) {
            return;
        }
        loaded = true;
        active = true;
        IChunkLoaderHandler.instance().addChunkLoader(this);
        ChickenChunksNetwork.sendTileUpdate(this);
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
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
        active = input.getBooleanOr("active", false);
    }

    public void writeToPacket(RegistryFriendlyByteBuf packet) {
        packet.writeBoolean(active);
        packet.writeBoolean(owner != null);
        if (owner != null) {
            packet.writeUUID(owner);
            assert ownerName != null;
            packet.cc$writeWithRegistryCodec(ComponentSerialization.STREAM_CODEC, ownerName);
        }
    }

    public void readFromPacket(RegistryFriendlyByteBuf packet) {
        active = packet.readBoolean();
        if (packet.readBoolean()) {
            owner = packet.readUUID();
            ownerName = packet.cc$readWithRegistryCodec(ComponentSerialization.STREAM_CODEC);
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
