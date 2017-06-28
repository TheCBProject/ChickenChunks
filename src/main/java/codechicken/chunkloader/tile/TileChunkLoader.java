package codechicken.chunkloader.tile;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;

import java.util.Collection;
import java.util.Set;

public class TileChunkLoader extends TileChunkLoaderBase {

    public int radius;
    public ChunkLoaderShape shape = ChunkLoaderShape.Square;

    public boolean setShapeAndRadius(ChunkLoaderShape newShape, int newRadius) {
        if (world.isRemote) {
            radius = newRadius;
            shape = newShape;
            return true;
        }
        Set<ChunkPos> chunks = getContainedChunks(newShape, getPos().getX(), getPos().getZ(), newRadius);
        if (chunks.size() > ChunkLoaderManager.maxChunksPerLoader()) {
            return false;
        } else if (powered) {
            radius = newRadius;
            shape = newShape;
            IBlockState state = world.getBlockState(getPos());
            world.notifyBlockUpdate(getPos(), state, state, 3);
            return true;
        } else if (ChunkLoaderManager.canLoaderAdd(this, chunks)) {
            radius = newRadius;
            shape = newShape;
            ChunkLoaderManager.updateLoader(this);
            IBlockState state = world.getBlockState(getPos());
            world.notifyBlockUpdate(getPos(), state, state, 3);
            return true;
        }
        return false;
    }

    @Override
    public void writeToPacket(MCDataOutput packet) {
        super.writeToPacket(packet);
        packet.writeByte(shape.ordinal());
        packet.writeByte(radius);
    }

    @Override
    public void readFromPacket(MCDataInput packet) {
        super.readFromPacket(packet);
        setShapeAndRadius(ChunkLoaderShape.values()[packet.readUByte()], packet.readUByte());
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte("radius", (byte) radius);
        tag.setByte("shape", (byte) shape.ordinal());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        radius = tag.getByte("radius");
        shape = ChunkLoaderShape.values()[tag.getByte("shape")];
    }

    @Override
    public Set<ChunkPos> getChunks() {
        return getContainedChunks(shape, getPos().getX(), getPos().getZ(), radius);
    }

    public static Set<ChunkPos> getContainedChunks(ChunkLoaderShape shape, int xCoord, int zCoord, int radius) {
        return shape.getLoadedChunks(xCoord >> 4, zCoord >> 4, radius - 1);
    }

    public int countLoadedChunks() {
        return getChunks().size();
    }

    @Override
    public void activate() {
        if (radius == 0) {
            //create a small one and try and increment it to 2
            radius = 1;
            shape = ChunkLoaderShape.Square;
            setShapeAndRadius(ChunkLoaderShape.Square, 2);
        }

        super.activate();
    }
}
