package link;

import crypto.Cipher;
import link.instructions.InstructionDatum;
import main.LogHub;

/**
 * The local version of DataLink.
 * All data is stored in byte arrays, and the link maintains pointers to each array.
 * LocalDataLinks must be paired via the static method pair().
 */
public class LocalDataLink extends DataLink {

    private class ByteArrayPointer {
        byte[] value = null;

        void set(byte[] val) {
            value = val;
        }
        byte[] get() {
            return value;
        }
    }

    private ByteArrayPointer input = new ByteArrayPointer();
    private ByteArrayPointer output = new ByteArrayPointer();

    public LocalDataLink(DataHandler dataHandler) {
        super(dataHandler);
    }

    /**
     * Local data links function on the same machine, so encryption is not required.
     * If it has been enabled during pairing, we use the session secret key.
     */
    @Override
    public String decrypt(String message) {
        return encrypted ? Cipher.decrypt(message) : message;
    }

    @Override
    public String encrypt(String message) {
        return encrypted ? Cipher.encrypt(message) : message;
    }

    /**
     * Reception is accomplished by looping infinitely, checking with a delay for any changes to the input array.
     * When the input array has data, it's immediately passed on to the associated DataHandler, then cleared from the
     * input array.
     */
    @Override
    protected void receive() {
        for (;;) {
            while (input.get() == null) { //wait until we have something to do
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    LogHub.logFatalCrash("Thread interrupted during local data reception.", e);
                }
            }
            byte[] data = input.get();
            byte instructionCode = data[0];
            int remainderSize = data.length - InstructionDatum.HEADER_LENGTH;
            byte[] remainder = new byte[remainderSize];
            System.arraycopy(data, InstructionDatum.HEADER_LENGTH, remainder, 0, remainderSize);
            DATA_HANDLER.handle(instructionCode, remainder, this);
            input.set(null);
        }
    }

    /**
     * Since a local data link need not worry about encryption, simply send the transmission for the datum.
     */
    @Override
    public void transmit(InstructionDatum id) {
        transmit(id.pack());
    }
    /**
     * Transmission is accomplished locally by storing the data to be transmitted in the output array, which becomes
     * the input array for the paired Link.
     * A short delay is built in to ensure that pending output data is not overwritten until it has been cleared by the
     * paired Link's receive() method.
     */
    @Override
    protected void transmit(byte[] data) {
        while(output.get() != null) { //don't overwrite the current output until it's been handled
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                LogHub.logFatalCrash("Thread interrupted during local data transmission.", e);
            }
        }
        output.set(data);
    }

    public static void pair(LocalDataLink link1, LocalDataLink link2) {
        pair(link1, link2, false);
    }
    /**
     * Pair two LocalDataLinks so that their input and output pointers match each other's output and input arrays.
     * If local encryption is desired, it should be specified as part of this call.
     */
    public static void pair(LocalDataLink link1, LocalDataLink link2, boolean forceEncryption) {
        link1.input = link2.output;
        link1.output = link2.input;
        if (forceEncryption) {
            link1.establishEndToEndEncryption();
            link2.establishEndToEndEncryption();
        }
        new Thread(link1).start();
        new Thread(link2).start();
    }
}
