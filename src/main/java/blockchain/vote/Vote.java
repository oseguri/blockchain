package blockchain.vote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 투표 데이터
 */
public class Vote {

    public enum VoteType {
        APPROVE,    // 찬성
        REJECT,     // 반대
        ABSTAIN     // 기권
    }

    private final String voterAddress;      // 투표자 주소
    private final VoteType voteType;        // 투표 유형
    private final byte[] signature;         // 투표 서명
    private final long timestamp;           // 투표 시간

    public Vote(String voterAddress, VoteType voteType, byte[] signature) {
        this.voterAddress = voterAddress;
        this.voteType = voteType;
        this.signature = signature;
        this.timestamp = System.currentTimeMillis();
    }

    @JsonCreator
    public Vote(
            @JsonProperty("voterAddress") String voterAddress,
            @JsonProperty("voteType") VoteType voteType,
            @JsonProperty("signature") byte[] signature,
            @JsonProperty("timestamp") long timestamp) {
        this.voterAddress = voterAddress;
        this.voteType = voteType;
        this.signature = signature;
        this.timestamp = timestamp;
    }

    // Getters
    public String getVoterAddress() { return voterAddress; }
    public VoteType getVoteType() { return voteType; }
    public byte[] getSignature() { return signature; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "Vote{" +
                "voter='" + voterAddress.substring(0, 10) + "...'" +
                ", type=" + voteType +
                ", time=" + timestamp +
                '}';
    }
}
