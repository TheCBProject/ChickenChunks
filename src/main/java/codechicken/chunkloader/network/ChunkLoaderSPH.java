package codechicken.chunkloader.network;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.lib.packet.ICustomPacketHandler.IServerPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;

import static codechicken.chunkloader.network.ChickenChunksNetwork.*;

public class ChunkLoaderSPH implements IServerPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, ServerPlayer sender) {
        switch (packet.getType()) {
            case S_SET_SHAPE: {
                handleChunkLoaderChangePacket(sender.level(), sender, packet);
                break;
            }
        }
    }

    private void handleChunkLoaderChangePacket(Level world, ServerPlayer sender, PacketCustom packet) {
        if (world.getBlockEntity(packet.readPos()) instanceof TileChunkLoader tile) {
            tile.setShapeAndRadius(sender, packet.readEnum(ChunkLoaderShape.class), packet.readShort());
        }
    }

    public static void sendStateUpdate(TileChunkLoaderBase tile) {
        if (tile.world().isClientSide) return;
        PacketCustom packet = new PacketCustom(NET_CHANNEL, C_UPDATE_STATE, tile.getLevel().registryAccess());
        packet.writePos(tile.getBlockPos());
        tile.writeToPacket(packet);
        packet.sendToChunk(tile);
    }

    public static void sendGuiWarning(ServerPlayer player, Component component) {
        PacketCustom packet = new PacketCustom(NET_CHANNEL, C_ADD_GUI_WARNING, player.registryAccess());
        packet.writeTextComponent(component);
        packet.sendToPlayer(player);
    }
}
