package blockchain.transaction;

import blockchain.transaction.Transaction;
import blockchain.validation.TransactionValidator;
import blockchain.validation.ValidationResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 메모리 풀 (Mempool)
 * 미확인 트랜잭션을 관리하는 대기 공간
 */
public class Mempool {

    // 트랜잭션 저장소 (txid -> Transaction)
    private final Map<String, Transaction> transactions;

    private final TransactionValidator validator;

    // 최대 메모리 풀 크기 (트랜잭션 개수)
    private static final int MAX_POOL_SIZE = 5000;

    // 최소 수수료 (사토시/바이트)
    private static final long MIN_FEE_RATE = 1;

    public Mempool(TransactionValidator validator) {
        // ConcurrentHashMap 사용 (멀티스레드 환경 대비)
        this.transactions = new ConcurrentHashMap<>();
        this.validator = validator;
    }

    /**
     * 트랜잭션 추가
     * @param tx 추가할 트랜잭션
     * @return 추가 성공 여부
     */
    public boolean addTransaction(Transaction tx) {
        String txid = bytesToHex(tx.getTxid());

        // 이미 존재하는 트랜잭션인지 확인
        if (transactions.containsKey(txid)) {
            System.out.println("Transaction already in mempool: " + txid.substring(0, 16) + "...");
            return false;
        }

        // 트랜잭션 검증
        ValidationResult result = validator.validateTransaction(tx);
        if (!result.isValid()) {
            System.out.println("✗ Transaction validation failed: " + result.getMessage());
            return false;
        }

        // 메모리 풀이 가득 찬 경우
        if (transactions.size() >= MAX_POOL_SIZE) {
            evictLowestFeeTransaction(tx);
        }

        // 트랜잭션 추가
        transactions.put(txid, tx);
        System.out.println("✓ Transaction added to mempool");
        System.out.println("  TXID: " + txid.substring(0, 16) + "...");
        System.out.println("  Fee: " + tx.getFee() + " satoshis");
        System.out.println("  Mempool size: " + transactions.size());

        return true;
    }

    /**
     * 트랜잭션 제거 (블록에 포함된 후)
     * @param txid 트랜잭션 ID
     * @return 제거된 트랜잭션
     */
    public Transaction removeTransaction(String txid) {
        return transactions.remove(txid);
    }

    /**
     * 트랜잭션 제거 (여러 개)
     * @param txids 트랜잭션 ID 리스트
     */
    public void removeTransactions(List<String> txids) {
        for (String txid : txids) {
            transactions.remove(txid);
        }
        System.out.println("Removed " + txids.size() + " transactions from mempool");
    }

    /**
     * 트랜잭션 조회
     * @param txid 트랜잭션 ID
     * @return 트랜잭션
     */
    public Transaction getTransaction(String txid) {
        return transactions.get(txid);
    }

    /**
     * 트랜잭션 존재 여부 확인
     * @param txid 트랜잭션 ID
     * @return 존재 여부
     */
    public boolean containsTransaction(String txid) {
        return transactions.containsKey(txid);
    }

    /**
     * 수수료 순으로 정렬된 트랜잭션 리스트 조회
     * @param maxCount 최대 개수
     * @return 트랜잭션 리스트 (수수료 높은 순)
     */
    public List<Transaction> getTopTransactionsByFee(long maxCount) {
        return transactions.values().stream()
                .sorted((tx1, tx2) -> {
                    // 수수료율(satoshi/byte) 계산하여 비교
                    double feeRate1 = (double) tx1.getFee() / tx1.getSize();
                    double feeRate2 = (double) tx2.getFee() / tx2.getSize();
                    return Double.compare(feeRate2, feeRate1); // 내림차순
                })
                .limit(maxCount)
                .collect(Collectors.toList());
    }

