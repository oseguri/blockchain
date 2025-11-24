package blockchain.node.mine;

import blockchain.block.Block;
import blockchain.node.Node;
import blockchain.transaction.Mempool;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * 마이너 클래스
 * 블록 생성, 마이닝, 보상 처리
 */
public class Miner {

    private final Node node;
    private final Mempool mempool;
    private int currentDifficulty;
    private static final long BLOCK_REWARD = 5000000000L;
    private static final byte[] BLOCK_VERSION = new byte[]{0, 0, 0, 1};

    public Miner(Node node, Mempool mempool, int initialDifficulty) {
        this.node = node;
        this.mempool = mempool;
        this.currentDifficulty = initialDifficulty;
    }

    /**
     * 새 블록 마이닝 (Mempool에서 트랜잭션 선택)
     * @param maxTransactions 블록에 포함할 최대 트랜잭션 수
     * @return 마이닝된 블록
     */
    public Block mineNewBlock(long maxTransactions) throws Exception {
        // Mempool에서 수수료가 높은 트랜잭션 선택
        List<Transaction> selectedTxs = mempool.getTopTransactionsByFee(maxTransactions);

        System.out.println("\n=== Preparing Block ===");
        System.out.println("Selected " + selectedTxs.size() + " transactions from mempool");

        // 코인베이스 트랜잭션 생성
        Transaction coinbaseTx = createCoinbaseTransaction(selectedTxs);

        // 트랜잭션 리스트 (코인베이스 + 선택된 트랜잭션)
        List<Transaction> allTransactions = new ArrayList<>();
        allTransactions.add(coinbaseTx);
        allTransactions.addAll(selectedTxs);

        // 이전 블록 해시
        Block prevBlock = node.getLatestBlock();
        byte[] prevHash = prevBlock.getBlockHash();

        // 새 블록 생성
        Block newBlock = new Block(prevHash, allTransactions, BLOCK_VERSION);

        // 난이도 조정
        int chainLength = node.getChainLength();
        currentDifficulty = ProofOfWork.adjustDifficulty(
                node.getBlockList(),
                chainLength,
                currentDifficulty
        );

        // 작업 증명 (마이닝)
        boolean success = ProofOfWork.mineBlock(newBlock, currentDifficulty);

        if (success) {
            // 블록체인에 추가
            node.addBlock(newBlock);

            // Mempool에서 포함된 트랜잭션 제거
            List<String> txidsToRemove = new ArrayList<>();
            for (Transaction tx : selectedTxs) {
                txidsToRemove.add(bytesToHex(tx.getTxid()));
            }
            mempool.removeTransactions(txidsToRemove);

            return newBlock;
        }

        return null;
    }

    /**
     * 코인베이스 트랜잭션 생성 (채굴 보상 + 수수료)
     * @param transactions 블록에 포함된 트랜잭션들
     */
    private Transaction createCoinbaseTransaction(List<Transaction> transactions) throws Exception {
        // 총 수수료 계산
        long totalFees = transactions.stream()
                .mapToLong(Transaction::getFee)
                .sum();

        long totalReward = BLOCK_REWARD + totalFees;

        String minerAddress = node.getAddress();

        TransactionInput coinbaseInput = new TransactionInput(
                true,
                "coinbase",
                0L,
                new byte[0],
                createCoinbaseSignature(),
                -1,
                new byte[32]
        );

        List<TransactionInput> inputs = new ArrayList<>();
        inputs.add(coinbaseInput);

        TransactionOutput output = new TransactionOutput(
                minerAddress.getBytes(),
                new byte[0],
                totalReward
        );

        List<TransactionOutput> outputs = new ArrayList<>();
        outputs.add(output);

        System.out.println("Block Reward: " + BLOCK_REWARD + " guri");
        System.out.println("Transaction Fees: " + totalFees + " guri");
        System.out.println("Total Reward: " + totalReward + " guri");

        // 트랜잭션 생성
        Transaction coinbaseTx = new Transaction(inputs, outputs);

        // 명시적으로 voteList 초기화 확인
        if (coinbaseTx.getVoteList() == null) {
            coinbaseTx.setVoteList(new ArrayList<>());
        }

        return coinbaseTx;
    }

    private List<byte[]> createCoinbaseSignature() {
        List<byte[]> sigScript = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        String message = "Mined at " + timestamp;
        sigScript.add(message.getBytes());
        sigScript.add(new byte[0]);
        return sigScript;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
