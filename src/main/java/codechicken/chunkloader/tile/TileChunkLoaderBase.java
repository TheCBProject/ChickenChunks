package codechicken.chunkloader.tile;

import codechicken.chunkloader.ChickenChunks;
import codechicken.chunkloader.api.IChunkLoader;
import codechicken.chunkloader.api.IChunkLoaderHandler;
import codechicken.chunkloader.network.ChickenChunksNetwork;
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
    public CompoundNBT write(CompoundNBT tag) {
        super.write(tag);
        tag.putBoolean("powered", powered);
        if (owner != null) {
            tag.put("owner", NBTUtil.writeUniqueId(owner));
            tag.putString("owner_name", ITextComponent.Serializer.toJson(ownerName));
        }
        return tag;
    }

    @Override
    public void read(CompoundNBT tag) {
        super.read(tag);
        if (tag.contains("owner")) {
            owner = NBTUtil.readUniqueId(tag.getCompound("owner"));
            ownerName = ITextComponent.Serializer.fromJson(tag.getString("owner_name"));
        }
        if (tag.contains("powered")) {
            powered = tag.getBoolean("powered");
        }
        loaded = true;
    }

    public void validate() {
        super.validate();
        if (!world.isRemote && loaded && !powered) {
            activate();
        }

        if (world.isRemote) {
            renderInfo = new RenderInfo();
        }
    }

    public boolean isPowered() {
        for (Direction face : Direction.BY_INDEX) {
            boolean isPowered = isPoweringTo(world, getPos().offset(face), face);
            if (isPowered) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPoweringTo(World world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock().getWeakPower(state, world, pos, side) > 0;
    }

    @Override
    public void remove() {
        super.remove();
        if (!world.isRemote) {
            deactivate();
        }
    }

    public void destroyBlock() {
        //        ModBlocks.blockChunkLoader.dropBlockAsItem(world, getPos(), world.getBlockState(pos), 0);
        //        world.setBlockToAir(getPos());
    }

    public ChunkPos getChunkPosition() {
        return new ChunkPos(getPos().getX() >> 4, getPos().getZ() >> 4);
    }

    public void onBlockPlacedBy(LivingEntity entityliving) {
        if (entityliving instanceof PlayerEntity) {
            owner = entityliving.getUniqueID();
            ownerName = entityliving.getName();
        }
        //TODO
        //        if (owner.equals("")) {
        //            owner = null;
        //        }
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
        return world;
    }

    @Override
    public BlockPos pos() {
        return getPos();
    }

    @Override
    public void deactivate() {
        if (owner == null) {
            return;
        }
        loaded = true;
        active = false;
        IChunkLoaderHandler.getCapability(world).removeChunkLoader(this);
    }

    public void activate() {
        if (owner == null) {
            return;
        }
        loaded = true;
        active = true;
        IChunkLoaderHandler.getCapability(world).addChunkLoader(this);
    }

    @Override
    public boolean isValid() {
        return !removed;
    }

    @Override
    public void tick() {
        if (!world.isRemote) {
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

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        PacketCustom packet = new PacketCustom(NET_CHANNEL, 1);//Dummy Index.
        writeToPacket(packet);
        return packet.toTilePacket(getPos());
    }

    @Override
    public CompoundNBT getUpdateTag() {
        PacketCustom packet = new PacketCustom(NET_CHANNEL, 1);//Dummy Index.
        writeToPacket(packet);
        return packet.writeToNBT(super.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        readFromPacket(PacketCustom.fromTilePacket(pkt));
    }

    @Override
    public void handleUpdateTag(CompoundNBT tag) {
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
    public double getMaxRenderDistanceSquared() {
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
