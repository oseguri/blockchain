package blockchain;

import blockchain.block.Block;
import blockchain.node.Node;
import blockchain.node.mine.Miner;
import blockchain.transaction.Mempool;
import org.junit.jupiter.api.Test;

import java.io.File;

public class MainTest {
    @Test
    public void e2eTest() {
        try {
            System.out.println("\n╔════════════════════════════════════════════════════╗");
            System.out.println("║           P2P Network Test                         ║");
            System.out.println("║   3 Nodes connected via P2P                        ║");
            System.out.println("╚════════════════════════════════════════════════════╝\n");

            // Cleanup
            cleanupData();

            // ====================================
            // Phase 1: Create 3 Nodes
            // ====================================
            System.out.println("═══════════════════════════════════════");
            System.out.println("PHASE 1: Creating 3 Nodes");
            System.out.println("═══════════════════════════════════════\n");

            Node node1 = new Node("./p2p_node1", 7001);
            Node node2 = new Node("./p2p_node2", 7002);
            Node node3 = new Node("./p2p_node3", 7003);

            System.out.println("\nNode 1: " + node1.getAddress() + " (Port 7001)");
            System.out.println("Node 2: " + node2.getAddress() + " (Port 7002)");
            System.out.println("Node 3: " + node3.getAddress() + " (Port 7003)");

            // ====================================
            // Phase 2: Start P2P Networks
            // ====================================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("PHASE 2: Starting P2P Networks");
            System.out.println("═══════════════════════════════════════\n");

            node1.startP2P();
            Thread.sleep(500);

            node2.startP2P();
            Thread.sleep(500);

            node3.startP2P();
            Thread.sleep(500);

            // ====================================
            // Phase 3: Connect Nodes (Ring Topology)
            // ====================================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("PHASE 3: Connecting Peers");
            System.out.println("═══════════════════════════════════════\n");

            System.out.println("[Node 1 → Node 2]");
            node1.connectToPeer("localhost", 7002);
            Thread.sleep(500);

            System.out.println("\n[Node 2 → Node 3]");
            node2.connectToPeer("localhost", 7003);
            Thread.sleep(500);

            System.out.println("\n[Node 3 → Node 1]");
            node3.connectToPeer("localhost", 7001);
            Thread.sleep(1000);

            System.out.println("\n✓ All nodes connected in a ring");
            System.out.println("Node 1 peers: " + node1.getP2PNetwork().getPeerCount());
            System.out.println("Node 2 peers: " + node2.getP2PNetwork().getPeerCount());
            System.out.println("Node 3 peers: " + node3.getP2PNetwork().getPeerCount());

            // ====================================
            // Phase 4: Node 1 Mines a Block
            // ====================================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("PHASE 4: Node 1 Mines Block");
            System.out.println("═══════════════════════════════════════\n");

            Mempool mempool1 = new Mempool(node1.getValidator());
            Miner miner1 = new Miner(node1, mempool1, 3);

            System.out.println("Node 1 mining block...");
            Block block1 = miner1.mineNewBlock(0);

            if (block1 != null) {
                System.out.println("✓ Node 1 mined block!");
                System.out.println("Block Hash: " + bytesToHex(block1.getBlockHash()).substring(0, 16) + "...");

                // Wait for propagation
                Thread.sleep(2000);

                // Check all nodes
                System.out.println("\n--- Chain Lengths After Block 1 ---");
                System.out.println("Node 1: " + node1.getChainLength() + " blocks");
                System.out.println("Node 2: " + node2.getChainLength() + " blocks");
                System.out.println("Node 3: " + node3.getChainLength() + " blocks");
            }

            // ====================================
            // Phase 5: Node 2 Mines a Block
            // ====================================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("PHASE 5: Node 2 Mines Block");
            System.out.println("═══════════════════════════════════════\n");

            Mempool mempool2 = new Mempool(node2.getValidator());
            Miner miner2 = new Miner(node2, mempool2, 3);

            System.out.println("Node 2 mining block...");
            Block block2 = miner2.mineNewBlock(0);

            if (block2 != null) {
                System.out.println("✓ Node 2 mined block!");
                System.out.println("Block Hash: " + bytesToHex(block2.getBlockHash()).substring(0, 16) + "...");

                Thread.sleep(2000);

                System.out.println("\n--- Chain Lengths After Block 2 ---");
                System.out.println("Node 1: " + node1.getChainLength() + " blocks");
                System.out.println("Node 2: " + node2.getChainLength() + " blocks");
                System.out.println("Node 3: " + node3.getChainLength() + " blocks");
            }

            // ====================================
            // Phase 6: Node 3 Mines a Block
            // ====================================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("PHASE 6: Node 3 Mines Block");
            System.out.println("═══════════════════════════════════════\n");

            Mempool mempool3 = new Mempool(node3.getValidator());
            Miner miner3 = new Miner(node3, mempool3, 3);

            System.out.println("Node 3 mining block...");
            Block block3 = miner3.mineNewBlock(0);

            if (block3 != null) {
                System.out.println("✓ Node 3 mined block!");
                System.out.println("Block Hash: " + bytesToHex(block3.getBlockHash()).substring(0, 16) + "...");

                Thread.sleep(2000);

                System.out.println("\n--- Chain Lengths After Block 3 ---");
                System.out.println("Node 1: " + node1.getChainLength() + " blocks");
                System.out.println("Node 2: " + node2.getChainLength() + " blocks");
                System.out.println("Node 3: " + node3.getChainLength() + " blocks");
            }

            // ====================================
            // Phase 7: Verify Synchronization
            // ====================================
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("PHASE 7: Verification");
            System.out.println("═══════════════════════════════════════\n");

            int len1 = node1.getChainLength();
            int len2 = node2.getChainLength();
            int len3 = node3.getChainLength();

            System.out.println("Final Chain Lengths:");
            System.out.println("  Node 1: " + len1);
            System.out.println("  Node 2: " + len2);
            System.out.println("  Node 3: " + len3);

            boolean allSynced = (len1 == len2) && (len2 == len3) && (len1 == 4); // Genesis + 3 mined

            // Verify same blocks
            boolean sameBlocks = true;
            if (allSynced) {
                for (int i = 0; i < len1; i++) {
                    byte[] hash1 = node1.getBlockAtHeight(i).getBlockHash();
                    byte[] hash2 = node2.getBlockAtHeight(i).getBlockHash();
                    byte[] hash3 = node3.getBlockAtHeight(i).getBlockHash();

                    if (!java.util.Arrays.equals(hash1, hash2) ||
                            !java.util.Arrays.equals(hash2, hash3)) {
                        sameBlocks = false;
                        System.out.println("✗ Block " + i + " hash mismatch!");
                        break;
                    }
                }
            }

            System.out.println("\nTest Results:");
            System.out.println("1. Chain Length Sync: " + (allSynced ? "✓ PASS" : "✗ FAIL"));
            System.out.println("2. Block Hash Match:  " + (sameBlocks ? "✓ PASS" : "✗ FAIL"));

            // Print blockchains
            System.out.println("\n--- Node 1 Blockchain ---");
            printBlockchain(node1);

            System.out.println("\n--- Node 2 Blockchain ---");
            printBlockchain(node2);

            System.out.println("\n--- Node 3 Blockchain ---");
            printBlockchain(node3);

            // ====================================
            // Final Results
            // ====================================
            System.out.println("\n╔════════════════════════════════════════════════════╗");
            if (allSynced && sameBlocks) {
                System.out.println("║        ✅ P2P TEST PASSED                         ║");
                System.out.println("║   All nodes synchronized successfully              ║");
            } else {
                System.out.println("║        ❌ P2P TEST FAILED                         ║");
                System.out.println("║   Nodes are not synchronized                       ║");
            }
            System.out.println("╚════════════════════════════════════════════════════╝\n");

            // Cleanup
            System.out.println("Shutting down nodes...");
            node1.shutdown();
            node2.shutdown();
            node3.shutdown();

            System.out.println("✓ Test complete");

        } catch (Exception e) {
            System.err.println("\n❌ P2P test failed: " + e.getMessage());
            e.printStackTrace();
        }

        cleanupData();

    }
    private static void printBlockchain(Node node) {
        for (int i = 0; i < node.getChainLength(); i++) {
            Block block = node.getBlockAtHeight(i);
            String hash = bytesToHex(block.getBlockHash()).substring(0, 16) + "...";
            System.out.println("  Block " + i + ": " + hash);
        }
    }
    private static void cleanupData() {
        System.out.println("Cleaning up old data...");
        deleteDirectory(new File("./p2p_node1"));
        deleteDirectory(new File("./p2p_node2"));
        deleteDirectory(new File("./p2p_node3"));
        System.out.println("✓ Cleanup complete\n");
    }

    private static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
