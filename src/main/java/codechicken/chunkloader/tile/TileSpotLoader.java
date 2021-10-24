package codechicken.chunkloader.tile;

import codechicken.chunkloader.init.ModContent;
import com.google.common.collect.Sets;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.ChunkPos;

import java.util.Set;

public class TileSpotLoader extends TileChunkLoaderBase {

    public TileSpotLoader() {
        super(ModContent.tileSpotLoaderType);
    }

    @Override
    public Set<ChunkPos> getChunks() {
        return Sets.newHashSet(getChunkPosition());
    }
}
