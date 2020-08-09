package link.instructions;

import crypto.ByteCipher;
import main.LogHub;

import java.io.*;
//todo - can we get rid of codes and simply read the class?
/**
 * InstructionData are used by DataLinks to transmit data in a format which can be interpreted and accessed by
 * DataHandlers.
 * The primary responsibility of the implementation is to add whatever fields are required to contain the desired data,
 * and ensure that the getInstructionCode operation corresponds to the implementation of DataHandler in such a way that
 * the desired data can be recovered and acted on.
 * It is also the responsibility of the end user to de-conflict implemented instruction code values with the
 * reserved codes in the provided abstract DataHandler, and with any additional implementions of this class.
 */
public abstract class InstructionDatum implements Serializable {

    public static final int HEADER_LENGTH = 2;

    private static int MASK0 = 0x0000_ff00;
    private static int MASK1 = 0x0000_00ff;

    private static final int MAX_DATUM_SIZE = MASK0 | MASK1;

    /**
     * Convert an array of bytes representing an InstructionDatum back into that InstructionDatum.
     */
    public static InstructionDatum fromByteArray(byte[] b) throws ClassCastException, StreamCorruptedException {
        ByteArrayInputStream bis = new ByteArrayInputStream(b);
        ObjectInput objectInput = null;
        InstructionDatum instructionDatum = null;
        try {
            objectInput = new ObjectInputStream(bis);
            instructionDatum = (InstructionDatum) objectInput.readObject();
        } catch (ClassCastException | StreamCorruptedException e) {
            throw e;
        } catch (Exception e) {
            LogHub.logFatalCrash("Data recovery failure", e);
        } finally {
            try {
                if (objectInput != null) {
                    objectInput.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return instructionDatum;
    }

    /**
     * Pack an InstructionDatum into a byte array carrying the instruction code corresponding to the InstructionDatum,
     * followed by the size of the byte array representation of the InstructionDatum, followed by that array.
     */
    public byte[] pack(boolean encrypt) {
        byte[] rawData = toByteArray();
        int size = rawData.length;
        if (size > MAX_DATUM_SIZE)
            throw new IllegalStateException("InstructionDatum too large to pack: " + size + " > " + MAX_DATUM_SIZE);
        byte[] packedData = new byte[size + HEADER_LENGTH];
        packedData[0] = (byte)((size & MASK0) >> 8);
        packedData[1] = (byte)(size & MASK1);
        System.arraycopy(encrypt ? ByteCipher.encrypt(rawData) : rawData, 0, packedData, HEADER_LENGTH, size);
        return packedData;
    }

    /**
     * Recover the integer value corresponding to size from the three bytes carrying that value.
     */
    public static int readSize(byte s0, byte s1) {
        return ((s0 << 8) & MASK0) | (s1 & MASK1);
    }

    /**
     * @return a byte array representation of this InstructionDatum.
     */
    private byte[] toByteArray() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] b = new byte[]{};
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            b = bos.toByteArray();
        } catch (Exception e) {
            LogHub.logFatalCrash("Data conversion failure", e);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return b;
    }
}
