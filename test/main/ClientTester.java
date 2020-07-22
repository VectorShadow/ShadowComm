package main;

import link.ClientTestDataHandler;
import link.RemoteDataLink;
import link.instructions.MessageInstructionDatum;
import link.instructions.TestQueryInstructionDatum;

import java.io.IOException;

public class ClientTester {

    public static void main(String[] args) throws IOException, InterruptedException {
        RemoteDataLink rdl = Client.connect(new ClientTestDataHandler(), "vps244728.vps.ovh.ca", 29387);
        Thread.sleep(3_000);
        rdl.transmit(new MessageInstructionDatum("Hello world!"));
        rdl.transmit(new TestQueryInstructionDatum());
    }
}
