package blockchain.encryption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bouncycastle.util.encoders.Base64;
import util.EncryptionUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 암호화된 계약 클래스
 * 계약 내용은 AES로 암호화되고, 각 참여자별로 AES 키가 공개키로 암호화됨
 */
public class EncryptedContract {

    private byte[] encryptedContent;  // AES로 암호화된 계약 내용
    private Map<String, byte[]> encryptedKeys;  // 참여자 주소 -> 암호화된 AES 키

    /**
     * 암호화된 계약 생성
     * @param plainContract 평문 계약 내용
     * @param participantPublicKeys 참여자 주소 -> 공개키 맵
     */
    public EncryptedContract(String plainContract, Map<String, byte[]> participantPublicKeys)
            throws Exception {

        // 1. AES 대칭키 생성
        byte[] aesKey = EncryptionUtil.generateAESKey();

        // 2. 계약 내용을 AES로 암호화
        this.encryptedContent = EncryptionUtil.encryptAES(plainContract.getBytes(), aesKey);

        // 3. 각 참여자의 공개키로 AES 키 암호화
        this.encryptedKeys = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : participantPublicKeys.entrySet()) {
            String address = entry.getKey();
            byte[] publicKey = entry.getValue();

            byte[] encryptedKey = EncryptionUtil.encryptAESKey(aesKey, publicKey);
            encryptedKeys.put(address, encryptedKey);
        }

        System.out.println("✓ Contract encrypted for " + participantPublicKeys.size() + " participants");
    }

    /**
     * Jackson 역직렬화용 생성자
     */
    @JsonCreator
    public EncryptedContract(
            @JsonProperty("encryptedContent") byte[] encryptedContent,
            @JsonProperty("encryptedKeys") Map<String, byte[]> encryptedKeys) {
        this.encryptedContent = encryptedContent;
        this.encryptedKeys = encryptedKeys;
    }

    /**
     * 계약 내용 복호화
     * @param participantAddress 참여자 주소
     * @param participantPrivateKey 참여자 개인키
     * @return 복호화된 계약 내용
     */
    public String decrypt(String participantAddress, byte[] participantPrivateKey)
            throws Exception {

        // 1. 해당 참여자의 암호화된 AES 키 조회
        byte[] encryptedKey = encryptedKeys.get(participantAddress);
        if (encryptedKey == null) {
            throw new SecurityException("Not a participant of this contract");
        }

        // 2. 개인키로 AES 키 복호화
        byte[] aesKey = EncryptionUtil.decryptAESKey(encryptedKey, participantPrivateKey);

        // 3. AES 키로 계약 내용 복호화
        byte[] plaintext = EncryptionUtil.decryptAES(encryptedContent, aesKey);

        return new String(plaintext);
    }

    /**
     * 참여자 확인
     */
    public boolean isParticipant(String address) {
        return encryptedKeys.containsKey(address);
    }

    /**
     * 참여자 목록 조회
     */
    public List<String> getParticipants() {
        return List.copyOf(encryptedKeys.keySet());
    }

    /**
     * JSON 문자열로 직렬화 (블록체인 저장용)
     */
    public String toJsonString() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"encryptedContent\":\"").append(Base64.toBase64String(encryptedContent)).append("\",");
        json.append("\"encryptedKeys\":{");

        boolean first = true;
        for (Map.Entry<String, byte[]> entry : encryptedKeys.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append("\"").append(Base64.toBase64String(entry.getValue())).append("\"");
            first = false;
        }

        json.append("}}");
        return json.toString();
    }

    // Getters
    public byte[] getEncryptedContent() {
        return encryptedContent;
    }

    public Map<String, byte[]> getEncryptedKeys() {
        return encryptedKeys;
    }

    @Override
    public String toString() {
        return "EncryptedContract{" +
                "participants=" + encryptedKeys.size() +
                ", encryptedContent=" + encryptedContent.length + " bytes" +
                '}';
    }
}
