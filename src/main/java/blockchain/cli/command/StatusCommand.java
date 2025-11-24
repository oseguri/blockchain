package blockchain.cli.command;

import blockchain.block.Block;
import blockchain.node.Node;

import java.util.List;

/**
 * 노드 상태 조회 명령
 */
public class StatusCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("  Node not started. Use 'start' command first.");
            return;
        }

        Node node = context.getNode();

        try {
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║          NODE STATUS                   ║");
            System.out.println("╚════════════════════════════════════════╝");

            System.out.println("  Address:    " + node.getAddress());
            System.out.println("  Balance:    " + node.getBalance(node.getAddress()) + "guri");

            List<Block> chain = node.getBlockList();
            System.out.println("⛓️  Chain:      " + (chain.size() - 1) + " blocks (height)");

            if (!chain.isEmpty()) {
                Block latest = chain.getLast();
                System.out.println("  Latest:     " + bytesToHex(latest.getBlockHash()).substring(0, 16) + "...");
            }

            System.out.println("  Peers:      " + node.getP2PNetwork().getPeerCount() + " connected");
            System.out.println("  Mempool:    " + context.getMempool().getAllTransactions().size() + " transactions");
            System.out.println();

        } catch (Exception e) {
            System.out.println("  Error fetching status: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public String getHelp() {
        return "status - Display node status";
    }

    @Override
    public String getName() {
        return "status";
    }
}
