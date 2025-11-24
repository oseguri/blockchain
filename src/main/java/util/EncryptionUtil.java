package util;


import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import javax.crypto.KeyAgreement;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class EncryptionUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final int AES_KEY_SIZE = 32; // 256 bits
    private static final int GCM_NONCE_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    /**
     * AES 대칭키 생성
     */
    public static byte[] generateAESKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[AES_KEY_SIZE];
        random.nextBytes(key);
        return key;
    }

    /**
     * AES-GCM으로 데이터 암호화
     * @param plaintext 평문
     * @param key AES 키 (32 bytes)
     * @return nonce(12) + 암호문 + tag(16)
     */
    public static byte[] encryptAES(byte[] plaintext, byte[] key) throws Exception {
        // Nonce 생성 (12 bytes)
        SecureRandom random = new SecureRandom();
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        random.nextBytes(nonce);

        // AES-GCM 암호화
        GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        AEADParameters parameters = new AEADParameters(
                new KeyParameter(key),
                GCM_TAG_LENGTH,
                nonce,
                null
        );

        cipher.init(true, parameters);

        byte[] ciphertext = new byte[cipher.getOutputSize(plaintext.length)];
        int len = cipher.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);
        cipher.doFinal(ciphertext, len);

        // nonce + ciphertext 합치기
        byte[] result = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, result, 0, nonce.length);
        System.arraycopy(ciphertext, 0, result, nonce.length, ciphertext.length);

        return result;
    }

    /**
     * AES-GCM으로 데이터 복호화
     * @param encrypted nonce + 암호문 + tag
     * @param key AES 키
     * @return 평문
     */
    public static byte[] decryptAES(byte[] encrypted, byte[] key) throws Exception {
        // nonce 추출
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        System.arraycopy(encrypted, 0, nonce, 0, GCM_NONCE_LENGTH);

        // 암호문 추출
        byte[] ciphertext = new byte[encrypted.length - GCM_NONCE_LENGTH];
        System.arraycopy(encrypted, GCM_NONCE_LENGTH, ciphertext, 0, ciphertext.length);

        // AES-GCM 복호화
        GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        AEADParameters parameters = new AEADParameters(
                new KeyParameter(key),
                GCM_TAG_LENGTH,
                nonce,
                null
        );

        cipher.init(false, parameters);

        byte[] plaintext = new byte[cipher.getOutputSize(ciphertext.length)];
        int len = cipher.processBytes(ciphertext, 0, ciphertext.length, plaintext, 0);
        cipher.doFinal(plaintext, len);

        return plaintext;
    }

    /**
     * ECIES로 대칭키 암호화 (공개키로)
     * @param aesKey AES 키
     * @param recipientPublicKey 수신자 공개키 (압축 형식, 33 bytes)
     * @return 암호화된 AES 키
     */
    public static byte[] encryptAESKey(byte[] aesKey, byte[] recipientPublicKey)
            throws Exception {

        // 임시 ECDH 키쌍 생성
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        keyGen.initialize(ecSpec, new SecureRandom());
        KeyPair ephemeralKeyPair = keyGen.generateKeyPair();

        // 수신자 공개키를 ECPublicKey로 변환
        ECPublicKey recipientPubKey = convertToECPublicKey(recipientPublicKey);

        // ECDH 공유 비밀 생성
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "BC");
        keyAgreement.init(ephemeralKeyPair.getPrivate());
        keyAgreement.doPhase(recipientPubKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        // 공유 비밀을 AES 키로 사용 (SHA-256 해싱)
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] derivedKey = sha256.digest(sharedSecret);

        // AES 키를 derived key로 암호화
        byte[] encryptedKey = xor(aesKey, Arrays.copyOf(derivedKey, aesKey.length));

        // 임시 공개키 + 암호화된 키
        byte[] ephemeralPubKey = ((org.bouncycastle.jce.interfaces.ECPublicKey)
                ephemeralKeyPair.getPublic())
                .getQ().getEncoded(true); // 압축 형식

        byte[] result = new byte[ephemeralPubKey.length + encryptedKey.length];
        System.arraycopy(ephemeralPubKey, 0, result, 0, ephemeralPubKey.length);
        System.arraycopy(encryptedKey, 0, result, ephemeralPubKey.length, encryptedKey.length);

        return result;
    }

    /**
     * ECIES로 대칭키 복호화 (개인키로)
     * @param encryptedData 임시 공개키 + 암호화된 AES 키
     * @param recipientPrivateKey 수신자 개인키 (32 bytes)
     * @return 복호화된 AES 키
     */
    public static byte[] decryptAESKey(byte[] encryptedData, byte[] recipientPrivateKey)
            throws Exception {

        // 임시 공개키 추출 (압축 형식, 33 bytes)
        byte[] ephemeralPubKey = new byte[33];
        System.arraycopy(encryptedData, 0, ephemeralPubKey, 0, 33);

        // 암호화된 키 추출
        byte[] encryptedKey = new byte[encryptedData.length - 33];
        System.arraycopy(encryptedData, 33, encryptedKey, 0, encryptedKey.length);

        // 수신자 개인키를 PrivateKey로 변환
        PrivateKey privKey = convertToPrivateKey(recipientPrivateKey);

        // 임시 공개키를 ECPublicKey로 변환
        ECPublicKey ephemeralPublicKey = convertToECPublicKey(ephemeralPubKey);

        // ECDH 공유 비밀 생성
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "BC");
        keyAgreement.init(privKey);
        keyAgreement.doPhase(ephemeralPublicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        // 공유 비밀을 AES 키로 사용
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] derivedKey = sha256.digest(sharedSecret);

        // AES 키 복호화
        byte[] aesKey = xor(encryptedKey, Arrays.copyOf(derivedKey, encryptedKey.length));

        return aesKey;
    }

    /**
     * 압축된 공개키를 ECPublicKey로 변환
     */
    private static ECPublicKey convertToECPublicKey(byte[] compressedPubKey)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        org.bouncycastle.math.ec.ECPoint point = spec.getCurve().decodePoint(compressedPubKey);

        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, spec);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");

        return (ECPublicKey) keyFactory.generatePublic(pubKeySpec);
    }

    /**
     * 개인키 바이트를 PrivateKey로 변환
     */
    private static PrivateKey convertToPrivateKey(byte[] privateKeyBytes)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        BigInteger d = new BigInteger(1, privateKeyBytes);

        org.bouncycastle.jce.spec.ECPrivateKeySpec privKeySpec =
                new org.bouncycastle.jce.spec.ECPrivateKeySpec(d, spec);

        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        return keyFactory.generatePrivate(privKeySpec);
    }

    /**
     * XOR 연산
     */
    private static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[Math.min(a.length, b.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }
}
