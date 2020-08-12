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
        byte[] readStream = new byte[MAX_PACKET_LENGTH];
        //the value of the last byte read from the socket input stream
        int byteRead;
        //reader counter
        int bytesRead = 0;
        //carryover array - when the outer loop ends before the reader array can be completely read, we preserve the
        // unread data here and prepend it to the reader array for the next loop iteration.
        byte[] carryover = new byte[0];
        //instruction writing array - build an instruction from the data present in the reader array
        byte[] instruction = new byte[MAX_DATUM_SIZE];
        //completed instruction array - copy the actual data from our instruction writing array for handling
        byte[] completedInstruction;
        //writing pointer - mark our position in the instruction array.
        int writingAt = 0;
        //expected instruction size - read from the instruction header, used for verification of packet integrity
        int expectedInstructionSize = -1;
        //the sequential index of the current instruction
        int instructionSequenceIndex = -1;
        //a sum of the values of all bytes received in an instruction, used for verification of packet integrity
        int instructionCheckSum = 0;
        //indicate whether we have found the trailer indicator for the current instruction
        boolean instructionTrailerFound = false;
        //reading pointer - mark our position in the reader array
        int readingAt = 0;
        do {
            LiveLog.log(
                    "Beginning outer receive loop - reading at: " + readingAt + " bytesRead: " + bytesRead,
                    LiveLog.LogEntryPriority.DEBUG
            );
            try {
                //check the read stream for any remaining data
                if (readingAt < bytesRead) {
                    //set bytes read to the number of bytes left to read from the read stream
                    bytesRead -= readingAt;
                    //copy the remaining data into the carry over array
                    carryover = new byte[bytesRead];
                    System.arraycopy(readStream, readingAt, carryover, 0, bytesRead);
                }
                //otherwise reset bytes read to zero
                else
                    bytesRead = 0;
                //reset the read counter and read array
                readingAt = 0;
                readStream = new byte[MAX_PACKET_LENGTH];
                //if we had carryover, prepend it to the read array, then reset it
                if (bytesRead > 0) {
                    System.arraycopy(carryover, 0, readStream, 0, bytesRead);
                    carryover = new byte[0];
                }
                //read from the socket input stream into the reader array
                LiveLog.log(
                        "Read array initialized.",
                        LiveLog.LogEntryPriority.DEBUG
                );
                // Read data from the stream so long as the stream has data available and we've not yet read
                // enough bytes to fill our read array
                // OR
                // we haven't yet read more bytes then might have been carried over from the previous iteration.
                while (
                        (bytesRead < MAX_PACKET_LENGTH && socket.getInputStream().available() > 0) ||
                                bytesRead < TRAILER_LENGTH
                ) {
                    byteRead = socket.getInputStream().read();
                    if (byteRead < 0) {
                        DATA_HANDLER.connectionLost(this);
                        break;
                    }
                    readStream[bytesRead++] = (byte)byteRead;
                }
                LiveLog.log(
                        "Reading on socket - read " + bytesRead + " bytes.",
                        LiveLog.LogEntryPriority.DEBUG
                );
                //attempt to derive instructions from the read array until we reach the end
                while (readingAt + TRAILER_LENGTH <= bytesRead) {
                    LiveLog.log(
                            "Beginning inner receive loop. Reading pointer is at " + readingAt + " out of "
                                    + bytesRead + " bytes.",
                            LiveLog.LogEntryPriority.DEBUG
                    );
                    //no current instruction, or current instruction contains a corrupted header:
                    if (
                            writingAt < 2 ||
                                    writingAt == 3 ||
                                    (writingAt > 4 && writingAt < HEADER_LENGTH)
                    ) {
                        LiveLog.log(
                                "No current instruction.",
                                LiveLog.LogEntryPriority.DEBUG
                        );
                        //reset instruction values
                        instruction = new byte[MAX_PACKET_LENGTH];
                        writingAt = 0;
                        instructionCheckSum = 0;
                        instructionTrailerFound = false;
                        //seek the next instruction header
                        while (readingAt + HEADER_LENGTH <= bytesRead) {
                            if (readingAt % 4_096 == 0)
                                LiveLog.log(
                                        "Seeking header indicator. Reading pointer is at " + readingAt + " out of "
                                                + bytesRead + " bytes.",
                                        LiveLog.LogEntryPriority.DEBUG
                                );
                            //check the next 2 bytes in the reading array to see if the indicate an instruction header
                            checkValue = toInt(readStream, readingAt, HEADER_INDICATOR_LENGTH);
                            //if so, we're done
                            if (checkValue == HEADER_INDICATOR) {
                                //track the header indicator
                                writingAt += HEADER_INDICATOR_LENGTH;
                                //advance the reading pointer to the next expected value
                                readingAt += HEADER_INDICATOR_LENGTH;
                                LiveLog.log(
                                        "Found header indicator. Now writing at " + writingAt +
                                                " and reading at " + readingAt,
                                        LiveLog.LogEntryPriority.DEBUG
                                );
                                //then stop looking for the
                                break;
                            }
                            //otherwise increment the reading pointer and try again
                            ++readingAt;
                        }
                    }
                    //current instruction contains only the header indicator:
                    //read the next two bytes to determine the expected size.
                    if (writingAt == HEADER_INDICATOR_LENGTH && readingAt + HEADER_SIZE_LENGTH <= bytesRead) {
                        expectedInstructionSize = toInt(readStream, readingAt, HEADER_SIZE_LENGTH);
                        writingAt += HEADER_SIZE_LENGTH;
                        readingAt += HEADER_SIZE_LENGTH;
                        LiveLog.log(
                                "Parsing for instruction size - " + expectedInstructionSize +
                                        " bytes. Now writing at " + writingAt + " and reading at " + readingAt,
                                LiveLog.LogEntryPriority.DEBUG
                        );
                    }
                    //current instruction contains only the header indicator and instruction size
                    //read the next four bytes to determine the packet sequence index.
                    if (writingAt == HEADER_INDICATOR_LENGTH + HEADER_SIZE_LENGTH &&
                            readingAt + HEADER_SEQUENCE_LENGTH <= bytesRead) {
                        instructionSequenceIndex = toInt(readStream, readingAt, HEADER_SEQUENCE_LENGTH);
                        writingAt += HEADER_SEQUENCE_LENGTH;
                        readingAt += HEADER_SEQUENCE_LENGTH;
                        LiveLog.log(
                                "Parsing for instruction sequence index - " + instructionSequenceIndex +
                                        ". Now writing at " + writingAt + " and reading at " + readingAt,
                                LiveLog.LogEntryPriority.DEBUG
                        );
                    }
                    //current instruction contains the header data and 0 or more bytes of the actual instruction:
                    //continue writing the instruction from the reader array until we find the trailer indicator
                    //or run out of data to read.
                    if (writingAt >= HEADER_LENGTH &&
                            (readingAt + HEADER_SIZE_LENGTH <= bytesRead) &&
                            !instructionTrailerFound
                    ) {
                        LiveLog.log(
                                "Building instruction body. Now writing at " + writingAt +
                                        " and reading at " + readingAt,
                                LiveLog.LogEntryPriority.DEBUG
                        );
                        //loop until we have no more bytes to read, or we have read the trailer indicator
                        while (readingAt + TRAILER_INDICATOR_LENGTH <= bytesRead && !instructionTrailerFound) {
                            checkValue = toInt(readStream, readingAt, 4);
                            //if we find the trailer indicator
                            if (checkValue == TRAILER_INDICATOR) {
                                LiveLog.log(
                                        "Found trailer indicator. Now writing at " + writingAt +
                                                " and reading at " + readingAt,
                                        LiveLog.LogEntryPriority.DEBUG
                                );
                                //mark it found
                                instructionTrailerFound = true;
                                //advance the reading pointer past the trailer indicator
                                readingAt += TRAILER_INDICATOR_LENGTH;
                                if (writingAt - HEADER_LENGTH == expectedInstructionSize)
                                    //if we've written exactly as much data as we expected from the instruction,
                                    //advance the writing pointer and continue
                                    writingAt += TRAILER_INDICATOR_LENGTH;
                                else
                                    //otherwise we have a bad instruction(or a very rare instance where four
                                    // consecutive bytes exactly equal the trailer instruction, a case we can safely
                                    // ignore) and we should reset the writing pointer so we can start looking for the
                                    // next instruction
                                    writingAt = 0;
                            }
                            //if we read the expected number of bytes without encountering the trailer, we must have
                            //lost some data, and this instruction is corrupt. As above, reset the writing pointer and
                            //begin looking for the next instruction
                            else if (writingAt - HEADER_LENGTH == expectedInstructionSize) {
                                writingAt = 0;
                                LiveLog.log(
                                        "Read " + expectedInstructionSize + " bytes without finding " +
                                                "trailer. Packet deemed corrupt.",
                                        LiveLog.LogEntryPriority.DEBUG
                                );
                            }
                            //paranoia case - this should not be able to occur.
                            else if (writingAt - HEADER_LENGTH > expectedInstructionSize) {
                                LogHub.logFatalCrash("Read past expected instruction size without " +
                                        "exactly reaching it or finding trailer.", new IllegalStateException());
                            }
                            else {
                                //otherwise, read the next byte and move the reading pointer
                                byte b = readStream[readingAt++];
                                //add the byte to our checksum - note that if this link is encrypted, the checksum must
                                // be calculated from the decrypted instruction before it can be validated.
                                if (!encrypted)
                                    instructionCheckSum += b;
                                //then write it to our instruction array and advance the writing pointer
                                instruction[writingAt++] = b;
                            }
                        }
                    }
                    //if we've found the instruction trailer and there's enough data left in the reading array to
                    // contain the checksum for this instruction, we need to validate it
                    if (instructionTrailerFound && (readingAt + TRAILER_CHECKSUM_LENGTH <= bytesRead)) {
                        LiveLog.log(
                                "Validating checksum. Now writing at " + writingAt +
                                        " and reading at " + readingAt,
                                LiveLog.LogEntryPriority.DEBUG
                        );
                        //if we haven't yet reset the writing pointer, we have a good instruction so far
                        if (writingAt >= HEADER_LENGTH + expectedInstructionSize) {
                            LiveLog.log(
                                    "Trailer indicator found at expected location. Now writing at "
                                            + writingAt + " and reading at " + readingAt,
                                    LiveLog.LogEntryPriority.DEBUG
                            );
                            //grab the checksum
                            checkValue = toInt(readStream, readingAt, TRAILER_CHECKSUM_LENGTH);
                            //parse the instruction down to just the data
                            completedInstruction = new byte[expectedInstructionSize];
                            System.arraycopy(instruction, HEADER_LENGTH, completedInstruction, 0, expectedInstructionSize);
                            //decrypt if necessary - if this is the case we also need to calculate the checksum from
                            // the decrypted instruction
                            if (encrypted) {
                                LiveLog.log(
                                        "Calculating enrypted checksum...",
                                        LiveLog.LogEntryPriority.DEBUG
                                );
                                completedInstruction = ByteCipher.decrypt(completedInstruction);
                                for (byte b : completedInstruction)
                                    instructionCheckSum += b;
                            }
                            //check the instruction's carried checksum against the checksum calculated from the
                            // instruction data
                            if (checkValue == instructionCheckSum) {
                                LiveLog.log(
                                        "Checksum validated. ",
                                        LiveLog.LogEntryPriority.DEBUG
                                );
                                //pass it to the data handler
                                DATA_HANDLER.handle(completedInstruction, this);
                                //todo - use the sequence to confirm receipt?
                            }
                            else
                                LiveLog.log("Checksum validation failed - passed value: " + checkValue +
                                                " != calculated value: " + instructionCheckSum,
                                        LiveLog.LogEntryPriority.DEBUG);
                            //else this instruction has been corrupted. No need to do anything here, in either case,
                            //we need to begin a new instruction
                        }
                        //reset the writing pointer
                        writingAt = 0;
                        //advance the reading pointer beyond the checksum
                        readingAt += TRAILER_CHECKSUM_LENGTH;
                    }
                } //end while - we've derived all the instructions we can from the data read from the socket stream so far
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
            transmit(instructionDatum.pack(0,false));
        else if (encrypted) transmit(instructionDatum.pack(0,true));
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
