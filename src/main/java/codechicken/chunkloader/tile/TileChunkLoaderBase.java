package codechicken.chunkloader.tile;

import codechicken.chunkloader.ChickenChunks;
import codechicken.chunkloader.api.IChunkLoader;
import codechicken.chunkloader.api.IChunkLoaderHandler;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.chunkloader.network.ChunkLoaderSPH;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

import static codechicken.chunkloader.network.ChickenChunksNetwork.*;

public abstract class TileChunkLoaderBase extends TileEntity implements ITickableTileEntity, IChunkLoader {

    public UUID owner;
    public ITextComponent ownerName;
    protected boolean loaded = false;
    protected boolean powered = false;
    public RenderInfo renderInfo;
    public boolean active = false;

    public TileChunkLoaderBase(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putBoolean("powered", powered);
        if (owner != null) {
            tag.putUUID("owner", owner);
            tag.putString("owner_name", ITextComponent.Serializer.toJson(ownerName));
        }
        return tag;
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        if (tag.contains("owner")) {
            owner = tag.getUUID("owner");
            ownerName = ITextComponent.Serializer.fromJson(tag.getString("owner_name"));
        }
        if (tag.contains("powered")) {
            powered = tag.getBoolean("powered");
        }
        loaded = true;
    }

    public void clearRemoved() {
        super.clearRemoved();
        if (!level.isClientSide && loaded && !powered) {
            activate();
        }

        if (level.isClientSide) {
            renderInfo = new RenderInfo();
        }
    }

    public boolean isPowered() {
        for (Direction face : Direction.BY_3D_DATA) {
            boolean isPowered = isPoweringTo(level, getBlockPos().relative(face), face);
            if (isPowered) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPoweringTo(World world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock().getSignal(state, world, pos, side) > 0;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!level.isClientSide) {
            deactivate();
        }
    }

    public ChunkPos getChunkPosition() {
        return new ChunkPos(getBlockPos().getX() >> 4, getBlockPos().getZ() >> 4);
    }

    public void onBlockPlacedBy(LivingEntity entityliving) {
        if (entityliving instanceof PlayerEntity) {
            owner = entityliving.getUUID();
            ownerName = entityliving.getName();
        }
        activate();
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public String getMod() {
        return ChickenChunks.MOD_ID;
    }

    @Override
    public World world() {
        return level;
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
        IChunkLoaderHandler.getCapability(level).removeChunkLoader(this);
        ChunkLoaderSPH.sendStateUpdate(this);
    }

    public void activate() {
        if (owner == null) {
            return;
        }
        loaded = true;
        active = true;
        IChunkLoaderHandler.getCapability(level).addChunkLoader(this);
        ChunkLoaderSPH.sendStateUpdate(this);
    }

    @Override
    public boolean isValid() {
        return !remove;
    }

    @Override
    public void tick() {
        if (!level.isClientSide) {
            boolean nowPowered = isPowered();
            if (powered != nowPowered) {
                powered = nowPowered;
                if (powered) {
                    deactivate();
                } else {
                    activate();
                }
            }
        } else {
            renderInfo.update(this);
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
    @Override
    public CompoundNBT getUpdateTag() {
        PacketCustom packet = new PacketCustom(NET_CHANNEL, 1);//Dummy Index.
        writeToPacket(packet);
        return packet.writeToNBT(super.getUpdateTag());
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        readFromPacket(PacketCustom.fromNBTTag(tag));
    }

    public void writeToPacket(MCDataOutput packet) {
        packet.writeBoolean(active);
        packet.writeBoolean(owner != null);
        if (owner != null) {
            packet.writeUUID(owner);
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

    @Override
    public double getViewDistance() {
        return 65536.0D;
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
