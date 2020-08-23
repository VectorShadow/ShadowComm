package link;

import crypto.ByteCipher;
import link.instructions.InstructionDatum;
import main.LogHub;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * The local version of DataLink.
 * All data is stored in byte arrays, and the link maintains pointers to each array.
 * LocalDataLinks must be paired via the static method pair().
 */
public class LocalDataLink extends DataLink {

    private class ByteArrayPointer {
        ArrayDeque<byte[]> pendingTransmissions = new ArrayDeque<>();

        void pendTransmission(byte[] val) {
            pendingTransmissions.addLast(val);
        }
        boolean isPendingTransmissions() {
            return !pendingTransmissions.isEmpty();
        }
        byte[] getNextPendingTransmission() {
            return pendingTransmissions.removeFirst();
        }
    }

    private ByteArrayPointer input = new ByteArrayPointer();
    private ByteArrayPointer output = new ByteArrayPointer();

    public LocalDataLink(DataHandler dataHandler) {
        super(dataHandler);
    }

    /**
     * Reception is accomplished by looping infinitely, checking with a delay for any changes to the input array.
     * When the input array has data, it's immediately passed on to the associated DataHandler, then cleared from the
     * input array.
     */
    @Override
    protected void receive() {
        do {
            while (!input.isPendingTransmissions()) { //wait until we have something to do
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    LogHub.logFatalCrash("Thread interrupted during local data reception.", e);
                }
            }
            byte[] data = input.getNextPendingTransmission();
            int remainderSize = data.length - (InstructionDatum.HEADER_LENGTH + InstructionDatum.TRAILER_LENGTH);
            byte[] remainder = new byte[remainderSize];
            System.arraycopy(data, InstructionDatum.HEADER_LENGTH, remainder, 0, remainderSize);
            if (encrypted) remainder = ByteCipher.decrypt(remainder); //decrypt if necessary
            DATA_HANDLER.handle(remainder, this);
        } while (!terminated);
    }

    /**
     * Since a local data link need not worry about encryption, simply send the transmission for the datum.
     */
    @Override
    public void transmit(InstructionDatum id) {
        transmit(id.pack(0, encrypted));
    }
    /**
     * Transmission is accomplished locally by storing the data to be transmitted in the output array, which becomes
     * the input array for the paired Link.
     * A short delay is built in to ensure that pending output data is not overwritten until it has been cleared by the
     * paired Link's receive() method.
     */
    @Override
    protected void transmit(byte[] data) {
        output.pendTransmission(data);
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
