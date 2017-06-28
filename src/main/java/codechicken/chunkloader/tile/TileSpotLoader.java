package codechicken.chunkloader.tile;

import com.google.common.collect.Sets;
import net.minecraft.util.math.ChunkPos;

import java.util.Set;

public class TileSpotLoader extends TileChunkLoaderBase {

    @Override
    public Set<ChunkPos> getChunks() {
        return Sets.newHashSet(getChunkPosition());
    }

}
