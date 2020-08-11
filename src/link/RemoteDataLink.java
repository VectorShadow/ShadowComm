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

import static link.instructions.InstructionDatum.*;

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
        //an integer representation of specific byte values, used for verification of packet integrity
        int checkValue;
        //reader array - bytes from the input stream go here
        byte[] readStream;
        //reader counter
        int bytesRead;
        //instruction array - build an instruction from the data present in the reader array
        byte[] instruction = new byte[MAX_PACKET_LENGTH];
        //writing pointer - mark our position in the instruction array.
        int writingAt = 0;
        //expected instruction size - read from the instruction header, used for verification of packet integrity
        int expectedInstructionSize;
        //the sequential index of the current instruction
        int instructionSequenceIndex;
        //a sum of the values of all bytes received in an instruction, used for verification of packet integrity
        int instructionCheckSum = 0;
        //indicate whether we have found the trailer indicator for the current instruction
        boolean instructionTrailerFound = false;
        //reading pointer - mark our position in the reader array
        int readingAt = 0;
        do {
            try {
                //reset the reader array
                readStream = new byte[MAX_PACKET_LENGTH];
                //read from the socket input stream into the reader array
                bytesRead = socket.getInputStream().read(readStream, 0, MAX_PACKET_LENGTH);
                if (bytesRead < 0) { //error reading on socket - connection lost
                    DATA_HANDLER.connectionLost(this); //report the connection lost to the data handler
                } else if (bytesRead > 0){ //data was read from the stream this pass
                    //no current instruction, or current instruction contains a corrupted header:
                    //reset all instruction values, and begin looking for a new header.
                    if (
                            writingAt < 2 ||
                                    writingAt == 3 ||
                                    (writingAt > 4 && writingAt < HEADER_LENGTH)
                    ) {
                        instruction = new byte[MAX_PACKET_LENGTH];
                        writingAt = 0;
                        instructionCheckSum = 0;
                        instructionTrailerFound = false;
                        while (readingAt < bytesRead) {
                            //check the next 2 bytes in the reading array to see if the indicate an instruction header
                            checkValue = toInt(readStream, readingAt, HEADER_INDICATOR_LENGTH);
                            //if so, we're done
                            if (checkValue == HEADER_INDICATOR) break;
                            //otherwise increment the reading pointer and try again
                            ++readingAt;
                        }
                        //track the header indicator
                        writingAt += HEADER_INDICATOR_LENGTH;
                        //advance the reading pointer to the next expected value
                        readingAt += HEADER_INDICATOR_LENGTH;
                    }
                    //current instruction contains only the header indicator:
                    //read the next two bytes to determine the expected size.
                    if (writingAt == HEADER_INDICATOR_LENGTH) {
                        expectedInstructionSize = toInt(readStream, readingAt, HEADER_SIZE_LENGTH);
                        writingAt += HEADER_SIZE_LENGTH;
                        readingAt += HEADER_SIZE_LENGTH;
                    }
                    //current instruction contains only the header indicator and instruction size
                    //read the next four bytes to determine the packet sequence index.
                    if (writingAt == HEADER_INDICATOR_LENGTH + HEADER_SIZE_LENGTH) {
                        instructionSequenceIndex = toInt(readStream, readingAt, HEADER_SEQUENCE_LENGTH);
                        writingAt += HEADER_SEQUENCE_LENGTH;
                        readingAt += HEADER_SEQUENCE_LENGTH;
                    }
                    //current instruction contains the header data and 0 or more bytes of the actual instruction:
                    //continue writing the instruction from the reader array until we find the trailer indicator
                    //or run out of data to read.
                    if (writingAt >= HEADER_LENGTH && !instructionTrailerFound) {
                        //loop until we have no more bytes to read, or we have read the trailer indicator
                        while (readingAt < bytesRead && !instructionTrailerFound) {
                            checkValue = toInt(readStream, readingAt, 4);
                            if (checkValue == TRAILER_INDICATOR) {
                                instructionTrailerFound = true;
                                writingAt += TRAILER_INDICATOR_LENGTH;
                                readingAt += TRAILER_INDICATOR_LENGTH;
                            }
                            else {
                                byte b = readStream[readingAt++];
                                instructionCheckSum += (int)b;
                                instruction[writingAt++] = b;
                            }
                        }
                        //todo - check instruction size (via writing at + header). if too short, do not mark trailer
                        // found. if we reach the exact expected size and have not found the trailer, corrupt the instruction and start over.
                    }
                    //todo - more cleanup, saving existing instructions, completing and handling instructions, etc.
                } //else bytesRead == 0 - no data was present in the stream
            } catch (SocketException se) {
                DATA_HANDLER.connectionLost(this);
            } catch (Exception e) {
                LogHub.logFatalCrash("Exception in RemoteDataLink thread.", e);
            }
        } while (!terminated);

