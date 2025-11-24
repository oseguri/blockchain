package blockchain.node.sign;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.crypto.params.ECNamedDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import util.Hash;
import util.Random;

import java.math.BigInteger;

public class Signature {
    static {
        java.security.Security.addProvider(new BouncyCastleProvider());
    }
    public static byte[] sign(byte[] privateKey, byte[] messageHash) {
        ECDSASigner signer = new ECDSASigner();
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier("1.2.840.10045.3.1.7");
        ECNamedDomainParameters params = new ECNamedDomainParameters(
                oid
                ,spec.getCurve()
                ,spec.getG()
                ,spec.getN()
                ,spec.getH()
        );
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(
                new BigInteger(1, privateKey),
                params
        );
        signer.init(true, privKey);

        // 2. 무작위 값 k 생성
        String value = Random.generateRandomString();
        Hash.HASH256(value);

        // 3. ECDSA 서명 수행
        BigInteger[] signature = signer.generateSignature(messageHash);

        // 4. DER 형식으로 인코딩
        byte[] r = signature[0].toByteArray();
        byte[] s = signature[1].toByteArray();

        // r 앞에 00 붙이기 (부호 비트가 1인 경우)

        // DER 형식 조합: 30 || [총길이] || 02 || [r길이] || r || 02 || [s길이] || s
        byte[] der = new byte[6 + r.length + s.length];
        der[0] = 0x30;
        der[1] = (byte)(4 + r.length + s.length);
        der[2] = 0x02;
        der[3] = (byte)r.length;
        System.arraycopy(r, 0, der, 4, r.length);
        der[4 + r.length] = 0x02;
        der[5 + r.length] = (byte)s.length;
        System.arraycopy(s, 0, der, 6 + r.length, s.length);

        return der;
    }
}
