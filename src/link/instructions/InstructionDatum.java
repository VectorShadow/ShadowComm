package link.instructions;

import crypto.ByteCipher;
import main.LogHub;

import java.io.*;
//todo - proper, verifiable headers and trailers - be sure to check for these in RemoteDataLink.listen().
// Also we need to design a scheme for acknowledging instructions, resending anything critical which failed, and
// tracking discrepancies automatically so the client need not request a GameZone retransmission.
// While we're here, let's also make sure we never have the client sending critical data (like Avatars - instead send a
// number index which the engine can use to select an Avatar from its end based on the connected account).
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

    private static final int MASK0 = 0xff00_0000;
    private static final int MASK1 = 0x00ff_0000;
    private static final int MASK2 = 0x0000_ff00;
    private static final int MASK3 = 0x0000_00ff;

    public static final int HEADER_INDICATOR = 0x7fff;
    public static final int TRAILER_INDICATOR = 0x1e2d4b87;

    public static final int HEADER_INDICATOR_LENGTH = 2;
    public static final int HEADER_SIZE_LENGTH = 2;
    public static final int HEADER_SEQUENCE_LENGTH = 4;
    public static final int TRAILER_INDICATOR_LENGTH = 4;
    public static final int TRAILER_CHECKSUM_LENGTH = 4;

    public static final int HEADER_LENGTH = HEADER_INDICATOR_LENGTH + HEADER_SIZE_LENGTH + HEADER_SEQUENCE_LENGTH;
    public static final int TRAILER_LENGTH = TRAILER_INDICATOR_LENGTH + TRAILER_CHECKSUM_LENGTH;

    private static final int MAX_DATUM_SIZE = MASK2 | MASK3;
    private static final int MAX_SEQUENCE_INDEX = MASK0 | MASK1 | MASK2 | MASK3;

    public static final int MAX_PACKET_LENGTH = HEADER_LENGTH + MAX_DATUM_SIZE + TRAILER_LENGTH;

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
    public byte[] pack(int sequenceIndex, boolean encrypt) {
        byte[] rawData = toByteArray();
        int size = rawData.length;
        if (size > MAX_DATUM_SIZE)
            throw new IllegalStateException("InstructionDatum too large to pack: " + size + " > " + MAX_DATUM_SIZE);
        if (sequenceIndex > MAX_SEQUENCE_INDEX)
            throw new IllegalStateException(
                    "InstructionDatum Sequence Index exceeds bounds: " + sequenceIndex + " > " + MAX_SEQUENCE_INDEX
            );
        int packedDataSize = HEADER_LENGTH + size + TRAILER_LENGTH;
        byte[] packedData = new byte[packedDataSize];
        //header - indicator
        packedData[0] = (byte)((HEADER_INDICATOR & MASK2) >> 8);
        packedData[1] = (byte)(HEADER_INDICATOR & MASK3);
        //header - size
        packedData[2] = (byte)((size & MASK2) >> 8);
        packedData[3] = (byte)(size & MASK3);
        //header - sequence index
        packedData[4] = (byte)(sequenceIndex & MASK0 >> 24);
        packedData[5] = (byte)(sequenceIndex & MASK1 >> 16);
        packedData[6] = (byte)(sequenceIndex & MASK2 >> 8);
        packedData[7] = (byte)(sequenceIndex & MASK3);
        int checksum = 0;
        for (int i = 0; i < size; ++i){
            byte b = rawData[i];
            checksum += (int)b;
            packedData[HEADER_LENGTH + i] = b;
        }
        //trailer - indicator
        packedData[packedDataSize - 8] = (byte)((TRAILER_INDICATOR & MASK0) >> 24);
        packedData[packedDataSize - 7] = (byte)((TRAILER_INDICATOR & MASK1) >> 16);
        packedData[packedDataSize - 6] = (byte)((TRAILER_INDICATOR & MASK2) >> 8);
        packedData[packedDataSize - 5] = (byte)(TRAILER_INDICATOR & MASK3);
        //trailer - validation code
        packedData[packedDataSize - 4] = (byte)(checksum & MASK0 >> 24);
        packedData[packedDataSize - 3] = (byte)(checksum & MASK1 >> 16);
        packedData[packedDataSize - 2] = (byte)(checksum & MASK2 >> 8);
        packedData[packedDataSize - 1] = (byte)(checksum & MASK3);
        System.arraycopy(encrypt ? ByteCipher.encrypt(rawData) : rawData, 0, packedData, HEADER_LENGTH, size);
        return packedData;
    }

    /**
     * Recover the integer value corresponding to size from the three bytes carrying that value.
     * @Deprecated by toInt().
     */
    @Deprecated
    public static int readSize(byte s0, byte s1) {
        return ((s0 << 8) & MASK0) | (s1 & MASK1);
    }
    /**
     * Find an integer value associated with up to 4 consecutive bytes at the specified offset within the provided
     * byte array.
     */
    public static int toInt(byte[] bytes, int offset, int size) {
        int value = 0;
        for (int counter = size; counter > 0; --counter) {
            byte b = bytes[offset + (size - counter)];
            switch (counter) {
                case 1:
                    value |= (int)b & MASK3;
                    break;
                case 2:
                    value |= ((int)b << 8) & MASK2;
                    break;
                case 3:
                    value |= ((int)b << 16) & MASK1;
                    break;
                case 4:
                    value |= ((int)b << 24) & MASK0;
                    break;
                    default:
                        throw new IllegalArgumentException("Size exceeds int capacity: " + size + " > " + " 4 bytes.");
            }
        }
        return value;
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
