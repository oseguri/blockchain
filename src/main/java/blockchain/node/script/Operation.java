package blockchain.node.script;

import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import util.Hash;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Operation {
    Stack<byte[]> stack;
    private final byte[] sigScriptValue;
    private final byte[] messageHash;

    public Operation(List<byte[]> pkScript, byte[] sigScriptValue, byte[] messageHash) {
        stack = new Stack<>();
        stack.push(pkScript.getFirst()); // 서명 push
        stack.push(pkScript.getLast()); // 공개키 push
        this.sigScriptValue = sigScriptValue;
        this.messageHash = messageHash;
    }

    public boolean execute() {
        try {
            // P2PKH 스크립트 실행 순서
            OP_DUP();
            OP_HASH160();
            OP_EQUALVERIFY();
            OP_CHECKSIG();

            // 모든 검증이 성공하면 true 반환
            return true;

        } catch (InvalidKeyException e) {
            System.err.println("Script execution failed: " + e.getMessage());
            return false;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Hash algorithm error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected error during script execution: " + e.getMessage());
            return false;
        }
    }

    public void OP_DUP() {
        byte[] top = stack.peek();
        byte[] dup = top.clone();
        stack.push(dup);
    }

    public void OP_HASH160() throws NoSuchAlgorithmException {
        byte[] top = stack.peek();
        Hash.HASH160(top);
        stack.push(this.sigScriptValue);
    }

    public void OP_EQUALVERIFY() throws InvalidKeyException {
        byte[] origin = stack.pop();
        byte[] calculatedValue = stack.pop();
        if(!Arrays.equals(origin, calculatedValue)) throw new InvalidKeyException("invalid pub key");
    }

    public void OP_CHECKSIG() throws InvalidKeyException {
        byte[] pubKey = stack.pop();
        byte[] sig = stack.pop();

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECDomainParameters domainParams = new ECDomainParameters(
                spec.getCurve(),
                spec.getG(),
                spec.getN(),
                spec.getH()
        );
        ECPoint q = spec.getCurve().decodePoint(pubKey);
        ECKeyParameters pubKeyParmas = new ECPublicKeyParameters(q,domainParams);

        ECDSASigner signer = new ECDSASigner();
        signer.init(false, pubKeyParmas);

        int rLen = sig[3];
        int rOffset = 4;
        byte[] r = Arrays.copyOfRange(sig,rOffset,rOffset + rLen);

        int sLen = sig[rOffset + rLen + 1];
        int sOffset = rOffset + rLen + 2;
        byte[] s = Arrays.copyOfRange(sig,sOffset,sOffset + sLen);

        boolean verified = signer.verifySignature(messageHash, new BigInteger(1, r), new BigInteger(1, s));
        if (!verified) throw new InvalidKeyException("invalid signature");

    }
}
