package crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Provides multiple cryptographic helper functions.
 */
public class Cipher {
    static final SecureRandom SECURE_RANDOM = new SecureRandom();


    private static final int SHIFT = 7;
    private static final int KEY_SIZE = 128;

    private static final char UPPER_MASK = 0xf0; //mask for converting hex chars to normal chars
    private static final char LOWER_MASK = 0x0f; //mask for converting hex chars to normal chars
    private static final int BYTE_MASK = 0x0000_007f;

    private static String sessionKey = generateSessionKey();

    /**
     * Client side - get the current session key for transmission to server.
     */
    public static String getSessionKey(){
        return sessionKey;
    }

    /**
     * Server side - override the automatically generated session key with an RSA encrypted transmission from the
     * client. Since the RSA encrypted key is a BigInteger, we transform it back into a radix16 string, which is
     * the format the sessionKey needs to be in to function.
     */
    public static void setSessionKey(BigInteger transmittedSessionKey) {
        sessionKey = transmittedSessionKey.toString(16);
    }

    /**
     * Encrypt a string for secure transmission.
     */
    public static String encrypt(String plainText){
        return encrypt(plainText, sessionKey);
    }

    public static String encrypt(String plainText, String secretKey) {
        return bytewiseShiftLeft(xorApplyKey(convertToHexString(plainText), secretKey));
    }

    /**
     * Decrypt a cipher string.
     */
    public static String decrypt(String plainText) {
        return decrypt(plainText, sessionKey);
    }
    public static String decrypt(String cipherText, String secretKey) {
        return convertFromHexString(xorApplyKey(bytewiseShiftRight(cipherText), secretKey));
    }

    /**
     * Decryption step -
     * Reverse to hexString by building a new string from the bytes represented by hexadecimal characters.
     */
    static String convertFromHexString(String hexString) {
        byte[] out = new byte[hexString.length() / 2];
        for (int i = 0; i < out.length; i++){
            char upperHex = hexString.charAt(2*i);
            char upper = (char)((hexToInt(upperHex) << 4) & UPPER_MASK);
            char lowerHex = hexString.charAt(2*i + 1);
            char lower = (char)(hexToInt(lowerHex) & LOWER_MASK);
            out[i] = (byte)(upper | lower);
        }
        return new String(out);
    }

    /**
     * Encryption step -
     * Converts a string into another string representing the byte values of the first string as hexadecimal characters.
     */
    static String convertToHexString(String string) {
        String hexString = "";
        for(char c : string.toCharArray()){
            hexString += hexFromInt(((c & UPPER_MASK) >> 4));
            hexString += hexFromInt((c & LOWER_MASK));
        }
        return hexString;
    }

    /**
     * Encryption step - rotate bytes leftwards.
     */
    private static String bytewiseShiftLeft(String input) {
        byte[] inBytes = input.getBytes();
        byte[] outBytes = new byte[inBytes.length];
        int shiftDegree;
        for (int i = 0; i < inBytes.length; ++i) {
            shiftDegree = (i % SHIFT);
            outBytes[i] = shift(inBytes[i], shiftDegree, true);
        }
        return new String(outBytes);
    }

    /**
     * Decryption step - rotate bytes rightwards (reverse bytewiseShiftLeft).
     */
    private static String bytewiseShiftRight(String input) {
        byte[] inBytes = input.getBytes();
        byte[] outBytes = new byte[inBytes.length];
        int shiftDegree;
        for (int i = 0; i < inBytes.length; ++i) {
            shiftDegree = (i % SHIFT);
            outBytes[i] = shift(inBytes[i], shiftDegree, false);
        }
        return new String(outBytes);
    }

    /**
     * Client side - generate a key to encrypt sensitive data (passwords etc.).
     * Use Radix16 form to facilitate BIG_INTEGER encryption with RSA.
     */
    private static String generateSessionKey(){
        StringBuilder sk = new StringBuilder();
        while (sk.length() < KEY_SIZE){
            sk.append((byte)SECURE_RANDOM.nextInt(Password.MAX_CHAR));
        }
        return convertToHexString(sk.toString());
    }

    /**
     * Convert an integer value to an alphanumeric character representing that value in hexadecimal.
     */
    private static char hexFromInt(int intValue) {
        switch (intValue){
            case 0: return '0';
            case 1: return '1';
            case 2: return '2';
            case 3: return '3';
            case 4: return '4';
            case 5: return '5';
            case 6: return '6';
            case 7: return '7';
            case 8: return '8';
            case 9: return '9';
            case 10: return 'a';
            case 11: return 'b';
            case 12: return 'c';
            case 13: return 'd';
            case 14: return 'e';
            case 15: return 'f';
            default: throw new IllegalArgumentException("Invalid sourceChar " + intValue);
        }
    }

    /**
     * Convert an alphanumeric character representing a hexadecimal value to an int corresponding to that value.
     */
    private static int hexToInt(char hexChar) {
        switch (hexChar){
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'a': return 10;
            case 'b': return 11;
            case 'c': return 12;
            case 'd': return 13;
            case 'e': return 14;
            case 'f': return 15;
            default: throw new IllegalArgumentException("Invalid sourceChar " + hexChar + " as Int: " + (int)hexChar);
        }
    }

    /**
     * Circular shift a byte by the specified magnitude, either left or right.
     */
    private static byte shift(byte b, int mag, boolean left) {
        if (mag < 0 || mag > SHIFT) throw new IllegalArgumentException("Mag must be in range [0-7].");
        if (mag == 0 || mag == SHIFT) return b;
        byte shiftMask = 0b0000_0000;
        int factor = 1;
        int oppositeMag = SHIFT - mag;
        for (int i = 0; i < (left ? oppositeMag : mag); ++i) {
            shiftMask += factor;
            factor *= 2;
        }
        byte oppositeMask = (byte)(~(int)shiftMask & BYTE_MASK);
        byte leftShiftPart = (byte)((int)b & (int)shiftMask);
        byte rightShiftPart = (byte)((int)b & (int)oppositeMask);
        return (byte)((leftShiftPart << (left ? mag : oppositeMag)) | (rightShiftPart >>> (left ? oppositeMag : mag)));
    }

    /**
     * Encryption/Decryption step - xor the source text with the provided key.
     * If no key is provided, use the session key.
     * This encrypts plain text or decrypts cipher text.
     */
    private static String xorApplyKey(String sourceText) {
        return xorApplyKey(sourceText, sessionKey);
    }
    private static String xorApplyKey(String sourceText, String secretKey) {
        byte[] sourceBytes = sourceText.getBytes();
        byte[] keyBytes = secretKey.getBytes();
        byte[] targetBytes = new byte[sourceBytes.length];
        for (int i = 0; i < sourceBytes.length; ++i)
            targetBytes[i] = (byte) (sourceBytes[i] ^ keyBytes[i % keyBytes.length]);
        return new String(targetBytes);
    }
}
