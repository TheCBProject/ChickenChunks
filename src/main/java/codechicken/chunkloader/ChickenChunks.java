package codechicken.chunkloader;

import codechicken.chunkloader.proxy.Proxy;
import codechicken.lib.CodeChickenLib;
import codechicken.lib.config.ConfigFile;
import codechicken.lib.internal.MigrationManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import java.io.File;

import static codechicken.lib.CodeChickenLib.MC_VERSION;

@Mod(modid = ChickenChunks.MOD_ID, name = ChickenChunks.MOD_NAME, dependencies = "required-after:codechickenlib@[" + CodeChickenLib.version + ",)", acceptedMinecraftVersions = CodeChickenLib.MC_VERSION_DEP, certificateFingerprint = "f1850c39b2516232a2108a7bd84d1cb5df93b261", updateJSON = ChickenChunks.UPDATE_URL)
public class ChickenChunks {

    public static final String MOD_ID = "chickenchunks";
    public static final String MOD_NAME = "ChickenChunks";
    static final String UPDATE_URL = "http://chickenbones.net/Files/notification/version.php?query=forge&version=" + MC_VERSION + "&file=ChickenChunks";


    @SidedProxy(clientSide = "codechicken.chunkloader.proxy.ProxyClient", serverSide = "codechicken.chunkloader.proxy.Proxy")
    public static Proxy proxy;

    public static ConfigFile config;

    @Mod.Instance(value = MOD_ID)
    public static ChickenChunks instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FingerprintChecker.runFingerprintChecks();
        MigrationManager.registerMigrationHandler("ChickenChunks", "chickenchunks");
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
