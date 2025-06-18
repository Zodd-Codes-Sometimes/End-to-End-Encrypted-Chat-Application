package src;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import utils.RSAUtils;
import utils.SerializationUtils;
import java.util.Base64;

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
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                ChatMessage msg = (ChatMessage) in.readObject();
                System.out.println(msg.getMessage());

                while (true) {
                    System.out.print("Enter username: ");
                    username = scanner.nextLine().replaceAll("\\s+", "-");
                    // Validate username client-side
                    if (username == null || username.trim().isEmpty()) {
                        System.out.println("[!] Username cannot be empty. Try again.");
                        continue;
                    }
                    synchronized (out) {
                        out.writeObject(username);
                        out.flush();
                    }
                    ChatMessage response = (ChatMessage) in.readObject();
                    System.out.println(response.getMessage());
                    if (response.getMessage().startsWith("[*] Username accepted")) break;
                }

                synchronized (out) {
                    out.writeObject(new ChatMessage(username, keyPair.getPublic()));
                    out.flush();
                }

                ChatMessage keyMapMsg = (ChatMessage) in.readObject();
                if (keyMapMsg.getType() == ChatMessage.MessageType.PUBLIC_KEY_MAP) {
                    Map<String, String> keyStrings = SerializationUtils.deserialize(keyMapMsg.getMessage());
                    for (Map.Entry<String, String> entry : keyStrings.entrySet()) {
                        byte[] keyBytes = Base64.getDecoder().decode(entry.getValue());
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        publicKeys.put(entry.getKey(), kf.generatePublic(spec));
                    }
                } else {
                    throw new IOException("Expected public key map");
                }

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
        if (this.username == null || this.username.trim().isEmpty()) {
            throw new Exception("[!] Username cannot be empty.");
        }
        int attempts = 0;
        while (attempts < 3) {
            try {
                socket = new Socket(ip, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                ChatMessage msg = (ChatMessage) in.readObject();
                System.out.println(msg.getMessage());

                synchronized (out) {
                    out.writeObject(this.username);
                    out.flush();
                }
                ChatMessage response = (ChatMessage) in.readObject();
                System.out.println(response.getMessage());
                if (response.getMessage().startsWith("[*] Username accepted")) {
                    synchronized (out) {
                        out.writeObject(new ChatMessage(this.username, keyPair.getPublic()));
                        out.flush();
                    }
                    ChatMessage keyMapMsg = (ChatMessage) in.readObject();
                    if (keyMapMsg.getType() == ChatMessage.MessageType.PUBLIC_KEY_MAP) {
                        Map<String, String> keyStrings = SerializationUtils.deserialize(keyMapMsg.getMessage());
                        for (Map.Entry<String, String> entry : keyStrings.entrySet()) {
                            byte[] keyBytes = Base64.getDecoder().decode(entry.getValue());
                            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                            KeyFactory kf = KeyFactory.getInstance("RSA");
                            publicKeys.put(entry.getKey(), kf.generatePublic(spec));
                        }
                    } else {
                        throw new IOException("Expected public key map");
                    }
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
        synchronized (out) {
            out.writeObject(message);
            out.flush();
        }
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

    public Map<String, PublicKey> getPublicKeys() {
        return publicKeys;
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