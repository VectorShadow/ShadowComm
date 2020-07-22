package link;

import crypto.ByteCipher;
import crypto.HexCipher;
import crypto.RSA;
import link.instructions.ConfirmEncryptionInstructionDatum;
import link.instructions.InstructionDatum;
import link.instructions.TransmitEncryptedSecretKeyInstructionDatum;
import link.instructions.TransmitPublicKeyInstructionDatum;

import java.math.BigInteger;
import java.net.Socket;

/**
 * DataHandler is designed to unpack and handle transmitted data in an implementation specific fashion.
 * Implementations must override the protected version of handle and provide instructions for all instruction codes
 * greater than or equal to the reserve code count.
 */
public abstract class DataHandler {

    /**
     * Implementation specific handling of a lost connection on the specified socket.
     */
    protected abstract void connectionLost(Socket socket);

    /**
     * Implementation specific handling instructions for implementation specific codes.
     * @param instructionCode the code to be handled
     * @param instructionDatum the instructionDatum reconstructed from the transmitted data
     * @param responseLink the DataLink on which to transmit any required response
     */
    protected abstract void handle(int instructionCode, InstructionDatum instructionDatum, DataLink responseLink);

    /**
     * Provided for the use of DataLink and its descendants.
     * @param instructionCode the code to be tested
     * @param data all bytes belonging to a particular transmission to be handled
     * @param responseLink the DataLink on which to transmit any required response
     */
    void handle(byte instructionCode, byte[] data, DataLink responseLink) {
        InstructionDatum instructionDatum = InstructionDatum.fromByteArray(data);
        if (test(instructionCode, instructionDatum, responseLink))
            handle(instructionCode, instructionDatum, responseLink);
    }
    /**
     * Test an instruction code to see if it belongs to the set of instruction codes reserved for internal use.
     * @param instructionCode the code to be tested
     * @param instructionDatum the instructionDatum reconstructed from the transmitted data
     * @param responseLink the DataLink on which to transmit any required response
     * @return true if the instruction should be handled by the implementation, false if already handled internally.
     */
    private boolean test(int instructionCode, InstructionDatum instructionDatum, DataLink responseLink) {
        if (instructionCode >= InstructionDatum.FIRST_ENCRYPTABLE_INSTRUCTION_CODE) return true;
        switch (instructionCode) {
            /*
             * Receive a public key.
             * This is a server side operation - the response is to use the public key to encrypt the
             * session key, then transmit the encrypted session key back to the client.
             */
            case InstructionDatum.HANDSHAKE_INSTRUCTION_CODE_TRANSMIT_PUBLIC_KEY:
                BigInteger encryptedSessionKey =
                        RSA.encrypt(
                                new BigInteger(
                                        HexCipher.convertToHexString(
                                                ByteCipher.getSessionKey()
                                        ),
                                        16
                                ),
                                ((TransmitPublicKeyInstructionDatum)instructionDatum).PUBLIC_KEY
                        );
                responseLink.transmit(new TransmitEncryptedSecretKeyInstructionDatum(encryptedSessionKey));
                break;
            /*
             * Receive an encrypted secret key.
             * This is a client side operation - the response is to decrypt the transmitted key via our private key,
             * then overwrite the session secret key with it.
             * We also go ahead and establish end-to-end encryption on the dataLink on our end.
             */
            case InstructionDatum.HANDSHAKE_INSTRUCTION_CODE_TRANSMIT_ENCRYPTED_SECRET_KEY:
                ByteCipher.setSessionKey(
                        HexCipher.convertFromHexString(
                                RSA.decrypt(
                                        ((TransmitEncryptedSecretKeyInstructionDatum)instructionDatum)
                                                .ENCRYPTED_SECRET_KEY
                                ).toString(16)
                        )
                );
                responseLink.establishEndToEndEncryption();
                responseLink.transmit(new ConfirmEncryptionInstructionDatum());
            /*
             * Confirm key exchange.
             * This is a server side operation - we now know that the client is using our session secret key, so all
             * further encrypted operations will be successful.
             * End-to-end encryption is now confirmed on both ends of the link.
             */
            case InstructionDatum.HANDSHAKE_INSTRUCTION_CODE_CONFIRM_ENCRYPTION:
                responseLink.establishEndToEndEncryption();
                break;
            //todo - additional reserved codes, if necessary
            default:
                throw new IllegalArgumentException("Unsupported instruction code: " + instructionCode);
        }
        return false;
    }
}
