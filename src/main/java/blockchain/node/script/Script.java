package blockchain.node.script;

import org.bitcoinj.base.Base58;

public class Script {

    /**
     * 잠금 스크립트 생성
     * @return 잠금 스크립트 값 (hash160 공개키)
     */
    public static byte[] getSigScriptValue(String address) {
        byte[] base58decoded = Base58.decode(address);
        byte[] value = new byte[20]; // HASH160은 항상 20바이트
        System.arraycopy(base58decoded, 1, value, 0, 20);
        return value;
    }

}