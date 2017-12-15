package codechicken.chunkloader;

import codechicken.chunkloader.proxy.Proxy;
import codechicken.lib.CodeChickenLib;
import codechicken.lib.config.ConfigFile;
import codechicken.lib.internal.ModDescriptionEnhancer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static codechicken.lib.CodeChickenLib.MC_VERSION;

@Mod (modid = ChickenChunks.MOD_ID, name = ChickenChunks.MOD_NAME, dependencies = "required-after:codechickenlib@[" + CodeChickenLib.MOD_VERSION + ",)", acceptedMinecraftVersions = CodeChickenLib.MC_VERSION_DEP, certificateFingerprint = "f1850c39b2516232a2108a7bd84d1cb5df93b261", updateJSON = ChickenChunks.UPDATE_URL)
public class ChickenChunks {

    public static final String MOD_ID = "chickenchunks";
    public static final String MOD_NAME = "ChickenChunks";
    static final String UPDATE_URL = "http://chickenbones.net/Files/notification/version.php?query=forge&version=" + MC_VERSION + "&file=ChickenChunks";

    @SidedProxy (clientSide = "codechicken.chunkloader.proxy.ProxyClient", serverSide = "codechicken.chunkloader.proxy.Proxy")
    public static Proxy proxy;

    public static ConfigFile config;

    @Mod.Instance (value = MOD_ID)
    public static ChickenChunks instance;

    public static Logger logger = LogManager.getLogger("ChickenChunks");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new ConfigFile(new File(event.getModConfigurationDirectory(), "ChickenChunks.cfg")).setComment("ChunkLoader Configuration File\nDeleting any element will restore it to it's default value");
        proxy.preInit();
        ModMetadata metadata = event.getModMetadata();
        metadata.description = modifyDesc(metadata.description);
        ModDescriptionEnhancer.registerEnhancement(MOD_ID, MOD_NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.registerCommands(event);
    }

    private static String modifyDesc(String desc) {
        desc += "\n";
        desc += "    Credits: Sanguine - Texture\n";
        return desc;
    }
}
