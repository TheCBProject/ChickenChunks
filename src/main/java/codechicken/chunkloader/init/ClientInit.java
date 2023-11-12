package codechicken.chunkloader.init;

import codechicken.chunkloader.client.ChunkLoaderItemModel;
import codechicken.chunkloader.client.TileChunkLoaderRenderer;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientInit {

    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    public static void init() {
        LOCK.lock();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(ClientInit::onRegisterRenderers);
        bus.addListener(ClientInit::onRegisterGeometryLoaders);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityRenderers.register(ChickenChunksModContent.CHUNK_LOADER_TILE.get(), TileChunkLoaderRenderer::new);
        BlockEntityRenderers.register(ChickenChunksModContent.SPOT_LOADER_TILE.get(), TileChunkLoaderRenderer::new);
    }

    private static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("chunk_loader", new ChunkLoaderItemModel());

    }
}
