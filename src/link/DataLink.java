package link;

import java.net.Socket;

/**
 * DataLink provides two-way data transmission, either between two services connected locally,
 * or two services connected remotely. Each link must be constructed with a data handler to provide
 * implementation, link-type(local vs. remote), or service(frontend vs. backend) specific utility.
 */
public abstract class DataLink implements Runnable {

    protected final DataHandler DATA_HANDLER;

    public DataLink(DataHandler dataHandler) {
        DATA_HANDLER = dataHandler;
    }

    @Override
    public void run() {
        receive();
    }

    abstract void receive();


    abstract void transmit(byte[] data);
}
