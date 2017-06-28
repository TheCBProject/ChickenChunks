package codechicken.chunkloader.handler;

import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.chunkloader.manager.PlayerChunkViewerManager;
import codechicken.chunkloader.manager.PlayerChunkViewerManager.DimensionChange;
import codechicken.chunkloader.manager.PlayerChunkViewerManager.TicketChange;
import codechicken.lib.util.ServerUtils;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager.ForceChunkEvent;
import net.minecraftforge.common.ForgeChunkManager.UnforceChunkEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

public class ChunkLoaderEventHandler {

    @SubscribeEvent
    public void serverTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            PlayerChunkViewerManager.instance().update();
        }
    }

    @SubscribeEvent
    public void worldTick(WorldTickEvent event) {
        if (event.phase == Phase.END && !event.world.isRemote) {
            ChunkLoaderManager.onTickEnd((WorldServer) event.world);
            PlayerChunkViewerManager.instance().calculateChunkChanges((WorldServer) event.world);
        }
    }

    @SubscribeEvent
    public void playerLogout(PlayerLoggedOutEvent event) {
        PlayerChunkViewerManager.instance().logouts.add(event.player.getName());
    }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        ChunkLoaderManager.load((WorldServer) event.getWorld());
    }

    @SubscribeEvent
    public void onWorldLoad(Load event) {
        if (!event.getWorld().isRemote) {
            ChunkLoaderManager.load((WorldServer) event.getWorld());
            ChunkLoaderManager.loadWorld((WorldServer) event.getWorld());
            PlayerChunkViewerManager.instance().dimChanges.add(new DimensionChange((WorldServer) event.getWorld(), true));
        }
    }

    @SubscribeEvent
    public void onWorldUnload(Unload event) {
        if (!event.getWorld().isRemote) {
            if (ServerUtils.mc().isServerRunning()) {
                ChunkLoaderManager.onWorldUnload(event.getWorld());
                PlayerChunkViewerManager.instance().dimChanges.add(new DimensionChange((WorldServer) event.getWorld(), false));
            } else {
                PlayerChunkViewerManager.onServerShutdown();
                ChunkLoaderManager.onServerShutdown();
            }
        }
    }

    @SubscribeEvent
    public void onChunkForce(ForceChunkEvent event) {
        PlayerChunkViewerManager.instance().ticketChanges.add(new TicketChange(event.getTicket(), event.getLocation(), true));
    }

    @SubscribeEvent
    public void onChunkUnForce(UnforceChunkEvent event) {
        PlayerChunkViewerManager.instance().ticketChanges.add(new TicketChange(event.getTicket(), event.getLocation(), false));
    }
}
