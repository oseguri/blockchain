package blockchain.clitest;

import blockchain.node.Node;
import blockchain.cli.command.*;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CLI 통합 테스트
 * P2P 네트워크를 사용하여 실제 블록체인 동작 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CLIIntegrationTest {

    private static CommandContext context1;
    private static CommandContext context2;
    private static CommandContext context3;

    private static Node node1;
    private static Node node2;
    private static Node node3;

    private static final String STORAGE_PATH_1 = "./admin";
    private static final String STORAGE_PATH_2 = "./test_cli_node2";
    private static final String STORAGE_PATH_3 = "./test_cli_node3";

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeAll
    static void setupAll() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║        CLI INTEGRATION TEST START                  ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");

        // 이전 테스트 데이터 정리
        cleanupData();
    }

    @AfterAll
    static void teardownAll() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║        CLI INTEGRATION TEST COMPLETE               ║");
        System.out.println("╚════════════════════════════════════════════════════╝\n");

        // 테스트 후 정리
        if (node1 != null) node1.shutdown();
        if (node2 != null) node2.shutdown();
        if (node3 != null) node3.shutdown();

        cleanupData();
    }

    @BeforeEach
    void setup() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void cleanup() {
        System.setOut(originalOut);
    }

    private static void cleanupData() {
        deleteDirectory(new File(STORAGE_PATH_2));
        deleteDirectory(new File(STORAGE_PATH_3));
    }

    private static void deleteDirectory(File directory) {
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
            directory.delete();
        }
    }

    // ====================================
    // Test 1: Start Command
    // ====================================

    @Test
    @Order(1)
    @DisplayName("1. Start Command - 3개 노드 시작")
    void testStartCommand() throws Exception {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 1: Start Command");
        System.out.println("═══════════════════════════════════════\n");

        // Context 생성
        context1 = new CommandContext();
        context2 = new CommandContext();
        context3 = new CommandContext();

        // Node 1 시작
        StartCommand startCmd1 = new StartCommand();
        startCmd1.execute(context1, new String[]{"7001", STORAGE_PATH_1});

        assertNotNull(context1.getNode(), "Node 1 should be initialized");
        assertTrue(context1.isNodeInitialized(), "Node 1 should be running");

        node1 = context1.getNode();
        System.out.println("✓ Node 1 started: " + node1.getAddress());

        // Node 2 시작
        StartCommand startCmd2 = new StartCommand();
        startCmd2.execute(context2, new String[]{"7002", STORAGE_PATH_2});

        assertNotNull(context2.getNode(), "Node 2 should be initialized");
        node2 = context2.getNode();
        System.out.println("✓ Node 2 started: " + node2.getAddress());

        // Node 3 시작
        StartCommand startCmd3 = new StartCommand();
        startCmd3.execute(context3, new String[]{"7003", STORAGE_PATH_3});

        assertNotNull(context3.getNode(), "Node 3 should be initialized");
        node3 = context3.getNode();
        System.out.println("✓ Node 3 started: " + node3.getAddress());

        Thread.sleep(1000);
    }

    // ====================================
    // Test 2: Connect Command
    // ====================================

    @Test
    @Order(2)
    @DisplayName("2. Connect Command - P2P 네트워크 연결")
    void testConnectCommand() throws Exception {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 2: Connect Command");
        System.out.println("═══════════════════════════════════════\n");

        ConnectCommand connectCmd = new ConnectCommand();

        // Node1 -> Node2 연결
        System.out.println("Connecting Node1 -> Node2...");
        connectCmd.execute(context1, new String[]{"localhost", "7002"});
        Thread.sleep(1000);

        // Node2 -> Node3 연결
        System.out.println("Connecting Node2 -> Node3...");
        connectCmd.execute(context2, new String[]{"localhost", "7003"});
        Thread.sleep(1000);

        // Node3 -> Node1 연결 (Ring topology)
        System.out.println("Connecting Node3 -> Node1...");
        connectCmd.execute(context3, new String[]{"localhost", "7001"});
        Thread.sleep(1000);

        assertEquals(2, node1.getP2PNetwork().getPeerCount(), "Node1 should have 1 peer");
        assertEquals(2, node2.getP2PNetwork().getPeerCount(), "Node2 should have 1 peer");
        assertEquals(2, node3.getP2PNetwork().getPeerCount(), "Node3 should have 1 peer");

        System.out.println("✓ All nodes connected in ring topology");
    }

    // ====================================
    // Test 3: Status Command
    // ====================================

    @Test
    @Order(3)
    @DisplayName("3. Status Command - 노드 상태 확인")
    void testStatusCommand() {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 3: Status Command");
        System.out.println("═══════════════════════════════════════\n");

        StatusCommand statusCmd = new StatusCommand();

        System.out.println("--- Node 1 Status ---");
        statusCmd.execute(context1, new String[]{});

        System.out.println("--- Node 2 Status ---");
        statusCmd.execute(context2, new String[]{});

        System.out.println("--- Node 3 Status ---");
        statusCmd.execute(context3, new String[]{});

        assertEquals(1, node1.getBlockList().size(), "Should have genesis block");
    }

    // ====================================
    // Test 4: Balance Command
    // ====================================

    @Test
    @Order(4)
    @DisplayName("4. Balance Command - 초기 잔액 확인")
    void testBalanceCommand() throws Exception {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 4: Balance Command");
        System.out.println("═══════════════════════════════════════\n");

        BalanceCommand balanceCmd = new BalanceCommand();

        System.out.println("--- Node 1 Balance ---");
        balanceCmd.execute(context1, new String[]{});
        long balance1 = node1.getBalance(node1.getAddress());
        System.out.println("Balance: " + balance1);

        System.out.println("\n--- Node 2 Balance ---");
        balanceCmd.execute(context2, new String[]{});
        long balance2 = node2.getBalance(node2.getAddress());
        System.out.println("Balance: " + balance2);

        // Genesis block에서 50 guri를 받았을 것
        assertTrue(balance1 > 0 || balance2 > 0, "At least one node should have balance from genesis");
    }

    // ====================================
    // Test 5: Mine Command
    // ====================================

    @Test
    @Order(5)
    @DisplayName("5. Mine Command - 블록 채굴")
    void testMineCommand() throws Exception {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 5: Mine Command");
        System.out.println("═══════════════════════════════════════\n");

        MineCommand mineCmd = new MineCommand();

        // Node 1이 블록 채굴
        System.out.println("Node 1 mining block...");
        mineCmd.execute(context1, new String[]{"3"}); // difficulty 3
        Thread.sleep(2000); // 브로드캐스트 대기

        // 모든 노드의 체인 길이 확인
        assertEquals(2, node1.getBlockList().size(), "Node1 should have 2 blocks");
        assertEquals(2, node2.getBlockList().size(), "Node2 should have 2 blocks (synced)");
        assertEquals(2, node3.getBlockList().size(), "Node3 should have 2 blocks (synced)");

        System.out.println("✓ Block mined and synced across all nodes");

        // Node 2도 채굴
        System.out.println("\nNode 2 mining block...");
        mineCmd.execute(context2, new String[]{"3"});
        Thread.sleep(2000);

        assertEquals(3, node1.getBlockList().size(), "All nodes should have 3 blocks");
        assertEquals(3, node2.getBlockList().size(), "All nodes should have 3 blocks");
        assertEquals(3, node3.getBlockList().size(), "All nodes should have 3 blocks");

        System.out.println("✓ Second block mined and synced");
    }

    // ====================================
    // Test 6: List Command
    // ====================================

    @Test
    @Order(6)
    @DisplayName("6. List Command - 블록 리스트 조회")
    void testListCommand() {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 6: List Command");
        System.out.println("═══════════════════════════════════════\n");

        ListCommand listCmd = new ListCommand();

        System.out.println("--- Node 1 Blockchain ---");
        listCmd.execute(context1, new String[]{});

        System.out.println("\n--- Listing only last 2 blocks ---");
        listCmd.execute(context1, new String[]{"0", "2"});
    }

    // ====================================
    // Test 7: Block Command
    // ====================================

    @Test
    @Order(7)
    @DisplayName("7. Block Command - 블록 상세 조회")
    void testBlockCommand() {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 7: Block Command");
        System.out.println("═══════════════════════════════════════\n");

        BlockCommand blockCmd = new BlockCommand();

        System.out.println("--- Genesis Block (0) ---");
        blockCmd.execute(context1, new String[]{"0"});

        System.out.println("\n--- Block 1 ---");
        blockCmd.execute(context1, new String[]{"1"});
    }

    // ====================================
    // Test 8: Send Command
    // ====================================

    @Test
    @Order(8)
    @DisplayName("8. Send Command - 트랜잭션 전송")
    void testSendCommand() throws Exception {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 8: Send Command");
        System.out.println("═══════════════════════════════════════\n");

        SendCommand sendCmd = new SendCommand();

        // Node1의 초기 잔액 확인
        long node1BalanceBefore = node1.getBalance(node1.getAddress());
        long node2BalanceBefore = node2.getBalance(node2.getAddress());

        System.out.println("Node1 balance before: " + node1BalanceBefore);
        System.out.println("Node2 balance before: " + node2BalanceBefore);

        if (node1BalanceBefore > 1000000) {
            // Node1 -> Node2로 송금 (1,000,000 satoshi = 0.01 BTC)
            System.out.println("\nSending 1,000,000 satoshi from Node1 to Node2...");
            sendCmd.execute(context1, new String[]{node2.getAddress(), "1000000"});

            Thread.sleep(1000);

            // Mempool 확인
            MempoolCommand mempoolCmd = new MempoolCommand();
            System.out.println("\n--- Mempool Status ---");
            mempoolCmd.execute(context1, new String[]{});

            assertFalse(context1.getMempool().getAllTransactions().isEmpty(), "Transaction should be in mempool");

            // 블록 채굴하여 트랜잭션 확정
            System.out.println("\nMining block to confirm transaction...");
            MineCommand mineCmd = new MineCommand();
            mineCmd.execute(context1, new String[]{"3"});
            Thread.sleep(2000);

            // 잔액 변화 확인
            long node2BalanceAfter = node2.getBalance(node2.getAddress());
            System.out.println("\nNode2 balance after: " + node2BalanceAfter);

            assertTrue(node2BalanceAfter > node2BalanceBefore,
                    "Node2 balance should increase");

            System.out.println("✓ Transaction confirmed and balance updated");
        } else {
            System.out.println("⚠️ Node1 doesn't have enough balance for test");
        }
    }

    // ====================================
    // Test 9: Mempool Command
    // ====================================

    @Test
    @Order(9)
    @DisplayName("9. Mempool Command - Mempool 상태 확인")
    void testMempoolCommand() {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 9: Mempool Command");
        System.out.println("═══════════════════════════════════════\n");

        MempoolCommand mempoolCmd = new MempoolCommand();

        System.out.println("--- Node 1 Mempool ---");
        mempoolCmd.execute(context1, new String[]{});

        System.out.println("\n--- Node 2 Mempool ---");
        mempoolCmd.execute(context2, new String[]{});
    }

    // ====================================
    // Test 10: Peers Command
    // ====================================

    @Test
    @Order(10)
    @DisplayName("10. Peers Command - 피어 목록 확인")
    void testPeersCommand() {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 10: Peers Command");
        System.out.println("═══════════════════════════════════════\n");

        PeersCommand peersCmd = new PeersCommand();

        System.out.println("--- Node 1 Peers ---");
        peersCmd.execute(context1, new String[]{});

        System.out.println("\n--- Node 2 Peers ---");
        peersCmd.execute(context2, new String[]{});
    }

    // ====================================
    // Test 11: Sync Command
    // ====================================

    @Test
    @Order(11)
    @DisplayName("11. Sync Command - 체인 동기화")
    void testSyncCommand() throws Exception {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 11: Sync Command");
        System.out.println("═══════════════════════════════════════\n");

        SyncCommand syncCmd = new SyncCommand();

        System.out.println("Requesting sync from Node 1...");
        syncCmd.execute(context1, new String[]{});

        Thread.sleep(2000);

        System.out.println("✓ Sync request completed");
    }

    // ====================================
    // Test 12: Chain Consistency
    // ====================================

    @Test
    @Order(12)
    @DisplayName("12. Chain Consistency - 체인 일관성 검증")
    void testChainConsistency() {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 12: Chain Consistency");
        System.out.println("═══════════════════════════════════════\n");

        int chainLength1 = node1.getBlockList().size();
        int chainLength2 = node2.getBlockList().size();
        int chainLength3 = node3.getBlockList().size();

        System.out.println("Node1 chain length: " + chainLength1);
        System.out.println("Node2 chain length: " + chainLength2);
        System.out.println("Node3 chain length: " + chainLength3);

        assertEquals(chainLength1, chainLength2, "Node1 and Node2 should have same chain length");
        assertEquals(chainLength2, chainLength3, "Node2 and Node3 should have same chain length");

        // 마지막 블록 해시 비교
        byte[] lastHash1 = node1.getLatestBlock().getBlockHash();
        byte[] lastHash2 = node2.getLatestBlock().getBlockHash();
        byte[] lastHash3 = node3.getLatestBlock().getBlockHash();

        assertArrayEquals(lastHash1, lastHash2, "Node1 and Node2 should have same last block");
        assertArrayEquals(lastHash2, lastHash3, "Node2 and Node3 should have same last block");

        System.out.println("✓ All nodes have consistent blockchain");
    }

    // ====================================
    // Test 13: Help Command
    // ====================================

    @Test
    @Order(13)
    @DisplayName("13. Help Command - 도움말 표시")
    void testHelpCommand() {
        System.setOut(originalOut);
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("TEST 13: Help Command");
        System.out.println("═══════════════════════════════════════\n");

        // 임시 명령어 맵 생성
        java.util.Map<String, Command> commands = new java.util.HashMap<>();
        commands.put("help", new HelpCommand(commands));
        commands.put("start", new StartCommand());
        commands.put("status", new StatusCommand());

        HelpCommand helpCmd = new HelpCommand(commands);
        helpCmd.execute(context1, new String[]{});
    }
}
