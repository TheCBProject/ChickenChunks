package codechicken.chunkloader.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Set;

public interface IChickenChunkLoader {

    String getOwner();

    Object getMod();

    World getLoaderWorld();

    BlockPos getPosition();

    void deactivate();

    void activate();

    Set<ChunkPos> getChunks();
}
