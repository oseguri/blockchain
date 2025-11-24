package blockchain.cli.command;

public class ExitCommand implements Command{

    @Override
    public void execute(CommandContext context, String[] args) {
        System.out.println("  Shutting down blockchain node...");

        if (context.isNodeInitialized()) {
            try {
                context.getNode().shutdown();
                System.out.println("  Node stopped successfully!");
            } catch (Exception e) {
                System.out.println("  Error during shutdown: " + e.getMessage());
            }
        }

        context.stop();
        System.out.println("Goodbye!");
    }

    @Override
    public String getHelp() {
        return "exit - Shutdown node and exit";
    }

    @Override
    public String getName() {
        return "exit";
    }
}
