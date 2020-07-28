package main;

import link.DataHandler;
import link.DataLink;
import link.RemoteDataLink;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    private final DataHandler dataHandler;
    private final ArrayList<DataLink> openDataLinks;
    private final ServerSocket serverSocket;

    public Server(DataHandler dataHandler, int portNumber) throws IOException {
        this.dataHandler = dataHandler;
        openDataLinks = new ArrayList<>();
        serverSocket = new ServerSocket(portNumber);
        LiveLog.start();
    }

    @Override
    public void run() {
        Socket socket;
        for(;;) {
            try {
                socket = serverSocket.accept();
                LiveLog.log("Accepted new connection on " + socket.getLocalPort());
                RemoteDataLink rdl = new RemoteDataLink(dataHandler, socket);
                openDataLinks.add(rdl);
                rdl.start();
            } catch (IOException e) {
                LogHub.logFatalCrash("Failed to accept connection", e);
            }
        }
    }

    public ArrayList<DataLink> getOpenDataLinks() {
        return openDataLinks;
    }
}
