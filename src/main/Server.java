package main;

import link.DataHandler;
import link.RemoteDataLink;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable{
    private final DataHandler dataHandler;
    private final ArrayList<RemoteDataLink> openDataLinks;
    private final ServerSocket serverSocket;

    public Server(DataHandler dataHandler, int portNumber) throws IOException {
        this.dataHandler = dataHandler;
        openDataLinks = new ArrayList<>();
        serverSocket = new ServerSocket(portNumber);
    }

    @Override
    public void run() {
        Socket socket;
        for(;;) {
            try {
                socket = serverSocket.accept();
                RemoteDataLink rdl = new RemoteDataLink(dataHandler, socket);
                openDataLinks.add(rdl);
            } catch (IOException e) {
                LogHub.logFatalCrash("Failed to accept connection", e);
            }
        }
    }

    public ArrayList<RemoteDataLink> getOpenDataLinks() {
        return openDataLinks;
    }
}
