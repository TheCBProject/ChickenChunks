package codechicken.chunkloader.api;

import java.util.Collection;

import codechicken.lib.vec.BlockCoord;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public interface IChickenChunkLoader
{
    String getOwner();
    Object getMod();
    World getWorld();
    BlockCoord getPosition();
    void deactivate();
    void activate();
    Collection<ChunkCoordIntPair> getChunks();
}
