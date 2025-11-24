package blockchain.cli.command;

public class SyncCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("   Node not started. Use 'start' command first.");
            return;
        }

        try {
            System.out.println("   Requesting chain synchronization from peers...");

            boolean success = context.getNode().requestChainSync();

            if (success) {
                System.out.println("   Synchronization request sent!");
                System.out.println("   Waiting for peer responses...");
                System.out.println("   Check 'status' for updated chain height");
            } else {
                System.out.println("   No peers connected to sync with");
                System.out.println("   Use 'connect' to add peers first");
            }

        } catch (Exception e) {
            System.out.println("   Sync error: " + e.getMessage());
        }
    }

    @Override
    public String getHelp() {
        return "sync - Request blockchain synchronization from peers";
    }

    @Override
    public String getName() {
        return "sync";
    }
}
