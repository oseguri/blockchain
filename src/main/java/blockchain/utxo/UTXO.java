package blockchain.utxo;

import blockchain.transaction.TransactionOutput;

import java.util.Arrays;

/**
 * UTXO (Unspent Transaction Output)
 * 사용되지 않은 트랜잭션 출력
 */
public class UTXO {
    private final byte[] txid;           // 트랜잭션 ID
    private final int outputIndex;        // 출력 인덱스
    private final TransactionOutput output; // 출력 정보
    private final long blockHeight;       // 생성된 블록 높이

    public UTXO(byte[] txid, int outputIndex, TransactionOutput output, long blockHeight) {
        this.txid = txid;
        this.outputIndex = outputIndex;
        this.output = output;
        this.blockHeight = blockHeight;
    }

    /**
     * UTXO 키 생성 (txid + outputIndex)
     */
    public String getKey() {
        return bytesToHex(txid) + ":" + outputIndex;
    }

    public byte[] getTxid() {
        return txid;
    }

    public int getOutputIndex() {
        return outputIndex;
    }

    public TransactionOutput getOutput() {
        return output;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public long getValue() {
        return output.getValue();
    }

    public byte[] getAddress() {
        return output.getAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UTXO utxo = (UTXO) o;
        return outputIndex == utxo.outputIndex && Arrays.equals(txid, utxo.txid);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(txid);
        result = 31 * result + outputIndex;
        return result;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
