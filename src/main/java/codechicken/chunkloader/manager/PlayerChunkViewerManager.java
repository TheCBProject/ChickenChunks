package codechicken.chunkloader.manager;

import codechicken.core.CommonUtils;
import codechicken.core.ServerUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

import java.util.*;

public class PlayerChunkViewerManager {
    public static class TicketChange {
        public final Ticket ticket;
        public final ChunkCoordIntPair chunk;
        public final boolean force;
        public final int dimension;

        public TicketChange(Ticket ticket, ChunkCoordIntPair chunk, boolean force) {
            this.ticket = ticket;
            this.dimension = CommonUtils.getDimension(ticket.world);
            this.chunk = chunk;
            this.force = force;
        }
    }

    public static class ChunkChange {
        public final ChunkCoordIntPair chunk;
        public final boolean add;
        public final int dimension;

        public ChunkChange(int dimension, ChunkCoordIntPair chunk, boolean add) {
            this.dimension = dimension;
            this.chunk = chunk;
            this.add = add;
        }
    }

    public static class DimensionChange {
        public final WorldServer world;
        public final boolean add;

        public DimensionChange(WorldServer world, boolean add) {
            this.world = world;
            this.add = add;
        }
    }

    public LinkedList<PlayerChunkViewerTracker> playerViewers = new LinkedList<PlayerChunkViewerTracker>();
    public HashMap<Ticket, Integer> ticketIDs = new HashMap<Ticket, Integer>();
    private int ticketID = 0;
    private int time = 0;
    //for tracking chunk changes
    private HashMap<Integer, HashSet<ChunkCoordIntPair>> lastLoadedChunkMap = new HashMap<Integer, HashSet<ChunkCoordIntPair>>();
    //changes to be processed
    public LinkedList<ChunkChange> chunkChanges = new LinkedList<ChunkChange>();
    public LinkedList<TicketChange> ticketChanges = new LinkedList<TicketChange>();
    public LinkedList<DimensionChange> dimChanges = new LinkedList<DimensionChange>();
    public LinkedList<String> logouts = new LinkedList<String>();
    public LinkedList<String> addViewers = new LinkedList<String>();

    private static PlayerChunkViewerManager instance;

    public static PlayerChunkViewerManager instance() {
        if (instance == null) {
            instance = new PlayerChunkViewerManager();
        }
        return instance;
    }

    public void update() {
        time++;
        for (String username : logouts) {
            for (Iterator<PlayerChunkViewerTracker> iterator = playerViewers.iterator(); iterator.hasNext(); ) {
                if (iterator.next().owner.getName().equals(username)) {
                    iterator.remove();
                }
            }
        }

        for (String username : logouts) {
            for (PlayerChunkViewerTracker tracker : playerViewers) {
                tracker.removePlayer(username);
            }
        }

        for (DimensionChange change : dimChanges) {
            if (change.add) {
                for (PlayerChunkViewerTracker tracker : playerViewers) {
                    tracker.loadDimension(change.world);
                }
            }
        }

        for (ChunkChange change : chunkChanges) {
            for (PlayerChunkViewerTracker tracker : playerViewers) {
                tracker.sendChunkChange(change);
            }
        }

        for (TicketChange change : ticketChanges) {
            if (ticketIDs.containsKey(change.ticket)) {
                for (PlayerChunkViewerTracker tracker : playerViewers) {
                    tracker.sendTicketChange(change);
                }
            } else {
                ticketIDs.put(change.ticket, ticketID++);
                for (PlayerChunkViewerTracker tracker : playerViewers) {
                    tracker.addTicket(CommonUtils.getDimension(change.ticket.world), change.ticket);
                }
            }
        }

        for (DimensionChange change : dimChanges) {
            if (!change.add) {
                for (PlayerChunkViewerTracker tracker : playerViewers) {
                    tracker.unloadDimension(CommonUtils.getDimension(change.world));
                }
            }
        }

        if (time % 10 == 0) {
            for (EntityPlayer player : ServerUtils.getPlayers()) {
                for (PlayerChunkViewerTracker tracker : playerViewers) {
                    tracker.updatePlayer(player);
                }
            }
        }

        for (String username : addViewers) {
            EntityPlayer player = ServerUtils.getPlayer(username);
            if (player == null) {
                continue;
            }

            if (playerViewers.isEmpty()) {
                updateChunkChangeMap();
            }

            playerViewers.add(new PlayerChunkViewerTracker(player, this));
        }

        addViewers.clear();
        dimChanges.clear();
        logouts.clear();
        chunkChanges.clear();
        ticketChanges.clear();
    }

    @SuppressWarnings("unchecked")
    private void updateChunkChangeMap() {
        for (WorldServer world : DimensionManager.getWorlds()) {
            HashSet<ChunkCoordIntPair> allChunks = new HashSet<ChunkCoordIntPair>();
            ArrayList<Chunk> loadedChunkCopy = new ArrayList<Chunk>(world.getChunkProvider().loadedChunks);
            for (Chunk chunk : loadedChunkCopy) {
                allChunks.add(chunk.getChunkCoordIntPair());
            }

            lastLoadedChunkMap.put(CommonUtils.getDimension(world), allChunks);
        }
    }

    @SuppressWarnings("unchecked")
    public void calculateChunkChanges(WorldServer world) {
        if (playerViewers.isEmpty()) {
            return;
        }

        int dimension = CommonUtils.getDimension(world);
        HashSet<ChunkCoordIntPair> wasLoadedChunks = lastLoadedChunkMap.get(dimension);
        if (wasLoadedChunks == null) {
            wasLoadedChunks = new HashSet<ChunkCoordIntPair>();
        }

        HashSet<ChunkCoordIntPair> allChunks = new HashSet<ChunkCoordIntPair>();
        ArrayList<Chunk> loadedChunkCopy = new ArrayList<Chunk>(world.getChunkProvider().loadedChunks);
        for (Chunk chunk : loadedChunkCopy) {
            ChunkCoordIntPair coord = chunk.getChunkCoordIntPair();
            allChunks.add(coord);
            if (!wasLoadedChunks.remove(coord)) {
                chunkChanges.add(new ChunkChange(dimension, coord, true));
            }
        }

        for (ChunkCoordIntPair coord : wasLoadedChunks) {
            chunkChanges.add(new ChunkChange(dimension, coord, false));
        }

        lastLoadedChunkMap.put(dimension, allChunks);
    }

    public boolean isViewerOpen(String username) {
        for (PlayerChunkViewerTracker tracker : playerViewers) {
            if (tracker.owner.getName().equals(username)) {
                return true;
            }
        }

        return false;
    }

    public static void serverShutdown() {
        instance = null;
    }

    public void closeViewer(String username) {
        for (Iterator<PlayerChunkViewerTracker> iterator = playerViewers.iterator(); iterator.hasNext(); ) {
            if (iterator.next().owner.getName().equals(username)) {
                iterator.remove();
            }
        }
    }
}
