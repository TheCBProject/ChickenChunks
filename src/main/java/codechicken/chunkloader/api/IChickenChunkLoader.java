package codechicken.chunkloader.api;

import java.util.Collection;

import codechicken.lib.vec.BlockCoord;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public abstract interface IChickenChunkLoader
{
    public String getOwner();
    public Object getMod();
    public World getWorld();
    public BlockCoord getPosition();
    public void deactivate();
    public void activate();
    public Collection<ChunkCoordIntPair> getChunks();
}
