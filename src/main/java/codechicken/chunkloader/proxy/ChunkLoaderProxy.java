package codechicken.chunkloader.proxy;

import codechicken.chunkloader.command.CommandChunkLoaders;
import codechicken.chunkloader.command.CommandDebugInfo;
import codechicken.chunkloader.handler.ChunkLoaderEventHandler;
import codechicken.chunkloader.init.ModBlocks;
import codechicken.chunkloader.init.ModRecipes;
import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.chunkloader.network.ChunkLoaderSPH;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.command.CommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import static codechicken.chunkloader.ChickenChunks.config;
import static codechicken.chunkloader.ChickenChunks.instance;

public class ChunkLoaderProxy {

    public void preInit() {
        ModBlocks.init();
    }

    public void init() {
        PacketCustom.assignHandler(ChunkLoaderSPH.channel, new ChunkLoaderSPH());
        ChunkLoaderManager.initConfig(config);
        ModRecipes.init();
        MinecraftForge.EVENT_BUS.register(new ChunkLoaderEventHandler());
        //FMLCommonHandler.instance().bus().register(new ChunkLoaderEventHandler());
        ChunkLoaderManager.registerMod(instance);
    }

    public void registerCommands(FMLServerStartingEvent event) {
        CommandHandler commandManager = (CommandHandler) event.getServer().getCommandManager();
        commandManager.registerCommand(new CommandChunkLoaders());
        commandManager.registerCommand(new CommandDebugInfo());
    }

    public void openGui(TileChunkLoader tile) {
    }
}
