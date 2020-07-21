package main;

import link.ClientTestDataHandler;
import link.RemoteDataLink;
import link.instructions.TestQueryInstructionDatum;

import java.io.IOException;

public class ClientTester {

    public static void main(String[] args) throws IOException, InterruptedException {
        RemoteDataLink rdl = Client.connect(new ClientTestDataHandler(), "vps244728.vps.ovh.ca", 29387);
        Thread.sleep(1_000);
        rdl.transmit(new TestQueryInstructionDatum());
    }
}
