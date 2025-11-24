package blockchain.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import util.BytesUtil;
import util.Hash;

import javax.crypto.*;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Transaction implements Serializable {
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private byte[] txid;
    private int size;
    private long fee;
    private final String time;
    private TXBlockInfo block;
    private boolean deleted;

    private final List<TransactionInput> inputs;
    private final List<TransactionOutput> outputs;

    private String contract = null; //거래 내용
    private List<String> voteList; //참여자 주소 및 찬성/반대 정보 저장

    public Transaction(String contract, List<TransactionInput> inputs,
                       List<TransactionOutput> outputs) throws NoSuchAlgorithmException {
        this.contract = contract;
        this.voteList = new ArrayList<>();
        this.inputs = inputs;
        this.outputs = outputs;
        this.time = getCurrentTimeUTC();
        this.txid = Hash.HASH256(toHash());
        this.fee = 0;
    }

    // contract 없는 생성자
    public Transaction(List<TransactionInput> inputs,
                       List<TransactionOutput> outputs) throws NoSuchAlgorithmException {
        this.contract = null;
        this.voteList = null;
        this.inputs = inputs;
        this.outputs = outputs;
        this.time = getCurrentTimeUTC();
        this.txid = Hash.HASH256(toHash());
        this.fee = 0;
    }

    // Jackson 역직렬화용 생성자
    @JsonCreator
    public Transaction(
            @JsonProperty("txid") byte[] txid,
            @JsonProperty("size") int size,
            @JsonProperty("fee") long fee,
            @JsonProperty("time") String time,
            @JsonProperty("block") TXBlockInfo block,
            @JsonProperty("deleted") boolean deleted,
            @JsonProperty("inputs") List<TransactionInput> inputs,
            @JsonProperty("outputs") List<TransactionOutput> outputs,
            @JsonProperty("contract") String contract,
            @JsonProperty("voteList") List<String> voteList) {
        this.txid = txid;
        this.size = size;
        this.fee = fee;
        this.time = time;
        this.block = block;
        this.deleted = deleted;
        this.inputs = inputs;
        this.outputs = outputs;
        this.contract = contract;
        this.voteList = voteList != null ? voteList : new ArrayList<>(); // null 방지
    }

    public byte[] getTxid() {
        return txid;
    }

    public int getSize() {
        if (this.size <= 0) {
            this.size = calculateSize();
        }
        return this.size;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public long getFee() {
        return fee;
    }

    public String getTime() {
        return time;
    }

    public TXBlockInfo getBlock() {
        return block;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public List<TransactionOutput> getOutputs() {
        return outputs;
    }

    public String getContract() {
        return contract;
    }

    // 헬퍼 메서드
    private String getCurrentTimeUTC() {
        return ZonedDateTime.now(ZoneId.of("UTC")).format(TIME_FORMATTER);
    }

    public void addVote(String address, Boolean vote) {
        String v = vote ? "1" : "0";
        String voteInfo = address.concat(v);
        voteList.add(voteInfo);
    }

    public List<String> getVoteList() {
        // null이면 즉시 새 ArrayList 생성 후 할당
        if (this.voteList == null) {
            this.voteList = new ArrayList<>();
        }
        return this.voteList;
    }
    public void setVoteList(List<String> voteList){
        this.voteList = voteList;
    }

    private int getByteSize() {
        int len = 0;
        //input 개수
        len += 1;
        //각 input bytes length
        for(TransactionInput i : this.inputs) len += i.toBytes().length;
        //output 개수
        len += 1;
        //각 output bytes length
        for(TransactionOutput o : this.outputs) len += o.toBytes().length;
        return len;
    }

    public byte[] toBytes() {
        int len = getByteSize();
        ByteBuffer buffer = ByteBuffer.allocate(len);

        buffer.put(BytesUtil.intToBytes(inputs.size(), 1));
        for(TransactionInput i : this.inputs) buffer.put(i.toBytes());

        buffer.put(BytesUtil.intToBytes(outputs.size(), 1));
        for(TransactionOutput o : this.outputs) buffer.put(o.toBytes());

        return buffer.array();
    }

    public byte[] toHash() {
        return Hash.HASH256(toBytes());
    }

    public List<TransactionInput> getInputs() {
        return inputs;
    }

    /**
     * 트랜잭션 크기 계산
     */
    private int calculateSize() {
        try {
            return toBytes().length;
        } catch (Exception e) {
            System.err.println("Error calculating transaction size: " + e.getMessage());
            // 최소 크기 반환 (0 방지)
            return 250; // 평균 트랜잭션 크기
        }
    }
}