//        boolean activeInstruction = false;
//        int instructionBodySize = 0; //the number of bytes expected by the current instruction
//        int bytesReadInInstruction = 0; //the number of bytes read from the stream so far for this instruction
//        byte[] instructionBody = new byte[0]; //an array which accumulates the data for this instruction
//        byte[] streamBlock; //the block read from the stream this iteration
//        ArrayList<Byte> excess = new ArrayList<>(); //leftover data in the stream after completing the last instruction
//        int bytesRead = 0; //the number of bytes read from the stream this iteration
//        boolean firstBlock = true; //only the first block of a transmission should exclude the header segment
//        do {
//            try {
//                if (excess.size() > 0) {
//                    streamBlock = listToArray(excess);
//                    bytesRead = excess.size();
//                    excess = new ArrayList<>();
//                } else {
//                    streamBlock = new byte[BLOCK_SIZE];
//                    bytesRead = socket.getInputStream().read(streamBlock,0, BLOCK_SIZE);
//                }
//                if (bytesRead < 0) { //error reading on socket - connection lost
//                    DATA_HANDLER.connectionLost(this); //report the connection lost to the data handler
//                } else if (bytesRead > 0){ //data was read from the stream this pass - we do nothing on 0
//                    if (!activeInstruction) { //if we have no current instruction, parse for the next one
//                        if (bytesRead < HEADER_LENGTH) throw new IllegalArgumentException( //we need a 4 byte header to begin
//                                "Stream contained too few bytes to parse: " + bytesRead + " bytes were in the stream.");
//                        activeInstruction = true;
//                        instructionBodySize = readSize(streamBlock[0], streamBlock[1]); //get the size as an int
//                        instructionBody = new byte[instructionBodySize]; //initialize the body block
//                    } else firstBlock = false; //else ensure we no longer exclude the header until this instruction is complete
//                    for (int i = firstBlock ? HEADER_LENGTH : 0; i < bytesRead; ++i) { //iterate through the bytes read this pass
//                        if (bytesReadInInstruction < instructionBodySize){ //bytes from the current instruction
//                            instructionBody[bytesReadInInstruction++] = streamBlock[i];
//                        } else { //bytes from a new instruction
//                            excess.add(streamBlock[i]); //these go into excess to be handled next pass
//                        }
//                    }
//                    if (bytesReadInInstruction >= instructionBodySize) { //if we finished an instruction, handle it
//                        if (encrypted) //post handshake transmissions are encrypted
//                            instructionBody = ByteCipher.decrypt(instructionBody);
//                        DATA_HANDLER.handle(instructionBody, this);
//                        activeInstruction = false; //then reset the data members
//                        instructionBodySize = 0;
//                        bytesReadInInstruction = 0;
//                        firstBlock = true;
//                    }
//                }
//            } catch (SocketException se) {
//                DATA_HANDLER.connectionLost(this);
//            } catch (Exception e) {
//                LogHub.logFatalCrash("Exception in RemoteDataLink thread.", e);
//            }
//        } while (!terminated);
    }

    /**
     * Unencrypted transmission on a remote data link is only permitted to establish end-to-end encryption.
     * Once it has been established, or if the instruction datum being transmitted is part of the handshake,
     * it proceeds as usual.
     */
    @Override
    public void transmit(InstructionDatum instructionDatum) {
        LiveLog.log("transmitting instruction datum of class " + instructionDatum.getClass(), LiveLog.LogEntryPriority.DEBUG);
        if (instructionDatum instanceof HandshakeInstructionDatum)
            transmit(pack(false));
        else if (encrypted) transmit(pack(true));
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
