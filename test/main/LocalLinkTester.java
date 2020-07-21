package main;

import link.ClientTestDataHandler;
import link.LocalDataLink;
import link.ServerTestDataHandler;
import link.instructions.TestQueryInstructionDatum;

public class LocalLinkTester {
    public static void main(String[] args) {
        LocalDataLink frontend = new LocalDataLink(new ClientTestDataHandler());
        LocalDataLink backend = new LocalDataLink(new ServerTestDataHandler());
        LocalDataLink.pair(frontend, backend);
        frontend.transmit(new TestQueryInstructionDatum());
    }
}
