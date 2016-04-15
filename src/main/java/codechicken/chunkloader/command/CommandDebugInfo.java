package codechicken.chunkloader.command;

import codechicken.core.commands.CoreCommand;
import net.minecraft.command.ICommandSender;

public class CommandDebugInfo extends CoreCommand {
    @Override
    public String getCommandName() {
        return "ccdebug";
    }

    @Override
    public boolean isOpOnly() {
        return false;
    }

    @Override
    public void handleCommand(String command, String playername, String[] args, ICommandSender listener) {

    }

    @Override
    public void printHelp(ICommandSender listener) {
        chatT(listener, "command.ccdebug");
    }

    @Override
    public int minimumParameters() {
        return 0;
    }
}
