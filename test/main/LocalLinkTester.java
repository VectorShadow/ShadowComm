package main;

import link.ClientTestDataHandler;
import link.LocalDataLink;
import link.ServerTestDataHandler;
import link.instructions.MessageInstructionDatum;
import link.instructions.TestQueryInstructionDatum;

public class LocalLinkTester {
    public static void main(String[] args) throws InterruptedException {
        LocalDataLink frontend = new LocalDataLink(new ClientTestDataHandler());
        LocalDataLink backend = new LocalDataLink(new ServerTestDataHandler());
        LocalDataLink.pair(frontend, backend, true);
        frontend.transmit(new MessageInstructionDatum("Outbound message - hello world!"));
        frontend.transmit(new TestQueryInstructionDatum());
    }
}
