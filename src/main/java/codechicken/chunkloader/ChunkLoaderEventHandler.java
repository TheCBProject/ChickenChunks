package codechicken.chunkloader;

import codechicken.chunkloader.PlayerChunkViewerManager.DimensionChange;
import codechicken.chunkloader.PlayerChunkViewerManager.TicketChange;
import codechicken.core.ServerUtils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager.ForceChunkEvent;
import net.minecraftforge.common.ForgeChunkManager.UnforceChunkEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Save;
import net.minecraftforge.event.world.WorldEvent.Unload;

public class ChunkLoaderEventHandler
{
    @SubscribeEvent
    public void serverTick(ServerTickEvent event) {
        if (event.phase == Phase.END)
            PlayerChunkViewerManager.instance().update();
    }

    @SubscribeEvent
    public void worldTick(WorldTickEvent event) {
        if (event.phase == Phase.END && !event.world.isRemote) {
            ChunkLoaderManager.tickEnd((WorldServer) event.world);
            PlayerChunkViewerManager.instance().calculateChunkChanges((WorldServer) event.world);
        }
    }

    @SubscribeEvent
    public void playerLogin(PlayerLoggedInEvent event) {
        ChunkLoaderManager.playerLogin(event.player.getCommandSenderName());
    }

    @SubscribeEvent
    public void playerLogout(PlayerLoggedOutEvent event) {
        PlayerChunkViewerManager.instance().logouts.add(event.player.getCommandSenderName());
        ChunkLoaderManager.playerLogout(event.player.getCommandSenderName());
    }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        ChunkLoaderManager.load((WorldServer) event.world);
    }

    @SubscribeEvent
    public void onWorldLoad(Load event) {
        if (!event.world.isRemote) {
            ChunkLoaderManager.load((WorldServer) event.world);
            ChunkLoaderManager.loadWorld((WorldServer) event.world);
            PlayerChunkViewerManager.instance().dimChanges.add(new DimensionChange((WorldServer) event.world, true));
        }
    }

    @SubscribeEvent
    public void onWorldUnload(Unload event) {
        if (!event.world.isRemote) {
            if (ServerUtils.mc().isServerRunning()) {
                ChunkLoaderManager.unloadWorld(event.world);
                PlayerChunkViewerManager.instance().dimChanges.add(new DimensionChange((WorldServer) event.world, false));
            } else {
                PlayerChunkViewerManager.serverShutdown();
                ChunkLoaderManager.serverShutdown();
            }
        }
    }

    @SubscribeEvent
    public void onWorldSave(Save event) {
        ChunkLoaderManager.save((WorldServer) event.world);
    }

    @SubscribeEvent
    public void onChunkForce(ForceChunkEvent event) {
        PlayerChunkViewerManager.instance().ticketChanges.add(new TicketChange(event.ticket, event.location, true));
    }

    @SubscribeEvent
    public void onChunkUnForce(UnforceChunkEvent event) {
        PlayerChunkViewerManager.instance().ticketChanges.add(new TicketChange(event.ticket, event.location, false));
    }
}
