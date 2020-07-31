package link;

import link.instructions.InstructionDatum;
import link.instructions.MessageInstructionDatum;
import link.instructions.TestQueryInstructionDatum;
import link.instructions.TestResponseInstructionDatum;

import java.net.Socket;

public class ServerTestDataHandler extends DataHandler {
    @Override
    protected void connectionLost(Socket socket) {
        System.out.println("Lost connection on port " + socket.getLocalPort());
    }

    @Override
    protected void handle(InstructionDatum instructionDatum, DataLink responseLink) {
        if (instructionDatum instanceof TestQueryInstructionDatum) {
            System.out.println("Received client query... responding.");
            responseLink.transmit(new TestResponseInstructionDatum());
        } else if (instructionDatum instanceof MessageInstructionDatum) {
            System.out.println("Received message: " + ((MessageInstructionDatum)instructionDatum).getMessage());
        } else {
            System.out.println("Unexpected InstructionDatum class: " + instructionDatum.getClass());
        }
    }
}
