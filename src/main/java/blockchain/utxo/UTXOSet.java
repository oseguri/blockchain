package blockchain.utxo;

import blockchain.block.Block;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * UTXO Set 관리
 * 모든 미사용 출력을 추적하고 관리
 */
public class UTXOSet {

    // UTXO 저장소 (key -> UTXO)
    // key = txid:outputIndex
    private final Map<String, UTXO> utxos;

    public UTXOSet() {
        this.utxos = new ConcurrentHashMap<>();
    }

    /**
     * 블록 추가 시 UTXO 세트 업데이트
     * @param block 추가된 블록
     * @param blockHeight 블록 높이
     */
    public void updateWithBlock(Block block, long blockHeight) {
        System.out.println("\n=== Updating UTXO Set ===");
        int added = 0;
        int removed = 0;

        for (Transaction tx : block.getTransactions()) {
            byte[] txid = tx.getTxid();

            // 1. 입력으로 사용된 UTXO 제거 (코인베이스 제외)
            for (TransactionInput input : tx.getInputs()) {
                if (input.getIsCoinbase() != null && input.getIsCoinbase()) {
                    continue; // 코인베이스는 이전 UTXO가 없음
                }

                String key = bytesToHex(input.getPrevTXID()) + ":" + input.getOutputIdx();
                if (utxos.remove(key) != null) {
                    removed++;
                }
            }

            // 2. 출력으로 생성된 새 UTXO 추가
            List<TransactionOutput> outputs = tx.getOutputs();
            for (int i = 0; i < outputs.size(); i++) {
                TransactionOutput output = outputs.get(i);
                UTXO utxo = new UTXO(txid, i, output, blockHeight);
                utxos.put(utxo.getKey(), utxo);
                added++;
            }
        }

        System.out.println("UTXOs added: " + added);
        System.out.println("UTXOs removed: " + removed);
        System.out.println("Total UTXOs: " + utxos.size());
        System.out.println("========================\n");
    }

    /**
     * 특정 주소의 UTXO 조회
     * @param address 주소
     * @return UTXO 리스트
     */
    public List<UTXO> getUTXOsByAddress(String address) {
        byte[] addressBytes = address.getBytes();

        return utxos.values().stream()
                .filter(utxo -> Arrays.equals(utxo.getAddress(), addressBytes))
                .collect(Collectors.toList());
    }

    /**
     * 특정 주소의 잔액 조회
     * @param address 주소
     * @return 잔액 (satoshi)
     */
    public long getBalance(String address) {
        return getUTXOsByAddress(address).stream()
                .mapToLong(UTXO::getValue)
                .sum();
    }

    /**
     * 특정 UTXO 존재 여부 확인
     * @param txid 트랜잭션 ID
     * @param outputIndex 출력 인덱스
     * @return 존재 여부
     */
    public boolean containsUTXO(byte[] txid, int outputIndex) {
        String key = bytesToHex(txid) + ":" + outputIndex;
        return utxos.containsKey(key);
    }

    /**
     * 특정 UTXO 조회
     * @param txid 트랜잭션 ID
     * @param outputIndex 출력 인덱스
     * @return UTXO
     */
    public UTXO getUTXO(byte[] txid, int outputIndex) {
        String key = bytesToHex(txid) + ":" + outputIndex;
        return utxos.get(key);
    }

    /**
     * 트랜잭션에 필요한 UTXO 선택 (코인 선택 알고리즘)
     * @param address 송신자 주소
     * @param amount 필요한 금액
     * @return 선택된 UTXO 리스트
     */
    public List<UTXO> selectUTXOs(String address, long amount) {
        List<UTXO> available = getUTXOsByAddress(address);

        // 금액 순으로 정렬 (큰 것부터)
        available.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<UTXO> selected = new ArrayList<>();
        long total = 0;

        // 탐욕 알고리즘: 큰 UTXO부터 선택
        for (UTXO utxo : available) {
            selected.add(utxo);
            total += utxo.getValue();

            if (total >= amount) {
                break;
            }
        }

        // 충분한 금액이 없는 경우
        if (total < amount) {
            System.out.println("Insufficient funds. Required: " + amount + ", Available: " + total);
            return null;
        }

        return selected;
    }

    /**
     * UTXO 세트 크기 조회
     * @return UTXO 개수
     */
    public int size() {
        return utxos.size();
    }

    /**
     * 모든 UTXO 조회
     * @return UTXO 리스트
     */
    public Collection<UTXO> getAllUTXOs() {
        return new ArrayList<>(utxos.values());
    }

    /**
     * UTXO 세트 초기화
     */
    public void clear() {
        utxos.clear();
    }

    /**
     * UTXO 세트 상태 출력
     */
    public void printStatus() {
        System.out.println("\n=== UTXO Set Status ===");
        System.out.println("Total UTXOs: " + utxos.size());

        long totalValue = utxos.values().stream()
                .mapToLong(UTXO::getValue)
                .sum();

        System.out.println("Total Value: " + totalValue + " satoshis");
        System.out.println("             " + (totalValue / 100000000.0) + " BTC");

        // 주소별 그룹화
        Map<String, Long> balanceByAddress = new HashMap<>();
        for (UTXO utxo : utxos.values()) {
            String address = new String(utxo.getAddress());
            balanceByAddress.merge(address, utxo.getValue(), Long::sum);
        }

        System.out.println("\nBalances by Address:");
        balanceByAddress.forEach((address, balance) -> {
            String shortAddress = address.length() > 20 ?
                    address.substring(0, 20) + "..." : address;
            System.out.println("  " + shortAddress + ": " + balance + " sat");
        });

        System.out.println("======================\n");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
