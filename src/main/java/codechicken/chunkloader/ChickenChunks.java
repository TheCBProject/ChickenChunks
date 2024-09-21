package codechicken.chunkloader;

import codechicken.chunkloader.command.ChickenChunksCommand;
import codechicken.chunkloader.handler.ChickenChunksConfig;
import codechicken.chunkloader.init.ChickenChunksModContent;
import codechicken.chunkloader.init.ClientInit;
import codechicken.chunkloader.init.DataGenerators;
import codechicken.chunkloader.network.ChickenChunksNetwork;
import codechicken.chunkloader.world.ChunkLoaderHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

@Mod (ChickenChunks.MOD_ID)
public class ChickenChunks {

    public static final String MOD_ID = "chickenchunks";

    private static @Nullable ModContainer container;

    public ChickenChunks(ModContainer container, IEventBus modBus) {
        ChickenChunks.container = container;
        ChickenChunksConfig.load();

        ChickenChunksModContent.init(modBus);

        if (FMLEnvironment.dist.isClient()) {
            ClientInit.init(modBus);
        }

        ChickenChunksCommand.init();
        ChickenChunksNetwork.init(modBus);

        ChunkLoaderHandler.init(modBus);

        DataGenerators.init(modBus);
    }

    public static ModContainer container() {
        return requireNonNull(container);
    }
}
