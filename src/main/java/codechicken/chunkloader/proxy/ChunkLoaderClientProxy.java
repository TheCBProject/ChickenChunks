package codechicken.chunkloader.proxy;

import codechicken.chunkloader.client.TileChunkLoaderRenderer;
import codechicken.chunkloader.gui.GuiChunkLoader;
import codechicken.chunkloader.init.ModBlocks;
import codechicken.chunkloader.network.ChunkLoaderCPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileSpotLoader;
import codechicken.core.CCUpdateChecker;
import codechicken.core.ModDescriptionEnhancer;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import static codechicken.chunkloader.ChickenChunks.config;

public class ChunkLoaderClientProxy extends ChunkLoaderProxy {

    @Override
    public void preInit() {
        super.preInit();
        ModBlocks.initModelVariants();
    }

    @Override
    public void init() {
        if (config.getTag("checkUpdates").getBooleanValue(true)) {
            CCUpdateChecker.updateCheck("ChickenChunks");
        }
        ModDescriptionEnhancer.enhanceMod("ChickenChunks");

        super.init();

        PacketCustom.assignHandler(ChunkLoaderCPH.channel, new ChunkLoaderCPH());

        ClientRegistry.bindTileEntitySpecialRenderer(TileChunkLoader.class, new TileChunkLoaderRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileSpotLoader.class, new TileChunkLoaderRenderer());
    }

    @Override
    public void openGui(TileChunkLoader tile) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiChunkLoader(tile));
    }
}