    /**
     * 모든 트랜잭션 조회
     * @return 트랜잭션 리스트
     */
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions.values());
    }

    /**
     * 메모리 풀 크기 조회
     * @return 트랜잭션 개수
     */
    public int size() {
        return transactions.size();
    }

    /**
     * 메모리 풀 비우기
     */
    public void clear() {
        transactions.clear();
        System.out.println("Mempool cleared");
    }

    /**
     * 트랜잭션 검증
     * @param tx 검증할 트랜잭션
     * @return 유효성 여부
     */
    private boolean validateTransaction(Transaction tx) {
        try {
            // 기본 검증
            if (tx.getTxid() == null || tx.getTxid().length == 0) {
                System.out.println("Invalid txid");
                return false;
            }

            if (tx.getInputs() == null || tx.getInputs().isEmpty()) {
                System.out.println("No inputs");
                return false;
            }

            if (tx.getOutputs() == null || tx.getOutputs().isEmpty()) {
                System.out.println("No outputs");
                return false;
            }

            // 크기 검증
            int txSize = tx.getSize();
            if (txSize <= 0) {
                System.out.println("Invalid transaction size: " + txSize);
                return false;
            }

            // 수수료 검증
            long fee = tx.getFee();
            if (fee < 0) {
                System.out.println("Negative fee: " + fee);
                return false;
            }

            // 수수료율 계산
            long feeRate = fee / txSize;

            if (feeRate < MIN_FEE_RATE) {
                System.out.println("Fee rate too low: " + feeRate + " sat/byte (min: " + MIN_FEE_RATE + ")");
                // 개발 중에는 경고만 출력하고 통과시킴
                // return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("Error validating transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 수수료가 가장 낮은 트랜잭션 제거
     * @param newTx 새로 추가할 트랜잭션
     */
    private void evictLowestFeeTransaction(Transaction newTx) {
        // 새 트랜잭션의 수수료율
        double newFeeRate = (double) newTx.getFee() / newTx.getSize();

        // 가장 낮은 수수료율의 트랜잭션 찾기
        Transaction lowestFeeTx = transactions.values().stream()
                .min((tx1, tx2) -> {
                    double feeRate1 = (double) tx1.getFee() / tx1.getSize();
                    double feeRate2 = (double) tx2.getFee() / tx2.getSize();
                    return Double.compare(feeRate1, feeRate2);
                })
                .orElse(null);

        if (lowestFeeTx != null) {
            double lowestFeeRate = (double) lowestFeeTx.getFee() / lowestFeeTx.getSize();

            // 새 트랜잭션의 수수료율이 더 높은 경우에만 교체
            if (newFeeRate > lowestFeeRate) {
                String removedTxid = bytesToHex(lowestFeeTx.getTxid());
                transactions.remove(removedTxid);
                System.out.println("Evicted low fee transaction: " + removedTxid);
            }
        }
    }

    /**
     * 메모리 풀 상태 출력
     */
    public void printStatus() {
        System.out.println("\n=== Mempool Status ===");
        System.out.println("Total transactions: " + transactions.size());

        if (transactions.isEmpty()) {
            System.out.println("(Empty)");
        } else {
            long totalFees = transactions.values().stream()
                    .mapToLong(Transaction::getFee)
                    .sum();

            double avgFeeRate = transactions.values().stream()
                    .mapToDouble(tx -> (double) tx.getFee() / tx.getSize())
                    .average()
                    .orElse(0.0);

            System.out.println("Total fees: " + totalFees + " satoshis");
            System.out.println("Average fee rate: " + String.format("%.2f", avgFeeRate) + " sat/byte");

            // 상위 5개 트랜잭션 출력
            System.out.println("\nTop 5 transactions by fee:");
            getTopTransactionsByFee(5).forEach(tx -> {
                String txid = bytesToHex(tx.getTxid());
                double feeRate = (double) tx.getFee() / tx.getSize();
                System.out.println("  " + txid.substring(0, 16) + "... | " +
                        tx.getFee() + " sat | " +
                        String.format("%.2f", feeRate) + " sat/byte");
            });
        }
        System.out.println("=====================\n");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
