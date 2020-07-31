package link.instructions;

import java.math.BigInteger;

public class TransmitPublicKeyInstructionDatum extends HandshakeInstructionDatum {

    public final BigInteger PUBLIC_KEY;

    public TransmitPublicKeyInstructionDatum(BigInteger publicKey) {
        PUBLIC_KEY = publicKey;
    }

}
