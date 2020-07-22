package crypto;

import main.LogHub;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Provides RSA encryption and decryption for secure remote data transmission.
 */
public class RSA {
    private static final BigInteger E = new BigInteger("65537"); //public key e, by convention

    private static BigInteger sessionPublicKey = BigInteger.ZERO;
    private static BigInteger sessionPrivateKey = BigInteger.ZERO;

    /**
     * Server side
     * @param cipherText - the RSA-encrypted cipherText received from the client, encrypted with sessionPublicKey
     * @return the plainText corresponding to cipherText
     */
    public static BigInteger decrypt(BigInteger cipherText){
        if (sessionPublicKey.equals(BigInteger.ZERO) || sessionPrivateKey.equals(BigInteger.ZERO))
            throw new IllegalStateException("Session keys have not been generated.");
        return cipherText.modPow(sessionPrivateKey, sessionPublicKey); //P = C^d mod n
    }

    /**
     * Client side
     * @param plainText - the BigInteger representation of the message to encrypt
     * @param publicKey - the public key received from the server(same as sessionPublicKey in server's RSA)
     * @return the RSA-encrypted cipherText corresponding to plainText
     * @throws IllegalArgumentException if plainText bitLength exceeds publicKey bitLength
     */
    public static BigInteger encrypt(BigInteger plainText, BigInteger publicKey){
        checkSize(plainText, publicKey);
        return plainText.modPow(E, publicKey); //C = P^e mod n
    }

    public static void generateSessionKeys() {
        final BigInteger DIFF = BigInteger.valueOf(2L).pow(1000); //2^1000, the minimum difference between p and q
        BigInteger p, q, phiN; //as in RSA description
        final SecureRandom SECURE_RANDOM = new SecureRandom();
        do {
            p = BigInteger.probablePrime(1536, SECURE_RANDOM); //get a value for p with 1536 bits
            q = BigInteger.probablePrime(1536, SECURE_RANDOM); //get a value for q with 1536 bits
        } while (p.subtract(q).abs().compareTo(DIFF) < 0); //ensure p and q are far enough apart
        sessionPublicKey = p.multiply(q); //n = pq
        phiN = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE)); //phi(n) = (p - 1)(q - 1)
        sessionPrivateKey = E.modInverse(phiN); //d = e^-1 mod(phi(n))
    }

    public static BigInteger getSessionPublicKey(){
        if (sessionPublicKey.equals(BigInteger.ZERO))
            generateSessionKeys();
        return sessionPublicKey;
    }


    /**
     * Ensure we do not break encryption by allowing inputs which exceed our public key size.
     * todo - build support for multi-step encryption of larger inputs.
     */
    private static void checkSize(BigInteger inputData, BigInteger publicKey) {
        if (inputData.bitLength() > publicKey.bitLength())
            LogHub.logFatalCrash(
                    "Encryption of very large values not supported (input size " +
                            inputData.bitLength() + " > maximum allowed size " + publicKey.bitLength() + ")",
                    new IllegalArgumentException()
            );
    }
}