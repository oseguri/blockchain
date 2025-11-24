package blockchain.cli.command;

public class PeersCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("   Node not started. Use 'start' command first.");
            return;
        }

        try {

            int peerCount = context.getNode().getP2PNetwork().getPeerCount();

            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║          CONNECTED PEERS               ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println("   Total Peers: " + peerCount);
            System.out.println();

            if (peerCount == 0) {
                System.out.println("   No peers connected");
                System.out.println("   Use 'connect <host> <port>' to add peers");
            } else {
                System.out.println("   " + peerCount + " peer(s) connected");
                System.out.println("   (Detailed peer list not implemented yet)");
            }

            System.out.println();

        } catch (Exception e) {
            System.out.println("   Error fetching peers: " + e.getMessage());
        }
    }

    @Override
    public String getHelp() {
        return "peers - Display connected peers";
    }

    @Override
    public String getName() {
        return "peers";
    }
}
