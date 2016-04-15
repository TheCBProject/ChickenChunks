package codechicken.chunkloader.api;

import codechicken.lib.vec.BlockCoord;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import java.util.Collection;

public interface IChickenChunkLoader {
    String getOwner();
    Object getMod();
    World getWorld();
    BlockCoord getPosition();
    void deactivate();
    void activate();
    Collection<ChunkCoordIntPair> getChunks();
}
