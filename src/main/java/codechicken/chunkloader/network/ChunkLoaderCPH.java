package codechicken.chunkloader.network;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.gui.GuiChunkLoader;
import codechicken.chunkloader.gui.PlayerChunkViewer;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.ICustomPacketHandler.IClientPacketHandler;
import codechicken.lib.util.CommonUtils;
import codechicken.lib.vec.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.ChunkPos;

public class ChunkLoaderCPH implements IClientPacketHandler {
    public static String channel = "ChickenChunks";

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc, INetHandlerPlayClient handler) {
        switch (packet.getType()) {
        case 1:
            PlayerChunkViewer.openViewer((int) mc.player.posX, (int) mc.player.posZ, CommonUtils.getDimension(mc.world));
            break;
        case 2:
            PlayerChunkViewer.instance().loadDimension(packet, mc.world);
            break;
        case 3:
            PlayerChunkViewer.instance().unloadDimension(packet.readInt());
            break;
        case 4:
            PlayerChunkViewer.instance().handleChunkChange(packet.readInt(), new ChunkPos(packet.readInt(), packet.readInt()), packet.readBoolean());
            break;
        case 5:
            PlayerChunkViewer.instance().handleTicketChange(packet.readInt(), packet.readInt(), new ChunkPos(packet.readInt(), packet.readInt()), packet.readBoolean());
            break;
        case 6:
            PlayerChunkViewer.instance().handlePlayerUpdate(packet.readString(), packet.readInt(), new Vector3(packet.readFloat(), packet.readFloat(), packet.readFloat()));
            break;
        case 7:
            PlayerChunkViewer.instance().removePlayer(packet.readString());
            break;
        case 8:
            PlayerChunkViewer.instance().handleNewTicket(packet, mc.world);
            break;
        //case 10:
        //    TileChunkLoader.handleDescriptionPacket(packet, mc.theWorld);
        //    break;
        //case 11:
        //    TileSpotLoader.handleDescriptionPacket(packet, mc.theWorld);
        //    break;
        case 12:
            TileEntity tile = mc.world.getTileEntity(packet.readPos());
            if (tile instanceof TileChunkLoader) {
                mc.displayGuiScreen(new GuiChunkLoader((TileChunkLoader) tile));
            }
            break;

        }
    }

    public static void sendGuiClosing() {
        PacketCustom packet = new PacketCustom(channel, 1);
        packet.sendToServer();
    }

    public static void sendShapeChange(TileChunkLoader tile, ChunkLoaderShape shape, int radius) {
        PacketCustom packet = new PacketCustom(channel, 2);
        packet.writePos(tile.getPos());
        packet.writeByte(shape.ordinal());
        packet.writeByte(radius);
        packet.sendToServer();
    }
}
