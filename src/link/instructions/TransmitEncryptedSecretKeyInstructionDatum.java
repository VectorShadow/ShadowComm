package link.instructions;

import link.DataHandler;

import java.math.BigInteger;

public class TransmitEncryptedSecretKeyInstructionDatum extends InstructionDatum {
    public final BigInteger ENCRYPTED_SECRET_KEY;

    public TransmitEncryptedSecretKeyInstructionDatum(BigInteger encryptedSecretKey) {
        ENCRYPTED_SECRET_KEY = encryptedSecretKey;
    }
    @Override
    protected int getInstructionCode() {
        return DataHandler.RESERVED_INSTRUCTION_CODE_TRANSMIT_ENCRYPTED_SECRET_KEY;
    }
}
