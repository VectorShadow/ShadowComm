package link;

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
     * Reception is accomplished by looping infinitely, checking with a delay for any changes to the input array.
     * When the input array has data, it's immediately passed on to the associated DataHandler, then cleared from the
     * input array.
     */
    @Override
    public void receive() {
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
            int remainderSize = data.length - 1;
            byte[] remainder = new byte[remainderSize];
            System.arraycopy(data, 1, remainder, 0, remainderSize);
            DATA_HANDLER.handle(instructionCode, remainder);
            input.set(null);
        }
    }

    /**
     * Transmission is accomplished locally by storing the data to be transmitted in the output array, which becomes
     * the input array for the paired Link.
     * A short delay is built in to ensure that pending output data is not overwritten until it has been cleared by the
     * paired Link's receive() method.
     */
    @Override
    public void transmit(byte[] data) {
        while(output.get() != null) { //don't overwrite the current output until it's been handled
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                LogHub.logFatalCrash("Thread interrupted during local data transmission.", e);
            }
        }
        output.set(data);
    }

    /**
     * Pair two LocalDataLinks so that their input and output pointers match each other's output and input arrays.
     */
    public static void pair(LocalDataLink link1, LocalDataLink link2) {
        link1.input = link2.output;
        link1.output = link2.input;
        new Thread(link1).start();
        new Thread(link2).start();
    }
}
