package codechicken.chunkloader.handler;

import codechicken.lib.config.ConfigTag;
import codechicken.lib.config.StandardConfigFile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by covers1624 on 5/4/20.
 */
public class ChickenChunksConfig {

    private static ConfigTag config;
    private static ConfigTag playerRestrictions;
    private static final Map<UUID, Restrictions> perPlayerRestrictions = new HashMap<>();
    private static boolean opsBypassRestrictions;
    private static boolean opsAccessAllLoaders;
    private static boolean globalAllowOffline;
    private static int globalOfflineTimeout;
    private static int globalTotalAllowedChunks;
    private static int globalChunksPerLoader;

    public static void load() {
        config = new StandardConfigFile(Paths.get("./config/ChickenChunks.cfg")).load();
        opsBypassRestrictions = config.getTag("opsBypassRestrictions")//
                .setComment("If Players with OP permissions bypass chunk loading restrictions.")//
                .setDefaultBoolean(false)//
                .getBoolean();
        opsAccessAllLoaders = config.getTag("opsAccessAllLoaders")//
                .setComment("If Players with OP permissions can manage other users ChunkLoaders")//
                .setDefaultBoolean(true)//
                .getBoolean();
        globalAllowOffline = config.getTag("allowOffline")//
                .setComment("If chunks should stay loaded when a ChunkLoader's owner is offline.")//
                .setDefaultBoolean(true)//
                .getBoolean();
        globalOfflineTimeout = config.getTag("offlineTimeout")//
                .setComment("How long in minutes ChickenChunks should wait after a Player logs out to unload their chunks. Only effective when allowOffline=false")//
                .setDefaultInt(0)//
                .getInt();
        globalTotalAllowedChunks = config.getTag("totalAllowedChunks")//
                .setComment("The number of chunks each player is allowed to load in total.")//
                .setDefaultInt(5000)//
                .getInt();
        globalChunksPerLoader = config.getTag("chunksPerLoader")//
                .setComment("The number of chunks each ChunkLoader is allowed to load in total.")//
                .setDefaultInt(400)//
                .getInt();
        playerRestrictions = config.getTag("playerRestrictions")//
                .setSyncToClient()//
                .setComment("Specifies restrictions for each player, Use /chickenchunks instead.");
        //TODO Re add this when the config system gets rewritten..
        //ConfigSyncManager.registerSync(new ResourceLocation(MOD_ID, "player_restrictions"), playerRestrictions);
        playerRestrictions.setSyncCallback((tag, syncType) -> {
            perPlayerRestrictions.clear();
            for (String uuidString : tag.getChildNames()) {
                ConfigTag playerTag = tag.getTag(uuidString);
                UUID uuid = UUID.fromString(uuidString);
                perPlayerRestrictions.put(uuid, new Restrictions(playerTag));
            }
        });
        config.save();
        playerRestrictions.runSync();
    }

    public static boolean doesBypassRestrictions(MinecraftServer server, UUID playerUUID) {
        ServerPlayerEntity player = server.getPlayerList().getPlayerByUUID(playerUUID);
        if (player != null && server.getPlayerList().getOppedPlayers().hasEntry(player.getGameProfile())) {
            return opsBypassRestrictions;
        }
        return false;
    }

    public static boolean doesBypassLoaderAccess(ServerPlayerEntity player) {
        if (player.getServer().getPlayerList().getOppedPlayers().hasEntry(player.getGameProfile())) {
            return opsAccessAllLoaders;
        }
        return false;
    }

    public static Restrictions getOrCreateRestrictions(UUID player) {
        return perPlayerRestrictions.computeIfAbsent(player, e -> {
            ConfigTag tag = playerRestrictions.getTag(player.toString()).markDirty();
            tag.save();
            return new Restrictions(tag);
        });
    }

    public static void resetRestrictions(UUID player) {
        perPlayerRestrictions.remove(player);
        playerRestrictions.deleteTag(player.toString());
        playerRestrictions.save();
    }

    public static Restrictions getRestrictions(UUID player) {
        return perPlayerRestrictions.getOrDefault(player, Restrictions.EMPTY);
    }

    public static class Restrictions {

        public static final Restrictions EMPTY = new Restrictions();

        private Optional<Boolean> allowOffline = Optional.empty();
        private Optional<Integer> offlineTimeout = Optional.empty();
        private Optional<Integer> totalAllowedChunks = Optional.empty();
        private Optional<Integer> chunksPerLoader = Optional.empty();

        private ConfigTag tag;

        public Restrictions() {
        }

        public Restrictions(ConfigTag tag) {
            this.tag = tag;
            allowOffline = Optional.ofNullable(tag.getTagIfPresent("allowOffline"))//
                    .map(ConfigTag::getBoolean);
            offlineTimeout = Optional.ofNullable(tag.getTagIfPresent("offlineTimeout"))//
                    .map(ConfigTag::getInt);
            totalAllowedChunks = Optional.ofNullable(tag.getTagIfPresent("totalAllowedChunks"))//
                    .map(ConfigTag::getInt);
            chunksPerLoader = Optional.ofNullable(tag.getTagIfPresent("chunksPerLoader"))//
                    .map(ConfigTag::getInt);
        }

        public boolean canLoadOffline() {
            return allowOffline.orElse(globalAllowOffline);
        }

        public int getOfflineTimeout() {
            return offlineTimeout.orElse(globalOfflineTimeout);
        }

        public int getTotalAllowedChunks() {
            return totalAllowedChunks.orElse(globalTotalAllowedChunks);
        }

        public int getChunksPerLoader() {
            return chunksPerLoader.orElse(globalChunksPerLoader);
        }

        public void setAllowOffline(boolean state) {
            tag.getTag("allowOffline").setBoolean(state).save();
            allowOffline = Optional.of(state);
        }

        public void setOfflineTimeout(int num) {
            tag.getTag("offlineTimeout").setInt(num).save();
            offlineTimeout = Optional.of(num);
        }

        public void setTotalAllowedChunks(int num) {
            tag.getTag("totalAllowedChunks").setInt(num).save();
            totalAllowedChunks = Optional.of(num);
        }

        public void setChunksPerLoader(int num) {
            tag.getTag("chunksPerLoader").setInt(num).save();
            chunksPerLoader = Optional.of(num);
        }

        public void remAllowOffline() {
            tag.deleteTag("allowOffline").save();
            allowOffline = Optional.empty();
        }

        public void remOfflineTimeout() {
            tag.deleteTag("offlineTimeout").save();
            offlineTimeout = Optional.empty();
        }

        public void remTotalAllowedChunks() {
            tag.deleteTag("totalAllowedChunks").save();
            totalAllowedChunks = Optional.empty();
        }

        public void remChunksPerLoader() {
            tag.deleteTag("chunksPerLoader").save();
            chunksPerLoader = Optional.empty();
        }

        public boolean isEmpty() {
            return !allowOffline.isPresent() && !offlineTimeout.isPresent() && !totalAllowedChunks.isPresent() && !chunksPerLoader.isPresent();
        }
    }

}
