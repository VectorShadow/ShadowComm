package link;

import xmit.StreamConverter;

import java.io.Serializable;
import java.net.Socket;

/**
 * DataHandler is designed to unpack and handle transmitted data in an implementation specific fashion.
 * Implementations must override the protected version of handle and provide instructions for all instruction codes
 * greater than or equal to the reserve code count.
 */
public abstract class DataHandler {

    protected static final int RESERVED_CODE_COUNT = 0;

    /**
     * Implementation specific handling of a lost connection on the specified socket.
     */
    protected abstract void connectionLost(Socket socket);

    /**
     * Implementation specific handling instructions for implementation specific codes.
     * @param instructionCode
     * @param data
     */
    protected abstract void handle(int instructionCode, Serializable data);

    /**
     * Provided for the use of DataLink and its descendants.
     * @param data all bytes belonging to a particular transmission to be handled
     */
    void handle(byte instructionCode, byte[] data) {
        Serializable serializable = StreamConverter.toObject(data);
        if (test(instructionCode, serializable))
            handle(instructionCode, serializable);
    }
    /**
     * Test an instruction code to see if it belongs to the set of instruction codes reserved for internal use.
     * @param instructionCode the code to be tested
     * @param data the data transmitted under the code
     * @return true if the instruction should be handled by the implementation, false if already handled internally.
     */
    private boolean test(int instructionCode, Serializable data) {
        if (instructionCode >= RESERVED_CODE_COUNT) return true;
        switch (instructionCode) {
            //todo - reserved codes
            default:
                throw new IllegalArgumentException("Unsupported instruction code: " + instructionCode);
        }
        //todo - currently unreachable: return false;
    }
}
