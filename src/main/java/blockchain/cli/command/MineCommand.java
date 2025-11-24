package blockchain.cli.command;

import blockchain.block.Block;
import blockchain.node.Node;
import blockchain.node.mine.Miner;
import blockchain.transaction.Mempool;

import java.util.Arrays;

public class MineCommand implements Command{
    @Override
    public void execute(CommandContext context, String[] args) {
        if (!context.isNodeInitialized()) {
            System.out.println("   Node not started. Use 'start' command first.");
            return;
        }

        Node node = context.getNode();
        Mempool mempool = context.getMempool();

        try {
            // 난이도 설정 (기본값: 3)
            int difficulty = 3;
            if (args.length > 0) {
                difficulty = Integer.parseInt(args[0]);
                if (difficulty < 1 || difficulty > 6) {
                    System.out.println("   Difficulty should be between 1 and 6. Using default: 3");
                    difficulty = 3;
                }
            }

            System.out.println("   Mining new block...");
            System.out.println("   Difficulty: " + difficulty + " (target: " + "0".repeat(difficulty) + "...)");
            System.out.println("   Pending transactions: " + mempool.getAllTransactions().size());

            // 채굴 시작 시간
            long startTime = System.currentTimeMillis();

            // Miner 생성 및 채굴
            Miner miner = new Miner(node, mempool, difficulty);
            long reward = 50_00000000L; // 50 BTC (satoshi 단위)
            Block minedBlock = miner.mineNewBlock(reward);

            long endTime = System.currentTimeMillis();
            double elapsed = (endTime - startTime) / 1000.0;

            if (minedBlock != null) {
                // 블록 추가 (브로드캐스트 포함)
                boolean added = node.addBlock(minedBlock, true);

                if (added) {
                    System.out.println("   Block mined successfully!");
                    System.out.println("   Block Hash: " + bytesToHex(minedBlock.getBlockHash()));
                    System.out.println("   Nonce: " + Arrays.toString(minedBlock.getNonce()));
                    System.out.println("   Transactions: " + minedBlock.getTransactions().size());
                    System.out.println("   Mining time: " + String.format("%.2f", elapsed) + " seconds");
                    System.out.println("   Reward: " + reward + " satoshi");
                    System.out.println("   New balance: " + node.getBalance(node.getAddress()) + " satoshi");

                    // Mempool 정리
                    mempool.removeTransactions(minedBlock.getTransactionTxidList());
                    System.out.println("   Mempool cleared: " + minedBlock.getTransactions().size() + " transactions");
                } else {
                    System.out.println("   Failed to add mined block to chain");
                }
            } else {
                System.out.println("   Mining failed");
            }

        } catch (NumberFormatException e) {
            System.out.println("   Invalid difficulty: " + args[0]);
        } catch (Exception e) {
            System.out.println("   Mining error: " + e.getMessage());
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
        return "mine [difficulty] - Mine a new block (default difficulty: 3)";
    }

    @Override
    public String getName() {
        return "mine";
    }
}
