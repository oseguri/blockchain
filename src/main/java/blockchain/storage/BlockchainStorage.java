package blockchain.storage;

import blockchain.block.Block;
import blockchain.transaction.Transaction;
import blockchain.utxo.UTXO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.rocksdb.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RocksDB 기반 블록체인 저장소
 * 블록, 트랜잭션, UTXO 등을 영구 저장
 */
public class BlockchainStorage {

    private RocksDB blocksDB;        // 블록 저장소
    private RocksDB txDB;            // 트랜잭션 저장소
    private RocksDB utxoDB;          // UTXO 저장소
    private RocksDB metaDB;          // 메타데이터 (체인 높이, tip 등)

    private final ObjectMapper objectMapper;
    private final String dbPath;

    // 메타데이터 키
    private static final String META_CHAIN_HEIGHT = "chain_height";
    private static final String META_BEST_BLOCK_HASH = "best_block_hash";

    static {
        RocksDB.loadLibrary();
    }

    public BlockchainStorage(String dbPath) throws RocksDBException {
        this.dbPath = dbPath;
        this.objectMapper = new ObjectMapper();

        // 데이터베이스 초기화
        initializeDatabases();

        System.out.println("✓ Blockchain storage initialized at: " + dbPath);
    }

    /**
     * 데이터베이스 초기화
     */
    private void initializeDatabases() throws RocksDBException {
        Options options = new Options()
                .setCreateIfMissing(true)
                .setCompressionType(CompressionType.LZ4_COMPRESSION);

        // 디렉토리 생성
        new File(dbPath).mkdirs();

        // 각 DB 열기
        blocksDB = RocksDB.open(options, dbPath + "/blocks");
        txDB = RocksDB.open(options, dbPath + "/transactions");
        utxoDB = RocksDB.open(options, dbPath + "/utxo");
        metaDB = RocksDB.open(options, dbPath + "/meta");

        System.out.println("  - Blocks DB: " + dbPath + "/blocks");
        System.out.println("  - Transactions DB: " + dbPath + "/transactions");
        System.out.println("  - UTXO DB: " + dbPath + "/utxo");
        System.out.println("  - Meta DB: " + dbPath + "/meta");
    }

    // ========== 블록 저장/조회 ==========

    /**
     * 블록 저장
     * @param block 저장할 블록
     */
    public void saveBlock(Block block) throws Exception {
        byte[] blockHash = block.getBlockHash();
        byte[] blockData = objectMapper.writeValueAsBytes(block);

        blocksDB.put(blockHash, blockData);

        System.out.println("Block saved: " + bytesToHex(blockHash).substring(0, 16) + "...");
    }

    /**
     * 블록 조회
     * @param blockHash 블록 해시
     * @return 블록
     */
    public Block getBlock(byte[] blockHash) throws Exception {
        byte[] blockData = blocksDB.get(blockHash);

        if (blockData == null) {
            return null;
        }

        return objectMapper.readValue(blockData, Block.class);
    }

    /**
     * 블록 존재 여부 확인
     */
    public boolean hasBlock(byte[] blockHash) throws RocksDBException {
        return blocksDB.get(blockHash) != null;
    }

    // ========== 트랜잭션 저장/조회 ==========

    /**
     * 트랜잭션 저장
     */
    public void saveTransaction(Transaction tx) throws Exception {
        byte[] txid = tx.getTxid();
        byte[] txData = objectMapper.writeValueAsBytes(tx);

        txDB.put(txid, txData);
    }

    /**
     * 트랜잭션 조회
     */
    public Transaction getTransaction(byte[] txid) throws Exception {
        byte[] txData = txDB.get(txid);

        if (txData == null) {
            return null;
        }

        return objectMapper.readValue(txData, Transaction.class);
    }

    /**
     * 블록의 모든 트랜잭션 저장
     */
    public void saveBlockTransactions(Block block) throws Exception {
        for (Transaction tx : block.getTransactions()) {
            saveTransaction(tx);
        }
    }

    // ========== UTXO 저장/조회 ==========

    /**
     * UTXO 저장
     */
    public void saveUTXO(UTXO utxo) throws Exception {
        String key = utxo.getKey(); // txid:index
        byte[] utxoData = objectMapper.writeValueAsBytes(utxo);

        utxoDB.put(key.getBytes(StandardCharsets.UTF_8), utxoData);
    }

    /**
     * UTXO 조회
     */
    public UTXO getUTXO(String key) throws Exception {
        byte[] utxoData = utxoDB.get(key.getBytes(StandardCharsets.UTF_8));

        if (utxoData == null) {
            return null;
        }

        return objectMapper.readValue(utxoData, UTXO.class);
    }

