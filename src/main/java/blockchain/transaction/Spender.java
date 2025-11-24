package blockchain.transaction;

import java.io.Serializable;

public class Spender implements Serializable {
    private static final long serialVersionUID = 1L;

    private byte[] txid;
    private int inputIndex;

    public Spender(byte[] txid, int inputIndex) {
        this.txid = txid;
        this.inputIndex = inputIndex;
    }

    public byte[] getTxid() {
        return txid;
    }

    public int getInputIndex() {
        return inputIndex;
    }
}
