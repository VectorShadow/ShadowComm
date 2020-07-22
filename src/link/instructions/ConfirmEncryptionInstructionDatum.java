package link.instructions;

public class ConfirmEncryptionInstructionDatum extends InstructionDatum {
    @Override
    protected int getInstructionCode() {
        return HANDSHAKE_INSTRUCTION_CODE_CONFIRM_ENCRYPTION;
    }
}