    /**
     * UTXO 삭제 (사용됨)
     */
    public void deleteUTXO(String key) throws RocksDBException {
        utxoDB.delete(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 모든 UTXO 조회
     */
    public List<UTXO> getAllUTXOs() throws Exception {
        List<UTXO> utxos = new ArrayList<>();

        try (RocksIterator iterator = utxoDB.newIterator()) {
            iterator.seekToFirst();

            while (iterator.isValid()) {
                byte[] utxoData = iterator.value();
                UTXO utxo = objectMapper.readValue(utxoData, UTXO.class);
                utxos.add(utxo);
                iterator.next();
            }
        }

        return utxos;
    }

    // ========== 메타데이터 저장/조회 ==========

    /**
     * 체인 높이 저장
     */
    public void setChainHeight(int height) throws RocksDBException {
        metaDB.put(
                META_CHAIN_HEIGHT.getBytes(StandardCharsets.UTF_8),
                String.valueOf(height).getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 체인 높이 조회
     */
    public int getChainHeight() throws RocksDBException {
        byte[] heightData = metaDB.get(META_CHAIN_HEIGHT.getBytes(StandardCharsets.UTF_8));

        if (heightData == null) {
            return 0;
        }

        return Integer.parseInt(new String(heightData, StandardCharsets.UTF_8));
    }

    /**
     * 최신 블록 해시 저장
     */
    public void setBestBlockHash(byte[] blockHash) throws RocksDBException {
        metaDB.put(
                META_BEST_BLOCK_HASH.getBytes(StandardCharsets.UTF_8),
                blockHash
        );
    }

    /**
     * 최신 블록 해시 조회
     */
    public byte[] getBestBlockHash() throws RocksDBException {
        return metaDB.get(META_BEST_BLOCK_HASH.getBytes(StandardCharsets.UTF_8));
    }

    // ========== 블록체인 복원 ==========

    /**
     * 전체 블록체인 로드
     */
    public List<Block> loadBlockchain() throws Exception {
        List<Block> blocks = new ArrayList<>();

        try (RocksIterator iterator = blocksDB.newIterator()) {
            iterator.seekToFirst();

            while (iterator.isValid()) {
                byte[] blockData = iterator.value();
                Block block = objectMapper.readValue(blockData, Block.class);
                blocks.add(block);
                iterator.next();
            }
        }

        // 블록 높이 순으로 정렬 (타임스탬프 기준)
        blocks.sort((b1, b2) -> Long.compare(b1.getTimestamp(), b2.getTimestamp()));

        System.out.println("Loaded " + blocks.size() + " blocks from storage");
        return blocks;
    }

    /**
     * UTXO Set 복원
     */
    public void restoreUTXOSet(blockchain.utxo.UTXOSet utxoSet) throws Exception {
        List<UTXO> utxos = getAllUTXOs();

        // UTXO Set에 로드 (UTXOSet에 직접 추가하는 메서드 필요)
        System.out.println("Loaded " + utxos.size() + " UTXOs from storage");
    }

    // ========== 데이터베이스 닫기 ==========

    /**
     * 모든 데이터베이스 닫기
     */
    public void close() {
        if (blocksDB != null) blocksDB.close();
        if (txDB != null) txDB.close();
        if (utxoDB != null) utxoDB.close();
        if (metaDB != null) metaDB.close();

        System.out.println("Blockchain storage closed");
    }

    /**
     * 스토리지 상태 출력
     */
    public void printStatus() throws Exception {
        System.out.println("\n=== Storage Status ===");
        System.out.println("DB Path: " + dbPath);
        System.out.println("Chain Height: " + getChainHeight());

        byte[] bestHash = getBestBlockHash();
        if (bestHash != null) {
            System.out.println("Best Block: " + bytesToHex(bestHash).substring(0, 16) + "...");
        }

        // 통계
        long blockCount = 0;
        try (RocksIterator it = blocksDB.newIterator()) {
            it.seekToFirst();
            while (it.isValid()) {
                blockCount++;
                it.next();
            }
        }

        long txCount = 0;
        try (RocksIterator it = txDB.newIterator()) {
            it.seekToFirst();
            while (it.isValid()) {
                txCount++;
                it.next();
            }
        }

        long utxoCount = 0;
        try (RocksIterator it = utxoDB.newIterator()) {
            it.seekToFirst();
            while (it.isValid()) {
                utxoCount++;
                it.next();
            }
        }

        System.out.println("Blocks: " + blockCount);
        System.out.println("Transactions: " + txCount);
        System.out.println("UTXOs: " + utxoCount);
        System.out.println("=====================\n");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static final String META_PRIVATE_KEY = "private_key";

    /**
     * 개인키 저장
     */
    public void savePrivateKey(byte[] privateKey) throws RocksDBException {
        metaDB.put(
                META_PRIVATE_KEY.getBytes(StandardCharsets.UTF_8),
                privateKey
        );
        System.out.println("Private key saved to storage");
    }

    /**
     * 개인키 조회
     */
    public byte[] loadPrivateKey() throws RocksDBException {
        byte[] privateKey = metaDB.get(META_PRIVATE_KEY.getBytes(StandardCharsets.UTF_8));

        if (privateKey != null) {
            System.out.println("Private key loaded from storage");
        }

        return privateKey;
    }

    /**
     * 개인키 존재 여부 확인
     */
    public boolean hasPrivateKey() throws RocksDBException {
        return metaDB.get(META_PRIVATE_KEY.getBytes(StandardCharsets.UTF_8)) != null;
    }
}
