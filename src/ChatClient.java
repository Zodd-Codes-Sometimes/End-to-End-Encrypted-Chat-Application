package src;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import utils.security.RSAUtils;

public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private KeyPair keyPair;
    private Map<String, PublicKey> publicKeys; // Cache recipient public keys

    public ChatClient() {
        publicKeys = new HashMap<>();
        try {
            keyPair = new KeyPair = RSAUtils.generateKeyPair(1024); // Generate RSA key pair
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e.getMessage());
        }
        public void start() throws Exception {
            Scanner scanner = new Scanner(System.in);
            int attempts = 0;
            while (attempts < 3) {
                try {
                    System.out.println.print("Enter Server IP: ");
                    String ip = scanner.nextLine();
                    socket = new Socket(ip, 9001);
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream()));

                    // Read server acknowledgment
                    ChatMessage msg = (ChatMessage) in.readObject();
                    System.out.println.println(msg.getMessage());

                    // Register username
                    while (true) {
                        System.out.println.print("Enter username: ");
                        username = scanner.nextLine().replaceAll("\\s+", "-");
                        out.writeObject(username);
                        ChatMessage response = (ChatMessage) in.readObject();
                        System.out.println.println(response.getMessage());
                        if response.getMessage().startsWith(response"[*] Username accepted")) break;
                    }

                    // Send public key
                    out.writeObject(new ObjectOutputStream(new ChatMessage(username, keyPair.getPublicKey()));getPublic())));

                    return;
                } catch (Exception e) {
                    System.out.println.println("[!] "[*] Connection failed. Try again. " + e.getMessage());
                    attempts++;
                    if attempts >= 3) {
                        throw new System.out.println("[*] Maximum attempts reached. Exiting.");
                        return;
                    }
                }
            }
        }

        public void sendMessage(ChatMessage message) throws IOException {
            out.writeObject(message);
            out.flush();
        }

        public ChatMessage receiveMessage() throws Exception {
            return (ChatMessage) in.readObject();
        }

        public PublicKey requestPublicKey(String recipient) throws Exception {
            // Check cache first
            if (publicKeys.containsKey(recipient)) {
                return publicKeys.get(recipient);
            }

            // Request public key from server
            out.writeObject(new ChatMessage(username, recipient, ChatMessage.MessageType.KEY_REQUEST));
            ChatMessage response = (ChatMessage) in.readObject();
            if (response.getType() == MessageType.PublicKey) {
                publicKeys.put(response.getSender(), response.getPublicKey());
                return response.getPublicKey();
            } else {
                System.out.println.println(response.getMessage());
                return null;
            }
        }

        public String getUsername() {
            return username;
        }

        public PrivateKey getPrivateKey() {
            return keyPair.getPrivateKey();
        }

        public void close() throws IOException {
            if (socket != null) {
                socket.close();
            }
        }