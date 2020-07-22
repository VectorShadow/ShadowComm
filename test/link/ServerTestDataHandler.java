package link;

import link.instructions.InstructionCodes;
import link.instructions.InstructionDatum;
import link.instructions.MessageInstructionDatum;
import link.instructions.TestResponseInstructionDatum;

import java.net.Socket;

public class ServerTestDataHandler extends DataHandler {
    @Override
    protected void connectionLost(Socket socket) {
        System.out.println("Lost connection on port " + socket.getLocalPort());
    }

    @Override
    protected void handle(int instructionCode, InstructionDatum instructionDatum, DataLink responseLink) {
        switch (instructionCode) {
            case InstructionCodes.INSTRUCTION_CODE_QUERY:
                System.out.println("Received client query... responding.");
                responseLink.transmit(new TestResponseInstructionDatum());
                break;
            case InstructionCodes.INSTRUCTION_CODE_MESSAGE:
                System.out.println("Received message: " + ((MessageInstructionDatum)instructionDatum).getMessage());
                break;
                default:
                    System.out.println("Unexpected code: " + instructionCode);
        }
    }
}
