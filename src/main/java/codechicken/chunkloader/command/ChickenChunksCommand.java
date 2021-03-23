package codechicken.chunkloader.command;

import codechicken.chunkloader.handler.ChickenChunksConfig;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.UUID;

import static codechicken.chunkloader.ChickenChunks.MOD_ID;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

/**
 * Created by covers1624 on 28/7/20.
 */
public class ChickenChunksCommand {

    private static final String RESTRICTIONS_RESET_FOR = MOD_ID + ":commands.restrictions.player.reset";

    private static final String OFFLINE_ENABLE_FOR = MOD_ID + ":commands.restrictions.player.offline.enable";
    private static final String OFFLINE_DISABLE_FOR = MOD_ID + ":commands.restrictions.player.offline.disable";
    private static final String OFFLINE_RESET_FOR = MOD_ID + ":commands.restrictions.player.offline.reset";

    private static final String TIMEOUT_SET_FOR = MOD_ID + ":commands.restrictions.player.timeout.set";
    private static final String TIMEOUT_RESET_FOR = MOD_ID + ":commands.restrictions.player.timeout.reset";

    private static final String ALLOWED_CHUNKS_SET_FOR = MOD_ID + ":commands.restrictions.player.allowed_chunks.set";
    private static final String ALLOWED_CHUNKS_RESET_FOR = MOD_ID + ":commands.restrictions.player.allowed_chunks.reset";

    private static final String CHUNKS_PER_LOADER_SET_FOR = MOD_ID + ":commands.restrictions.player.chunks_per_loader.set";
    private static final String CHUNKS_PER_LOADER_RESET_FOR = MOD_ID + ":commands.restrictions.player.chunks_per_loader.reset";

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ChickenChunksCommand::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        dispatcher.register(literal("chickenchunks")//
                .requires(e -> e.hasPermission(4))//
                .then(literal("restrict")//
                        .then(literal("player")//
                                .then(argument("players", GameProfileArgument.gameProfile())//
                                        .then(literal("reset").executes(ChickenChunksCommand::resetRestrictions))//
                                        .then(literal("allow_offline")//
                                                .then(literal("toggle").executes(ChickenChunksCommand::toggleOfflineFor))//
                                                .then(literal("enable").executes(ChickenChunksCommand::enableOfflineFor))//
                                                .then(literal("disable").executes(ChickenChunksCommand::disableOfflineFor))//
                                                .then(literal("reset").executes(ChickenChunksCommand::resetOfflineFor))//
                                        )//
                                        .then(literal("offline_timeout")//
                                                .then(argument("timeout", IntegerArgumentType.integer()).executes(ChickenChunksCommand::setTimeoutFor))//
                                                .then(literal("reset").executes(ChickenChunksCommand::resetTimeoutFor))//
                                        )//
                                        .then(literal("allowed_chunks")//
                                                .then(argument("allowed_chunks", IntegerArgumentType.integer()).executes(ChickenChunksCommand::setAllowedChunksFor))//
                                                .then(literal("reset").executes(ChickenChunksCommand::resetAllowedChunksFor))//
                                        )//
                                        .then(literal("chunks_per_loader")//
                                                .then(argument("chunks_per_loader", IntegerArgumentType.integer()).executes(ChickenChunksCommand::setChunksPerLoaderFor))//
                                                .then(literal("reset").executes(ChickenChunksCommand::resetChunksPerLoaderFor))//
                                        )//
                                )//
                        )//
                )//
        );
    }

    private static int resetRestrictions(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.resetRestrictions(profile.getId());
            src.sendSuccess(new TranslationTextComponent(RESTRICTIONS_RESET_FOR, profile.getName()), true);
        }
        return profiles.size();
    }

    private static int toggleOfflineFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            boolean state = !restrictions.canLoadOffline();
            restrictions.setAllowOffline(state);
            src.sendSuccess(new TranslationTextComponent(state ? OFFLINE_ENABLE_FOR : OFFLINE_DISABLE_FOR, profile.getName()), true);
        }
        return profiles.size();
    }

    private static int enableOfflineFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        return setOfflineFor(ctx, true);
    }

    private static int disableOfflineFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        return setOfflineFor(ctx, false);
    }

    private static int setOfflineFor(CommandContext<CommandSource> ctx, boolean state) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            restrictions.setAllowOffline(state);
            src.sendSuccess(new TranslationTextComponent(state ? OFFLINE_ENABLE_FOR : OFFLINE_DISABLE_FOR, profile.getName()), true);
        }
        return profiles.size();
    }

    private static int resetOfflineFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            restrictions.remAllowOffline();
            src.sendSuccess(new TranslationTextComponent(OFFLINE_RESET_FOR, profile.getName()), true);
        }
        return profiles.size();
    }

    public static int setTimeoutFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        int timeout = IntegerArgumentType.getInteger(ctx, "timeout");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            restrictions.setOfflineTimeout(timeout);
            src.sendSuccess(new TranslationTextComponent(TIMEOUT_SET_FOR, profile.getName(), timeout), true);
        }
        return profiles.size();
    }

    private static int resetTimeoutFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            restrictions.remOfflineTimeout();
            src.sendSuccess(new TranslationTextComponent(TIMEOUT_RESET_FOR, profile.getName()), true);
        }
        return profiles.size();
    }

    public static int setAllowedChunksFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        int chunks = IntegerArgumentType.getInteger(ctx, "allowed_chunks");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            restrictions.setTotalAllowedChunks(chunks);
            src.sendSuccess(new TranslationTextComponent(ALLOWED_CHUNKS_SET_FOR, profile.getName(), chunks), true);
        }
        return profiles.size();
    }

    private static int resetAllowedChunksFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            restrictions.remTotalAllowedChunks();
            src.sendSuccess(new TranslationTextComponent(ALLOWED_CHUNKS_RESET_FOR, profile.getName()), true);
        }
        return profiles.size();
    }

    public static int setChunksPerLoaderFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        int chunks = IntegerArgumentType.getInteger(ctx, "chunks_per_loader");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            restrictions.setChunksPerLoader(chunks);
            src.sendSuccess(new TranslationTextComponent(CHUNKS_PER_LOADER_SET_FOR, profile.getName(), chunks), true);
        }
        return profiles.size();
    }

    private static int resetChunksPerLoaderFor(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "players");
        for (GameProfile profile : profiles) {
            ChickenChunksConfig.Restrictions restrictions = ChickenChunksConfig.getOrCreateRestrictions(profile.getId());
            restrictions.remChunksPerLoader();
            src.sendSuccess(new TranslationTextComponent(CHUNKS_PER_LOADER_RESET_FOR, profile.getName()), true);
        }
        return profiles.size();
    }

    private static ChickenChunksConfig.Restrictions getPlayerRestrictions(UUID player) {
        return ChickenChunksConfig.getOrCreateRestrictions(player);
    }
}
