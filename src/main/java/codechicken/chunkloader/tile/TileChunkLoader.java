package codechicken.chunkloader.tile;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.api.IChunkLoaderHandler;
import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.init.ModContent;
import codechicken.chunkloader.network.ChunkLoaderSPH;
import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Set;

public class TileChunkLoader extends TileChunkLoaderBase {

    public int radius;
    public ChunkLoaderShape shape = ChunkLoaderShape.SQUARE;

    public TileChunkLoader() {
        super(ModContent.tileChunkLoaderType);
    }

    public boolean setShapeAndRadius(ChunkLoaderShape newShape, int newRadius) {
        return setShapeAndRadius(null, newShape, newRadius);
    }

    public boolean setShapeAndRadius(ServerPlayerEntity player, ChunkLoaderShape newShape, int newRadius) {
        if (owner == null || newRadius < 0 || newRadius > 255) return false;

        if (level.isClientSide) {
            radius = newRadius;
            shape = newShape;
            return true;
        }

        Set<ChunkPos> chunks = getContainedChunks(newShape, getBlockPos().getX(), getBlockPos().getZ(), newRadius);
        ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getRestrictions(owner);
        if (chunks.size() > restrictions.getChunksPerLoader()) {
            if (player != null) {
                int more = chunks.size() - restrictions.getChunksPerLoader();
                ChunkLoaderSPH.sendGuiWarning(player, new TranslationTextComponent("chickenchunks.gui.morechunks", more));
            }
            return false;
        }
        if (powered) {
            radius = newRadius;
            shape = newShape;
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
            ChunkLoaderSPH.sendStateUpdate(this);
            return true;
        }
        IChunkLoaderHandler handler = IChunkLoaderHandler.getCapability(level);
        if (handler.canLoadChunks(this, chunks)) {
            radius = newRadius;
            shape = newShape;
            handler.updateLoader(this);
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
            ChunkLoaderSPH.sendStateUpdate(this);
            return true;
        }
        return false;
    }

    @Override
    public void writeToPacket(MCDataOutput packet) {
        super.writeToPacket(packet);
        packet.writeEnum(shape);
        packet.writeByte(radius);
    }

    @Override
    public void readFromPacket(MCDataInput packet) {
        super.readFromPacket(packet);
        setShapeAndRadius(packet.readEnum(ChunkLoaderShape.class), packet.readUByte());
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putByte("radius", (byte) radius);
        tag.putByte("shape", (byte) shape.ordinal());
        return tag;
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        radius = tag.getByte("radius");
        shape = ChunkLoaderShape.values()[tag.getByte("shape")];
    }

    @Override
    public Set<ChunkPos> getChunks() {
        return getContainedChunks(shape, getBlockPos().getX(), getBlockPos().getZ(), radius);
    }

    public static Set<ChunkPos> getContainedChunks(ChunkLoaderShape shape, int xCoord, int zCoord, int radius) {
        return shape.getLoadedChunks(xCoord >> 4, zCoord >> 4, radius - 1);
    }

    public int countLoadedChunks() {
        return getChunks().size();
    }

    @Override
    public void activate() {
        if (owner == null) {
            return;
        }
        if (radius == 0) {
            //create a small one and try and increment it to 2
            radius = 1;
            shape = ChunkLoaderShape.SQUARE;
            setShapeAndRadius(ChunkLoaderShape.SQUARE, 2);
        }

        super.activate();
    }
}
