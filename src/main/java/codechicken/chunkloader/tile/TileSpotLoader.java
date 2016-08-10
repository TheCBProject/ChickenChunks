package codechicken.chunkloader.tile;

import net.minecraft.util.math.ChunkPos;

import java.util.Collection;
import java.util.Collections;

public class TileSpotLoader extends TileChunkLoaderBase {
    @Override
    public Collection<ChunkPos> getChunks() {
        return Collections.singletonList(getChunkPosition());
    }

}
