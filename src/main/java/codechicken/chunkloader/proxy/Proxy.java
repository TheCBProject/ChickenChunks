package codechicken.chunkloader.proxy;

import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.world.ChunkLoaderHandler;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class Proxy {

    public void commonSetup(FMLCommonSetupEvent event) {
        ChickenChunksNetwork.init();
        ChunkLoaderHandler.init();
        //ChunkLoaderManager.initConfig(ChickenChunks.config);
        //        MinecraftForge.EVENT_BUS.register(new ChunkLoaderEventHandler());
        //        ChunkLoaderManager.registerMod(ChickenChunks.MOD_ID);
    }

    public void clientSetup(FMLClientSetupEvent event) {
    }

    //    public void preInit() {
    //        ModBlocks.init();
    //    }

    //    public void init() {
    //        PacketCustom.assignHandler(ChunkLoaderSPH.channel, new ChunkLoaderSPH());

    //    }

    //    public void registerCommands(FMLServerStartingEvent event) {
    //        event.registerServerCommand(new CommandChunkLoaders());
    //    }

    public void openGui(TileChunkLoader tile) {
    }
}
