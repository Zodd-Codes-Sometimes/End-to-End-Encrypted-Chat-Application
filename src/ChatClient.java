package src;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import utils.RSAUtils;

public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private KeyPair keyPair;
    private Map<String, PublicKey> publicKeys;

    public ChatClient() {
        publicKeys = new HashMap<>();
        try {
            keyPair = RSAUtils.generateRSAKeyPair(2048);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public void start() throws Exception {
        Scanner scanner = new Scanner(System.in);
        int attempts = 0;

        while (attempts < 3) {
            try {
                System.out.print("Enter Server IP: ");
                String ip = scanner.nextLine();
                socket = new Socket(ip, 9001);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                ChatMessage msg = (ChatMessage) in.readObject();
                System.out.println(msg.getMessage());

                while (true) {
                    System.out.print("Enter username: ");
                    username = scanner.nextLine().replaceAll("\\s+", "-");
                    out.writeObject(username);
                    ChatMessage response = (ChatMessage) in.readObject();
                    System.out.println(response.getMessage());
                    if (response.getMessage().startsWith("[*] Username accepted")) break;
                }

                out.writeObject(new ChatMessage(username, keyPair.getPublic()));
                return;

            } catch (Exception e) {
                System.out.println("[!] Connection failed. Try again. " + e.getMessage());
                attempts++;
                if (attempts >= 3) {
                    System.out.println("[!] Maximum attempts reached. Exiting.");
                    return;
                }
            }
        }
    }

    public void start(String ip, int port, String username) throws Exception {
        this.username = username.replaceAll("\\s+", "-");
        int attempts = 0;
        while (attempts < 3) {
            try {
                socket = new Socket(ip, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                ChatMessage msg = (ChatMessage) in.readObject();
                System.out.println(msg.getMessage());

                out.writeObject(this.username);
                ChatMessage response = (ChatMessage) in.readObject();
                System.out.println(response.getMessage());
                if (response.getMessage().startsWith("[*] Username accepted")) {
                    out.writeObject(new ChatMessage(this.username, keyPair.getPublic()));
                    return;
                }
                attempts++;
                System.out.println("[!] Username rejected. Try another.");
            } catch (Exception e) {
                attempts++;
                System.out.println("[!] Connection failed: " + e.getMessage());
                if (attempts >= 3) {
                    throw new Exception("[!] Max attempts reached. Exiting.");
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

    public ChatMessage receiveMessage(long timeoutMillis) throws Exception {
        socket.setSoTimeout((int) timeoutMillis);
        try {
            return (ChatMessage) in.readObject();
        } finally {
            socket.setSoTimeout(0);
        }
    }

    public PublicKey requestPublicKey(String recipient) throws Exception {
        if (publicKeys.containsKey(recipient)) {
            return publicKeys.get(recipient);
        }

        out.writeObject(new ChatMessage(username, recipient, ChatMessage.MessageType.KEY_REQUEST));
        ChatMessage response = (ChatMessage) in.readObject();

        if (response.getType() == ChatMessage.MessageType.PUBLIC_KEY) {
            publicKeys.put(response.getSender(), response.getPublicKey());
            return response.getPublicKey();
        } else {
            System.out.println(response.getMessage());
            return null;
        }
    }

    public String getUsername() {
        return username;
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}