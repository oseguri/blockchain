package blockchain.cli.command;

import blockchain.block.Block;
import blockchain.node.Node;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BlockCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("   Node not started. Use 'start' command first.");
            return;
        }

        if (args.length < 1) {
            System.out.println("Usage: block <height>");
            return;
        }

        Node node = context.getNode();
        List<Block> chain = node.getBlockList();

        try {
            int height = Integer.parseInt(args[0]);

            if (height < 0 || height >= chain.size()) {
                System.out.println("   Invalid block height. Chain height: " + (chain.size() - 1));
                return;
            }

            Block block = chain.get(height);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.println("║          BLOCK #" + height + " DETAILS                        ║");
            System.out.println("╚════════════════════════════════════════════════════╝\n");

            System.out.println("   Block Information:");
            System.out.println("   Hash:       " + bytesToHex(block.getBlockHash()));
            System.out.println("   Prev Hash:  " + bytesToHex(block.getPrevHash()));
            System.out.println("   Merkle:     " + bytesToHex(block.getMerkleRoot()));
            System.out.println("   Timestamp:  " + sdf.format(new Date(block.getTimestamp())));
            System.out.println("   Nonce:      " + Arrays.toString(block.getNonce()));
            System.out.println("   TX Count:   " + block.getTransactions().size());
            System.out.println();

            System.out.println("   Transactions:");
            List<Transaction> txs = block.getTransactions();

            for (int i = 0; i < txs.size(); i++) {
                Transaction tx = txs.get(i);
                System.out.println("\n   [" + (i + 1) + "] TX ID: " + bytesToHex(tx.getTxid()));

                System.out.println("       Inputs (" + tx.getInputs().size() + "):");
                for (TransactionInput input : tx.getInputs()) {
                    System.out.println("         - UTXO: " + bytesToHex(input.getPrevTXID()).substring(0, 16) + "... [" + input.getValue() + "]");
                }

                System.out.println("       Outputs (" + tx.getOutputs().size() + "):");
                for (int j = 0; j < tx.getOutputs().size(); j++) {
                    TransactionOutput output = tx.getOutputs().get(j);
                    System.out.println("         [" + j + "] " + output.getValue() + " guri → " +
                            bytesToHex(output.getAddress()) + "...");
                }
            }

            System.out.println();

        } catch (NumberFormatException e) {
            System.out.println("   Invalid block height: " + args[0]);
        } catch (Exception e) {
            System.out.println("   Error fetching block: " + e.getMessage());
            e.printStackTrace();
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
        return "block <height> - Display detailed block information";
    }

    @Override
    public String getName() {
        return "block";
    }
}
