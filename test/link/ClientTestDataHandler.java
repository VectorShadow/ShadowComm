package link;

import link.instructions.InstructionCodes;
import link.instructions.InstructionDatum;

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
                default:
                System.out.println("Unexpected code: " + instructionCode);
        }
    }
}
