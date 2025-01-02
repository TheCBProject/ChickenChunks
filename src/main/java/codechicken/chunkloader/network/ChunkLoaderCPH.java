package codechicken.chunkloader.network;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.gui.GuiChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.lib.packet.ICustomPacketHandler.IClientPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.client.Minecraft;

import static codechicken.chunkloader.network.ChickenChunksNetwork.*;
import static java.util.Objects.requireNonNull;

public class ChunkLoaderCPH implements IClientPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc) {
        switch (packet.getType()) {
            case C_UPDATE_STATE -> {
                if (requireNonNull(mc.level).getBlockEntity(packet.readPos()) instanceof TileChunkLoaderBase chunkLoader) {
                    chunkLoader.readFromPacket(packet);
                }
                break;
            }
            case C_OPEN_LOADER_GUI -> {
                if (requireNonNull(mc.level).getBlockEntity(packet.readPos()) instanceof TileChunkLoader tile) {
                    mc.setScreen(new GuiChunkLoader(tile));
                }
                break;
            }
            case C_ADD_GUI_WARNING -> {
                if (mc.screen instanceof GuiChunkLoader) {
                    ((GuiChunkLoader) mc.screen).addWarning(packet.readTextComponent());
                }
                break;
            }
        }
    }

    public static void sendShapeChange(TileChunkLoader tile, ChunkLoaderShape shape, int radius) {
        if (radius < 0 || radius > 255) return;

        PacketCustom packet = new PacketCustom(ChickenChunksNetwork.NET_CHANNEL, S_SET_SHAPE, tile.getLevel().registryAccess());
        packet.writePos(tile.getBlockPos());
        packet.writeEnum(shape);
        packet.writeShort(radius);
        packet.sendToServer();
    }
}
