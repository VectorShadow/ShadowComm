package link.instructions;

import link.DataHandler;

import java.math.BigInteger;

public class TransmitPublicKeyInstructionDatum extends InstructionDatum {

    public final BigInteger PUBLIC_KEY;

    public TransmitPublicKeyInstructionDatum(BigInteger publicKey) {
        PUBLIC_KEY = publicKey;
    }

    @Override
    protected int getInstructionCode() {
        return DataHandler.RESERVED_INSTRUCTION_CODE_TRANSMIT_PUBLIC_KEY;
    }
}
