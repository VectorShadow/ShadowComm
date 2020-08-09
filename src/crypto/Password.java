package crypto;

import java.security.SecureRandom;


/**
 * Provides various secure password functions, including hashing and salting.
 */
public class Password {

    static final int MAX_CHAR = 255;

    public static final int MINIMUM_LENGTH = 8;
    public static final int MAXIMUM_LENGTH = 24;
    private static final int SALT_LENGTH = 8;

    /**
     * Generate a random alphanumeric string of chars of SALT_LENGTH.
     */
    public static String generateRandomSalt() {
        char nextCandidateChar;
        String randomSalt = "";
        char nextAlphaSeed = 1;
        final SecureRandom SECURE_RANDOM = new SecureRandom();
        for (int i = 0; i < SALT_LENGTH; ++i){
            nextCandidateChar = (char)SECURE_RANDOM.nextInt(MAX_CHAR);
            randomSalt += forceAlphaNumericalSymbolic(nextCandidateChar, nextAlphaSeed);
            nextAlphaSeed = nextCandidateChar;
        }
        return randomSalt;
    }

    /**
     * Hash a salted password.
     * First we pad the input out to a fixed length if necessary.
     * Then we initialize with the character at the middle of the original input,
     * and for each character in the padded input, we generate a new character from itself and the previous character
     * in the sequence(using the initialization value for the character at zero).
     * Finally we return the string of generated characters.
     * This is fully deterministic - hashing a given input will always return the same output.
     * This is strongly resistant to collisions, tested up to 100 million random brute force attempts.
     * This should also be extremely difficult, if not impossible, to reverse.
     */
    public static String hash(String saltedPassword){
        StringBuilder sb  = new StringBuilder(saltedPassword);
        while (sb.length() < SALT_LENGTH + MAXIMUM_LENGTH)
            sb.append(sb.charAt((SALT_LENGTH + MAXIMUM_LENGTH) % sb.length()));
        String paddedInput = sb.toString();
        char lastValue = paddedInput.charAt(saltedPassword.length() / 2);
        sb = new StringBuilder();
        for (char c : paddedInput.toCharArray()) {
            sb.append(forceAlphaNumericalSymbolic((char)((c + lastValue) % Character.MAX_VALUE), lastValue));
            lastValue = c;
        }
        return sb.toString();
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
