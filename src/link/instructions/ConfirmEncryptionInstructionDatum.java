package link.instructions;

import link.DataHandler;

public class ConfirmEncryptionInstructionDatum extends InstructionDatum {
    @Override
    protected int getInstructionCode() {
        return DataHandler.RESERVED_INSTRUCTION_CODE_CONFIRM_ENCRYPTION;
    }
}
