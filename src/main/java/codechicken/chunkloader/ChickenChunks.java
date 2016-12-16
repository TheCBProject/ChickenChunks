package codechicken.chunkloader;

import codechicken.chunkloader.proxy.ChunkLoaderProxy;
import codechicken.core.launch.CodeChickenCorePlugin;
import codechicken.lib.CodeChickenLib;
import codechicken.lib.config.ConfigFile;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.io.File;

@Mod(modid = "ChickenChunks", name = "ChickenChunks", dependencies = "required-after:CodeChickenCore@[" + CodeChickenCorePlugin.version + ",)", acceptedMinecraftVersions = CodeChickenLib.mcVersion, certificateFingerprint = "f1850c39b2516232a2108a7bd84d1cb5df93b261")
public class ChickenChunks {
    @SidedProxy(clientSide = "codechicken.chunkloader.proxy.ChunkLoaderClientProxy", serverSide = "codechicken.chunkloader.proxy.ChunkLoaderProxy")
    public static ChunkLoaderProxy proxy;

    public static ConfigFile config;

    @Mod.Instance(value = "ChickenChunks")
    public static ChickenChunks instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FingerprintChecker.runFingerprintChecks();
        config = new ConfigFile(new File(event.getModConfigurationDirectory(), "ChickenChunks.cfg")).setComment("ChunkLoader Configuration File\nDeleting any element will restore it to it's default value");
        proxy.preInit();
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
