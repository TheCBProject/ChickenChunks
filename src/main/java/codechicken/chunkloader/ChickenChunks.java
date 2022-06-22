package codechicken.chunkloader;

import codechicken.chunkloader.command.ChickenChunksCommand;
import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.init.ChickenChunksModContent;
import codechicken.chunkloader.init.ClientInit;
import codechicken.chunkloader.init.DataGenerators;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.chunkloader.world.ChunkLoaderHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod (ChickenChunks.MOD_ID)
public class ChickenChunks {

    public static Logger logger = LogManager.getLogger("ChickenChunks");

    public static final String MOD_ID = "chickenchunks";

    public ChickenChunks() {
        ChickenChunksConfig.load();

        ChickenChunksModContent.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientInit::init);

        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        ChickenChunksCommand.init();
        ChickenChunksNetwork.init();

        ChunkLoaderHandler.init();

        DataGenerators.init();
    }
}
