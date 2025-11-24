package blockchain.wallet;

import blockchain.node.Node;
import org.bitcoinj.base.Base58;
import org.bouncycastle.util.encoders.Hex;
import util.Hash;
import util.KeyGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 여러 지갑(개인키) 관리
 */
public class WalletManager {

    private final Node node;
    private final Map<String, byte[]> wallets; // 이름 -> 개인키

    public WalletManager(Node node) {
        this.node = node;
        this.wallets = new HashMap<>();
    }

    /**
     * 새 지갑 생성
     */
    public String createWallet(String name) throws Exception {
        byte[] privateKey = KeyGenerator.generateBySHA256();
        wallets.put(name, privateKey);

        String address = getAddress(privateKey);
        System.out.println("✓ Wallet created: " + name + " (" + address + ")");

        return address;
    }

    /**
     * 지갑의 개인키 조회
     */
    public byte[] getPrivateKey(String name) {
        return wallets.get(name);
    }

    /**
     * 지갑의 주소 조회
     */
    public String getWalletAddress(String name) throws Exception {
        byte[] privateKey = wallets.get(name);
        if (privateKey == null) {
            throw new IllegalArgumentException("Wallet not found: " + name);
        }
        return getAddress(privateKey);
    }

    /**
     * 지갑의 잔액 조회
     */
    public long getBalance(String name) throws Exception {
        String address = getWalletAddress(name);
        return node.getBalance(address);
    }

    /**
     * 지갑의 공개키 조회
     */
    public byte[] getPublicKey(String name) throws Exception {
        byte[] privateKey = wallets.get(name);
        if (privateKey == null) {
            throw new IllegalArgumentException("Wallet not found: " + name);
        }
        return KeyGenerator.getCompressedPublicKey(privateKey);
    }

    /**
     * 개인키로부터 주소 생성
     */
    private String getAddress(byte[] privateKey) throws Exception {
        byte[] publicKey = KeyGenerator.getCompressedPublicKey(privateKey);
        String pubKeyHex = Hex.toHexString(publicKey);
        byte[] hash160 = Hash.HASH160(pubKeyHex);
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

    public Map<String, byte[]> getWallets() {
        return wallets;
    }
}
