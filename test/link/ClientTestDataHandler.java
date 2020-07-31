package link;

import link.instructions.InstructionDatum;
import link.instructions.MessageInstructionDatum;
import link.instructions.TestResponseInstructionDatum;

import java.net.Socket;

public class ClientTestDataHandler extends DataHandler {
    @Override
    protected void connectionLost(Socket socket) {
        //todo
    }

    @Override
    protected void handle(InstructionDatum instructionDatum, DataLink responseLink) {
        if (instructionDatum instanceof TestResponseInstructionDatum) {
            System.out.println("Received server response.");
            System.exit(0);
        } else if (instructionDatum instanceof MessageInstructionDatum) {
            System.out.println("Received message: " + ((MessageInstructionDatum)instructionDatum).getMessage());
        } else {
            System.out.println("Unexpected InstructionDatum class: " + instructionDatum.getClass());
        }
    }
}
