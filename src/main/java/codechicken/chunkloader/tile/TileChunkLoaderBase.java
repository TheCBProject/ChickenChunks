package codechicken.chunkloader.tile;

import codechicken.chunkloader.ChickenChunks;
import codechicken.chunkloader.api.IChickenChunkLoader;
import codechicken.chunkloader.client.TileChunkLoaderRenderer.RenderInfo;
import codechicken.chunkloader.init.ModBlocks;
import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.lib.packet.INBTPacketTile;
import codechicken.lib.vec.BlockCoord;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public abstract class TileChunkLoaderBase extends TileEntity implements ITickable, IChickenChunkLoader, INBTPacketTile {

    public String owner;
    protected boolean loaded = false;
    protected boolean powered = false;
    public RenderInfo renderInfo;
    public boolean active = false;

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("powered", powered);
        if (owner != null) {
            tag.setString("owner", owner);
        }
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("owner")) {
            owner = tag.getString("owner");
        }
        if (tag.hasKey("powered")) {
            powered = tag.getBoolean("powered");
        }
        loaded = true;
    }

    public void validate() {
        super.validate();
        if (!worldObj.isRemote && loaded && !powered) {
            activate();
        }

        if (worldObj.isRemote) {
            renderInfo = new RenderInfo();
        }
    }

    public boolean isPowered() {
        for (EnumFacing face : EnumFacing.VALUES) {
            boolean isPowered = isPoweringTo(worldObj, getPos().offset(face), face);
            if (isPowered) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPoweringTo(World world, BlockPos pos, EnumFacing side) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().getWeakPower(state, world, pos, side) > 0;
    }

    public void invalidate() {
        super.invalidate();
        if (!worldObj.isRemote) {
            deactivate();
        }
    }

    public void destroyBlock() {
        ModBlocks.blockChunkLoader.dropBlockAsItem(worldObj, getPos(), worldObj.getBlockState(pos), 0);
        worldObj.setBlockToAir(getPos());
    }

    public ChunkPos getChunkPosition() {
        return new ChunkPos(getPos().getX() >> 4, getPos().getZ() >> 4);
    }

    public void onBlockPlacedBy(EntityLivingBase entityliving) {
        if (entityliving instanceof EntityPlayer) {
            owner = entityliving.getName();
        }
        if (owner.equals("")) {
            owner = null;
        }
        activate();
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public Object getMod() {
        return ChickenChunks.instance;
    }

    @Override
    public World getLoaderWorld() {
        return worldObj;
    }

    @Override
    public BlockCoord getPosition() {
        return new BlockCoord(this);
    }

    @Override
    public void deactivate() {
        loaded = true;
        active = false;
        ChunkLoaderManager.remChunkLoader(this);
        //IBlockState state = worldObj.getBlockState(getPos());
        //worldObj.notifyBlockUpdate(getPos(), state, state, 3);
    }

    public void activate() {
        loaded = true;
        active = true;
        ChunkLoaderManager.addChunkLoader(this);
        //IBlockState state = worldObj.getBlockState(getPos());
        //worldObj.notifyBlockUpdate(getPos(), state, state, 3);
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
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
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        writePacketData(tagCompound);
        return new SPacketUpdateTileEntity(getPos(), 0, tagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readPacketData(pkt.getNbtCompound());
    }

    @Override
    public void writePacketData(NBTTagCompound tagCompound) {
        tagCompound.setBoolean("active", active);
        if (owner != null) {
            tagCompound.setString("owner", owner);
        }
    }

    @Override
    public void readPacketData(NBTTagCompound tagCompound) {
        active = tagCompound.getBoolean("active");
        if (tagCompound.hasKey("owner")) {
            owner = tagCompound.getString("owner");
        }
    }
}
