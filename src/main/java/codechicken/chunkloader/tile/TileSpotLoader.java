package codechicken.chunkloader.tile;

import net.minecraft.util.math.ChunkPos;

import java.util.Collection;
import java.util.Collections;

public class TileSpotLoader extends TileChunkLoaderBase {
    @Override
    public Collection<ChunkPos> getChunks() {
        return Collections.singletonList(getChunkPosition());
    }

    //public static HashSet<ChunkPos> getContainedChunks(ChunkLoaderShape shape, int xCoord, int zCoord, int radius) {
    //    return shape.getLoadedChunks(xCoord >> 4, zCoord >> 4, radius - 1);
    //}
}
