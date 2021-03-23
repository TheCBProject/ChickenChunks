package codechicken.chunkloader.network;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.gui.GuiChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.packet.ICustomPacketHandler.IClientPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.tileentity.TileEntity;

import static codechicken.chunkloader.network.ChickenChunksNetwork.C_OPEN_LOADER_GUI;
import static codechicken.chunkloader.network.ChickenChunksNetwork.S_SET_SHAPE;

public class ChunkLoaderCPH implements IClientPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc, IClientPlayNetHandler handler) {
        switch (packet.getType()) {
            case C_OPEN_LOADER_GUI:
                TileEntity tile = mc.level.getBlockEntity(packet.readPos());
                if (tile instanceof TileChunkLoader) {
                    mc.setScreen(new GuiChunkLoader((TileChunkLoader) tile));
                }
                break;

        }
    }

    public static void sendShapeChange(TileChunkLoader tile, ChunkLoaderShape shape, int radius) {
        PacketCustom packet = new PacketCustom(ChickenChunksNetwork.NET_CHANNEL, S_SET_SHAPE);
        packet.writePos(tile.getBlockPos());
        packet.writeByte(shape.ordinal());
        packet.writeByte(radius);
        packet.sendToServer();
    }
}
