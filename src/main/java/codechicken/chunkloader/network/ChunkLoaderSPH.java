package codechicken.chunkloader.network;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.chunkloader.tile.TileChunkLoaderBase;
import codechicken.lib.packet.ICustomPacketHandler.IServerPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.IServerPlayNetHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import static codechicken.chunkloader.network.ChickenChunksNetwork.*;

public class ChunkLoaderSPH implements IServerPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, ServerPlayerEntity sender, IServerPlayNetHandler handler) {
        switch (packet.getType()) {
            case S_SET_SHAPE: {
                handleChunkLoaderChangePacket(sender.level, sender, packet);
                break;
            }
        }
    }

    private void handleChunkLoaderChangePacket(World world, ServerPlayerEntity sender, PacketCustom packet) {
        TileEntity tile = world.getBlockEntity(packet.readPos());
        if (tile instanceof TileChunkLoader) {
            TileChunkLoader ctile = (TileChunkLoader) tile;
            ctile.setShapeAndRadius(sender, packet.readEnum(ChunkLoaderShape.class), packet.readShort());
        }
    }

    public static void sendStateUpdate(TileChunkLoaderBase tile) {
        if (tile.world().isClientSide) return;
        PacketCustom packet = new PacketCustom(NET_CHANNEL, C_UPDATE_STATE);
        packet.writePos(tile.getBlockPos());
        tile.writeToPacket(packet);
        packet.sendToChunk(tile);
    }

    public static void sendGuiWarning(ServerPlayerEntity player, ITextComponent component) {
        PacketCustom packet = new PacketCustom(NET_CHANNEL, C_ADD_GUI_WARNING);
        packet.writeTextComponent(component);
        packet.sendToPlayer(player);
    }
}
