package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import org.bitcoinj.base.Sha256Hash;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Hash {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] HASH256(String value){
        return Sha256Hash.hash(value.getBytes());
    }

    public static byte[] HASH256(byte[] value){
        return Sha256Hash.hash(value);
    }

    public static byte[] HASH160(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("RIPEMD160");
        return md.digest(value.getBytes());
    }
    public static byte[] HASH160(byte[] value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("RIPEMD160");
        return md.digest(value);
    }


}
