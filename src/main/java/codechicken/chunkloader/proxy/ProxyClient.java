package codechicken.chunkloader.proxy;

import codechicken.chunkloader.ChickenChunks;
import codechicken.chunkloader.client.TileChunkLoaderRenderer;
import codechicken.chunkloader.gui.GuiChunkLoader;
import codechicken.chunkloader.init.ModBlocks;
import codechicken.chunkloader.network.ChunkLoaderCPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileSpotLoader;
import codechicken.lib.internal.ModDescriptionEnhancer;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class ProxyClient extends Proxy {

    public static boolean lasersRenderHollow;

    @Override
    public void preInit() {
        super.preInit();
        ModBlocks.registerModels();
    }

    @Override
    public void init() {
        lasersRenderHollow = ChickenChunks.config.getTag("lasersRenderHollow").setComment("Sets lasers to render as an outline instead of a solid square.").getBooleanValue(false);
        ModDescriptionEnhancer.enhanceMod(ChickenChunks.MOD_ID);

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
