package link.instructions;

public class MessageInstructionDatum extends InstructionDatum {

    private final String MESSAGE;

    public MessageInstructionDatum(String message) {
        MESSAGE = message;
    }

    public String getMessage() {
        return MESSAGE;
    }
}
