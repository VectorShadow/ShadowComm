package crypto;

import java.security.SecureRandom;

public class ByteCipher {

    private static final int SHIFT = 8;
    private static final int KEY_SIZE = 256;

    private static final char UPPER_MASK = 0xf0; //mask for converting hex chars to normal chars
    private static final char LOWER_MASK = 0x0f; //mask for converting hex chars to normal chars
    private static final int BYTE_MASK = 0x0000_00ff;

    private static byte[] sessionKey = null;

    /**
     * Encrypt a byte array for secure transmission.
     */
    public static byte[] encrypt(byte[] data){
        return encrypt(data, getSessionKey());
    }

    public static byte[] encrypt(byte[] data, byte[] key) {
        return bytewiseShiftLeft(xorApplyKey(data, key));
    }

    /**
     * Decrypt an encrypted byte array.
     */
    public static byte[] decrypt(byte[] encryptedData) {
        return decrypt(encryptedData, getSessionKey());
    }
    public static byte[] decrypt(byte[] encryptedData, byte[] key) {
        return xorApplyKey(bytewiseShiftRight(encryptedData), key);
    }

    /**
     * Generate a new random session key.
     */
    private static void generateSessionKey() {
        sessionKey = new byte[KEY_SIZE];
        for (int i = 0; i < KEY_SIZE; ++i) {
            sessionKey[i] = randomByte();
        }
    }

    /**
     * Get the current session key, generating it first if necessary.
     */
    public static byte[] getSessionKey() {
        if (sessionKey == null) generateSessionKey();
        return sessionKey;
    }

    /**
     * Set the session key.
     */
    public static void setSessionKey(byte[] key) {
        sessionKey = key;
    }

    /**
     * Encryption step - rotate bytes leftwards.
     */
    private static byte[] bytewiseShiftLeft(byte[] inputData) {
        byte[] outputData = new byte[inputData.length];
        int shiftDegree;
        for (int i = 0; i < inputData.length; ++i) {
            shiftDegree = (i % SHIFT);
            outputData[i] = shift(inputData[i], shiftDegree, true);
        }
        return outputData;
    }

    /**
     * Decryption step - rotate bytes rightwards (reverse bytewiseShiftLeft).
     */
    private static byte[] bytewiseShiftRight(byte[] inputData) {
        byte[] outputData = new byte[inputData.length];
        int shiftDegree;
        for (int i = 0; i < inputData.length; ++i) {
            shiftDegree = (i % SHIFT);
            outputData[i] = shift(inputData[i], shiftDegree, false);
        }
        return outputData;
    }

    private static byte randomByte() {
        int b = 0;
        final SecureRandom SECURE_RANDOM = new SecureRandom();
        for (int i = 7; i >= 0; --i) {
            b |= (int)(Math.pow(2, i) * SECURE_RANDOM.nextInt(2));
        }
        return (byte)b;
    }

    /**
     * Circular shift a byte by the specified magnitude, either left or right.
     */
    private static byte shift(byte b, int mag, boolean left) {
        if (mag < 0 || mag >= SHIFT) throw new IllegalArgumentException("Mag must be in range [0-7].");
        if (mag == 0) return b;
        byte shiftMask = 0b0000_0000;
        int factor = 1;
        int oppositeMag = SHIFT - mag;
        for (int i = 0; i < (left ? oppositeMag : mag); ++i) {
            shiftMask += factor;
            factor *= 2;
        }
        int oppositeMask = ~shiftMask & BYTE_MASK;
        int leftShiftPart = b & shiftMask;
        int rightShiftPart = b & oppositeMask;
        return (byte)((leftShiftPart << (left ? mag : oppositeMag)) | (rightShiftPart >>> (left ? oppositeMag : mag)));
    }

    /**
     * Encryption/Decryption step - xor the source text with the provided key.
     * If no key is provided, use the session key.
     * This encrypts plain text or decrypts cipher text.
     */
    private static byte[] xorApplyKey(byte[] data) {
        return xorApplyKey(data, sessionKey);
    }
    private static byte[] xorApplyKey(byte[] inputData, byte[] key) {
        int dataSize = inputData.length;
        byte[] outputData = new byte[dataSize];
        for (int i = 0; i < dataSize; ++i)
            outputData[i] = (byte) (inputData[i] ^ key[i % KEY_SIZE]);
        return outputData;
    }
}
