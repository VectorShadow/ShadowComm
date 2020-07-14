package crypto;

import main.LogHub;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Provides various secure password functions, including hashing and salting.
 */
public class Password {

    static final int MAX_CHAR = 255;

    private static final int MINIMUM_LENGTH = 8;
    private static final int MAXIMUM_LENGTH = 32;
    private static final int SALT_LENGTH = 8;

    /**
     * Generate a random alphanumeric string of chars of SALT_LENGTH.
     */
    public static String generateRandomSalt() {
        char nextCandidateChar;
        String randomSalt = "";
        char nextAlphaSeed = 1;
        for (int i = 0; i < SALT_LENGTH; ++i){
            nextCandidateChar = (char)Cipher.SECURE_RANDOM.nextInt(MAX_CHAR);
            randomSalt += forceAlphaNumericalSymbolic(nextCandidateChar, nextAlphaSeed);
            nextAlphaSeed = nextCandidateChar;
        }
        return randomSalt;
    }

    /**
     * Hash a salted password with SHA-256.
     */
    public static String hash(String saltedPassword){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String sha256 = new String(md.digest(saltedPassword.getBytes()));
            return Cipher.convertToHexString(sha256);
        } catch (NoSuchAlgorithmException e) {
            LogHub.logFatalCrash("Unexpected exception in Password.hash().", e);
            return null;
        }
    }

    /**
     * Prepend salt to a password. Salt may be randomly generated with generateRandomSalt() when a password
     * is initially recorded, or may be pulled from a password table to verify the password.
     */
    public static String salt(String salt, String unsaltedPassword) {
        return salt + unsaltedPassword;
    }

    /**
     * Ensure that a randomly generated character is alphanumeric.
     */
    private static char forceAlphaNumericalSymbolic(char candidate, int seed){
        char result = candidate;
        int iterations = 0;
        while (result < '!' || result == '/' || result == '\\' || result > '~'){
            int productStep = (result + iterations) * seed;
            int modStep = productStep % MAX_CHAR;
            seed += iterations++;
            result = (char)modStep;
        }
        return result;
    }
}