package xmit;

import main.LogHub;

import java.io.*;

public class StreamConverter {
    public static byte[] toByteArray(Serializable s) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] b = new byte[]{};
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(s);
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
    public static Serializable toObject(byte[] b) {
        ByteArrayInputStream bis = new ByteArrayInputStream(b);
        ObjectInput in = null;
        Serializable ad = null;
        try {
            in = new ObjectInputStream(bis);
            ad = (Serializable) in.readObject();
        } catch (Exception e) {
            LogHub.logFatalCrash("Data recovery failure", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return ad;
    }
}
