package blockchain.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import util.BytesUtil;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class TransactionOutput implements Serializable {
    private final byte[] address;
    private final byte[] pkscript;
    private final long value;
    private boolean spent;
    private Spender spender;

    public TransactionOutput(byte[] address, byte[] pkscript, long value) {
        this.address = address;
        this.pkscript = pkscript;
        this.value = value;
    }

    public void setSpent(boolean spent) {
        this.spent = spent;
    }
    public void setSpender(Spender spender) {
        this.spender = spender;
    }

    public byte[] toBytes() {
        byte[] valueBytes = BytesUtil.longToBytes(this.value, 8);
        ByteBuffer buffer = ByteBuffer.allocate(valueBytes.length + pkscript.length);
        return buffer.put(valueBytes).put(this.pkscript).array();
    }

    public byte[] getAddress() {
        return address;
    }

    public byte[] getPkscript() {
        return pkscript;
    }

    public long getValue() {
        return value;
    }

    public boolean isSpent() {
        return spent;
    }

    public Spender getSpender() {
        return spender;
    }

    // Jackson 역직렬화용 생성자
    @JsonCreator
    public TransactionOutput(
            @JsonProperty("address") byte[] address,
            @JsonProperty("pkscript") byte[] pkscript,
            @JsonProperty("value") long value,
            @JsonProperty("spent") boolean spent,
            @JsonProperty("spender") Spender spender) {
        this.address = address;
        this.pkscript = pkscript;
        this.value = value;
        this.spent = spent;
        this.spender = spender;
    }
}
