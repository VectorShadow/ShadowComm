package link.instructions;

public class MessageInstructionDatum extends InstructionDatum {

    private final String MESSAGE;

    public MessageInstructionDatum(String message) {
        MESSAGE = message;
    }

    public String getMessage() {
        return MESSAGE;
    }

    @Override
    protected int getInstructionCode() {
        return InstructionCodes.INSTRUCTION_CODE_MESSAGE;
    }
}
