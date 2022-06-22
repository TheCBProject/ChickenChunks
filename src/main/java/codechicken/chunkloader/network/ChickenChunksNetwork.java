package codechicken.chunkloader.network;

import codechicken.lib.packet.PacketCustomChannelBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.event.EventNetworkChannel;

/**
 * Created by covers1624 on 1/11/19.
 */
public class ChickenChunksNetwork {

    public static final ResourceLocation NET_CHANNEL = new ResourceLocation("chickenchunks:network");
    public static EventNetworkChannel netChannel;

    //Client handled.
    public static final int C_OPEN_LOADER_GUI = 1;
    public static final int C_UPDATE_STATE = 2;
    public static final int C_ADD_GUI_WARNING = 3;

    //Server handled.
    public static final int S_SET_SHAPE = 1;

    public static void init() {
        netChannel = PacketCustomChannelBuilder.named(NET_CHANNEL)
                .assignClientHandler(() -> ChunkLoaderCPH::new)
                .assignServerHandler(() -> ChunkLoaderSPH::new)
                .build();
    }

}
