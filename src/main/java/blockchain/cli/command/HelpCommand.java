package blockchain.cli.command;

import java.util.Map;

public class HelpCommand implements Command{
    private final Map<String, Command> commands;

    public HelpCommand(Map<String, Command> commands) {
        this.commands = commands;
    }

    @Override
    public void execute(CommandContext context, String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║          BLOCKCHAIN CLI - HELP                     ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");

        System.out.println("  Available Commands:\n");

        commands.values().forEach(cmd -> System.out.printf("  %-15s %s%n", cmd.getName(), cmd.getHelp()));

        System.out.println();
    }

    @Override
    public String getHelp() {
        return "help - Display this help message";
    }

    @Override
    public String getName() {
        return "help";
    }
}
