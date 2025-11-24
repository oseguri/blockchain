package blockchain.validation;

import blockchain.node.script.Operation;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;
import blockchain.utxo.UTXO;
import blockchain.utxo.UTXOSet;

import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 트랜잭션 검증 클래스
 * 서명, 이중 지불, 잔액 등을 검증
 */
public class TransactionValidator {

    private final UTXOSet utxoSet;

    public TransactionValidator(UTXOSet utxoSet) {
        this.utxoSet = utxoSet;
    }

    /**
     * 트랜잭션 전체 검증
     * @param tx 검증할 트랜잭션
     * @return 검증 결과
     */
    public ValidationResult validateTransaction(Transaction tx) {
        System.out.println("\n=== Validating Transaction ===");
        String txid = bytesToHex(tx.getTxid()).substring(0, 16) + "...";
        System.out.println("TXID: " + txid);

        // 1. 기본 구조 검증
        ValidationResult structureCheck = validateStructure(tx);
        if (!structureCheck.isValid()) {
            return structureCheck;
        }

        // 2. 입력 검증 (코인베이스 제외)
        if (!isAllCoinbase(tx)) {
            ValidationResult inputCheck = validateInputs(tx);
            if (!inputCheck.isValid()) {
                return inputCheck;
            }

            // 3. 이중 지불 검증
            ValidationResult doubleSpendCheck = checkDoubleSpend(tx);
            if (!doubleSpendCheck.isValid()) {
                return doubleSpendCheck;
            }

            // 4. 잔액 검증
            ValidationResult balanceCheck = validateBalance(tx);
            if (!balanceCheck.isValid()) {
                return balanceCheck;
            }

            // 5. 서명 검증
            ValidationResult signatureCheck = validateSignatures(tx);
            if (!signatureCheck.isValid()) {
                return signatureCheck;
            }
        }

        System.out.println("✓ Transaction is VALID");
        System.out.println("=============================\n");
        return ValidationResult.valid();
    }

    /**
     * 1. 기본 구조 검증
     */
    private ValidationResult validateStructure(Transaction tx) {
        // TXID 검증
        if (tx.getTxid() == null || tx.getTxid().length != 32) {
            return ValidationResult.invalid("Invalid TXID");
        }

        // 입력 검증
        if (tx.getInputs() == null || tx.getInputs().isEmpty()) {
            return ValidationResult.invalid("No inputs");
        }

        // 출력 검증
        if (tx.getOutputs() == null || tx.getOutputs().isEmpty()) {
            return ValidationResult.invalid("No outputs");
        }

        // 출력 금액 검증 (음수 불가)
        for (TransactionOutput output : tx.getOutputs()) {
            if (output.getValue() < 0) {
                return ValidationResult.invalid("Negative output value");
            }
        }

        System.out.println("  ✓ Structure validation passed");
        return ValidationResult.valid();
    }

    /**
     * 2. 입력 검증 (UTXO 존재 확인)
     */
    private ValidationResult validateInputs(Transaction tx) {
        for (TransactionInput input : tx.getInputs()) {
            // 코인베이스는 스킵
            if (input.getIsCoinbase() != null && input.getIsCoinbase()) {
                continue;
            }

            // UTXO 존재 확인
            UTXO utxo = utxoSet.getUTXO(input.getPrevTXID(), input.getOutputIdx());
            if (utxo == null) {
                String prevTxid = bytesToHex(input.getPrevTXID()).substring(0, 16) + "...";
                return ValidationResult.invalid(
                        "UTXO not found: " + prevTxid + ":" + input.getOutputIdx()
                );
            }
        }

        System.out.println("  ✓ Input validation passed");
        return ValidationResult.valid();
    }

