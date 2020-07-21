package link.instructions;

public class TestResponseInstructionDatum extends InstructionDatum {
    @Override
    protected int getInstructionCode() {
        return InstructionCodes.INSTRUCTION_CODE_RESPONSE;
    }
}
