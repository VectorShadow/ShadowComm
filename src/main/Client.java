package main;

import java.io.IOException;
import java.net.Socket;

/**
 * Provides access to a remote server.
 */
public class Client {
    /**
     * Connect to a remote server at the specified host name and port number.
     * @return a socket connected to the specified host on the specified port.
     * @throws IOException if socket creation fails.
     */
    public static Socket connect(String hostName, int portNumber) throws IOException {
        return new Socket(hostName, portNumber);
    }
}
