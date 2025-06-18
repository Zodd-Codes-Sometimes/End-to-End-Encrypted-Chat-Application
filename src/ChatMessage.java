package src;

import java.security.PublicKey;
import java.io.Serializable;

public class ChatMessage implements Serializable {
    private final String sender;
    private final String recipient; // New: For directed messages
    private final String message; // Plaintext or encrypted content
    private final MessageType type; // Type of message
    private final PublicKey publicKey; // For key exchange messages

    public enum MessageType {
        TEXT, ENCRYPTED_TEXT, PUBLIC_KEY, KEY_REQUEST
    }

    // Constructor for text/encrypted messages
    public ChatMessage(String sender, String recipient, String message, MessageType type) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.type = type;
        this.publicKey = null;
    }

    // Constructor for public key exchange
    public ChatMessage(String sender, PublicKey publicKey) {
        this.sender = sender;
        this.recipient = null;
        this.message = null;
        this.type = MessageType.PUBLIC_KEY;
        this.publicKey = publicKey;
    }

    // Constructor for key request
    public ChatMessage(String sender, String recipient, MessageType type) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = null;
        this.type = type;
        this.publicKey = null;
    }

    // Returns the sender of the message.
    // @return The sender as a String.
    public String getSender() {
        return sender;
    }

    // Returns the recipient of the message.
    // @return The recipient as a String.
    public String getRecipient() {
        return recipient;
    }

    // Returns the content of the message.
    // @return The message as a String.
    public String getMessage() {
        return message;
    }

    // Returns the type of the message.
    // @return The message type as a MessageType enum.
    public MessageType getType() {
        return type;
    }

    // Returns the public key associated with the message.
    // @return The public key as a PublicKey object.
    public PublicKey getPublicKey() {
        return publicKey;
    }

}
