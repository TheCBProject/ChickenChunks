package codechicken.chunkloader.proxy;

import codechicken.chunkloader.client.ChunkLoaderItemModel;
import codechicken.chunkloader.client.TileChunkLoaderRenderer;
import codechicken.chunkloader.gui.GuiChunkLoader;
import codechicken.chunkloader.init.ModContent;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.model.ModelRegistryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static codechicken.chunkloader.init.ModContent.*;

public class ProxyClient extends Proxy {

    private static final ModelRegistryHelper modelHelper = new ModelRegistryHelper();

    @Override
    public void clientSetup(FMLClientSetupEvent event) {
        super.clientSetup(event);
        ClientRegistry.bindTileEntityRenderer(ModContent.tileChunkLoaderType, TileChunkLoaderRenderer::new);
        ClientRegistry.bindTileEntityRenderer(ModContent.tileSpotLoaderType, TileChunkLoaderRenderer::new);

        //Pull our block models, and wrap them with a ChunkLoaderItemModel as our item model.
        modelHelper.registerCallback(e -> {
            IBakedModel loaderModel = e.getModelRegistry().get(new ModelResourceLocation(blockChunkLoader.getRegistryName(), ""));
            IBakedModel spotModel = e.getModelRegistry().get(new ModelResourceLocation(blockSpotLoader.getRegistryName(), ""));
            e.getModelRegistry().put(new ModelResourceLocation(itemChunkLoader.getRegistryName(), "inventory"), new ChunkLoaderItemModel(loaderModel, false));
            e.getModelRegistry().put(new ModelResourceLocation(itemSpotLoader.getRegistryName(), "inventory"), new ChunkLoaderItemModel(spotModel, true));
        });
    }
}
