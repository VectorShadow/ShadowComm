package crypto;

/**
 * Convert strings to hexadecimal representations of their bytes.
 */
public class HexCipher {

    private static final char UPPER_MASK = 0xf0; //mask for converting hex chars to normal chars
    private static final char LOWER_MASK = 0x0f; //mask for converting hex chars to normal chars

    /**
     * Convert a hexString into a byte array - reversal of convertToHexString().
     */
    public static byte[] convertFromHexString(String hexString) {
        byte[] out = new byte[hexString.length() / 2];
        for (int i = 0; i < out.length; i++){
            char upperHex = hexString.charAt(2*i);
            int upper = ((hexToInt(upperHex) << 4) & UPPER_MASK);
            char lowerHex = hexString.charAt(2*i + 1);
            int lower = (hexToInt(lowerHex) & LOWER_MASK);
            out[i] = (byte)(upper | lower);
        }
        return out;
    }

    /**
     * Converts an array of bytes into a hexString. This permits data to be encrypted via RSA.
     */
    public static String convertToHexString(byte[] data) {
        String hexString = "";
        for(byte b : data){
            hexString += hexFromInt(((b & UPPER_MASK) >> 4));
            hexString += hexFromInt((b & LOWER_MASK));
        }
        return hexString;
    }

    /**
     * Convert an integer value to an alphanumeric character representing that value in hexadecimal.
     */
    private static char hexFromInt(int intValue) {
        switch (intValue){
            case 0: return '0';
            case 1: return '1';
            case 2: return '2';
            case 3: return '3';
            case 4: return '4';
            case 5: return '5';
            case 6: return '6';
            case 7: return '7';
            case 8: return '8';
            case 9: return '9';
            case 10: return 'a';
            case 11: return 'b';
            case 12: return 'c';
            case 13: return 'd';
            case 14: return 'e';
            case 15: return 'f';
            default: throw new IllegalArgumentException("Invalid sourceChar " + intValue);
        }
    }

    /**
     * Convert an alphanumeric character representing a hexadecimal value to an int corresponding to that value.
     */
    private static int hexToInt(char hexChar) {
        switch (hexChar){
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'a': return 10;
            case 'b': return 11;
            case 'c': return 12;
            case 'd': return 13;
            case 'e': return 14;
            case 'f': return 15;
            default: throw new IllegalArgumentException("Invalid sourceChar " + hexChar + " as Int: " + (int)hexChar);
        }
    }
}
