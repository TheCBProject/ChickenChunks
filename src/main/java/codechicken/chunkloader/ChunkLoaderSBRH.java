package codechicken.chunkloader;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraft.client.renderer.RenderBlocks;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ChunkLoaderSBRH implements ISimpleBlockRenderingHandler
{
    public static int renderID = RenderingRegistry.getNextAvailableRenderId();
    
    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer)
    {
        if(block != ChickenChunks.blockChunkLoader)
            return;

        ChickenChunks.blockChunkLoader.setBlockBoundsForItemRender(metadata);
        int actualRenderID = renderID;
        renderID = 0;
        renderer.renderBlockAsItem(block, metadata, 1);
        renderID = actualRenderID;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer)
    {
        if(block != ChickenChunks.blockChunkLoader)
            return false;
        
        ChickenChunks.blockChunkLoader.setBlockBoundsBasedOnState(world, x, y, z);
        renderer.renderStandardBlock(block, x, y, z);
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelID)
    {
        return true;
    }

    @Override
    public int getRenderId()
    {
        return renderID;
    }
}
