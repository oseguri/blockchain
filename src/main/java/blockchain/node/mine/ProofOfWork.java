package blockchain.node.mine;

import blockchain.block.Block;
import util.BytesUtil;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * 작업 증명 (Proof of Work) 구현
 * 블록 해시가 난이도 타겟보다 작은 nonce를 찾는 과정
 */
public class ProofOfWork {

    // 난이도 상수
    private static final int MAX_NONCE = Integer.MAX_VALUE;
    private static final int DIFFICULTY_ADJUSTMENT_INTERVAL = 2016; // 난이도 조정 주기
    private static final long TARGET_BLOCK_TIME = 600; // 목표 블록 생성 시간 (10분 = 600초)

    /**
     * 블록 마이닝 (작업 증명)
     * @param block 마이닝할 블록
     * @param difficulty 난이도 (앞에 필요한 0의 개수)
     * @return 마이닝 성공 여부
     */
    public static boolean mineBlock(Block block, int difficulty) {
        // 타겟 해시 계산 (앞에 difficulty 개수만큼 0이 있어야 함)
        String target = getTargetString(difficulty);

        System.out.println("\n=== Mining Block ===");
        System.out.println("Target: " + target);
        System.out.println("Difficulty: " + difficulty);

        long startTime = System.currentTimeMillis();
        int nonce = 0;

        // nonce를 증가시키며 조건을 만족하는 해시 찾기
        while (nonce < MAX_NONCE) {
            // nonce 설정
            block.setNonce(BytesUtil.intToBytes(nonce, 4));

            // 블록 해시 계산
            byte[] hash = block.getBlockHash();
            String hashHex = bytesToHex(hash);

            // 진행 상황 출력 (100,000번마다)
            if (nonce % 100000 == 0) {
                System.out.println("Trying nonce: " + nonce + " | Hash: " + hashHex);
            }

            // 타겟과 비교
            if (hashHex.compareTo(target) < 0) {
                long endTime = System.currentTimeMillis();
                long elapsed = endTime - startTime;

                System.out.println("\n✓ Block Mined!");
                System.out.println("Nonce: " + nonce);
                System.out.println("Hash: " + hashHex);
                System.out.println("Time: " + elapsed + "ms");
                System.out.println("Hash Rate: " + (nonce * 1000 / elapsed) + " H/s");
                return true;
            }

            nonce++;
        }

        System.out.println("Mining failed - nonce overflow");
        return false;
    }

    /**
     * 난이도 기반 타겟 문자열 생성
     * @param difficulty 난이도 (앞에 필요한 0의 개수)
     * @return 타겟 문자열 (16진수)
     */
    private static String getTargetString(int difficulty) {
        StringBuilder target = new StringBuilder();

        // difficulty 개수만큼 0 추가
        for (int i = 0; i < difficulty; i++) {
            target.append("0");
        }

        // 나머지는 'f'로 채움 (최대값)
        for (int i = difficulty; i < 64; i++) {
            target.append("f");
        }

        return target.toString();
    }

    /**
     * 블록 해시 검증 (난이도 조건 만족 여부)
     * @param block 검증할 블록
     * @param difficulty 난이도
     * @return 검증 성공 여부
     */
    public static boolean validateProofOfWork(Block block, int difficulty) {
        String target = getTargetString(difficulty);
        String hashHex = bytesToHex(block.getBlockHash());

        return hashHex.compareTo(target) < 0;
    }

    /**
     * 난이도 조정 계산
     * @param blocks 블록 리스트
     * @param currentHeight 현재 블록 높이
     * @param currentDifficulty 현재 난이도
     * @return 새로운 난이도
     */
    public static int adjustDifficulty(java.util.List<Block> blocks, int currentHeight, int currentDifficulty) {
        // 난이도 조정 주기가 아니면 현재 난이도 유지
        if (currentHeight % DIFFICULTY_ADJUSTMENT_INTERVAL != 0) {
            return currentDifficulty;
        }

        // 첫 조정이거나 블록이 충분하지 않으면 현재 난이도 유지
        if (currentHeight < DIFFICULTY_ADJUSTMENT_INTERVAL) {
            return currentDifficulty;
        }

        // 이전 조정 시점의 블록
        Block previousAdjustmentBlock = blocks.get(currentHeight - DIFFICULTY_ADJUSTMENT_INTERVAL);
        Block latestBlock = blocks.get(currentHeight - 1);

        // 실제 소요 시간 계산
        long timeExpected = TARGET_BLOCK_TIME * DIFFICULTY_ADJUSTMENT_INTERVAL; // 초
        long timeActual = latestBlock.getTimestamp() - previousAdjustmentBlock.getTimestamp();

        System.out.println("\n=== Difficulty Adjustment ===");
        System.out.println("Current Height: " + currentHeight);
        System.out.println("Expected Time: " + timeExpected + "s (" + (timeExpected / 60) + " min)");
        System.out.println("Actual Time: " + timeActual + "s (" + (timeActual / 60) + " min)");
        System.out.println("Current Difficulty: " + currentDifficulty);

        // 난이도 조정
        int newDifficulty = currentDifficulty;

        // 너무 빠르면 난이도 증가
        if (timeActual < timeExpected / 2) {
            newDifficulty = currentDifficulty + 1;
            System.out.println("Blocks too fast - Increasing difficulty");
        }
        // 너무 느리면 난이도 감소
        else if (timeActual > timeExpected * 2) {
            newDifficulty = Math.max(1, currentDifficulty - 1); // 최소 난이도 1
            System.out.println("Blocks too slow - Decreasing difficulty");
        }
        // 정상 범위
        else {
            System.out.println("Difficulty unchanged");
        }

        System.out.println("New Difficulty: " + newDifficulty);
        System.out.println("===========================\n");

        return newDifficulty;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
