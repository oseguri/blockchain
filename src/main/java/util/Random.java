package util;
import java.security.SecureRandom;
public class Random {
    public static String generateRandomString(int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            char value = getRandomAlphabet();
            result[i] = value;
        }
        return new String(result);
    }

    private static int getRandomNumber(int bound) {
        SecureRandom sr = new SecureRandom();
        return sr.nextInt(bound);
    }

    private static char getRandomAlphabet(){
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        char value = alphabet.charAt(getRandomNumber(alphabet.length()));
        return toRandomUpperLower(value);
    }

    private static char toRandomUpperLower(char value) {
        int toUpper = getRandomNumber(2);
        if(toUpper == 1) return Character.toUpperCase(value);
        return value;
    }
}
