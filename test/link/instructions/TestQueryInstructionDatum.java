package link.instructions;

public class TestQueryInstructionDatum extends InstructionDatum {
    @Override
    protected int getInstructionCode() {
        return InstructionCodes.INSTRUCTION_CODE_QUERY;
    }
}
