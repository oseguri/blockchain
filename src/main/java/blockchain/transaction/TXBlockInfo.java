package blockchain.transaction;

import java.io.Serializable;

public class TXBlockInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int height;
    private byte[] blockHash;

    public TXBlockInfo(int height, byte[] blockHash) {
        this.height = height;
        this.blockHash = blockHash;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }
}