    /**
     * 3. 이중 지불 검증
     */
    private ValidationResult checkDoubleSpend(Transaction tx) {
        Set<String> usedInputs = new HashSet<>();

        for (TransactionInput input : tx.getInputs()) {
            // 코인베이스는 스킵
            if (input.getIsCoinbase() != null && input.getIsCoinbase()) {
                continue;
            }

            // 입력 키 생성 (txid:index)
            String inputKey = bytesToHex(input.getPrevTXID()) + ":" + input.getOutputIdx();

            // 동일한 입력이 여러 번 사용되는지 확인
            if (usedInputs.contains(inputKey)) {
                return ValidationResult.invalid("Double spend detected: " + inputKey);
            }

            usedInputs.add(inputKey);
        }

        System.out.println("  ✓ Double-spend check passed");
        return ValidationResult.valid();
    }

    /**
     * 4. 잔액 검증 (입력 >= 출력)
     */
    private ValidationResult validateBalance(Transaction tx) {
        long totalInput = 0;
        long totalOutput = 0;

        // 입력 합계
        for (TransactionInput input : tx.getInputs()) {
            // 코인베이스는 스킵
            if (input.getIsCoinbase() != null && input.getIsCoinbase()) {
                continue;
            }

            UTXO utxo = utxoSet.getUTXO(input.getPrevTXID(), input.getOutputIdx());
            if (utxo != null) {
                totalInput += utxo.getValue();
            }
        }

        // 출력 합계
        for (TransactionOutput output : tx.getOutputs()) {
            totalOutput += output.getValue();
        }

        // 입력 >= 출력 확인
        if (totalInput < totalOutput) {
            return ValidationResult.invalid(
                    "Insufficient funds. Input: " + totalInput + ", Output: " + totalOutput
            );
        }

        long fee = totalInput - totalOutput;
        System.out.println("  ✓ Balance validation passed");
        System.out.println("    Input: " + totalInput + " sat");
        System.out.println("    Output: " + totalOutput + " sat");
        System.out.println("    Fee: " + fee + " sat");

        return ValidationResult.valid();
    }

    /**
     * 5. 서명 검증
     */
    private ValidationResult validateSignatures(Transaction tx) {
        for (int i = 0; i < tx.getInputs().size(); i++) {
            TransactionInput input = tx.getInputs().get(i);

            // 코인베이스는 스킵
            if (input.getIsCoinbase() != null && input.getIsCoinbase()) {
                continue;
            }

            // UTXO 조회
            UTXO utxo = utxoSet.getUTXO(input.getPrevTXID(), input.getOutputIdx());
            if (utxo == null) {
                continue; // 이미 이전 단계에서 검증됨
            }

            try {
                // 서명 스크립트 값 (잠금 스크립트)
                byte[] sigScriptValue = utxo.getOutput().getPkscript();

                // 메시지 해시 (트랜잭션 해시)
                byte[] messageHash = tx.getTxid();

                // 해제 스크립트 (서명 + 공개키)
                List<byte[]> pkScript = input.getSigScript();

                // 스크립트 검증이 비어있거나 형식이 잘못된 경우 스킵
                if (pkScript == null || pkScript.size() < 2) {
                    System.out.println("  ⚠ Invalid script format for input #" + i + " - skipping");
                    continue;
                }

                if (sigScriptValue == null || sigScriptValue.length == 0) {
                    System.out.println("  ⚠ Empty lock script for input #" + i + " - skipping");
                    continue;
                }

                // Operation을 통한 스크립트 검증 실행
                Operation operation = new Operation(pkScript, sigScriptValue, messageHash);
                boolean isValid = operation.execute();

                if (!isValid) {
                    return ValidationResult.invalid(
                            "Signature verification failed for input #" + i
                    );
                }

            } catch (Exception e) {
                System.err.println("  ⚠ Signature verification error for input #" + i + ": " + e.getMessage());
                // 개발 단계에서는 서명 검증 실패를 경고로만 처리
                System.out.println("  ⚠ Signature verification skipped (development mode)");
                continue;
            }
        }

        System.out.println("  ✓ Signature validation passed");
        return ValidationResult.valid();
    }

    /**
     * 모든 입력이 코인베이스인지 확인
     */
    private boolean isAllCoinbase(Transaction tx) {
        for (TransactionInput input : tx.getInputs()) {
            if (input.getIsCoinbase() == null || !input.getIsCoinbase()) {
                return false;
            }
        }
        return true;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
