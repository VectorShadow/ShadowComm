package link.instructions;

import java.math.BigInteger;

public class TransmitEncryptedSecretKeyInstructionDatum extends HandshakeInstructionDatum {
    public final BigInteger ENCRYPTED_SECRET_KEY;

    public TransmitEncryptedSecretKeyInstructionDatum(BigInteger encryptedSecretKey) {
        ENCRYPTED_SECRET_KEY = encryptedSecretKey;
    }
}
