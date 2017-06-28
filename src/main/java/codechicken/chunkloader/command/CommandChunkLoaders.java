package codechicken.chunkloader.command;

import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.chunkloader.manager.PlayerChunkViewerManager;
import codechicken.lib.command.PlayerCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

public class CommandChunkLoaders extends PlayerCommand {

    @Override
    public String getName() {
        return "chunkloaders";
    }

    @Override
    public boolean isOpOnly() {
        return false;
    }

    @Override
    public String getUsage(ICommandSender var1) {
        return "chunkloaders";
    }

    @Override
    public void handleCommand(WorldServer world, EntityPlayerMP player, String[] args) {
        if (PlayerChunkViewerManager.instance().isViewerOpen(player.getName())) {
            chatT(player, "command.chunkloaders.alreadyopen");
            return;
        }
        if (!ChunkLoaderManager.allowChunkViewer(player.getName())) {
            chatT(player, "command.chunkloaders.denied");
            return;
        }
        PlayerChunkViewerManager.instance().addViewers.add(player.getName());
    }

    @Override
    public void printHelp(ICommandSender listener) {
        chatT(listener, "command.chunkloaders");
    }

    @Override
    public int minimumParameters() {
        return 0;
    }
}
