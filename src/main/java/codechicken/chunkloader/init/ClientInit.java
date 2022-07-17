package codechicken.chunkloader.init;

import codechicken.chunkloader.client.ChunkLoaderItemModel;
import codechicken.chunkloader.client.TileChunkLoaderRenderer;
import codechicken.chunkloader.init.ChickenChunksModContent;
import codechicken.lib.model.ModelRegistryHelper;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static codechicken.chunkloader.init.ChickenChunksModContent.*;

public class ClientInit {

    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    private static final ModelRegistryHelper modelHelper = new ModelRegistryHelper();

    public static void init() {
        LOCK.lock();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(ClientInit::onRegisterRenderers);

        //Pull our block models, and wrap them with a ChunkLoaderItemModel as our item model.
        modelHelper.registerCallback(e -> {
            BakedModel loaderModel = e.getModels().get(new ModelResourceLocation(CHUNK_LOADER_BLOCK.getId(), ""));
            BakedModel spotModel = e.getModels().get(new ModelResourceLocation(SPOT_LOADER_BLOCK.getId(), ""));
            e.getModels().put(new ModelResourceLocation(CHUNK_LOADER_ITEM.getId(), "inventory"), new ChunkLoaderItemModel(loaderModel, false));
            e.getModels().put(new ModelResourceLocation(SPOT_LOADER_TILE.getId(), "inventory"), new ChunkLoaderItemModel(spotModel, true));
        });
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityRenderers.register(ChickenChunksModContent.CHUNK_LOADER_TILE.get(), TileChunkLoaderRenderer::new);
        BlockEntityRenderers.register(ChickenChunksModContent.SPOT_LOADER_TILE.get(), TileChunkLoaderRenderer::new);
    }
}
