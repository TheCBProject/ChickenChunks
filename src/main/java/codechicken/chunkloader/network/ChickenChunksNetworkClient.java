package codechicken.chunkloader.network;

import codechicken.chunkloader.gui.GuiChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Created by covers1624 on 11/4/26.
 */
public class ChickenChunksNetworkClient extends ChickenChunksNetwork {

    public static void initClient() {
        CHANNEL.assignClientHandler(OPEN_LOADER_GUI, ChickenChunksNetworkClient::handleOpenLoaderGui);
        CHANNEL.assignClientHandler(TILE_UPDATE, ChickenChunksNetworkClient::handleTileUpdate);
        CHANNEL.assignClientHandler(ADD_GUI_WARNING, ChickenChunksNetworkClient::handleAddGuiWarning);
    }

    private static void handleOpenLoaderGui(RegistryFriendlyByteBuf packet, IPayloadContext ctx) {
        if (ctx.player().level().getBlockEntity(packet.readBlockPos()) instanceof TileChunkLoader tile) {
            Minecraft.getInstance().setScreen(new GuiChunkLoader(tile));
        }
    }

    private static void handleTileUpdate(RegistryFriendlyByteBuf packet, IPayloadContext ctx) {
        if (ctx.player().level().getBlockEntity(packet.readBlockPos()) instanceof TileChunkLoaderBase chunkLoader) {
            chunkLoader.readFromPacket(packet);
        }
    }

    private static void handleAddGuiWarning(RegistryFriendlyByteBuf packet, IPayloadContext ctx) {
        if (Minecraft.getInstance().screen instanceof GuiChunkLoader gui) {
            gui.addWarning(packet.cc$readWithRegistryCodec(ComponentSerialization.STREAM_CODEC));
        }
    }
}
