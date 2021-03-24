package codechicken.chunkloader;

import codechicken.chunkloader.command.ChickenChunksCommand;
import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.chunkloader.proxy.Proxy;
import codechicken.chunkloader.proxy.ProxyClient;
import codechicken.chunkloader.world.ChunkLoaderHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod (ChickenChunks.MOD_ID)
public class ChickenChunks {

    public static Logger logger = LogManager.getLogger("ChickenChunks");

    public static final String MOD_ID = "chickenchunks";

    public static Proxy proxy;

    public ChickenChunks() {
        proxy = DistExecutor.safeRunForDist(() -> ProxyClient::new, () -> Proxy::new);
        ChickenChunksConfig.load();
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        ChickenChunksCommand.init();
        ChickenChunksNetwork.init();
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        proxy.commonSetup(event);
        ChunkLoaderHandler.init();
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        proxy.clientSetup(event);
    }

    @SubscribeEvent
    public void onServerSetup(FMLDedicatedServerSetupEvent event) {
    }

    private static String modifyDesc(String desc) {
        desc += "\n";
        desc += "    Credits: Sanguine - Texture\n";
        return desc;
    }
}
