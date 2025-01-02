package codechicken.chunkloader.network;

import codechicken.chunkloader.ChickenChunks;
import codechicken.lib.packet.PacketCustomChannel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;

/**
 * Created by covers1624 on 1/11/19.
 */
public class ChickenChunksNetwork {

    public static final ResourceLocation NET_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, "network");
    public static final PacketCustomChannel channel = new PacketCustomChannel(NET_CHANNEL)
            .versioned(ChickenChunks.container().getModInfo().getVersion().toString())
            .client(() -> ChunkLoaderCPH::new)
            .server(() -> ChunkLoaderSPH::new);

    //Client handled.
    public static final int C_OPEN_LOADER_GUI = 1;
    public static final int C_UPDATE_STATE = 2;
    public static final int C_ADD_GUI_WARNING = 3;

    //Server handled.
    public static final int S_SET_SHAPE = 1;

    public static void init(IEventBus modBus) {
        channel.init(modBus);
    }
}
