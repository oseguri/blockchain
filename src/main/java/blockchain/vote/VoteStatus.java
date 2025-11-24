package blockchain.vote;

/**
 * 투표 현황 (진행중)
 */
public class VoteStatus {
    private final int voteCount;
    private final int totalParticipants;
    private final int approvals;
    private final int rejections;
    private final int abstentions;
    private final long votingDeadline;
    private final boolean finalized;

    public VoteStatus(int voteCount, int totalParticipants, int approvals,
                      int rejections, int abstentions, long votingDeadline, boolean finalized) {
        this.voteCount = voteCount;
        this.totalParticipants = totalParticipants;
        this.approvals = approvals;
        this.rejections = rejections;
        this.abstentions = abstentions;
        this.votingDeadline = votingDeadline;
        this.finalized = finalized;
    }

    // Getters
    public int getVoteCount() { return voteCount; }
    public int getTotalParticipants() { return totalParticipants; }
    public int getApprovals() { return approvals; }
    public int getRejections() { return rejections; }
    public int getAbstentions() { return abstentions; }
    public long getVotingDeadline() { return votingDeadline; }
    public boolean isFinalized() { return finalized; }
}
