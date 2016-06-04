package codechicken.chunkloader.api;

import codechicken.lib.vec.BlockCoord;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Collection;

public interface IChickenChunkLoader {
    String getOwner();

    Object getMod();

    World getLoaderWorld();

    BlockCoord getPosition();

    void deactivate();

    void activate();

    Collection<ChunkPos> getChunks();
}
