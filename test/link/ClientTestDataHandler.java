package link;

import link.instructions.InstructionCodes;
import link.instructions.InstructionDatum;
import link.instructions.MessageInstructionDatum;

import java.net.Socket;

public class ClientTestDataHandler extends DataHandler {
    @Override
    protected void connectionLost(Socket socket) {
        //todo
    }

    @Override
    protected void handle(int instructionCode, InstructionDatum instructionDatum, DataLink responseLink) {
        switch (instructionCode) {
            case InstructionCodes.INSTRUCTION_CODE_RESPONSE:
                System.out.println("Received server response.");
                System.exit(0);
                break;
            case InstructionCodes.INSTRUCTION_CODE_MESSAGE:
                System.out.println("Received message: " + ((MessageInstructionDatum)instructionDatum).getMessage());
                break;
                default:
                System.out.println("Unexpected code: " + instructionCode);
        }
    }
}
