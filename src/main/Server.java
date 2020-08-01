package main;

import link.DataHandler;
import link.RemoteDataLink;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {
    private final DataHandler dataHandler;
    private final DataLinkAggregator openDataLinks;
    private final ServerSocket serverSocket;

    public Server(DataHandler dataHandler, DataLinkAggregator dataLinkAggregator, int portNumber) throws IOException {
        this.dataHandler = dataHandler;
        openDataLinks = dataLinkAggregator;
        serverSocket = new ServerSocket(portNumber);
    }

    @Override
    public void run() {
        Socket socket;
        for(;;) {
            try {
                socket = serverSocket.accept();
                LiveLog.log(
                        "Accepted new connection on " + socket.getLocalPort(),
                        LiveLog.LogEntryPriority.ALERT
                );
                RemoteDataLink rdl = new RemoteDataLink(dataHandler, socket);
                openDataLinks.addDataLink(rdl);
                rdl.start();
            } catch (IOException e) { //no need to kill the server here, log the error and continue
                LogHub.logNonFatalError("Failed to accept connection", e);
            }
        }
    }
}
