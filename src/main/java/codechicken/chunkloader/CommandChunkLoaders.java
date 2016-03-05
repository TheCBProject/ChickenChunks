package codechicken.chunkloader;

import codechicken.core.commands.PlayerCommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.WorldServer;

public class CommandChunkLoaders extends PlayerCommand
{
    @Override
    public String getCommandName()
    {
        return "chunkloaders";
    }

    @Override
    public String getCommandUsage(ICommandSender var1)
    {
        return "chunkloaders";
    }

    @Override
    public void handleCommand(WorldServer world, EntityPlayerMP player, String[] args)
    {
        WCommandSender wrapped = new WCommandSender(player);
        if(PlayerChunkViewerManager.instance().isViewerOpen(player.getCommandSenderName()))
        {
            wrapped.chatT("command.chunkloaders.alreadyopen");
            return;
        }
        if(!ChunkLoaderManager.allowChunkViewer(player.getCommandSenderName()))
        {
            wrapped.chatT("command.chunkloaders.denied");
            return;
        }
        PlayerChunkViewerManager.instance().addViewers.add(player.getCommandSenderName());
    }
    
    @Override
    public void printHelp(WCommandSender listener)
    {
        listener.chatT("command.chunkloaders");
    }

    @Override
    public boolean OPOnly()
    {
        return false;
    }
    
    @Override
    public int minimumParameters()
    {
        return 0;
    }
}
