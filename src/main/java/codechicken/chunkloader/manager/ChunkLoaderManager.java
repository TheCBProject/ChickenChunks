package codechicken.chunkloader.manager;

import codechicken.chunkloader.ChickenChunks;
import codechicken.chunkloader.api.IChickenChunkLoader;
import codechicken.chunkloader.manager.OrganiserStorage.IOrganiserStorage;
import codechicken.chunkloader.manager.PlayerLoginTracker.ILoginTracker;
import codechicken.lib.config.ConfigFile;
import codechicken.lib.config.ConfigTag;
import codechicken.lib.util.CommonUtils;
import codechicken.lib.util.ServerUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.OrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.PlayerOrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static codechicken.chunkloader.ChickenChunks.instance;
import static codechicken.chunkloader.ChickenChunks.logger;

public class ChunkLoaderManager {

    private static class DimChunkCoord {

        public final int dimension;
        public final int chunkX;
        public final int chunkZ;

        public DimChunkCoord(int dim, ChunkPos coord) {
            this(dim, coord.x, coord.z);
        }

        public DimChunkCoord(int dim, int x, int z) {
            dimension = dim;
            chunkX = x;
            chunkZ = z;
        }

        @Override
        public int hashCode() {
            return ((chunkX * 31) + chunkZ) * 31 + dimension;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DimChunkCoord) {
                DimChunkCoord o2 = (DimChunkCoord) o;
                return dimension == o2.dimension && chunkX == o2.chunkX && chunkZ == o2.chunkZ;
            }
            return false;
        }

