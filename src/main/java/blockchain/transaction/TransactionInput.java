package blockchain.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import util.BytesUtil;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;

public class TransactionInput implements Serializable {
    private final Boolean isCoinbase;
    byte[] prevTXID;
    int outputIdx; //이전 tx output index
    List<byte[]> sigScript;
    byte[] pkScript;
    long value;
    String address;

    public TransactionInput(Boolean isCoinbase, String address, long value, byte[] pkScript, List<byte[]> sigScript, int outputIdx, byte[] prevTXID) {
        this.isCoinbase = isCoinbase;
        this.address = address;
        this.value = value;
        this.pkScript = pkScript;
        this.sigScript = sigScript;
        this.outputIdx = outputIdx;
        this.prevTXID = prevTXID;
    }


    public byte[] toBytes() {
        byte[] sig = this.sigScript.get(0);
        byte[] pub = this.sigScript.get(1);
        byte[] outIdx = BytesUtil.intToBytes(this.outputIdx, 4);
        //이전 트랜잭션 + 출력번호 + 서명길이 + 서명 + 공개키 길이 + 공개키 + 시퀀스(ffffffff 고정)
        int totalLen = 32 + 4 + 1 + sig.length + 1 + pub.length + 4;
        ByteBuffer buffer = ByteBuffer.allocate(totalLen);
        buffer.put(prevTXID).put(outIdx).put(BytesUtil.intToBytes(sig.length, 1)).put(sig)
                .put(BytesUtil.intToBytes(pub.length, 1)).put(pub).put(BytesUtil.intToBytes(-1, 4));
        return buffer.array();
    }

    public byte[] getPrevTXID() {
        return prevTXID;
    }

    public int getOutputIdx() {
        return outputIdx;
    }

    public List<byte[]> getSigScript() {
        return sigScript;
    }

    public Boolean getIsCoinbase() {
        return isCoinbase;
    }

    public byte[] getPkScript() {
        return pkScript;
    }

    public long getValue() {
        return value;
    }

    public String getAddress() {
        return address;
    }

    /**
     * 서명 스크립트 설정 (서명 후)
     */
    public void setSigScript(List<byte[]> sigScript) {
        this.sigScript = sigScript;
    }

    // Jackson 역직렬화용 생성자
    @JsonCreator
    public TransactionInput(
            @JsonProperty("isCoinbase") Boolean isCoinbase,
            @JsonProperty("prevTXID") byte[] prevTXID,
            @JsonProperty("outputIdx") int outputIdx,
            @JsonProperty("sigScript") List<byte[]> sigScript,
            @JsonProperty("pkScript") byte[] pkScript,
            @JsonProperty("value") long value,
            @JsonProperty("address") String address) {
        this.isCoinbase = isCoinbase;
        this.prevTXID = prevTXID;
        this.outputIdx = outputIdx;
        this.sigScript = sigScript;
        this.pkScript = pkScript;
        this.value = value;
        this.address = address;
    }
}
                                        
