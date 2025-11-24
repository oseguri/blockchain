package util;
import org.apache.commons.codec.DecoderException;
import org.bouncycastle.jcajce.provider.symmetric.AES;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;

public class KeyGenerator {
    private static final BigInteger N = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    /**
     * String value로 hash 값 반환
     */
    public static byte[] generateBySHA256() {
        BigInteger priv;
        byte[] hash;
        do {
            String value = Random.generateRandomString();
            hash = Hash.HASH256(value);
            priv = new BigInteger(1, hash);
        } while (priv.compareTo(BigInteger.ZERO) <= 0 || priv.compareTo(N) >= 0);
        return hash;
    }

    /**
     * 개인키로 seck256k1 공개키 좌표 계산
     */
    public static ECPoint getPublicPoint(byte[] privateKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, DecoderException {
        Security.addProvider(new BouncyCastleProvider());
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        BigInteger privateKeyInteger = new BigInteger(1, privateKey);
        return ecSpec.getG().multiply(privateKeyInteger);
    }

    /**
     * 압축 공개키 계산
     */
    public static byte[] getCompressedPublicKey(byte[] privateKey) throws DecoderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        return getPublicPoint(privateKey).getEncoded(true);
    }

    public static SecretKey generateSymmetricKey() throws NoSuchAlgorithmException {
        javax.crypto.KeyGenerator generator = javax.crypto.KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    public static ECPrivateKey toECPrivateKey(byte[] privBytes) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Security.addProvider(new BouncyCastleProvider());
        BigInteger s = new BigInteger(1, privBytes);

        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPrivateKeySpec keySpec = new ECPrivateKeySpec(s, ecSpec);
        KeyFactory kf = KeyFactory.getInstance("EC","BC");
        return (ECPrivateKey) kf.generatePrivate(keySpec);
    }


}
