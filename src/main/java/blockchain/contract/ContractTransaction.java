package blockchain.contract;

import blockchain.encryption.EncryptedContract;
import blockchain.node.Node;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;

import java.util.*;

/**
 * 계약 트랜잭션 생성 헬퍼
 */
public class ContractTransaction {

    /**
     * 암호화된 계약 트랜잭션 생성
     * @param senderNode 송신자 노드
     * @param plainContract 평문 계약 내용
     * @param participants 참여자 노드 리스트
     * @param amount 계약 금액
     * @return 계약 트랜잭션
     */
    public static Transaction createEncryptedContract(
            Node senderNode,
            String plainContract,
            List<Node> participants,
            long amount) throws Exception {

        System.out.println("\n=== Creating Encrypted Contract Transaction ===");
        System.out.println("Contract: " + plainContract);
        System.out.println("Participants: " + participants.size());
        System.out.println("Amount: " + amount + " satoshis");

        // 1. 참여자 공개키 수집
        Map<String, byte[]> participantPublicKeys = new HashMap<>();

        // 송신자도 참여자에 포함
        participantPublicKeys.put(senderNode.getAddress(), senderNode.getPublicKeyBytes());

        for (Node participant : participants) {
            String address = participant.getAddress();
            byte[] publicKey = participant.getPublicKeyBytes();
            participantPublicKeys.put(address, publicKey);
        }

        // 2. 계약 암호화
        EncryptedContract encryptedContract = new EncryptedContract(
                plainContract,
                participantPublicKeys
        );

        // 3. 트랜잭션 생성
        List<TransactionInput> inputs = new ArrayList<>();
        List<TransactionOutput> outputs = new ArrayList<>();

        // 더미 입력 (실제로는 UTXO 선택 필요)
        TransactionInput input = new TransactionInput(
                false,
                senderNode.getAddress(),
                amount,
                new byte[0],
                new ArrayList<>(),
                0,
                new byte[32]
        );
        inputs.add(input);

        // 출력 (참여자들에게 분배)
        long amountPerParticipant = amount / participantPublicKeys.size();
        for (String address : participantPublicKeys.keySet()) {
            TransactionOutput output = new TransactionOutput(
                    address.getBytes(),
                    new byte[0],
                    amountPerParticipant
            );
            outputs.add(output);
        }

        // 4. 트랜잭션 생성 (암호화된 계약을 contract 필드에 저장)
        Transaction tx = new Transaction(
                encryptedContract.toJsonString(),  // 암호화된 계약
                inputs,
                outputs
        );

        // 5. voteList에 참여자 주소 저장
        List<String> participantAddresses = new ArrayList<>(participantPublicKeys.keySet());
        tx.setVoteList(participantAddresses);

        System.out.println("✓ Encrypted contract transaction created");
        System.out.println("  TXID: " + bytesToHex(tx.getTxid()).substring(0, 16) + "...");
        System.out.println("  Participants: " + participantAddresses);
        System.out.println("===============================================\n");

        return tx;
    }

    /**
     * 트랜잭션에서 계약 내용 복호화
     * @param tx 계약 트랜잭션
     * @param participantNode 참여자 노드
     * @return 복호화된 계약 내용
     */
    public static String decryptContract(Transaction tx, Node participantNode) throws Exception {
        String contractJson = tx.getContract();

        if (contractJson == null || contractJson.isEmpty()) {
            throw new IllegalArgumentException("Transaction does not contain a contract");
        }

        // JSON에서 EncryptedContract 복원 (간단한 파싱)
        // 실제로는 Jackson 사용 권장
        EncryptedContract encryptedContract = parseEncryptedContract(contractJson);

        // 복호화
        String participantAddress = participantNode.getAddress();
        byte[] participantPrivateKey = participantNode.getPrivateKey();

        return encryptedContract.decrypt(participantAddress, participantPrivateKey);
    }

    /**
     * JSON에서 EncryptedContract 파싱 (간단한 구현)
     */
    private static EncryptedContract parseEncryptedContract(String json) {
        // 실제로는 Jackson ObjectMapper 사용
        // 여기서는 간단하게 구현
        // TODO: Jackson으로 변경
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
