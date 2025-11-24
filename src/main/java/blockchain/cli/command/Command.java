package blockchain.cli.command;

public interface Command {
    void execute(CommandContext context, String[] args);

    String getHelp();

    String getName();
}
