package link;

import link.instructions.InstructionDatum;

/**
 * DataLink provides two-way data transmission, either between two services connected locally,
 * or two services connected remotely. Each link must be constructed with a data handler to provide
 * implementation, link-type(local vs. remote), or service(frontend vs. backend) specific utility.
 */
public abstract class DataLink extends Thread {

    protected final DataHandler DATA_HANDLER;

    protected boolean encrypted = false;

    public DataLink(DataHandler dataHandler) {
        DATA_HANDLER = dataHandler;
    }

    public abstract String decrypt(String message);
    public abstract String encrypt(String message);

    void establishEndToEndEncryption() {
        encrypted = true;
    }

    @Override
    public void run() {
        receive();
    }

    protected abstract void receive();

    public abstract void transmit(InstructionDatum instructionDatum);
    protected abstract void transmit(byte[] data);
}
