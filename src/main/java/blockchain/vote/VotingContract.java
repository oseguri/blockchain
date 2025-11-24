package blockchain.vote;

import blockchain.encryption.EncryptedContract;
import blockchain.node.Node;
import blockchain.node.sign.Signature;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VotingContract {

    private final EncryptedContract encryptedContract;
    private final List<String> participants;
    private final Map<String, Vote> votes;
    private final long votingDeadline;
    private final int requiredApprovals;

    private boolean finalized;
    private VoteResult finalResult;

    public VotingContract(
            String plainContract,
            Map<String, byte[]> participantPublicKeys,
            long votingDurationSeconds,
            int requiredApprovals) throws Exception {

        this.encryptedContract = new EncryptedContract(plainContract, participantPublicKeys);
        this.participants = new ArrayList<>(participantPublicKeys.keySet());
        this.votes = new ConcurrentHashMap<>();
        this.votingDeadline = System.currentTimeMillis() + (votingDurationSeconds * 1000);
        this.requiredApprovals = requiredApprovals;
        this.finalized = false;

        System.out.println("✓ Voting contract created");
        System.out.println("  Participants: " + participants.size());
        System.out.println("  Required approvals: " + requiredApprovals);
        System.out.println("  Voting deadline: " + new Date(votingDeadline));
    }

    @JsonCreator
    public VotingContract(
            @JsonProperty("encryptedContract") EncryptedContract encryptedContract,
            @JsonProperty("participants") List<String> participants,
            @JsonProperty("votes") Map<String, Vote> votes,
            @JsonProperty("votingDeadline") long votingDeadline,
            @JsonProperty("requiredApprovals") int requiredApprovals,
            @JsonProperty("finalized") boolean finalized,
            @JsonProperty("finalResult") VoteResult finalResult) {
        this.encryptedContract = encryptedContract;
        this.participants = participants;
        this.votes = votes != null ? new ConcurrentHashMap<>(votes) : new ConcurrentHashMap<>();
        this.votingDeadline = votingDeadline;
        this.requiredApprovals = requiredApprovals;
        this.finalized = finalized;
        this.finalResult = finalResult;
    }

    /**
     * 투표하기
     */
    public boolean castVote(Node voterNode, Vote.VoteType voteType) throws Exception {
        String voterAddress = voterNode.getAddress();

        if (finalized) {
            System.out.println("✗ Voting is already finalized");
            return false;
        }

        if (System.currentTimeMillis() > votingDeadline) {
            System.out.println("✗ Voting deadline has passed");
            finalizeVoting(); // ✅ 이름 변경
            return false;
        }

        if (!participants.contains(voterAddress)) {
            System.out.println("✗ Not a participant");
            return false;
        }

        if (votes.containsKey(voterAddress)) {
            System.out.println("✗ Already voted");
            return false;
        }

        // 투표 서명 생성
        String voteMessage = voterAddress + ":" + voteType.name() + ":" + votingDeadline;
        byte[] signature = Signature.sign(
                voterNode.getPrivateKey(),
                voteMessage.getBytes()
        );

        // 투표 기록
        Vote vote = new Vote(voterAddress, voteType, signature);
        votes.put(voterAddress, vote);

        System.out.println("✓ Vote recorded");
        System.out.println("  Voter: " + voterAddress.substring(0, 10) + "...");
        System.out.println("  Type: " + voteType);
        System.out.println("  Total votes: " + votes.size() + "/" + participants.size());

        // 모든 참여자가 투표하면 자동 완료
        if (votes.size() == participants.size()) {
            finalizeVoting(); // ✅ 이름 변경
        }

        return true;
    }

    /**
     * 투표 완료 처리
     */
    public VoteResult finalizeVoting() { // ✅ finalize → finalizeVoting
        if (finalized) {
            return finalResult;
        }

        System.out.println("\n=== Finalizing Vote ===");

        // 투표 집계
        int approvals = 0;
        int rejections = 0;
        int abstentions = 0;

        for (Vote vote : votes.values()) {
            switch (vote.getVoteType()) {
                case APPROVE:
                    approvals++;
                    break;
                case REJECT:
                    rejections++;
                    break;
                case ABSTAIN:
                    abstentions++;
                    break;
            }
        }

        // 결과 판정
        boolean approved = approvals >= requiredApprovals;

        this.finalResult = new VoteResult(
                approved,
                approvals,
                rejections,
                abstentions,
                participants.size(),
                System.currentTimeMillis()
        );

        this.finalized = true;

        System.out.println("Approvals: " + approvals + "/" + requiredApprovals);
        System.out.println("Rejections: " + rejections);
        System.out.println("Abstentions: " + abstentions);
        System.out.println("Result: " + (approved ? "✓ APPROVED" : "✗ REJECTED"));
        System.out.println("=======================\n");

        return finalResult;
    }

    /**
     * 계약 내용 복호화
     */
    public String decryptContract(Node participantNode) throws Exception {
        return encryptedContract.decrypt(
                participantNode.getAddress(),
                participantNode.getPrivateKey()
        );
    }

    /**
     * 투표 현황 조회
     */
    public VoteStatus getVoteStatus() {
        int approvals = (int) votes.values().stream()
                .filter(v -> v.getVoteType() == Vote.VoteType.APPROVE)
                .count();

        int rejections = (int) votes.values().stream()
                .filter(v -> v.getVoteType() == Vote.VoteType.REJECT)
                .count();

        int abstentions = (int) votes.values().stream()
                .filter(v -> v.getVoteType() == Vote.VoteType.ABSTAIN)
                .count();

        return new VoteStatus(
                votes.size(),
                participants.size(),
                approvals,
                rejections,
                abstentions,
                votingDeadline,
                finalized
        );
    }

    /**
     * 투표 상태 출력
     */
    public void printVoteStatus() {
        VoteStatus status = getVoteStatus();

        System.out.println("\n=== Vote Status ===");
        System.out.println("Votes: " + status.getVoteCount() + "/" + status.getTotalParticipants());
        System.out.println("Approvals: " + status.getApprovals() + " (need " + requiredApprovals + ")");
        System.out.println("Rejections: " + status.getRejections());
        System.out.println("Abstentions: " + status.getAbstentions());

        if (!finalized) {
            long remainingSeconds = (votingDeadline - System.currentTimeMillis()) / 1000;
            if (remainingSeconds > 0) {
                System.out.println("Time remaining: " + remainingSeconds + " seconds");
            } else {
                System.out.println("Voting period ended");
            }
        } else {
            System.out.println("Status: FINALIZED - " +
                    (finalResult.isApproved() ? "APPROVED" : "REJECTED"));
        }

        System.out.println("\nVotes cast:");
        for (Map.Entry<String, Vote> entry : votes.entrySet()) {
            String address = entry.getKey().substring(0, 10) + "...";
            Vote vote = entry.getValue();
            System.out.println("  " + address + ": " + vote.getVoteType());
        }

        System.out.println("==================\n");
    }

    // Getters
    public EncryptedContract getEncryptedContract() { return encryptedContract; }
    public List<String> getParticipants() { return participants; }
    public Map<String, Vote> getVotes() { return votes; }
    public long getVotingDeadline() { return votingDeadline; }
    public int getRequiredApprovals() { return requiredApprovals; }
    public boolean isFinalized() { return finalized; }
    public VoteResult getFinalResult() { return finalResult; }
}
