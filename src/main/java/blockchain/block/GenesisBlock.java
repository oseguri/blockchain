package blockchain.block;

import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;
import util.BytesUtil;


import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 제네시스 블록 - 하드코딩된 네트워크의 첫 번째 블록
 * 모든 노드가 동일한 제네시스 블록을 공유
 */
public class GenesisBlock {

    // 제네시스 블록 상수 (하드코딩)
    private static final byte[] GENESIS_PREV_HASH = new byte[32]; // 모두 0
    private static final byte[] GENESIS_VERSION = BytesUtil.intToBytes(1, 4);
    private static final String GENESIS_MESSAGE = "Genesis Block - MyBlockchain 2025-11-19";
    private static final long GENESIS_TIMESTAMP = 1731960000L; // 고정된 타임스탬프
    private static final String GENESIS_MINER = "18tK1onRJBkx3eZyqjjxsBT7EYUFjh64PV";
    private static final long GENESIS_REWARD = 500L; // 500 코인

    // 제네시스 블록 캐시 (싱글톤)
    private static Block cachedGenesisBlock = null;

    /**
     * 제네시스 블록 조회 (항상 동일한 블록 반환)
     * @return 제네시스 블록
     */
    public static Block getGenesisBlock(){
        if (cachedGenesisBlock == null) {
            try {
                cachedGenesisBlock = createGenesisBlock();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to create genesis block", e);
            }
        }

        return cachedGenesisBlock;
    }

    /**
     * 제네시스 블록 생성 (내부적으로만 사용)
     * @return 제네시스 블록
     */
    static Block createGenesisBlock() throws NoSuchAlgorithmException {
        // 코인베이스 트랜잭션 생성
        Transaction coinbaseTx = createCoinbaseTransaction();

        // 트랜잭션 리스트
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(coinbaseTx);

        // 제네시스 블록 생성 (타임스탬프 고정)

        return new Block(
                GENESIS_PREV_HASH,
                transactions,
                GENESIS_VERSION,
                GENESIS_TIMESTAMP  // 고정된 타임스탬프 사용
        );
    }

    /**
     * 제네시스 블록용 코인베이스 트랜잭션 생성
     */
    private static Transaction createCoinbaseTransaction() throws NoSuchAlgorithmException {
        // 코인베이스 입력
        TransactionInput coinbaseInput = new TransactionInput(
                true,                           // isCoinbase = true
                "coinbase",                     // address
                0L,                             // value
                new byte[0],                    // pkScript
                createGenesisSignature(),       // sigScript (제네시스 메시지)
                -1,                             // outputIdx (-1은 코인베이스)
                new byte[32]                    // prevTXID (모두 0)
        );

        List<TransactionInput> inputs = new ArrayList<>();
        inputs.add(coinbaseInput);

        // 출력 (고정된 주소로 보상)
        TransactionOutput output = new TransactionOutput(
                GENESIS_MINER.getBytes(),       // 고정된 주소
                new byte[0],                    // pkscript
                GENESIS_REWARD                  // 고정된 보상
        );

        List<TransactionOutput> outputs = new ArrayList<>();
        outputs.add(output);

        // 트랜잭션 생성
        Transaction coinbaseTx = new Transaction(GENESIS_MESSAGE, inputs, outputs);

        // 명시적으로 voteList 초기화 확인
        if (coinbaseTx.getVoteList() == null) {
            coinbaseTx.setVoteList(new ArrayList<>());
        }

        return coinbaseTx;
    }

    /**
     * 제네시스 블록 서명 스크립트
     */
    private static List<byte[]> createGenesisSignature() {
        List<byte[]> sigScript = new ArrayList<>();
        sigScript.add(GENESIS_MESSAGE.getBytes());
        sigScript.add(new byte[0]);
        return sigScript;
    }

    /**
     * 제네시스 블록 검증
     */
    public static boolean isGenesisBlock(Block block) {
        if (block == null) {
            return false;
        }

        // 제네시스 블록과 해시 비교
        Block genesis = getGenesisBlock();
        return java.util.Arrays.equals(block.getBlockHash(), genesis.getBlockHash());
    }
}

