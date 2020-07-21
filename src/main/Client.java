package main;

import crypto.RSA;
import link.DataHandler;
import link.RemoteDataLink;
import link.instructions.TransmitPublicKeyInstructionDatum;

import java.io.IOException;
import java.net.Socket;

/**
 * Provides access to a remote server.
 */
public class Client {

    /**
     * Connect to a remote server at the specified host name and port number.
     * @return a remote data link connected by socket to the specified host on the specified port.
     * @throws IOException if socket creation fails.
     */
    public static RemoteDataLink connect(DataHandler dataHandler, String hostName, int portNumber) throws IOException {
        RemoteDataLink rdl = new RemoteDataLink(dataHandler, new Socket(hostName, portNumber));
        RSA.generateSessionKeys();
        rdl.transmit(new TransmitPublicKeyInstructionDatum(RSA.getSessionPublicKey()));
        return rdl;
    }
}
