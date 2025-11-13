package util;
import org.apache.commons.codec.DecoderException;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class KeyGenerator {
    /**
     * String value로 hash 값 반환
     * @param value
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static byte[] generateBySHA256(String value) throws NoSuchAlgorithmException {
        return Hash.HASH256(value);
    }

    /**
     * 개인키로 seck256k1 공개키 좌표 계산
     * @param privateKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidKeySpecException
     */
    private static ECPoint getPublicPoint(byte[] privateKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, DecoderException {
        Security.addProvider(new BouncyCastleProvider());
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
        BigInteger privateKeyInteger = new BigInteger(1, privateKey);
        return ecSpec.getG().multiply(privateKeyInteger);
    }

    /**
     * 압축 공개키 계산
     * @param privateKey
     * @return
     * @throws DecoderException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchProviderException
     */
    public static byte[] getCompressedPublicKey(byte[] privateKey) throws DecoderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        return getPublicPoint(privateKey).getEncoded(true);
    }
}
