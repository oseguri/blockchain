package util;

import blockchain.transaction.Transaction;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 머클 트리 생성 및 검증 유틸리티
 * 트랜잭션 리스트로부터 머클 루트를 계산
 */
public class MerkleTree {

    /**
     * 트랜잭션 리스트로부터 머클 루트 계산
     * @param transactions 트랜잭션 리스트
     * @return 머클 루트 해시 (32 bytes)
     */
    public static byte[] calculateMerkleRoot(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return new byte[32]; // 빈 해시 반환
        }

        // 트랜잭션이 1개인 경우
        if (transactions.size() == 1) {
            return Hash.HASH256(transactions.get(0).toHash());
        }

        // 1단계: 모든 트랜잭션의 해시를 리스트로 변환
        List<byte[]> merkleTree = new ArrayList<>();
        for (Transaction tx : transactions) {
            merkleTree.add(tx.toHash());
        }

        // 2단계: 머클 트리 구축 (상향식)
        return buildMerkleRoot(merkleTree);
    }

    /**
     * 해시 리스트로부터 머클 루트를 재귀적으로 계산
     * @param hashes 현재 레벨의 해시 리스트
     * @return 머클 루트 해시
     */
    private static byte[] buildMerkleRoot(List<byte[]> hashes) {
        // 루트에 도달 (해시가 1개만 남음)
        if (hashes.size() == 1) {
            return hashes.get(0);
        }

        List<byte[]> nextLevel = new ArrayList<>();

        // 2개씩 묶어서 해싱
        for (int i = 0; i < hashes.size(); i += 2) {
            byte[] left = hashes.get(i);
            byte[] right;

            // 홀수 개인 경우 마지막 해시를 복제
            if (i + 1 < hashes.size()) {
                right = hashes.get(i + 1);
            } else {
                right = left; // 마지막 노드 복제
            }

            // 두 해시를 연결하여 다시 해싱
            byte[] combined = combineAndHash(left, right);
            nextLevel.add(combined);
        }

        // 다음 레벨로 재귀 호출
        return buildMerkleRoot(nextLevel);
    }

    /**
     * 두 해시를 연결하고 다시 해싱
     * @param left 왼쪽 해시
     * @param right 오른쪽 해시
     * @return 결합된 해시
     */
    private static byte[] combineAndHash(byte[] left, byte[] right) {
        ByteBuffer buffer = ByteBuffer.allocate(left.length + right.length);
        buffer.put(left);
        buffer.put(right);
        return Hash.HASH256(buffer.array());
    }

    /**
     * 머클 경로 생성 (특정 트랜잭션의 포함 증명)
     * @param transactions 전체 트랜잭션 리스트
     * @param txIndex 검증할 트랜잭션의 인덱스
     * @return 머클 경로 (검증에 필요한 해시 리스트)
     */
    public static List<byte[]> getMerklePath(List<Transaction> transactions, int txIndex) {
        if (transactions == null || transactions.isEmpty() || txIndex >= transactions.size()) {
            return new ArrayList<>();
        }

        List<byte[]> path = new ArrayList<>();
        List<byte[]> currentLevel = new ArrayList<>();

        // 모든 트랜잭션 해시로 시작
        for (Transaction tx : transactions) {
            currentLevel.add(tx.toHash());
        }

        int currentIndex = txIndex;

        // 트리를 따라 올라가며 경로 수집
        while (currentLevel.size() > 1) {
            List<byte[]> nextLevel = new ArrayList<>();

            for (int i = 0; i < currentLevel.size(); i += 2) {
                byte[] left = currentLevel.get(i);
                byte[] right;

                if (i + 1 < currentLevel.size()) {
                    right = currentLevel.get(i + 1);
                } else {
                    right = left;
                }

                // 현재 인덱스의 형제 노드를 경로에 추가
                if (i == currentIndex || i + 1 == currentIndex) {
                    if (i == currentIndex && i + 1 < currentLevel.size()) {
                        path.add(right); // 오른쪽 형제
                    } else if (i + 1 == currentIndex) {
                        path.add(left); // 왼쪽 형제
                    }
                }

                nextLevel.add(combineAndHash(left, right));
            }

            currentIndex = currentIndex / 2;
            currentLevel = nextLevel;
        }

        return path;
    }

    /**
     * 머클 경로를 사용하여 트랜잭션 포함 여부 검증
     * @param txHash 검증할 트랜잭션 해시
     * @param merklePath 머클 경로
     * @param merkleRoot 머클 루트
     * @param txIndex 트랜잭션 인덱스
     * @return 검증 성공 여부
     */
    public static boolean verifyMerklePath(byte[] txHash, List<byte[]> merklePath,
                                           byte[] merkleRoot, int txIndex) {
        byte[] currentHash = txHash;
        int currentIndex = txIndex;

        // 경로를 따라 루트까지 계산
        for (byte[] siblingHash : merklePath) {
            if (currentIndex % 2 == 0) {
                // 왼쪽 노드인 경우
                currentHash = combineAndHash(currentHash, siblingHash);
            } else {
                // 오른쪽 노드인 경우
                currentHash = combineAndHash(siblingHash, currentHash);
            }
            currentIndex = currentIndex / 2;
        }

        // 계산된 루트와 실제 루트 비교
        return java.util.Arrays.equals(currentHash, merkleRoot);
    }
}
