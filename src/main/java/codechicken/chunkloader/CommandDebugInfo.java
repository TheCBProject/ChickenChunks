package codechicken.chunkloader;

import codechicken.core.commands.CoreCommand;

public class CommandDebugInfo extends CoreCommand
{
    @Override
    public String getCommandName() {
        return "ccdebug";
    }

    @Override
    public boolean OPOnly() {
        return false;
    }

    @Override
    public void handleCommand(String command, String playername, String[] args, WCommandSender listener) {

    }

    @Override
    public void printHelp(WCommandSender listener) {
        listener.chatT("command.ccdebug");
    }

    @Override
    public int minimumParameters() {
        return 0;
    }
}
