package xmit;

import java.io.Serializable;

public class DataPacker {
    public static final int HEADER_LENGTH = 4;
    private static int MASK1 = 0x00ff_0000;
    private static int MASK2 = 0x0000_ff00;
    private static int MASK3 = 0x0000_00ff;

    public static int readSize(byte s1, byte s2, byte s3) {
        return ((s1 << 16) & MASK1) | ((s2 << 8) & MASK2) | (s3 & MASK3);
    }
    public static byte[] pack(Serializable s, byte instructionCode) {
        byte[] rawData = StreamConverter.toByteArray(s);
        int size = rawData.length;
        byte[] packedData = new byte[size + HEADER_LENGTH];
        packedData[0] = instructionCode;
        packedData[1] = (byte)((size & MASK1) >> 16);
        packedData[2] = (byte)((size & MASK2) >> 8);
        packedData[3] = (byte)(size & MASK3);
        for (int i = 0; i < size; ++i) packedData[HEADER_LENGTH + i] = rawData[i];
        return packedData;
    }
}