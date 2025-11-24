package blockchain.block;

import blockchain.transaction.Transaction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import util.BytesUtil;
import util.Hash;
import util.MerkleTree;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Block implements Serializable {
    private final byte[] prevHash;
    private final int txNum;
    private final int size;
    private final byte[] version;
    private final byte[] merkleRoot;
    private byte[] nonce;
    private final long timestamp;
    private final List<Transaction> transactions;
    private byte[] blockHash;

    // 일반 블록 생성자 (현재 시간 사용)
    public Block(byte[] prevHash, List<Transaction> transactions, byte[] version) {
        this(prevHash, transactions, version, System.currentTimeMillis() / 1000);
    }

    // 제네시스 블록용 생성자 (고정된 타임스탬프 사용)
    public Block(byte[] prevHash, List<Transaction> transactions, byte[] version, long timestamp) {
        this.prevHash = prevHash;
        this.transactions = transactions;
        this.txNum = transactions.size();
        this.version = version;
        this.timestamp = timestamp;  // 파라미터로 받은 타임스탬프 사용
        this.merkleRoot = calculateMerkleRoot(transactions);
        this.nonce = new byte[4];
        this.size = calculateBlockSize();
        this.blockHash = calculateBlockHash();
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<String> getTransactionTxidList() {
        List<Transaction> Txs = getTransactions();
        List<String> txids = new ArrayList<>();
        for (Transaction tx : Txs) {
            txids.add(bytesToHex(tx.getTxid()));
        }
        return txids;
    }
    /**
     * Jackson 역직렬화용 생성자
     */
    @JsonCreator
    public Block(
            @JsonProperty("prevHash") byte[] prevHash,
            @JsonProperty("txNum") int txNum,
            @JsonProperty("size") int size,
            @JsonProperty("version") byte[] version,
            @JsonProperty("merkleRoot") byte[] merkleRoot,
            @JsonProperty("nonce") byte[] nonce,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("transactions") List<Transaction> transactions,
            @JsonProperty("blockHash") byte[] blockHash) {
        this.prevHash = prevHash;
        this.txNum = txNum;
        this.size = size;
        this.version = version;
        this.merkleRoot = merkleRoot;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.transactions = transactions;
        this.blockHash = blockHash;
    }
    private byte[] calculateMerkleRoot(List<Transaction> transactions) {
        return MerkleTree.calculateMerkleRoot(transactions);
    }

    public byte[] calculateBlockHash() {
        byte[] headerBytes = getHeaderBytes();
        return Hash.HASH256(headerBytes);
    }

    private byte[] getHeaderBytes() {
        // 버전(4) + 이전해시(32) + 머클루트(32) + 타임스탬프(4) + nonce(4) = 76 bytes
        ByteBuffer buffer = ByteBuffer.allocate(76);
        buffer.put(version);                                    // 4 bytes
        buffer.put(prevHash);                                   // 32 bytes
        buffer.put(merkleRoot);                                 // 32 bytes
        buffer.put(BytesUtil.intToBytes((int)timestamp, 4));   // 4 bytes
        buffer.put(nonce);                                      // 4 bytes
        return buffer.array();
    }


    private int calculateBlockSize() {
        int headerSize = 76; // 블록 헤더 크기
        int txSize = 0;
        for (Transaction tx : transactions) {
            txSize += tx.toBytes().length;
        }
        return headerSize + txSize;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
        this.blockHash = calculateBlockHash(); // 해시 재계산
    }

    public void incrementNonce() {
        // 4바이트 nonce를 int로 변환하여 1 증가
        int nonceInt = ByteBuffer.wrap(nonce).getInt();
        nonceInt++;
        this.nonce = BytesUtil.intToBytes(nonceInt, 4);
        this.blockHash = calculateBlockHash();
    }

    public String toString() {
        return "Block{" +
                "blockHash=" + bytesToHex(blockHash) +
                ", prevHash=" + bytesToHex(prevHash) +
                ", txNum=" + txNum +
                ", size=" + size +
                ", timestamp=" + timestamp +
                '}';
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // getter

    public byte[] getPrevHash() {
        return prevHash;
    }

    public int getTxNum() {
        return txNum;
    }

    public int getSize() {
        return size;
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public long getTimestamp() {
        return timestamp;
    }


    public byte[] getBlockHash() {
        return blockHash;
    }
}
