package codechicken.chunkloader.init;

import codechicken.chunkloader.client.ChunkLoaderPearlItemRenderer;
import codechicken.chunkloader.client.TileChunkLoaderRenderer;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;

public class ClientInit {

    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    public static void init(IEventBus modBus) {
        LOCK.lock();
        modBus.addListener(ClientInit::onRegisterRenderers);
        modBus.addListener(ClientInit::onRegisterSpecialModelRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityRenderers.register(ChickenChunksModContent.CHUNK_LOADER_TILE.get(), TileChunkLoaderRenderer::new);
        BlockEntityRenderers.register(ChickenChunksModContent.SPOT_LOADER_TILE.get(), TileChunkLoaderRenderer::new);
    }

    private static void onRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(MOD_ID, "pearl"), ChunkLoaderPearlItemRenderer.Unbaked.MAP_CODEC);
    }
}
