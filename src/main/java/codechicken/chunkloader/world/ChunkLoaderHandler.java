package codechicken.chunkloader.world;

import codechicken.chunkloader.api.IChunkLoader;
import codechicken.chunkloader.api.IChunkLoaderHandler;
import codechicken.chunkloader.handler.ChickenChunksConfig;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Created by covers1624 on 5/4/20.
 */
public class ChunkLoaderHandler implements IChunkLoaderHandler, INBTSerializable<CompoundTag> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation KEY = new ResourceLocation(MOD_ID, "chunk_loaders");
    private static final boolean DEBUG = Boolean.getBoolean("chickenchunks.loading.debug");

    public static final Capability<IChunkLoaderHandler> HANDLER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() { });

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ChunkLoaderHandler::onRegisterCaps);

        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderHandler::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderHandler::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderHandler::onWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(ChunkLoaderHandler::onWorldTick);
        MinecraftForge.EVENT_BUS.addGenericListener(Level.class, ChunkLoaderHandler::attachCapabilities);
        ForgeChunkManager.setForcedChunkLoadingCallback(MOD_ID, (world, ticketHelper) -> {
            // On load, nuke everything. We manually re-register tickets.
            ticketHelper.getBlockTickets().keySet().forEach(ticketHelper::removeAllTickets);
            ticketHelper.getEntityTickets().keySet().forEach(ticketHelper::removeAllTickets);
        });
    }

    private static void onRegisterCaps(RegisterCapabilitiesEvent event) {
        event.register(IChunkLoaderHandler.class);
    }

    //region Events
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level instanceof ServerLevel) {
            ChunkLoaderHandler handler = getHandler(player.level);
            handler.login(event);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player.level instanceof ServerLevel) {
            ChunkLoaderHandler handler = getHandler(player.level);
            handler.logout(event);
        }
    }

    private static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel world) {
            if (world.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                ChunkLoaderHandler handler = getHandler(world);
                handler.onOverWorldLoad();
            }
        }
    }

    private static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level instanceof ServerLevel world) {
            if (world.dimension() == Level.OVERWORLD) {
                ChunkLoaderHandler handler = getHandler(world);
                if (handler != null) {
                    handler.tick(event);
                }
            }
        }
    }

    //Attach our IChunkLoaderHandler capability to the overworld.
    private static void attachCapabilities(AttachCapabilitiesEvent<Level> event) {
        if (event.getObject().isClientSide) {
            return;
        }
        ServerLevel world = (ServerLevel) event.getObject();
        if (world.dimension() != Level.OVERWORLD) {
            return;
        }

        event.addCapability(KEY, new ICapabilitySerializable<CompoundTag>() {

            private final ChunkLoaderHandler handler = new ChunkLoaderHandler(world.getServer());
            private final LazyOptional<ChunkLoaderHandler> opt = LazyOptional.of(() -> handler);

            @Override
            public CompoundTag serializeNBT() {
                return handler.serializeNBT();
            }

            @Override
            public void deserializeNBT(CompoundTag tag) {
                handler.deserializeNBT(tag);
            }

            @NotNull
            @Override
            public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @org.jetbrains.annotations.Nullable Direction side) {
                if (cap == HANDLER_CAPABILITY) {
                    return unsafeCast(opt);
                }
                return LazyOptional.empty();
            }
        });
    }
    //endregion

    private final MinecraftServer server;

    // <Player, DimensionType, Organiser> / For each player, their per dimension Organiser instance.
    private final Table<UUID, ResourceLocation, Organiser> playerOrganisers = HashBasedTable.create();

    // <DimensionType, Chunk, ChunkTicket> / Each dimensions, ChunkTicket instances per chunk.
    private final Table<ResourceLocation, ChunkPos, ChunkTicket> activeTickets = HashBasedTable.create();
    private final List<Organiser> deviveList = new LinkedList<>();

    private final List<Organiser> reviveList = new LinkedList<>();

    // When, in Millis was the player last seen online.
    private final Object2LongMap<UUID> loginTimes = new Object2LongOpenHashMap<>();

    protected ChunkLoaderHandler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void addChunkLoader(IChunkLoader loader) {
        Objects.requireNonNull(loader);
        if (loader.getOwner() == null) {
            LOGGER.error("ChunkLoader at {} has null owner. Not processing.", loader.pos());
            return;
        }
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
        loginTimes.put(event.getEntity().getUUID(), System.currentTimeMillis());
        reviveList.addAll(playerOrganisers.row(event.getEntity().getUUID()).values());
    }

    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID player = event.getEntity().getUUID();
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
            // If the user is allowed to load things offline, or their timeout hasn't expired yet.
            if (restrictions.canLoadOffline() || (lastSeen != -1 || (curr - lastSeen) / 60000L < timeout)) {
                if (DEBUG) LOGGER.info("Adding {} organizers to revive list for {}", playerEntry.getValue().values().size(), player);
                reviveList.addAll(playerEntry.getValue().values());
            }
        }
    }

    public void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Every minute.
            if (event.level.getGameTime() % 1200 == 0) {
                long curr = System.currentTimeMillis();
                // Update login times of players.
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    loginTimes.put(player.getUUID(), curr);
                }
                // Queue their Organisers for unload if needed.
                for (Map.Entry<UUID, Map<ResourceLocation, Organiser>> playerEntry : playerOrganisers.rowMap().entrySet()) {
                    UUID player = playerEntry.getKey();
                    ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getRestrictions(player);
                    if (!restrictions.canLoadOffline()) {
                        int timeout = restrictions.getOfflineTimeout();
                        long lastSeen = loginTimes.getOrDefault(player, -1L);
                        if (lastSeen != curr && (timeout == 0 || lastSeen == -1 || (curr - lastSeen) / 60000L < timeout)) {
                            deviveList.addAll(playerEntry.getValue().values());
                        }
                    }
                }
            }

            // Tick each organizer, it may want to load/unload things.
            playerOrganisers.values().forEach(Organiser::onTickEnd);

            // Handle devive / revive list.
            for (Organiser organiser : reviveList) {
                ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, organiser.dim);
                ServerLevel world = server.getLevel(key);
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
            if (ticket.remLoader(loader)) {
                activeTickets.remove(dim, pos);
            }
            if (DEBUG) {
                LOGGER.info("Loader {} Un-Forcing chunk: {}", loader.pos(), pos);
            }
        }
    }

    public void addChunk(IChunkLoader loader, ResourceLocation dim, ChunkPos pos) {
        ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, dim);
        ServerLevel world = server.getLevel(key);
        ChunkTicket ticket = computeIfAbsent(activeTickets, dim, pos, () -> new ChunkTicket(world, pos));
        ticket.addLoader(loader);
        if (DEBUG) {
            LOGGER.info("Loader {} Forcing chunk: {}", loader.pos(), pos);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, Map<ResourceLocation, Organiser>> playerEntry : playerOrganisers.rowMap().entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", playerEntry.getKey());
            ListTag dimensions = new ListTag();
            for (Map.Entry<ResourceLocation, Organiser> dimEntry : playerEntry.getValue().entrySet()) {
                if (dimEntry.getValue().isEmpty()) continue; // Culling.

                CompoundTag dimTag = new CompoundTag();
                dimTag.putString("dimension", dimEntry.getKey().toString());
                dimTag.put("organiser", dimEntry.getValue().write(new CompoundTag()));
                dimensions.add(dimTag);
            }
            if (dimensions.isEmpty()) continue; // Culling.

            playerTag.put("dimensions", dimensions);
            playerList.add(playerTag);
        }
        tag.put("playerOrganisers", playerList);

        ListTag loginList = new ListTag();
        for (Object2LongMap.Entry<UUID> uuidEntry : loginTimes.object2LongEntrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", uuidEntry.getKey());
            playerTag.putLong("time", uuidEntry.getLongValue());
            loginList.add(playerTag);
        }
        tag.put("loginTimes", loginList);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        ListTag playerList = tag.getList("playerOrganisers", 10);
        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerTag = playerList.getCompound(i);
            UUID player = playerTag.getUUID("player");
            ListTag dimensions = playerTag.getList("dimensions", 10);
            for (int j = 0; j < dimensions.size(); j++) {
                CompoundTag dimTag = dimensions.getCompound(j);
                ResourceLocation dim = new ResourceLocation(dimTag.getString("dimension"));
                Organiser organiser = new Organiser(this, dim, player).read(dimTag.getCompound("organiser"));
                playerOrganisers.put(player, dim, organiser);
            }
        }

        ListTag loginList = tag.getList("times", 10);
        for (int i = 0; i < loginList.size(); i++) {
            CompoundTag playerTag = loginList.getCompound(i);
            loginTimes.put(playerTag.getUUID("player"), playerTag.getLong("time"));
        }
    }

    //region Utility methods.
    public int getLoadedChunkCount(UUID player) {
        return playerOrganisers.row(player).values().stream()
                .mapToInt(organiser -> organiser.forcedChunksByChunk.size())
                .sum();
    }

    public Organiser getOrganiser(IChunkLoader loader) {
        Objects.requireNonNull(loader);
        UUID player = Objects.requireNonNull(loader.getOwner());
        return getOrganiser(loader.world().dimension(), player);
    }

    public Organiser getOrganiser(ResourceKey<Level> dim, UUID player) {
        return getOrganiser(dim.location(), player);
    }

    public Organiser getOrganiser(ResourceLocation dim, UUID player) {
        return computeIfAbsent(playerOrganisers, player, dim, () -> new Organiser(this, dim, player));
    }

    private static ChunkLoaderHandler getHandler(LevelAccessor world) {
        return (ChunkLoaderHandler) IChunkLoaderHandler.getCapability(world);
    }
    //endregion

    private static <R, C, V> V computeIfAbsent(Table<R, C, V> table, R r, C c, Supplier<V> vFunc) {
        V val = table.get(r, c);
        if (val == null) {
            val = vFunc.get();
            table.put(r, c, val);
        }
        return val;
    }
}
