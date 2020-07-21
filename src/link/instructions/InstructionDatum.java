package link.instructions;

import main.LogHub;

import java.io.*;

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
    /**
     * Reserved Instruction Codes.
     */
    public static final int RESERVED_INSTRUCTION_CODE_TRANSMIT_PUBLIC_KEY = 0;
    public static final int RESERVED_INSTRUCTION_CODE_TRANSMIT_ENCRYPTED_SECRET_KEY = 1;
    public static final int RESERVED_INSTRUCTION_CODE_CONFIRM_ENCRYPTION = 2;
    /**
     * Used by DataLinks and static methods for packing and unpacking data.
     */
    public static final int HEADER_LENGTH = 4;
    private static int MASK1 = 0x00ff_0000;
    private static int MASK2 = 0x0000_ff00;
    private static int MASK3 = 0x0000_00ff;

    /**
     * Implementations must begin their own instruction codes at or above this value.
     * @return the first integer which is not associated with a reserved instruction code.
     */
    public static int firstUnreservedInstructionCode() {
        return 3; //This should always be equal to the value of the last reserved code plus one. todo - keep up to date
    }

    /**
     * Convert an array of bytes representing an InstructionDatum back into that InstructionDatum.
     */
    public static InstructionDatum fromByteArray(byte[] b) {
        ByteArrayInputStream bis = new ByteArrayInputStream(b);
        ObjectInput objectInput = null;
        InstructionDatum instructionDatum = null;
        try {
            objectInput = new ObjectInputStream(bis);
            instructionDatum = (InstructionDatum)objectInput.readObject();
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
     * Implementation specific.
     * @return an integer that can be used by the DataHandler to understand how to cast and process this
     * InstructionDatum.
     */
    protected abstract int getInstructionCode();

    /**
     * Pack an InstructionDatum into a byte array carrying the instruction code corresponding to the InstructionDatum,
     * followed by the size of the byte array representation of the InstructionDatum, followed by that array.
     */
    public byte[] pack() {
        byte[] rawData = toByteArray();
        int size = rawData.length;
        byte[] packedData = new byte[size + HEADER_LENGTH];
        packedData[0] = (byte)getInstructionCode();
        packedData[1] = (byte)((size & MASK1) >> 16);
        packedData[2] = (byte)((size & MASK2) >> 8);
        packedData[3] = (byte)(size & MASK3);
        System.arraycopy(rawData, 0, packedData, HEADER_LENGTH, size);
        return packedData;
    }

    /**
     * Recover the integer value corresponding to size from the three bytes carrying that value.
     */
    public static int readSize(byte s1, byte s2, byte s3) {
        return ((s1 << 16) & MASK1) | ((s2 << 8) & MASK2) | (s3 & MASK3);
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
