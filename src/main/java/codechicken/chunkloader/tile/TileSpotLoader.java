package codechicken.chunkloader.tile;

import codechicken.chunkloader.init.ChickenChunksModContent;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public class TileSpotLoader extends TileChunkLoaderBase {

    public TileSpotLoader(BlockPos pos, BlockState state) {
        super(ChickenChunksModContent.SPOT_LOADER_TILE.get(), pos, state);
    }

    @Override
    public Set<ChunkPos> getChunks() {
        return Sets.newHashSet(getChunkPosition());
    }
}
