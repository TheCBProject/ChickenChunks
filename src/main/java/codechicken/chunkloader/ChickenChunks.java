package codechicken.chunkloader;

import java.io.File;

import codechicken.chunkloader.block.BlockChunkLoader;
import codechicken.chunkloader.proxy.ChunkLoaderProxy;
import codechicken.core.launch.CodeChickenCorePlugin;
import codechicken.lib.config.ConfigFile;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = "ChickenChunks", dependencies = "required-after:CodeChickenCore@[" + CodeChickenCorePlugin.version + ",)", acceptedMinecraftVersions = CodeChickenCorePlugin.mcVersion)
public class ChickenChunks
{
    @SidedProxy(clientSide = "codechicken.chunkloader.ChunkLoaderClientProxy", serverSide = "codechicken.chunkloader.ChunkLoaderProxy")
    public static ChunkLoaderProxy proxy;

    public static ConfigFile config;

    public static BlockChunkLoader blockChunkLoader;

    @Mod.Instance(value = "ChickenChunks")
    public static ChickenChunks instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new ConfigFile(new File(event.getModConfigurationDirectory(), "ChickenChunks.cfg"))
                .setComment("ChunkLoader Configuration File\nDeleting any element will restore it to it's default value\nBlock ID's will be automatically generated the first time it's run");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.registerCommands(event);
    }
}
