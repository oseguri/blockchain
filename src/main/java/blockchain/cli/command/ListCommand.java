package blockchain.cli.command;

import blockchain.block.Block;
import blockchain.node.Node;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ListCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("   Node not started. Use 'start' command first.");
            return;
        }

        Node node = context.getNode();
        List<Block> chain = node.getBlockList();

        try {
            int start = 0;
            int count = 10; // 기본값: 최근 10개

            if (args.length > 0) {
                start = Integer.parseInt(args[0]);
            }
            if (args.length > 1) {
                count = Integer.parseInt(args[1]);
            }

            // 최신 블록부터 표시하기 위해 역순
            int totalBlocks = chain.size();
            int fromIndex = Math.max(0, totalBlocks - start - count);
            int toIndex = Math.max(0, totalBlocks - start);

            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.println("║          BLOCKCHAIN (Height: " + (totalBlocks - 1) + ")                  ║");
            System.out.println("╚════════════════════════════════════════════════════╝\n");

            if (fromIndex >= toIndex) {
                System.out.println("   No blocks to display");
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (int i = toIndex - 1; i >= fromIndex; i--) {
                Block block = chain.get(i);

                System.out.println("   Block #" + i);
                System.out.println("   Hash:      " + bytesToHex(block.getBlockHash()).substring(0, 32) + "...");
                System.out.println("   Prev:      " + bytesToHex(block.getPrevHash()).substring(0, 32) + "...");
                System.out.println("   Time:      " + sdf.format(new Date(block.getTimestamp())));
                System.out.println("   Nonce:     " + Arrays.toString(block.getNonce()));
                System.out.println("   TX Count:  " + block.getTransactions().size());
                System.out.println();
            }

            System.out.println("Showing blocks " + fromIndex + " to " + (toIndex - 1) + " of " + (totalBlocks - 1));

        } catch (NumberFormatException e) {
            System.out.println("   Invalid number format");
        } catch (Exception e) {
            System.out.println("   Error listing blocks: " + e.getMessage());
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
        return "list [start] [count] - List blocks (recent 10 by default)";
    }

    @Override
    public String getName() {
        return "list";
    }
}
