package codechicken.chunkloader.network;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.packet.ICustomPacketHandler.IServerPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.IServerPlayNetHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import static codechicken.chunkloader.network.ChickenChunksNetwork.S_SET_SHAPE;

public class ChunkLoaderSPH implements IServerPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, ServerPlayerEntity sender, IServerPlayNetHandler handler) {
        switch (packet.getType()) {
            case S_SET_SHAPE:
                handleChunkLoaderChangePacket(sender.level, packet);
                break;

        }
    }

    private void handleChunkLoaderChangePacket(World world, PacketCustom packet) {
        TileEntity tile = world.getBlockEntity(packet.readPos());
        if (tile instanceof TileChunkLoader) {
            TileChunkLoader ctile = (TileChunkLoader) tile;
            ctile.setShapeAndRadius(ChunkLoaderShape.values()[packet.readUByte()], packet.readUByte());
        }
    }
}
