package blockchain.network;



import java.io.Serial;
import java.io.Serializable;

/**
 * P2P 네트워크 메시지
 */
public class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        NEW_BLOCK,          // 새 블록 브로드캐스트
        NEW_TRANSACTION,    // 새 트랜잭션 브로드캐스트
        REQUEST_CHAIN,      // 블록체인 요청
        RESPONSE_CHAIN,     // 블록체인 응답
        PING,              // 연결 확인
        PONG               // 연결 응답
    }

    private final MessageType type;
    private final Object payload;
    private final String senderId;
    private final long timestamp;

    public Message(MessageType type, Object payload, String senderId) {
        this.type = type;
        this.payload = payload;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", senderId='" + senderId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
