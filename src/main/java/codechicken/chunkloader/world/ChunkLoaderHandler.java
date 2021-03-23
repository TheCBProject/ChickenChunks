package codechicken.chunkloader.world;

import codechicken.chunkloader.api.IChunkLoader;
import codechicken.chunkloader.api.IChunkLoaderHandler;
import codechicken.chunkloader.handler.ChickenChunksConfig;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;
import static codechicken.lib.util.SneakyUtils.nullC;

/**
 * Created by covers1624 on 5/4/20.
 */
public class ChunkLoaderHandler implements IChunkLoaderHandler {

    private static final Logger logger = LogManager.getLogger();
    private static final ResourceLocation KEY = new ResourceLocation(MOD_ID, "chunk_loaders");
    private static final boolean DEBUG = Boolean.getBoolean("chickenchunks.loading.debug");

    @CapabilityInject (IChunkLoaderHandler.class)
    public static final Capability<IChunkLoaderHandler> HANDLER_CAPABILITY = null;

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderHandler::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderHandler::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderHandler::onWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderHandler::onWorldTick);
        MinecraftForge.EVENT_BUS.addGenericListener(World.class, ChunkLoaderHandler::attachCapabilities);
        CapabilityManager.INSTANCE.register(IChunkLoaderHandler.class, new Storage(), nullC());
    }

    //region Events
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player.level instanceof ServerWorld) {
            ChunkLoaderHandler handler = getHandler(player.level);
            handler.login(event);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player.level instanceof ServerWorld) {
            ChunkLoaderHandler handler = getHandler(player.level);
            handler.logout(event);
        }
    }

    private static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) event.getWorld();
            if (world.dimension() == World.OVERWORLD) {
                ChunkLoaderHandler handler = getHandler(world);
                handler.onOverWorldLoad();
            }
        }
    }

    private static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) event.world;
            if (world.dimension() == World.OVERWORLD) {
                ChunkLoaderHandler handler = getHandler(world);
                if (handler != null) {
                    handler.tick(event);
                }
            }
        }
    }

    //Attach our IChunkLoaderHandler capability to the overworld.

    private static void attachCapabilities(AttachCapabilitiesEvent<World> event) {
        if (event.getObject().isClientSide) {
            return;
        }
        ServerWorld world = (ServerWorld) event.getObject();
        if (world.dimension() != World.OVERWORLD) {
            return;
        }
        IChunkLoaderHandler handler = new ChunkLoaderHandler(world.getServer());
        LazyOptional<IChunkLoaderHandler> handlerOpt = LazyOptional.of(() -> handler);
        event.addCapability(KEY, new ICapabilitySerializable<INBT>() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                if (cap == HANDLER_CAPABILITY) {
                    return handlerOpt.cast();
                }
                return LazyOptional.empty();
            }

            //@formatter:off
            @Override public INBT serializeNBT() { return HANDLER_CAPABILITY.writeNBT(handler, null); }
            @Override public void deserializeNBT(INBT nbt) { HANDLER_CAPABILITY.readNBT(handler, null, nbt); }
            //@formatter:on
        });
    }

    //endregion
    private final MinecraftServer server;

    //Use ResourceLocation instead of DimensionType so we can write unknown DimensionTypes back to disk instead of voiding it.
    //<Player, DimensionType, Organiser> / For each player, their per dimension Organiser instance.

    private final Table<UUID, ResourceLocation, Organiser> playerOrganisers = HashBasedTable.create();
    //<DimensionType, Chunk, ChunkTicket> / Each dimensions, ChunkTicket instances per chunk.
    private final Table<ResourceLocation, ChunkPos, ChunkTicket> activeTickets = HashBasedTable.create();
    private final List<Organiser> deviveList = new LinkedList<>();

    private final List<Organiser> reviveList = new LinkedList<>();
    //When, in Millis was the player last seen online.

    private final Object2LongMap<UUID> loginTimes = new Object2LongOpenHashMap<>();

    protected ChunkLoaderHandler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void addChunkLoader(IChunkLoader loader) {
        Objects.requireNonNull(loader);
        Organiser organiser = getOrganiser(loader);
        if (canLoadChunks(loader, loader.getChunks())) {
            organiser.addChunkLoader(loader);
        } else {
            loader.deactivate();
        }
    }

    @Override
    public void removeChunkLoader(IChunkLoader loader) {
        Objects.requireNonNull(loader);
        Organiser organiser = getOrganiser(loader);
        organiser.remChunkLoader(loader);
    }

    @Override
    public boolean canLoadChunks(IChunkLoader loader, Set<ChunkPos> newChunks) {
        Objects.requireNonNull(loader);
        UUID player = Objects.requireNonNull(loader.getOwner());
        ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getRestrictions(player);
        int totalLoaded = getLoadedChunkCount(player);//How many the player is currently loading.

        Organiser organiser = getOrganiser(loader);
        Set<ChunkPos> currentLoaded = organiser.forcedChunksByLoader.get(loader);
        int netChange = newChunks.size();
        if (currentLoaded != null && !currentLoaded.isEmpty()) {
            netChange = newChunks.size() - Sets.intersection(newChunks, currentLoaded).size();
        }
        return ChickenChunksConfig.doesBypassRestrictions(server, player) || totalLoaded + netChange <= restrictions.getTotalAllowedChunks();
    }

    @Override
    public void updateLoader(IChunkLoader loader) {
        Objects.requireNonNull(loader);
        Organiser organiser = getOrganiser(loader);
        organiser.updateLoader(loader);
    }

    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        loginTimes.put(event.getPlayer().getUUID(), System.currentTimeMillis());
        reviveList.addAll(playerOrganisers.row(event.getPlayer().getUUID()).values());
    }

    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID player = event.getPlayer().getUUID();
        ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getRestrictions(player);
        if (!restrictions.canLoadOffline()) {
            deviveList.addAll(playerOrganisers.row(player).values());
        }
    }

    private void onOverWorldLoad() {
        for (Map.Entry<UUID, Map<ResourceLocation, Organiser>> playerEntry : playerOrganisers.rowMap().entrySet()) {
            long curr = System.currentTimeMillis();
            UUID player = playerEntry.getKey();
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getRestrictions(player);
            int timeout = restrictions.getOfflineTimeout();
            long lastSeen = loginTimes.getOrDefault(player, -1L);
            //If the user is allowed to load things offline, or their timeout hasn't expired yet.
            if (restrictions.canLoadOffline() || (lastSeen != -1 || (curr - lastSeen) / 60000L < timeout)) {
                reviveList.addAll(playerEntry.getValue().values());
            }
        }
    }

    public void tick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            //Every minute.
            if (event.world.getGameTime() % 1200 == 0) {
                long curr = System.currentTimeMillis();
                //Update login times of players.
                for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
                    loginTimes.put(player.getUUID(), curr);
                }
                //Queue their Organisers for unload if needed.
                for (Map.Entry<UUID, Map<ResourceLocation, Organiser>> playerEntry : playerOrganisers.rowMap().entrySet()) {
                    UUID player = playerEntry.getKey();
                    ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getRestrictions(player);
                    if (!restrictions.canLoadOffline()) {
                        int timeout = restrictions.getOfflineTimeout();
                        long lastSeen = loginTimes.getOrDefault(player, -1L);
                        if (timeout == 0 || lastSeen == -1 || (curr - lastSeen) / 60000L < timeout) {
                            deviveList.addAll(playerEntry.getValue().values());
                        }
                    }
                }
            }
            //Tick unload queue for each organiser.
            playerOrganisers.values().forEach(Organiser::tickUnloads);

            //Handle devive / revive list.
            for (Organiser organiser : reviveList) {
                RegistryKey<World> key = RegistryKey.create(Registry.DIMENSION_REGISTRY, organiser.dim);
                ServerWorld world = server.getLevel(key);
                if (world != null) {
                    organiser.revive(world);
                }
            }
            reviveList.clear();
            for (Organiser organiser : deviveList) {
                organiser.devive();
            }
            deviveList.clear();
        }
    }

    public void remChunk(IChunkLoader loader, ResourceLocation dim, ChunkPos pos) {
        ChunkTicket ticket = activeTickets.get(dim, pos);
        if (ticket != null) {
            ticket.loaders.remove(loader);
            if (ticket.tryFree()) {
                activeTickets.remove(dim, pos);
                if (DEBUG) {
                    logger.info("Un-Forcing chunk: {}", pos);
                }
            }
        }
    }

    public void addChunk(IChunkLoader loader, ResourceLocation dim, ChunkPos pos) {
        RegistryKey<World> key = RegistryKey.create(Registry.DIMENSION_REGISTRY, dim);
        ServerWorld world = server.getLevel(key);
        TicketManager ticketManager = world.getChunkSource().distanceManager;
        ChunkTicket ticket = computeIfAbsent(activeTickets, dim, pos, () -> new ChunkTicket(ticketManager, pos));
        ticket.loaders.add(loader);
        ticket.tryAlloc();
        if (DEBUG) {
            logger.info("Forcing chunk: {}", pos);
        }
    }

    //region Utility methods.
    public int getLoadedChunkCount(UUID player) {
        return playerOrganisers.row(player).values().stream()//
                .mapToInt(organiser -> organiser.forcedChunksByChunk.size())//
                .sum();
    }

    public Organiser getOrganiser(IChunkLoader loader) {
        Objects.requireNonNull(loader);
        UUID player = Objects.requireNonNull(loader.getOwner());
        return getOrganiser(loader.world().dimension(), player);
    }

    public Organiser getOrganiser(RegistryKey<World> dim, UUID player) {
        return getOrganiser(dim.location(), player);
    }

    public Organiser getOrganiser(ResourceLocation dim, UUID player) {
        return computeIfAbsent(playerOrganisers, player, dim, () -> new Organiser(this, dim, player));
    }

    private static ChunkLoaderHandler getHandler(IWorld world) {
        return (ChunkLoaderHandler) IChunkLoaderHandler.getCapability(world);
    }
    //endregion

    private static class Storage implements Capability.IStorage<IChunkLoaderHandler> {

        @Nullable
        @Override
        public INBT writeNBT(Capability<IChunkLoaderHandler> capability, IChunkLoaderHandler instance, Direction side) {
            if (!(instance instanceof ChunkLoaderHandler)) {
                return null;
            }
            ChunkLoaderHandler handler = (ChunkLoaderHandler) instance;

            CompoundNBT tag = new CompoundNBT();
            ListNBT playerList = new ListNBT();
            for (Map.Entry<UUID, Map<ResourceLocation, Organiser>> playerEntry : handler.playerOrganisers.rowMap().entrySet()) {
                CompoundNBT playerTag = new CompoundNBT();
                playerTag.putUUID("player", playerEntry.getKey());
                ListNBT dimensions = new ListNBT();
                for (Map.Entry<ResourceLocation, Organiser> dimEntry : playerEntry.getValue().entrySet()) {
                    if (dimEntry.getValue().isEmpty()) {
                        continue;//Culling.
                    }

                    CompoundNBT dimTag = new CompoundNBT();
                    dimTag.putString("dimension", dimEntry.getKey().toString());
                    dimTag.put("organiser", dimEntry.getValue().write(new CompoundNBT()));
                    dimensions.add(dimTag);
                }
                if (dimensions.isEmpty()) {
                    continue;//Culling.
                }

                playerTag.put("dimensions", dimensions);
                playerList.add(playerTag);
            }
            tag.put("playerOrganisers", playerList);

            ListNBT loginList = new ListNBT();
            for (Object2LongMap.Entry<UUID> uuidEntry : handler.loginTimes.object2LongEntrySet()) {
                CompoundNBT playerTag = new CompoundNBT();
                playerTag.putUUID("player", uuidEntry.getKey());
                playerTag.putLong("time", uuidEntry.getLongValue());
                loginList.add(playerTag);
            }
            tag.put("loginTimes", loginList);
            return tag;
        }

        @Override
        public void readNBT(Capability<IChunkLoaderHandler> capability, IChunkLoaderHandler instance, Direction side, INBT nbt) {
            if (!(instance instanceof ChunkLoaderHandler)) {
                return;
            }
            ChunkLoaderHandler handler = (ChunkLoaderHandler) instance;
            CompoundNBT tag = (CompoundNBT) nbt;

            ListNBT playerList = tag.getList("playerOrganisers", 10);
            for (int i = 0; i < playerList.size(); i++) {
                CompoundNBT playerTag = playerList.getCompound(i);
                UUID player = playerTag.getUUID("player");
                ListNBT dimensions = playerTag.getList("dimensions", 10);
                for (int j = 0; j < dimensions.size(); j++) {
                    CompoundNBT dimTag = dimensions.getCompound(j);
                    ResourceLocation dim = new ResourceLocation(dimTag.getString("dimension"));
                    Organiser organiser = new Organiser(handler, dim, player).read(dimTag.getCompound("organiser"));
                    handler.playerOrganisers.put(player, dim, organiser);
                }
            }

            ListNBT loginList = tag.getList("times", 10);
            for (int i = 0; i < loginList.size(); i++) {
                CompoundNBT playerTag = loginList.getCompound(i);
                handler.loginTimes.put(playerTag.getUUID("player"), playerTag.getLong("time"));
            }
        }
    }

    private static <R, C, V> V computeIfAbsent(Table<R, C, V> table, R r, C c, Supplier<V> vFunc) {
        V val = table.get(r, c);
        if (val == null) {
            val = vFunc.get();
            table.put(r, c, val);
        }
        return val;
    }
}
