package blockchain.peer;
import org.apache.commons.codec.DecoderException;
import org.bitcoinj.base.Base58;
import org.bouncycastle.util.encoders.Hex;
import util.Hash;
import util.KeyGenerator;
import util.Random;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;


public class Peer {

    private final byte[] privateKey;

    public Peer() throws NoSuchAlgorithmException {
        String v = Random.generateRandomString(32);
        this.privateKey = KeyGenerator.generateBySHA256(v);
    }

    public String getPublicKey() throws DecoderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        return Hex.toHexString(KeyGenerator.getCompressedPublicKey(this.privateKey));

    }

    public String getAddress() throws DecoderException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        byte[] hash160 = Hash.HASH160(getPublicKey());
        byte net = 0x00;

        byte[] payload = getPayload(hash160, net);
        byte[] checkSum4 = Arrays.copyOf(Hash.HASH256(payload),4 );
        byte[] addrBytes = makeAddr(payload, checkSum4);

        return Base58.encode(addrBytes);

    }

    private byte[] getPayload(byte[] hash160, byte net) {
        byte[] payload = new byte[1 + hash160.length];
        payload[0] = net;
        System.arraycopy(hash160, 0, payload, 1, hash160.length);
        return  payload;
    }

    private byte[] makeAddr(byte[] payload, byte[] checkSum4) {
        byte[] addrBytes = new byte[payload.length + 4];
        System.arraycopy(payload, 0, addrBytes, 0, payload.length);
        System.arraycopy(checkSum4, 0, addrBytes, payload.length, 4);
        return addrBytes;
    }
}
