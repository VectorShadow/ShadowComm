package main;

import link.ServerTestDataHandler;

import java.io.IOException;

public class ServerTester {

    public static void main(String[] args) throws IOException {
        new Server(new ServerTestDataHandler(), 29387).start();
    }
}