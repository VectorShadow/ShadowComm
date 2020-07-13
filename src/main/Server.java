package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable{
    private ArrayList<Socket> openSockets;
    private ServerSocket serverSocket;

    public Server(int portNumber) {
        //todo - init crypto
        openSockets = new ArrayList<>();
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            LogHub.logFatalCrash("Failed to initialize server socket on port " + portNumber, e);
        }
    }

    @Override
    public void run() {
        Socket socket;
        for(;;) {
            try {
                socket = serverSocket.accept();
                openSockets.add(socket);
                //todo - begin key exchange
            } catch (IOException e) {
                LogHub.logFatalCrash("Failed to accept connection", e);
            }
        }
    }

    public ArrayList<Socket> getOpenSockets() {
        return openSockets;
    }
}
