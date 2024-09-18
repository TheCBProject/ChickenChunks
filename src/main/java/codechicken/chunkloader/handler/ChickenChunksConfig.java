package codechicken.chunkloader.handler;

import codechicken.lib.config.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;
import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 5/4/20.
 */
public class ChickenChunksConfig {

    private static @Nullable ConfigCategory playerRestrictions;
    private static final Map<UUID, Restrictions> perPlayerRestrictions = new HashMap<>();
    private static boolean opsBypassRestrictions;
    private static boolean opsAccessAllLoaders;
    private static boolean globalAllowOffline;
    private static int globalOfflineTimeout;
    private static int globalTotalAllowedChunks;
    private static int globalChunksPerLoader;

    public static void load() {
        ConfigCategory config = new ConfigFile(MOD_ID)
                .path(Paths.get("./config/ChickenChunks.cfg"))
                .load();
        opsBypassRestrictions = config.getValue("opsBypassRestrictions")
                .setComment("If Players with OP permissions bypass chunk loading restrictions.")
                .setDefaultBoolean(false)
                .getBoolean();
        opsAccessAllLoaders = config.getValue("opsAccessAllLoaders")
                .setComment("If Players with OP permissions can manage other users ChunkLoaders")
                .setDefaultBoolean(true)
                .getBoolean();
        globalAllowOffline = config.getValue("allowOffline")
                .setComment("If chunks should stay loaded when a ChunkLoader's owner is offline.")
                .setDefaultBoolean(true)
                .getBoolean();
        globalOfflineTimeout = config.getValue("offlineTimeout")
                .setComment("How long in minutes ChickenChunks should wait after a Player logs out to unload their chunks. Only effective when allowOffline=false")
                .setDefaultInt(0)
                .getInt();
        globalTotalAllowedChunks = config.getValue("totalAllowedChunks")
                .setComment("The number of chunks each player is allowed to load in total.")
                .setDefaultInt(5000)
                .getInt();
        globalChunksPerLoader = config.getValue("chunksPerLoader")
                .setComment("The number of chunks each ChunkLoader is allowed to load in total.")
                .setDefaultInt(400)
                .getInt();
        playerRestrictions = config.getCategory("playerRestrictions")
                .setComment("Specifies restrictions for each player, Use /chickenchunks instead.");
        playerRestrictions.onSync((tag, syncType) -> {
            perPlayerRestrictions.clear();
            for (ConfigTag child : tag.getChildren()) {
                UUID uuid = UUID.fromString(child.getName());
                perPlayerRestrictions.put(uuid, new Restrictions((ConfigCategory) child));
            }
        });
        config.save();
        playerRestrictions.forceSync();
    }

    public static boolean doesBypassRestrictions(MinecraftServer server, UUID playerUUID) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
        if (player != null && server.getPlayerList().getOps().get(player.getGameProfile()) != null) {
            return opsBypassRestrictions;
        }
        return false;
    }

    public static boolean doesBypassLoaderAccess(ServerPlayer player) {
        if (requireNonNull(player.getServer()).getPlayerList().getOps().get(player.getGameProfile()) != null) {
            return opsAccessAllLoaders;
        }
        return false;
    }

    public static Restrictions getOrCreateRestrictions(UUID player) {
        return perPlayerRestrictions.computeIfAbsent(player, e -> {
            ConfigCategory tag = requireNonNull(playerRestrictions).getCategory(player.toString());
            tag.save();
            return new Restrictions(tag);
        });
    }

    public static void resetRestrictions(UUID player) {
        perPlayerRestrictions.remove(player);
        requireNonNull(playerRestrictions);
        playerRestrictions.delete(player.toString());
        playerRestrictions.save();
    }

    public static Restrictions getRestrictions(UUID player) {
        return perPlayerRestrictions.getOrDefault(player, Restrictions.EMPTY);
    }

    public static class Restrictions {

        public static final Restrictions EMPTY = new Restrictions(new ConfigCategoryImpl("", null));

        private Optional<Boolean> allowOffline;
        private Optional<Integer> offlineTimeout;
        private Optional<Integer> totalAllowedChunks;
        private Optional<Integer> chunksPerLoader;

        private final ConfigCategory tag;

        public Restrictions(ConfigCategory tag) {
            this.tag = tag;
            allowOffline = Optional.ofNullable(tag.findValue("allowOffline"))
                    .map(ConfigValue::getBoolean);
            offlineTimeout = Optional.ofNullable(tag.findValue("offlineTimeout"))
                    .map(ConfigValue::getInt);
            totalAllowedChunks = Optional.ofNullable(tag.findValue("totalAllowedChunks"))
                    .map(ConfigValue::getInt);
            chunksPerLoader = Optional.ofNullable(tag.findValue("chunksPerLoader"))
                    .map(ConfigValue::getInt);
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
            tag.getValue("allowOffline").setBoolean(state).save();
            allowOffline = Optional.of(state);
        }

        public void setOfflineTimeout(int num) {
            tag.getValue("offlineTimeout").setInt(num).save();
            offlineTimeout = Optional.of(num);
        }

        public void setTotalAllowedChunks(int num) {
            tag.getValue("totalAllowedChunks").setInt(num).save();
            totalAllowedChunks = Optional.of(num);
        }

        public void setChunksPerLoader(int num) {
            tag.getValue("chunksPerLoader").setInt(num).save();
            chunksPerLoader = Optional.of(num);
        }

        public void remAllowOffline() {
            tag.delete("allowOffline").save();
            allowOffline = Optional.empty();
        }

        public void remOfflineTimeout() {
            tag.delete("offlineTimeout").save();
            offlineTimeout = Optional.empty();
        }

        public void remTotalAllowedChunks() {
            tag.delete("totalAllowedChunks").save();
            totalAllowedChunks = Optional.empty();
        }

        public void remChunksPerLoader() {
            tag.delete("chunksPerLoader").save();
            chunksPerLoader = Optional.empty();
        }

        public boolean isEmpty() {
            return allowOffline.isEmpty() && offlineTimeout.isEmpty() && totalAllowedChunks.isEmpty() && chunksPerLoader.isEmpty();
        }
    }

}
