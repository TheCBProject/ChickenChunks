package codechicken.chunkloader.network;

import codechicken.lib.packet.PacketCustomChannelBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.event.EventNetworkChannel;

/**
 * Created by covers1624 on 1/11/19.
 */
public class ChickenChunksNetwork {

    public static final ResourceLocation NET_CHANNEL = new ResourceLocation("chickenchunks:network");
    public static EventNetworkChannel netChannel;

    //Client handled.
    public static final int C_OPEN_LOADER_GUI = 1;

    //Server handled.
    public static final int S_SET_SHAPE = 1;

    public static void init() {
        netChannel = PacketCustomChannelBuilder.named(NET_CHANNEL)//
                .assignClientHandler(() -> ChunkLoaderCPH::new)//
                .assignServerHandler(() -> ChunkLoaderSPH::new)//
                .build();
    }

}
