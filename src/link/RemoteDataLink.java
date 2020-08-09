package link;

import crypto.ByteCipher;
import link.instructions.HandshakeInstructionDatum;
import link.instructions.InstructionDatum;
import main.LiveLog;
import main.LogHub;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class RemoteDataLink extends DataLink {

    private static final int BLOCK_SIZE = 1_024;

    private final Socket socket;

    public RemoteDataLink(DataHandler dataHandler, Socket socket) {
        super(dataHandler);
        this.socket = socket;
        try { //clear the output stream of any remaining data if we re-use an old socket
            this.socket.getOutputStream().flush();
        } catch (IOException e) {
            LogHub.logFatalCrash("Socket stream reset failed.", e);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    /**
     * todo - write up of how receive works
     */
    protected void receive() {
        boolean activeInstruction = false;
        int instructionBodySize = 0; //the number of bytes expected by the current instruction
        int bytesReadInInstruction = 0; //the number of bytes read from the stream so far for this instruction
        byte[] instructionBody = new byte[0]; //an array which accumulates the data for this instruction
        byte[] streamBlock; //the block read from the stream this iteration
        ArrayList<Byte> excess = new ArrayList<>(); //leftover data in the stream after completing the last instruction
        int bytesRead = 0; //the number of bytes read from the stream this iteration
        boolean firstBlock = true; //only the first block of a transmission should exclude the header segment
        do {
            try {
                if (excess.size() > 0) {
                    streamBlock = listToArray(excess);
                    bytesRead = excess.size();
                    excess = new ArrayList<>();
                } else {
                    streamBlock = new byte[BLOCK_SIZE];
                    bytesRead = socket.getInputStream().read(streamBlock,0, BLOCK_SIZE);
                }
                if (bytesRead < 0) { //error reading on socket - connection lost
                    DATA_HANDLER.connectionLost(this); //report the connection lost to the data handler
                } else if (bytesRead > 0){ //data was read from the stream this pass - we do nothing on 0
                    if (!activeInstruction) { //if we have no current instruction, parse for the next one
                        if (bytesRead < InstructionDatum.HEADER_LENGTH) throw new IllegalArgumentException( //we need a 4 byte header to begin
                                "Stream contained too few bytes to parse: " + bytesRead + " bytes were in the stream.");
                        activeInstruction = true;
                        instructionBodySize = InstructionDatum.readSize(streamBlock[0], streamBlock[1]); //get the size as an int
                        instructionBody = new byte[instructionBodySize]; //initialize the body block
                    } else firstBlock = false; //else ensure we no longer exclude the header until this instruction is complete
                    for (int i = firstBlock ? InstructionDatum.HEADER_LENGTH : 0; i < bytesRead; ++i) { //iterate through the bytes read this pass
                        if (bytesReadInInstruction < instructionBodySize){ //bytes from the current instruction
                            instructionBody[bytesReadInInstruction++] = streamBlock[i];
                        } else { //bytes from a new instruction
                            excess.add(streamBlock[i]); //these go into excess to be handled next pass
                        }
                    }
                    if (bytesReadInInstruction >= instructionBodySize) { //if we finished an instruction, handle it
                        if (encrypted) //post handshake transmissions are encrypted
                            instructionBody = ByteCipher.decrypt(instructionBody);
                        DATA_HANDLER.handle(instructionBody, this);
                        activeInstruction = false; //then reset the data members
                        instructionBodySize = 0;
                        bytesReadInInstruction = 0;
                        firstBlock = true;
                    }
                }
            } catch (SocketException se) {
                DATA_HANDLER.connectionLost(this);
            } catch (Exception e) {
                LogHub.logFatalCrash("Exception in RemoteDataLink thread.", e);
            }
        } while (!terminated);
    }

    /**
     * Unencrypted transmission on a remote data link is only permitted to establish end-to-end encryption.
     * Once it has been established, or if the instruction datum being transmitted is part of the handshake,
     * it proceeds as usual.
     */
    @Override
    public void transmit(InstructionDatum instructionDatum) {
        if (instructionDatum instanceof HandshakeInstructionDatum)
            transmit(instructionDatum.pack(false));
        else if (encrypted) transmit(instructionDatum.pack(true));
        else
            throw new IllegalStateException("Attempted to transmit remotely without establishing end-to-end encryption.");
    }

    /**
     * Transmission is accomplished remotely by writing all data to the associated socket's output stream.
     */
    @Override
    protected void transmit(byte[] data){
        try {
            socket.getOutputStream().write(data);
        } catch (SocketException se) {
            DATA_HANDLER.connectionLost(this);
        } catch (IOException ioe) {
            LogHub.logFatalCrash("Unexpected IOException on data transmission.", ioe);
        }
    }

    private byte[] listToArray(ArrayList<Byte> list) {
        byte[] array = new byte[BLOCK_SIZE];
        for (int i = 0; i < list.size(); ++i) array[i] = list.get(i);
        return array;
    }
}
