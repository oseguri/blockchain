package util;


public class BytesUtil {

    /**
     * int를 size 크기의 byte 배열로 변환 (비트 연산 사용)
     * @param num 변환할 정수
     * @param size 바이트 배열 크기 (1~4)
     * @return byte 배열
     */
    public static byte[] intToBytes(int num, int size) {
        if (size < 1 || size > 4) {
            throw new IllegalArgumentException("Size must be between 1 and 4");
        }

        byte[] result = new byte[size];

        // 빅 엔디언 방식으로 변환 (최상위 바이트부터)
        for (int i = 0; i < size; i++) {
            result[i] = (byte) (num >> (8 * (size - 1 - i)));
        }

        return result;
    }

    /**
     * long을 size 크기의 byte 배열로 변환 (비트 연산 사용)
     * @param num 변환할 long
     * @param size 바이트 배열 크기 (1~8)
     * @return byte 배열
     */
    public static byte[] longToBytes(long num, int size) {
        if (size < 1 || size > 8) {
            throw new IllegalArgumentException("Size must be between 1 and 8");
        }

        byte[] result = new byte[size];

        // 빅 엔디언 방식으로 변환
        for (int i = 0; i < size; i++) {
            result[i] = (byte) (num >> (8 * (size - 1 - i)));
        }

        return result;
    }

    /**
     * byte 배열을 int로 변환
     * @param bytes 바이트 배열
     * @return int 값
     */
    public static int bytesToInt(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result = (result << 8) | (bytes[i] & 0xFF);
        }
        return result;
    }

    /**
     * byte 배열을 long으로 변환
     * @param bytes 바이트 배열
     * @return long 값
     */
    public static long bytesToLong(byte[] bytes) {
        long result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result = (result << 8) | (bytes[i] & 0xFF);
        }
        return result;
    }
}
