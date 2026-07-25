package codechicken.chunkloader.tile;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.api.IChunkLoaderHandler;
import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.init.ChickenChunksModContent;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class TileChunkLoader extends TileChunkLoaderBase {

    public int radius;
    public ChunkLoaderShape shape = ChunkLoaderShape.SQUARE;

    public TileChunkLoader(BlockPos pos, BlockState state) {
        super(ChickenChunksModContent.CHUNK_LOADER_TILE.get(), pos, state);
    }

    public boolean setShapeAndRadius(ChunkLoaderShape newShape, int newRadius) {
        return setShapeAndRadius(null, newShape, newRadius);
    }

    public boolean setShapeAndRadius(@Nullable ServerPlayer player, ChunkLoaderShape newShape, int newRadius) {
        if (owner == null || newRadius < 0 || newRadius > 255) return false;
        // Is the player authenticated to make this change.
        if (player != null && (!owner.equals(player.getUUID()) || !ChickenChunksConfig.doesBypassLoaderAccess(player))) return false;

        assert level != null;

        if (level.isClientSide()) {
            radius = newRadius;
            shape = newShape;
            return true;
        }

        Set<ChunkPos> chunks = getContainedChunks(newShape, getBlockPos().getX(), getBlockPos().getZ(), newRadius);
        ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getRestrictions(owner);
        if (chunks.size() > restrictions.getChunksPerLoader()) {
            if (player != null) {
                int more = chunks.size() - restrictions.getChunksPerLoader();
                ChickenChunksNetwork.sendAddGuiWarning(player, Component.translatable("chickenchunks.gui.morechunks", more));
            }
            return false;
        }
        if (powered) {
            radius = newRadius;
            shape = newShape;
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
            ChickenChunksNetwork.sendTileUpdate(this);
            return true;
        }
        IChunkLoaderHandler handler = IChunkLoaderHandler.instance();
        if (handler.canLoadChunks(this, chunks)) {
            radius = newRadius;
            shape = newShape;
            handler.updateLoader(this);
            BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
            ChickenChunksNetwork.sendTileUpdate(this);
            return true;
        }
        return false;
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf packet) {
        super.writeToPacket(packet);
        packet.writeEnum(shape);
        packet.writeShort(radius);
    }

    @Override
    public void readFromPacket(RegistryFriendlyByteBuf packet) {
        super.readFromPacket(packet);
        setShapeAndRadius(packet.readEnum(ChunkLoaderShape.class), packet.readShort());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("radius", (byte) radius);
        output.putByte("shape", (byte) shape.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        radius = input.getByteOr("radius", (byte) 0);
        shape = ChunkLoaderShape.values()[input.getByteOr("shape", (byte) 0)];
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