        public ChunkPos getChunkCoord() {
            return new ChunkPos(chunkX, chunkZ);
        }
    }

    public static abstract class TicketManager {

        public HashMap<Integer, Stack<Ticket>> ticketsWithSpace = new HashMap<>();
        public HashMap<DimChunkCoord, Ticket> heldChunks = new HashMap<>();

        protected void addChunk(DimChunkCoord coord) {
            if (heldChunks.containsKey(coord)) {
                return;
            }

            Stack<Ticket> freeTickets = ticketsWithSpace.computeIfAbsent(coord.dimension, k -> new Stack<>());

            Ticket ticket;
            if (freeTickets.isEmpty()) {
                freeTickets.push(ticket = createTicket(coord.dimension));
            } else {
                ticket = freeTickets.peek();
            }

            ForgeChunkManager.forceChunk(ticket, coord.getChunkCoord());
            heldChunks.put(coord, ticket);
            if (ticket.getChunkList().size() == ticket.getChunkListDepth() && !freeTickets.isEmpty()) {
                freeTickets.pop();
            }
        }

        protected abstract Ticket createTicket(int dimension);

        protected void remChunk(DimChunkCoord coord) {
            Ticket ticket = heldChunks.remove(coord);
            if (ticket == null) {
                return;
            }

            ForgeChunkManager.unforceChunk(ticket, coord.getChunkCoord());

            if (ticket.getChunkList().size() == ticket.getChunkListDepth() - 1) {
                Stack<Ticket> freeTickets = ticketsWithSpace.computeIfAbsent(coord.dimension, k -> new Stack<>());
                freeTickets.push(ticket);
            }
        }

        protected void unloadDimension(int dimension) {
            ticketsWithSpace.remove(dimension);
        }
    }

    private static abstract class ChunkLoaderOrganiser extends TicketManager {

        private HashMap<Integer, Set<BlockPos>> dormantLoaders = new HashMap<>();
        private HashMap<DimChunkCoord, List<IChickenChunkLoader>> forcedChunksByChunk = new HashMap<>();
        private HashMap<IChickenChunkLoader, Set<ChunkPos>> forcedChunksByLoader = new HashMap<>();
        private HashMap<DimChunkCoord, Integer> timedUnloadQueue = new HashMap<>();

        private boolean reviving;
        private boolean dormant = false;

        public boolean canForceNewChunks(int dimension, Collection<ChunkPos> chunks) {
            if (dormant) {
                return true;
            }

            int required = 0;
            for (ChunkPos coord : chunks) {
                List<IChickenChunkLoader> loaders = forcedChunksByChunk.get(new DimChunkCoord(dimension, coord));
                if (loaders == null || loaders.isEmpty()) {
                    required++;
                }
            }
            return canForceNewChunks(required, dimension);
        }

        public final int numLoadedChunks() {
            return forcedChunksByChunk.size();
        }

        public void addChunkLoader(IChickenChunkLoader loader) {
            if (reviving) {
                return;
            }

            int dim = CommonUtils.getDimension(loader.getLoaderWorld());
            if (dormant) {
                Set<BlockPos> coords = dormantLoaders.computeIfAbsent(dim, k -> new HashSet<>());
                coords.add(loader.getPosition());
            } else {
                forcedChunksByLoader.put(loader, new HashSet<>());
                forceChunks(loader, dim, loader.getChunks());
            }
            setDirty();
        }

        public void remChunkLoader(IChickenChunkLoader loader) {
            int dim = CommonUtils.getDimension(loader.getLoaderWorld());
            if (dormant) {
                Set<BlockPos> coords = dormantLoaders.get(dim);
                if (coords != null) {
                    coords.remove(loader.getPosition());
                }
            } else {
                Set<ChunkPos> chunks = forcedChunksByLoader.remove(loader);
                if (chunks == null) {
                    return;
                }
                unforceChunks(loader, dim, chunks, true);
            }
            setDirty();
        }

        private void unforceChunks(IChickenChunkLoader loader, int dim, Collection<ChunkPos> chunks, boolean remLoader) {
            for (ChunkPos coord : chunks) {
                DimChunkCoord dimCoord = new DimChunkCoord(dim, coord);
                List<IChickenChunkLoader> loaders = forcedChunksByChunk.get(dimCoord);
                if (loaders == null || !loaders.remove(loader)) {
                    continue;
                }

                if (loaders.isEmpty()) {
                    forcedChunksByChunk.remove(dimCoord);
                    timedUnloadQueue.put(dimCoord, 100);
                }
            }

            if (!remLoader) {
                forcedChunksByLoader.get(loader).removeAll(chunks);
            }
            setDirty();
        }

        private void forceChunks(IChickenChunkLoader loader, int dim, Collection<ChunkPos> chunks) {
            for (ChunkPos coord : chunks) {
                DimChunkCoord dimCoord = new DimChunkCoord(dim, coord);
                List<IChickenChunkLoader> loaders = forcedChunksByChunk.computeIfAbsent(dimCoord, k -> new LinkedList<>());
                if (loaders.isEmpty()) {
                    timedUnloadQueue.remove(dimCoord);
                    addChunk(dimCoord);
                }

                if (!loaders.contains(loader)) {
                    loaders.add(loader);
                }
            }

            forcedChunksByLoader.get(loader).addAll(chunks);
            setDirty();
        }

        public abstract boolean canForceNewChunks(int newChunks, int dim);

        public abstract void setDirty();

        public void updateChunkLoader(IChickenChunkLoader loader) {
            Set<ChunkPos> loaderChunks = forcedChunksByLoader.get(loader);
            if (loaderChunks == null) {
                addChunkLoader(loader);
                return;
            }
            HashSet<ChunkPos> oldChunks = new HashSet<>(loaderChunks);
            HashSet<ChunkPos> newChunks = new HashSet<>();
            for (ChunkPos chunk : loader.getChunks()) {
                if (!oldChunks.remove(chunk)) {
                    newChunks.add(chunk);
                }
            }

            int dim = CommonUtils.getDimension(loader.getLoaderWorld());
            if (!oldChunks.isEmpty()) {
                unforceChunks(loader, dim, oldChunks, false);
            }
            if (!newChunks.isEmpty()) {
                forceChunks(loader, dim, newChunks);
            }
        }

        public NBTTagCompound saveToNBT(NBTTagCompound tagCompound) {

            NBTTagList dormantLoaders = new NBTTagList();
            for (Entry<Integer, Set<BlockPos>> entry : this.dormantLoaders.entrySet()) {
                NBTTagCompound loader = new NBTTagCompound();
                loader.setInteger("dim", entry.getKey());
                NBTTagList loaders = new NBTTagList();
                for (BlockPos pos : entry.getValue()) {
                    loaders.appendTag(writeBlockPos(pos, new NBTTagCompound()));
                }
                loader.setTag("loaders", loaders);
                dormantLoaders.appendTag(loader);
            }
            tagCompound.setTag("dormantLoaders", dormantLoaders);

            NBTTagList loaders = new NBTTagList();
            for (IChickenChunkLoader loader : forcedChunksByLoader.keySet()) {
                NBTTagCompound loaderTag = new NBTTagCompound();
                loaderTag.setInteger("dim", CommonUtils.getDimension(loader.getLoaderWorld()));
                writeBlockPos(loader.getPosition(), loaderTag);
                loaders.appendTag(loaderTag);
            }
            tagCompound.setTag("loaders", loaders);

            return tagCompound;
        }

        @Deprecated//TODO 1.12 Remove this, as no more LegacyConversion
        public void loadLegacyData(DataInputStream datain) throws IOException {
            int dimensions = datain.readInt();
            for (int i = 0; i < dimensions; i++) {
                int dim = datain.readInt();
                HashSet<BlockPos> coords = new HashSet<>();
                dormantLoaders.put(dim, coords);
                int numCoords = datain.readInt();
                for (int j = 0; j < numCoords; j++) {
                    coords.add(new BlockPos(datain.readInt(), datain.readInt(), datain.readInt()));
                }
            }
            int numLoaders = datain.readInt();
            for (int i = 0; i < numLoaders; i++) {
                int dim = datain.readInt();
                Set<BlockPos> coords = dormantLoaders.computeIfAbsent(dim, k -> new HashSet<>());
                coords.add(new BlockPos(datain.readInt(), datain.readInt(), datain.readInt()));
            }
        }

        public void readFromNBT(NBTTagCompound nbt) {
            NBTTagList dormantLoaders = nbt.getTagList("dormantLoaders", 10);
            for (int i = 0; i < dormantLoaders.tagCount(); i++) {
                NBTTagCompound loader = dormantLoaders.getCompoundTagAt(i);
                int dim = loader.getInteger("dim");
                Set<BlockPos> loaderPositions = this.dormantLoaders.computeIfAbsent(dim, k -> new HashSet<>());
                NBTTagList loaders = loader.getTagList("loaders", 10);
                for (int j = 0; j < loaders.tagCount(); j++) {
                    NBTTagCompound pos = loaders.getCompoundTagAt(j);
                    loaderPositions.add(readPosition(pos));
                }
            }
            NBTTagList loaders = nbt.getTagList("loaders", 10);
            for (int i = 0; i < loaders.tagCount(); i++) {
                NBTTagCompound loader = loaders.getCompoundTagAt(i);
                int dim = loader.getInteger("dim");
                Set<BlockPos> positions = this.dormantLoaders.computeIfAbsent(dim, k -> new HashSet<>());
                positions.add(readPosition(loader));
            }
        }

        public void revive() {
            if (!dormant) {
                return;
            }
            dormant = false;
            for (int dim : dormantLoaders.keySet()) {
                World world = getWorld(dim, reloadDimensions);
                if (world != null) {
                    revive(world);
                }
            }
        }

        public void devive() {
            if (dormant) {
                return;
            }

            for (IChickenChunkLoader loader : new ArrayList<>(forcedChunksByLoader.keySet())) {
                int dim = CommonUtils.getDimension(loader.getLoaderWorld());
                Set<BlockPos> coords = dormantLoaders.computeIfAbsent(dim, k -> new HashSet<>());
                coords.add(loader.getPosition());
                remChunkLoader(loader);
            }

            dormant = true;
        }

        public void revive(World world) {
            Set<BlockPos> coords = dormantLoaders.get(CommonUtils.getDimension(world));
            if (coords == null) {
                return;
            }

            //addChunkLoader will add to the coord set if we are dormant
            ArrayList<BlockPos> verifyCoords = new ArrayList<>(coords);
            coords.clear();

            for (BlockPos coord : verifyCoords) {
                reviving = true;
                TileEntity tile = world.getTileEntity(coord);
                reviving = false;
                if (tile instanceof IChickenChunkLoader) {
                    ChunkLoaderManager.addChunkLoader((IChickenChunkLoader) tile);
                }
            }
        }

        public void setDormant() {
            dormant = true;
        }

        public boolean isDormant() {
            return dormant;
        }

        public void tickDownUnloads() {
            for (Iterator<Entry<DimChunkCoord, Integer>> iterator = timedUnloadQueue.entrySet().iterator(); iterator.hasNext(); ) {
                Entry<DimChunkCoord, Integer> entry = iterator.next();
                int ticks = entry.getValue();
                if (ticks <= 1) {
                    remChunk(entry.getKey());
                    iterator.remove();
                } else {
                    entry.setValue(ticks - 1);
                }
            }
        }
    }

    public static class PlayerOrganiser extends ChunkLoaderOrganiser {

        public static boolean dirty;

        public final String username;

        public PlayerOrganiser(String username) {
            this.username = username;
        }

        public boolean canForceNewChunks(int required, int dim) {
            return required + numLoadedChunks() < getPlayerChunkLimit(username) && required < ForgeChunkManager.ticketCountAvailableFor(username) * ForgeChunkManager.getMaxChunkDepthFor("ChickenChunks");
        }

        @Override
        public Ticket createTicket(int dimension) {
            return ForgeChunkManager.requestPlayerTicket(instance, username, DimensionManager.getWorld(dimension), Type.NORMAL);
        }

        @Override
        public void setDirty() {
            dirty = true;
        }
    }

    public static class ModOrganiser extends ChunkLoaderOrganiser {

        public final Object mod;
        public final ModContainer container;
        public boolean dirty;

        public ModOrganiser(Object mod, ModContainer container) {
            this.mod = mod;
            this.container = container;
        }

        @Override
        public boolean canForceNewChunks(int required, int dim) {
            return required < ForgeChunkManager.ticketCountAvailableFor(mod, DimensionManager.getWorld(dim)) * ForgeChunkManager.getMaxChunkDepthFor(container.getModId());
        }

        @Override
        public void setDirty() {
            dirty = false;
        }

        @Override
        protected Ticket createTicket(int dimension) {
            return ForgeChunkManager.requestTicket(mod, DimensionManager.getWorld(dimension), Type.NORMAL);
        }
    }

    private static class DummyLoadingCallback implements OrderedLoadingCallback, PlayerOrderedLoadingCallback {

        @Override
        public void ticketsLoaded(List<Ticket> tickets, World world) {
        }

        @Override
        public List<Ticket> ticketsLoaded(List<Ticket> tickets, World world, int maxTicketCount) {
            return new LinkedList<>();
        }

        @Override
        public ListMultimap<String, Ticket> playerTicketsLoaded(ListMultimap<String, Ticket> tickets, World world) {
            return LinkedListMultimap.create();
        }
    }

    //TODO, this is a little weird.. but ok.. Maybe a StateChanger.que/process.
    public enum ReviveChange {
        PlayerRevive,
        PlayerDevive,
        ModRevive,
        DimensionRevive;

        public LinkedList<Object> list;

        public static void load() {
            for (ReviveChange change : values()) {
                change.list = new LinkedList<>();
            }
        }
    }

    private static boolean reloadDimensions = false;
    private static boolean opInteract = false;
    private static int maxChunks;
    private static int awayTimeout;
    private static HashMap<Object, ModContainer> mods = new HashMap<>();

    private static boolean loaded = false;

    /**
     * By doing this you are delegating all chunks from your mod to be handled by yours truly.
     */
    public static void registerMod(Object mod) {
        ModContainer container = Loader.instance().getModObjectList().inverse().get(mod);
        if (container == null) {
            throw new NullPointerException("Mod container not found for: " + mod);
        }
        mods.put(mod, container);
        ForgeChunkManager.setForcedChunkLoadingCallback(mod, new DummyLoadingCallback());
    }

    public static ImmutableMap<Object, ModContainer> getHandledMods() {
        return ImmutableMap.copyOf(mods);
    }

    public static void loadWorld(WorldServer world) {
        ReviveChange.DimensionRevive.list.add(world);
    }

    public static World getWorld(int dim, boolean create) {
        if (create) {
            return FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dim);
        }
        return DimensionManager.getWorld(dim);
    }

    public static void load(WorldServer world) {
        if (loaded) {
            return;
        }

        loaded = true;

        ReviveChange.load();

        IOrganiserStorage storage = OrganiserStorage.getStorage(world);
        ILoginTracker tracker = PlayerLoginTracker.getTracker(world);
        //TODO 1.12 Remove this, as no more LegacyConversion
        try {
            File saveDir = new File(DimensionManager.getCurrentSaveRootDirectory(), "chickenchunks");
            if (saveDir.exists()) {
                storage.loadLegacyData(saveDir);
                tracker.loadLegacyData(saveDir);

                File[] list = saveDir.listFiles();
                if (list == null || list.length == 0) {
                    saveDir.delete();
                    logger.info("Old ChickenChunks conversion completed! Removing old folder..");
                } else {
                    logger.warn("After conversion {} files still exist in {}, Wot..", list.length, saveDir.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            logger.warn("Exception thrown whilst converting old ChickenChunks data!", e);
        }
        storage.load(world);
    }

    public static boolean loggedInRecently(World world, String playerName) {
        if (awayTimeout == 0) {
            return true;
        }

        Long lastLogin = PlayerLoginTracker.getTracker(world).getLoginTime(playerName);
        return lastLogin != null && (System.currentTimeMillis() - lastLogin) / 60000L < awayTimeout;

    }

    public static int getPlayerChunkLimit(String username) {
        ConfigTag config = ChickenChunks.config.getTag("players");
        if (config.containsTag(username)) {
            int ret = config.getTag(username).getIntValue(0);
            if (ret != 0) {
                return ret;
            }
        }

        if (ServerUtils.isPlayerOP(username)) {
            int ret = config.getTag("OP").getIntValue(0);
            if (ret != 0) {
                return ret;
            }
        }

        return config.getTag("DEFAULT").getIntValue(5000);
    }

    public static boolean allowOffline(String username) {
        ConfigTag config = ChickenChunks.config.getTag("allowoffline");
        if (config.containsTag(username)) {
            return config.getTag(username).getBooleanValue(true);
        }

        if (ServerUtils.isPlayerOP(username)) {
            return config.getTag("OP").getBooleanValue(true);
        }

        return config.getTag("DEFAULT").getBooleanValue(true);
    }

    public static boolean allowChunkViewer(String username) {
        ConfigTag config = ChickenChunks.config.getTag("allowchunkviewer");
        if (config.containsTag(username)) {
            return config.getTag(username).getBooleanValue(true);
        }

        if (ServerUtils.isPlayerOP(username)) {
            return config.getTag("OP").getBooleanValue(true);
        }

        return config.getTag("DEFAULT").getBooleanValue(true);
    }

    public static void initConfig(ConfigFile config) {
        config.getTag("players").setPosition(0).useBraces().setComment("Per player chunk limiting. Values ignored if 0.:Simply add <username>=<value>");
        config.getTag("players.DEFAULT").setComment("Forge gives everyone 12500 by default").getIntValue(5000);
        config.getTag("players.OP").setComment("For server op's only.").getIntValue(5000);
        config.getTag("allowoffline").setPosition(1).useBraces().setComment("If set to false, players will have to be logged in for their chunkloaders to work.:Simply add <username>=<true|false>");
        config.getTag("allowoffline.DEFAULT").getBooleanValue(true);
        config.getTag("allowoffline.OP").getBooleanValue(true);
        config.getTag("allowchunkviewer").setPosition(2).useBraces().setComment("Set to false to deny a player access to the chunk viewer");
        config.getTag("allowchunkviewer.DEFAULT").getBooleanValue(true);
        config.getTag("allowchunkviewer.OP").getBooleanValue(true);

        reloadDimensions = config.getTag("reload-dimensions").setComment("Set to false to disable the automatic reloading of mystcraft dimensions on server restart").getBooleanValue(true);
        opInteract = config.getTag("op-interact").setComment("Enabling this lets OPs alter other player's chunkloaders. WARNING: If you change a chunkloader, you have no idea what may break/explode by not being chunkloaded.").getBooleanValue(false);
        maxChunks = config.getTag("maxchunks").setComment("The maximum number of chunks per chunkloader").getIntValue(400);
        awayTimeout = config.getTag("awayTimeout").setComment("The number of minutes since last login within which chunks from a player will remain active, 0 for infinite.").getIntValue(0);
    }

    public static void addChunkLoader(IChickenChunkLoader loader) {
        int dim = CommonUtils.getDimension(loader.getLoaderWorld());
        ChunkLoaderOrganiser organiser = getOrganiser(loader);
        if (organiser.canForceNewChunks(dim, loader.getChunks())) {
            organiser.addChunkLoader(loader);
        } else {
            loader.deactivate();
        }
    }

    private static ChunkLoaderOrganiser getOrganiser(IChickenChunkLoader loader) {
        String owner = loader.getOwner();
        World world = loader.getLoaderWorld();
        IOrganiserStorage storage = OrganiserStorage.getStorage(world);
        return owner == null ? storage.getModOrganiser(loader.getMod()) : storage.getPlayerOrganiser(owner);
    }

    public static void remChunkLoader(IChickenChunkLoader loader) {
        getOrganiser(loader).remChunkLoader(loader);
    }

    public static void updateLoader(IChickenChunkLoader loader) {
        getOrganiser(loader).updateChunkLoader(loader);
    }

    /**
     * Checks if a loader can add more chunks.
     *
     * @param loader The loader asking.
     * @param chunks The new chunks.
     * @return If it can add more.
     */
    public static boolean canLoaderAdd(IChickenChunkLoader loader, Collection<ChunkPos> chunks) {
        String owner = loader.getOwner();
        World world = loader.getLoaderWorld();
        int dim = CommonUtils.getDimension(loader.getLoaderWorld());
        return owner != null && OrganiserStorage.getStorage(world).getPlayerOrganiser(owner).canForceNewChunks(dim, chunks);

    }

    public static void onServerShutdown() {
        loaded = false;
    }

    /**
     * Called on tick end.
     *
     * @param world The world.
     */
    public static void onTickEnd(WorldServer world) {
        if (world.getWorldTime() % 1200 == 0) {
            updateLoginTimes(world);
        }

        OrganiserStorage.getStorage(world).tickUnloads();
        processLoaderStates();
    }

    /**
     * Ticks and saves player login times.
     * Adds player Organizers to the unload Que if the logout timer has expired.
     */
    private static void updateLoginTimes(World world) {
        ILoginTracker tracker = PlayerLoginTracker.getTracker(world);
        for (EntityPlayer player : ServerUtils.getPlayers()) {
            tracker.updateLoginTime(player.getName());
        }
        tracker.forceSave(world);

        IOrganiserStorage storage = OrganiserStorage.getStorage(world);
        storage.queDormantUnloads(world);
    }

    /**
     * Called to actually revive and devive loaders.
     */
    private static void processLoaderStates() {
        for (Object organiser : ReviveChange.PlayerRevive.list) {
            ((PlayerOrganiser) organiser).revive();
        }
        ReviveChange.PlayerRevive.list.clear();

        for (Object organiser : ReviveChange.ModRevive.list) {
            ((ModOrganiser) organiser).revive();
        }
        ReviveChange.ModRevive.list.clear();

        for (Object worldObject : ReviveChange.DimensionRevive.list) {
            World world = (World) worldObject;
            OrganiserStorage.getStorage(world).onDimensionRevive(world);
        }
        ReviveChange.DimensionRevive.list.clear();

        for (Object organiser : ReviveChange.PlayerDevive.list) {
            ((PlayerOrganiser) organiser).devive();
        }
        ReviveChange.PlayerDevive.list.clear();
    }

    /**
     * The maximum number of chunks each loader can load.
     *
     * @return The max chunks.
     */
    public static int maxChunksPerLoader() {
        return maxChunks;
    }

    /**
     * Checks if a Server Operator can access chunk loaders.
     *
     * @return If op's can interact.
     */
    public static boolean canOpInteract() {
        return opInteract;
    }

    /**
     * Called when a world unloads to notify TicketManagers.
     *
     * @param world The world unloading.
     */
    public static void onWorldUnload(World world) {
        OrganiserStorage.getStorage(world).onDimensionUnload(world);
    }

    private static BlockPos readPosition(NBTTagCompound tag) {
        int x = tag.getInteger("x");
        int y = tag.getInteger("y");
        int z = tag.getInteger("z");
        return new BlockPos(x, y, z);
    }

    private static NBTTagCompound writeBlockPos(BlockPos pos, NBTTagCompound tag) {
        tag.setInteger("x", pos.getX());
        tag.setInteger("y", pos.getY());
        tag.setInteger("z", pos.getZ());
        return tag;
    }
}
