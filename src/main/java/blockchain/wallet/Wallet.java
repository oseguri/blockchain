package blockchain.wallet;

import blockchain.node.Node;
import blockchain.node.script.Script;
import blockchain.node.sign.Signature;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;
import blockchain.utxo.UTXO;

import java.util.ArrayList;
import java.util.List;

/**
 * 지갑 클래스
 * 트랜잭션 생성, 서명, 잔액 조회 등의 기능 제공
 */
public class Wallet {

    private final Node node;

    public Wallet(Node node) {
        this.node = node;
    }

    /**
     * 트랜잭션 생성 (단순 송금)
     * @param recipientAddress 수신자 주소
     * @param amount 송금 금액 (satoshi)
     * @param feeRate 수수료율 (sat/byte)
     * @return 생성된 트랜잭션
     */
    public Transaction createTransaction(String recipientAddress, long amount, long feeRate)
            throws Exception {

        String senderAddress = node.getAddress();
        System.out.println("\n=== Creating Transaction ===");
        System.out.println("From: " + senderAddress);
        System.out.println("To: " + recipientAddress);
        System.out.println("Amount: " + amount + " satoshis");

        // 1. UTXO 선택 (예상 수수료 포함)
        long estimatedFee = estimateFee(1, 2, feeRate); // 1 input, 2 outputs (추정)
        long totalNeeded = amount + estimatedFee;

        List<UTXO> selectedUTXOs = node.getUtxoSet().selectUTXOs(senderAddress, totalNeeded);

        if (selectedUTXOs == null || selectedUTXOs.isEmpty()) {
            throw new Exception("Insufficient funds. Required: " + totalNeeded);
        }

        // 2. 총 입력 금액 계산
        long totalInput = selectedUTXOs.stream()
                .mapToLong(UTXO::getValue)
                .sum();

        System.out.println("Selected " + selectedUTXOs.size() + " UTXOs");
        System.out.println("Total Input: " + totalInput + " satoshis");

        // 3. 입력 생성
        List<TransactionInput> inputs = new ArrayList<>();
        for (UTXO utxo : selectedUTXOs) {
            // 임시 서명 스크립트 (나중에 실제 서명으로 교체)
            List<byte[]> tempSigScript = new ArrayList<>();
            tempSigScript.add(new byte[71]); // DER 서명 크기 (평균)
            tempSigScript.add(node.getPublicKeyBytes()); // 공개키

            TransactionInput input = new TransactionInput(
                    false,                          // isCoinbase
                    senderAddress,                  // address
                    utxo.getValue(),                // value
                    utxo.getOutput().getPkscript(), // pkScript
                    tempSigScript,                  // sigScript (임시)
                    utxo.getOutputIndex(),          // outputIdx
                    utxo.getTxid()                  // prevTXID
            );

            inputs.add(input);
        }

        // 4. 출력 생성
        List<TransactionOutput> outputs = new ArrayList<>();

        // 출력 1: 수신자에게
        byte[] recipientPkScript = Script.getSigScriptValue(recipientAddress);
        TransactionOutput recipientOutput = new TransactionOutput(
                recipientAddress.getBytes(),
                recipientPkScript,
                amount
        );
        outputs.add(recipientOutput);

        // 5. 실제 수수료 계산
        Transaction tempTx = new Transaction(inputs, outputs);
        long actualFee = (long) (tempTx.getSize() * feeRate);

        // 6. 거스름돈 출력 추가
        long change = totalInput - amount - actualFee;

        if (change > 546) { // Dust limit (546 satoshis)
            byte[] changePkScript = Script.getSigScriptValue(senderAddress);
            TransactionOutput changeOutput = new TransactionOutput(
                    senderAddress.getBytes(),
                    changePkScript,
                    change
            );
            outputs.add(changeOutput);

            System.out.println("Change: " + change + " satoshis");
        } else if (change > 0) {
            // 거스름돈이 너무 작으면 수수료에 추가
            actualFee += change;
            System.out.println("Change too small, added to fee");
        }

        System.out.println("Fee: " + actualFee + " satoshis");

        // 7. 최종 트랜잭션 생성
        Transaction tx = new Transaction(inputs, outputs);
        tx.setFee(actualFee);

        // 8. 서명
        signTransaction(tx);

        System.out.println("✓ Transaction created");
        System.out.println("TXID: " + bytesToHex(tx.getTxid()).substring(0, 16) + "...");
        System.out.println("===========================\n");

        return tx;
    }

    /**
     * 트랜잭션 서명
     * @param tx 서명할 트랜잭션
     */
    private void signTransaction(Transaction tx) throws Exception {
        byte[] privateKey = node.getPrivateKey();
        byte[] messageHash = tx.getTxid();

        // 각 입력에 서명
        for (TransactionInput input : tx.getInputs()) {
            // 코인베이스는 서명 불필요
            if (input.getIsCoinbase() != null && input.getIsCoinbase()) {
                continue;
            }

            // 서명 생성
            byte[] signature = Signature.sign(privateKey, messageHash);

            // 공개키
            byte[] publicKey = node.getPublicKeyBytes();

            // 서명 스크립트 업데이트
            List<byte[]> sigScript = new ArrayList<>();
            sigScript.add(signature);
            sigScript.add(publicKey);

            // 입력의 서명 스크립트 교체 (리플렉션 또는 setter 필요)
            // 간단한 방법: TransactionInput에 setSigScript 메서드 추가
            input.setSigScript(sigScript);
        }
    }

    /**
     * 수수료 추정
     * @param inputCount 입력 개수
     * @param outputCount 출력 개수
     * @param feeRate 수수료율 (sat/byte)
     * @return 추정 수수료
     */
    private long estimateFee(int inputCount, int outputCount, long feeRate) {
        // 평균 트랜잭션 크기 계산
        // 기본 크기 + (입력 크기 * 개수) + (출력 크기 * 개수)
        int baseSize = 10; // 버전, 시간 등
        int inputSize = 148; // 평균 입력 크기 (압축 공개키 기준)
        int outputSize = 34; // 평균 출력 크기

        int estimatedSize = baseSize + (inputSize * inputCount) + (outputSize * outputCount);

        return estimatedSize * feeRate;
    }

    /**
     * 잔액 조회
     * @return 잔액 (satoshi)
     */
    public long getBalance() throws Exception {
        String address = node.getAddress();
        return node.getBalance(address);
    }

    /**
     * 잔액 출력 (읽기 쉬운 형식)
     */
    public void printBalance() throws Exception {
        long balance = getBalance();
        double btc = balance / 100000000.0;

        System.out.println("\n=== Wallet Balance ===");
        System.out.println("Address: " + node.getAddress());
        System.out.println("Balance: " + balance + " satoshis");
        System.out.println("       = " + String.format("%.8f", btc) + " BTC");
        System.out.println("======================\n");
    }

    /**
     * UTXO 목록 출력
     */
    public void printUTXOs() throws Exception {
        String address = node.getAddress();
        List<UTXO> utxos = node.getUtxoSet().getUTXOsByAddress(address);

        System.out.println("\n=== Wallet UTXOs ===");
        System.out.println("Total UTXOs: " + utxos.size());

        for (UTXO utxo : utxos) {
            String txid = bytesToHex(utxo.getTxid()).substring(0, 16) + "...";
            System.out.println("  " + txid + ":" + utxo.getOutputIndex() +
                    " | " + utxo.getValue() + " sat" +
                    " | Block #" + utxo.getBlockHeight());
        }
        System.out.println("====================\n");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}