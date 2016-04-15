package codechicken.chunkloader.network;

import codechicken.chunkloader.api.ChunkLoaderShape;
import codechicken.chunkloader.manager.PlayerChunkViewerManager;
import codechicken.chunkloader.tile.TileChunkLoader;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.packet.PacketCustom.IServerPacketHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ChunkLoaderSPH implements IServerPacketHandler {
    public static String channel = "ChickenChunks";

    @Override
    public void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer handler) {
        switch (packet.getType()) {
        case 1:
            PlayerChunkViewerManager.instance().closeViewer(sender.getName());
            break;
        case 2:
            handleChunkLoaderChangePacket(sender.worldObj, packet);
            break;

        }
    }

    private void handleChunkLoaderChangePacket(World world, PacketCustom packet) {
        TileEntity tile = world.getTileEntity(new BlockPos(packet.readInt(), packet.readInt(), packet.readInt()));
        if (tile instanceof TileChunkLoader) {
            TileChunkLoader ctile = (TileChunkLoader) tile;
            ctile.setShapeAndRadius(ChunkLoaderShape.values()[packet.readUByte()], packet.readUByte());
        }
    }
}
