package blockchain.vote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 투표 결과
 */
public class VoteResult {
    private final boolean approved;
    private final int approvals;
    private final int rejections;
    private final int abstentions;
    private final int totalParticipants;
    private final long finalizedAt;

    @JsonCreator
    public VoteResult(
            @JsonProperty("approved") boolean approved,
            @JsonProperty("approvals") int approvals,
            @JsonProperty("rejections") int rejections,
            @JsonProperty("abstentions") int abstentions,
            @JsonProperty("totalParticipants") int totalParticipants,
            @JsonProperty("finalizedAt") long finalizedAt) {
        this.approved = approved;
        this.approvals = approvals;
        this.rejections = rejections;
        this.abstentions = abstentions;
        this.totalParticipants = totalParticipants;
        this.finalizedAt = finalizedAt;
    }

    // Getters
    public boolean isApproved() { return approved; }
    public int getApprovals() { return approvals; }
    public int getRejections() { return rejections; }
    public int getAbstentions() { return abstentions; }
    public int getTotalParticipants() { return totalParticipants; }
    public long getFinalizedAt() { return finalizedAt; }

    @Override
    public String toString() {
        return "VoteResult{" +
                "approved=" + approved +
                ", approvals=" + approvals +
                ", rejections=" + rejections +
                ", abstentions=" + abstentions +
                ", total=" + totalParticipants +
                '}';
    }
}
