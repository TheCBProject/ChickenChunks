package codechicken.chunkloader.proxy;

import codechicken.chunkloader.command.CommandChunkLoaders;
import codechicken.chunkloader.handler.ChunkLoaderEventHandler;
import codechicken.chunkloader.init.ModBlocks;
import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.chunkloader.network.ChunkLoaderSPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.packet.PacketCustom;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import static codechicken.chunkloader.ChickenChunks.config;
import static codechicken.chunkloader.ChickenChunks.instance;

public class Proxy {

    public void preInit() {
        ModBlocks.init();
    }

    public void init() {
        PacketCustom.assignHandler(ChunkLoaderSPH.channel, new ChunkLoaderSPH());
        ChunkLoaderManager.initConfig(config);
        MinecraftForge.EVENT_BUS.register(new ChunkLoaderEventHandler());
        ChunkLoaderManager.registerMod(instance);
    }

    public void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandChunkLoaders());
    }

    public void openGui(TileChunkLoader tile) {
    }
}
