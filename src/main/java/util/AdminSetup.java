package util;

import org.bitcoinj.base.Base58;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Admin 계정 설정 - RocksDB에 저장
 */
public class AdminSetup {

    // 고정 Seed
    private static final String ADMIN_SEED = "MyBlockchain_Admin_2025_Genesis_Master_Key";
    private static final String ADMIN_STORAGE_PATH = "./admin";

//    public static void main(String[] args) {
//        System.out.println("\n╔════════════════════════════════════════════════════╗");
//        System.out.println("║        Admin Account Setup (RocksDB)               ║");
//        System.out.println("╚════════════════════════════════════════════════════╝\n");
//
//        try {
//            // BlockchainStorage 초기화
//            BlockchainStorage storage = new BlockchainStorage(ADMIN_STORAGE_PATH);
//
//            // Seed로부터 개인키 생성
//            byte[] privateKey = generatePrivateKeyFromSeed(ADMIN_SEED);
//            String address = generateAddress(privateKey);
//
//            // 기존 키 확인
//            if (storage.hasPrivateKey()) {
//                byte[] existingKey = storage.loadPrivateKey();
//                System.out.println("  Admin private key already exists in storage!");
//
//                if (Arrays.equals(existingKey, privateKey)) {
//                    System.out.println(" Existing key matches the seed-generated key!");
//                    String existingAddress = generateAddress(existingKey);
//                    System.out.println("\n Existing Admin Info:");
//                    System.out.println("   Address: " + existingAddress);
//                    storage.close();
//                    return;
//                } else {
//                    System.out.println("  Existing key DIFFERS from seed!");
//                    System.out.println("   Overwriting with seed-generated key...");
//                }
//            }
//
//            // 개인키 저장 (RocksDB metaDB에)
//            storage.savePrivateKey(privateKey);
//
//            System.out.println("\n Admin account configured successfully!");
//            System.out.println("\n Admin Account Info (Deterministic):");
//            System.out.println("   Seed:        \"" + ADMIN_SEED + "\"");
//            System.out.println("   Address:     " + address);
//            System.out.println("   Private Key: " + bytesToHex(privateKey));
//            System.out.println("   Storage:     " + ADMIN_STORAGE_PATH + "/meta (RocksDB)");
//
//            System.out.println("\n  IMPORTANT: Update GenesisBlock.java!");
//            System.out.println("   private static final String GENESIS_MINER = \"" + address + "\";");
//
//            System.out.println("\n This key is stored in RocksDB and will be");
//            System.out.println("   automatically loaded when the node starts.");
//
//            storage.close();
//
//        } catch (Exception e) {
//            System.err.println("  Error: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    /**
     * Seed 문자열로부터 개인키 생성
     */
    private static byte[] generatePrivateKeyFromSeed(String seed) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(seed.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 개인키로부터 주소 생성
     */
    private static String generateAddress(byte[] privateKey) throws Exception {
        byte[] publicKey = util.KeyGenerator.getCompressedPublicKey(privateKey);
        String pubKeyHex = bytesToHex(publicKey);
        byte[] hash160 = util.Hash.HASH160(pubKeyHex);

        byte net = 0x00;
        byte[] payload = new byte[1 + hash160.length];
        payload[0] = net;
        System.arraycopy(hash160, 0, payload, 1, hash160.length);

        byte[] checkSum4 = Arrays.copyOf(util.Hash.HASH256(payload), 4);
        byte[] addrBytes = new byte[payload.length + 4];
        System.arraycopy(payload, 0, addrBytes, 0, payload.length);
        System.arraycopy(checkSum4, 0, addrBytes, payload.length, 4);

        return Base58.encode(addrBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

