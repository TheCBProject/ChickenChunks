package codechicken.chunkloader.proxy;

import codechicken.chunkloader.ChunkLoaderCPH;
import codechicken.chunkloader.GuiChunkLoader;
import codechicken.chunkloader.TileChunkLoader;
import codechicken.chunkloader.TileSpotLoader;
import codechicken.chunkloader.client.TileChunkLoaderRenderer;
import codechicken.chunkloader.proxy.ChunkLoaderProxy;
import codechicken.core.ClientUtils;
import net.minecraft.client.Minecraft;
import codechicken.core.CCUpdateChecker;
import codechicken.lib.packet.PacketCustom;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import static codechicken.chunkloader.ChickenChunks.*;

public class ChunkLoaderClientProxy  extends ChunkLoaderProxy
{
    @Override
    public void init()
    {
        if(config.getTag("checkUpdates").getBooleanValue(true))
            CCUpdateChecker.updateCheck("ChickenChunks");
        ClientUtils.enhanceSupportersList("ChickenChunks");
        
        super.init();

        PacketCustom.assignHandler(ChunkLoaderCPH.channel, new ChunkLoaderCPH());
        
        ClientRegistry.bindTileEntitySpecialRenderer(TileChunkLoader.class, new TileChunkLoaderRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSpotLoader.class, new TileChunkLoaderRenderer());
        //RenderingRegistry.registerBlockHandler(new ChunkLoaderSBRH());
    }
    
    @Override
    public void openGui(TileChunkLoader tile)
    {
        Minecraft.getMinecraft().displayGuiScreen(new GuiChunkLoader(tile));
    }
}
