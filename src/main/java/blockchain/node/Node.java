package blockchain.node;

import blockchain.block.Block;
import blockchain.block.GenesisBlock;
import blockchain.network.P2PNetwork;
import blockchain.storage.BlockchainStorage;
import blockchain.transaction.Transaction;
import blockchain.transaction.TransactionInput;
import blockchain.transaction.TransactionOutput;
import blockchain.utxo.UTXO;
import blockchain.utxo.UTXOSet;
import blockchain.validation.TransactionValidator;
import blockchain.validation.ValidationResult;
import org.bitcoinj.base.Base58;
import org.bouncycastle.util.encoders.Hex;
import util.Hash;
import util.KeyGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Node {
    private final byte[] privateKey;
    private final List<Block> blockList;
    private UTXOSet utxoSet;
    private final TransactionValidator validator;
    private final BlockchainStorage storage;
    private P2PNetwork p2pNetwork;

    public Node(String storagePath, int p2pPort) throws Exception {
        this.storage = new BlockchainStorage(storagePath);
        this.privateKey = loadOrGeneratePrivateKey();
        this.blockList = new ArrayList<>();
        this.utxoSet = new UTXOSet();

        // ⭐ 블록체인 로드 또는 제네시스 생성 (한 번만 실행)
        loadOrInitializeBlockchain();

        this.validator = new TransactionValidator(utxoSet);

        // P2P 네트워크 초기화
        if (p2pPort > 0) {
            this.p2pNetwork = new P2PNetwork(this, p2pPort);
        }

        System.out.println("Node initialized");
        System.out.println("Address: " + getAddress());
        if (p2pPort > 0) {
            System.out.println("P2P Port: " + p2pPort);
        }
        System.out.println("Chain Height: " + blockList.size());
    }

    /**
     * 블록체인 로드 또는 초기화 (통합)
     */
    private void loadOrInitializeBlockchain() throws Exception {
        System.out.println("\n=== Initializing Blockchain ===");

        // 1. Storage에서 로드 시도
        List<Block> savedBlocks = storage.loadBlockchain();

        if (!savedBlocks.isEmpty()) {
            // Storage에 블록이 있으면 로드
            System.out.println("Found existing blockchain in storage");
            blockList.addAll(savedBlocks);

            // UTXO Set 재구축
            System.out.println("Rebuilding UTXO Set...");
            for (int i = 0; i < blockList.size(); i++) {
                utxoSet.updateWithBlock(blockList.get(i), i);
            }

            System.out.println("✓ Blockchain loaded: " + blockList.size() + " blocks");

        } else {
            // Storage가 비어있으면 제네시스 블록 생성
            System.out.println("No existing blockchain found");
            System.out.println("Creating Genesis Block...");

            Block genesisBlock = GenesisBlock.getGenesisBlock();

            // 메모리에 추가
            blockList.add(genesisBlock);
            utxoSet.updateWithBlock(genesisBlock, 0);

            // Storage에 저장
            storage.saveBlock(genesisBlock);
            storage.saveBlockTransactions(genesisBlock);
            storage.setChainHeight(1);
            storage.setBestBlockHash(genesisBlock.getBlockHash());

            System.out.println("✓ Genesis Block created and saved");
        }

        System.out.println("================================\n");
    }

    /**
     * P2P 없는 노드 생성 (로컬 전용)
     */
    public Node(String storagePath) throws Exception {
        this(storagePath, 0);
    }

    /**
     * P2P 네트워크 시작
     */
    public void startP2P() {
        if (p2pNetwork != null) {
            p2pNetwork.start();
        }
    }

    /**
     * 피어에 연결
     */
    public boolean connectToPeer(String host, int port) {
        if (p2pNetwork != null) {
            return p2pNetwork.connectToPeer(host, port);
        }
        return false;
    }

    /**
     * 블록 추가 (P2P 브로드캐스트 포함)
     */
    public void addBlock(Block block) {
        addBlock(block, true);
    }

    /**
     * 블록 추가
     * @param broadcast P2P로 브로드캐스트 할지 여부
     */
    public boolean addBlock(Block block, boolean broadcast) {
        if (isValidNewBlock(block, getLatestBlock())) {
            return false;
        }

        // 블록 내 트랜잭션 검증
        for (Transaction tx : block.getTransactions()) {
            if (isCoinbaseTransaction(tx)) {
                continue;
            }

            ValidationResult result = validator.validateTransaction(tx);
            if (!result.isValid()) {
                System.out.println("Invalid transaction in block: " + result.getMessage());
                return false;
            }
        }

        try {
            // 메모리에 추가
            blockList.add(block);
            int height = blockList.size() - 1;

            // UTXO Set 업데이트
            utxoSet.updateWithBlock(block, height);

            // Storage에 저장
            storage.saveBlock(block);
            storage.saveBlockTransactions(block);
            storage.setChainHeight(blockList.size());
            storage.setBestBlockHash(block.getBlockHash());

            System.out.println("✓ Block added! Height: " + height);

            // P2P 브로드캐스트
            if (broadcast && p2pNetwork != null) {
                p2pNetwork.broadcastBlock(block);
            }

            return true;

        } catch (Exception e) {
            System.err.println("Failed to add block: " + e.getMessage());
            e.printStackTrace();
            blockList.removeLast();
            return false;
        }
    }

    /**
     * 외부에서 받은 블록 처리 (브로드캐스트 안 함)
     */
    public boolean receiveBlock(Block block) {
        return addBlock(block, false);
    }

    /**
     * 체인 교체 (더 긴 체인 수신 시)
     */
    public void replaceChain(List<Block> newChain) {
        System.out.println("\n=== Evaluating New Chain ===");
        System.out.println("Current chain length: " + blockList.size());
        System.out.println("New chain length: " + newChain.size());

        // 길이 체크
        if (newChain.size() <= blockList.size()) {
            System.out.println("✗ New chain is not longer");
            return;
        }

        // 제네시스 블록 검증
        if (!GenesisBlock.isGenesisBlock(newChain.getFirst())) {
            System.out.println("✗ Invalid genesis block");
            return;
        }

        // 전체 체인 검증
        for (int i = 1; i < newChain.size(); i++) {
            if (isValidNewBlock(newChain.get(i), newChain.get(i - 1))) {
                System.out.println("✗ Invalid block at height: " + i);
                return;
            }
        }

        System.out.println("✓ New chain is valid");

        try {
            // 기존 체인 백업

            // 새 체인으로 교체
            blockList.clear();
            blockList.addAll(newChain);

            // UTXO Set 재구축
            utxoSet = new UTXOSet();
            for (int i = 0; i < blockList.size(); i++) {
                utxoSet.updateWithBlock(blockList.get(i), i);
            }

            // Storage 업데이트
            for (Block block : newChain) {
                storage.saveBlock(block);
                storage.saveBlockTransactions(block);
            }
            storage.setChainHeight(blockList.size());
            storage.setBestBlockHash(getLatestBlock().getBlockHash());

            System.out.println("✓ Chain replaced successfully");
            System.out.println("New chain height: " + blockList.size());

        } catch (Exception e) {
            System.err.println("Failed to replace chain: " + e.getMessage());
        }
    }

    /**
     * 체인 동기화 요청
     */
    public boolean requestChainSync() {
        if (p2pNetwork != null) {
            return p2pNetwork.requestChainSync();
        }
        return false;
    }

    private byte[] loadOrGeneratePrivateKey() throws Exception {
        byte[] key = storage.loadPrivateKey();

        if (key != null) {
            System.out.println("✓ Private key loaded from storage");
            return key;
        }

        System.out.println("No existing private key found");
        System.out.println("Generating new private key...");
        key = KeyGenerator.generateBySHA256();

        storage.savePrivateKey(key);
        System.out.println("✓ New private key generated and saved");

        return key;
    }

    private boolean isValidNewBlock(Block newBlock, Block previousBlock) {
        if (previousBlock == null) {
            return true;
        }

        if (!Arrays.equals(newBlock.getPrevHash(), previousBlock.getBlockHash())) {
            return true;
        }

        byte[] calculatedHash = newBlock.calculateBlockHash();
        return !Arrays.equals(calculatedHash, newBlock.getBlockHash());
    }

    private boolean isCoinbaseTransaction(Transaction tx) {
        if (tx.getInputs().isEmpty()) {
            return false;
        }
        TransactionInput firstInput = tx.getInputs().getFirst();
        return firstInput.getIsCoinbase() != null && firstInput.getIsCoinbase();
    }

    // Getters
    public Block getLatestBlock() {
        if (blockList.isEmpty()) {
            return null;
        }
        return blockList.getLast();
    }

    public Block getBlockAtHeight(int height) {
        if (height < 0 || height >= blockList.size()) {
            return null;
        }
        return blockList.get(height);
    }

    public int getChainLength() {
        return blockList.size();
    }

    public List<Block> getBlockList() {
        return new ArrayList<>(blockList);
    }

    public long getBalance(String address) {
        return utxoSet.getBalance(address);
    }

    public UTXOSet getUtxoSet() {
        return utxoSet;
    }

    public TransactionValidator getValidator() {
        return validator;
    }

    public P2PNetwork getP2PNetwork() {
        return p2pNetwork;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }


    public String getPublicKey() throws Exception {
        return Hex.toHexString(KeyGenerator.getCompressedPublicKey(this.privateKey));
    }

    public byte[] getPublicKeyBytes() throws Exception {
        return KeyGenerator.getCompressedPublicKey(this.privateKey);
    }

    public String getAddress() throws Exception {
        byte[] hash160 = Hash.HASH160(getPublicKey());
        byte net = 0x00;

        byte[] payload = new byte[1 + hash160.length];
        payload[0] = net;
        System.arraycopy(hash160, 0, payload, 1, hash160.length);

        byte[] checkSum4 = Arrays.copyOf(Hash.HASH256(payload), 4);

        byte[] addrBytes = new byte[payload.length + 4];
        System.arraycopy(payload, 0, addrBytes, 0, payload.length);
        System.arraycopy(checkSum4, 0, addrBytes, payload.length, 4);

        return Base58.encode(addrBytes);
    }

    public void shutdown() {
        System.out.println("Shutting down node...");

        if (p2pNetwork != null) {
            p2pNetwork.shutdown();
        }

        storage.close();
        System.out.println("Node shutdown complete");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 트랜잭션 생성
     * @param recipientAddress 수신자 주소
     * @param amount 송금 금액 (satoshi)
     * @return 생성된 트랜잭션
     */
    public Transaction createTransaction(String recipientAddress, long amount) throws Exception {
        String senderAddress = getAddress();
        long feeRate = 1; // 기본 수수료율: 1 sat/byte

        System.out.println("\n=== Creating Transaction ===");
        System.out.println("From: " + senderAddress);
        System.out.println("To: " + recipientAddress);
        System.out.println("Amount: " + amount + " satoshis");

        // 1. UTXO 선택 (예상 수수료 포함)
        long estimatedFee = estimateTransactionFee(feeRate); // 1 input, 2 outputs (추정)
        long totalNeeded = amount + estimatedFee;
        List<UTXO> selectedUTXOs = utxoSet.selectUTXOs(senderAddress, totalNeeded);

        if (selectedUTXOs == null || selectedUTXOs.isEmpty()) {
            throw new Exception("Insufficient funds. Required: " + totalNeeded + ", Available: " + getBalance(senderAddress));
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
            tempSigScript.add(getPublicKeyBytes()); // 공개키

            TransactionInput input = new TransactionInput(
                    false,                           // isCoinbase
                    senderAddress,                   // address
                    utxo.getValue(),                 // value
                    utxo.getOutput().getPkscript(),  // pkScript
                    tempSigScript,                   // sigScript (임시)
                    utxo.getOutputIndex(),           // outputIdx
                    utxo.getTxid()                   // prevTXID
            );
            inputs.add(input);
        }

        // 4. 출력 생성
        List<TransactionOutput> outputs = new ArrayList<>();

        // 출력 1: 수신자에게
        byte[] recipientPkScript = blockchain.node.script.Script.getSigScriptValue(recipientAddress);
        TransactionOutput recipientOutput = new TransactionOutput(
                recipientAddress.getBytes(),
                recipientPkScript,
                amount
        );
        outputs.add(recipientOutput);

        // 5. 실제 수수료 계산
        Transaction tempTx = new Transaction(inputs, outputs);
        long actualFee = tempTx.getSize() * feeRate;

        // 6. 거스름돈 출력 추가
        long change = totalInput - amount - actualFee;
        if (change > 546) { // Dust limit (546 satoshis)
            byte[] changePkScript = blockchain.node.script.Script.getSigScriptValue(senderAddress);
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
        byte[] messageHash = tx.getTxid();

        // 각 입력에 서명
        for (TransactionInput input : tx.getInputs()) {
            // 코인베이스는 서명 불필요
            if (input.getIsCoinbase() != null && input.getIsCoinbase()) {
                continue;
            }

            // 서명 생성
            byte[] signature = blockchain.node.sign.Signature.sign(privateKey, messageHash);
            byte[] publicKey = getPublicKeyBytes();

            // 서명 스크립트 업데이트
            List<byte[]> sigScript = new ArrayList<>();
            sigScript.add(signature);
            sigScript.add(publicKey);

            input.setSigScript(sigScript);
        }
    }

    /**
     * 트랜잭션 수수료 추정
     *
     * @param feeRate 수수료율 (sat/byte)
     * @return 추정 수수료
     */
    private long estimateTransactionFee(long feeRate) {
        // 평균 트랜잭션 크기 계산
        int baseSize = 10;      // 버전, 시간 등
        int inputSize = 148;    // 평균 입력 크기 (압축 공개키 기준)
        int outputSize = 34;    // 평균 출력 크기
        int estimatedSize = baseSize + (inputSize) + (outputSize * 2);
        return estimatedSize * feeRate;
    }

    /**
     * 트랜잭션을 P2P 네트워크에 브로드캐스트
     * @param tx 브로드캐스트할 트랜잭션
     */
    public void broadcastTransaction(Transaction tx) {
        if (p2pNetwork != null) {
            p2pNetwork.broadcastTransaction(tx);
            System.out.println("   Transaction broadcasted to peers");
        } else {
            System.out.println("   P2P network not initialized. Transaction not broadcasted.");
        }
    }

}

