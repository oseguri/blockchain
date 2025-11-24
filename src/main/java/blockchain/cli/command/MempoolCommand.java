package blockchain.cli.command;

import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionOutput;

import java.util.List;

public class MempoolCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("  Node not started. Use 'start' command first.");
            return;
        }

        List<Transaction> pending = context.getMempool().getAllTransactions();

        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║          MEMPOOL STATUS                ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("  Pending Transactions: " + pending.size());
        System.out.println();

        if (pending.isEmpty()) {
            System.out.println("   (empty)");
        } else {
            for (int i = 0; i < pending.size(); i++) {
                Transaction tx = pending.get(i);
                System.out.println("   [" + (i + 1) + "] TX ID: " + bytesToHex(tx.getTxid()).substring(0, 16) + "...");
                System.out.println("       Inputs: " + tx.getInputs().size() + ", Outputs: " + tx.getOutputs().size());

                // 총 전송 금액 계산
                long totalOut = tx.getOutputs().stream()
                        .mapToLong(TransactionOutput::getValue)
                        .sum();
                System.out.println("       Total: " + totalOut + " satoshi");
                System.out.println();
            }
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
        return "mempool - Display pending transactions";
    }

    @Override
    public String getName() {
        return "mempool";
    }
}
