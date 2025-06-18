package src;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;
import java.security.PublicKey;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import utils.RSAUtils;
import utils.SerializationUtils;

public class Main {
    private ChatClient client;
    private Scanner scanner;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        scanner = new Scanner(System.in);
        client = new ChatClient();

        try {
            client.start();
            synchronized (System.out) {
                System.out.print("[" + client.getUsername() + "]: ");
                System.out.flush();
            }
            new Thread(this::receiveMessages).start();
            handleConsoleInput();
        } catch (Exception e) {
            synchronized (System.out) {
                System.out.println("\r[!] Error: " + e.getMessage());
                System.out.print("[" + client.getUsername() + "]: ");
                System.out.flush();
            }
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    private void receiveMessages() {
        try {
            while (true) {
                ChatMessage msg = client.receiveMessage();
                synchronized (System.out) {
                    if (msg.getType() == ChatMessage.MessageType.ENCRYPTED_TEXT) {
                        Map<String, String> encryptedMap = SerializationUtils.deserialize(msg.getMessage());
                        String encryptedMessage = encryptedMap.get(client.getUsername());
                        if (encryptedMessage != null) {
                            String decrypted = RSAUtils.decrypt(encryptedMessage, client.getPrivateKey());
                            System.out.println("\r[" + msg.getSender() + "]: " + decrypted);
                        }
                        // Skip prompt if the message is from self to avoid double prompt
                        if (!msg.getSender().equals(client.getUsername())) {
                            System.out.print("[" + client.getUsername() + "]: ");
                            System.out.flush();
                        }
                    } else if (msg.getType() == ChatMessage.MessageType.TEXT) {
                        System.out.println("\r" + msg.getMessage());
                        System.out.print("[" + client.getUsername() + "]: ");
                        System.out.flush();
                    } else if (msg.getType() == ChatMessage.MessageType.PUBLIC_KEY_MAP) {
                        Map<String, String> keyStrings = SerializationUtils.deserialize(msg.getMessage());
                        for (Map.Entry<String, String> entry : keyStrings.entrySet()) {
                            byte[] keyBytes = Base64.getDecoder().decode(entry.getValue());
                            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                            KeyFactory kf = KeyFactory.getInstance("RSA");
                            client.getPublicKeys().put(entry.getKey(), kf.generatePublic(spec));
                        }
                        System.out.println("\r[*] Updated public key map.");
                        System.out.print("[" + client.getUsername() + "]: ");
                        System.out.flush();
                    }
                }
            }
        } catch (Exception e) {
            synchronized (System.out) {
                System.out.println("\r[!] Connection lost: " + e.getMessage());
                System.out.print("[" + client.getUsername() + "]: ");
                System.out.flush();
            }
        }
    }

    private void handleConsoleInput() {
        while (true) {
            String input;
            synchronized (scanner) {
                input = scanner.nextLine();
            }
            try {
                if (input.equalsIgnoreCase("/exit")) {
                    client.sendMessage(new ChatMessage(client.getUsername(), null, "/disconnect", ChatMessage.MessageType.TEXT));
                    break;
                } else if (input.trim().isEmpty()) {
                    continue;
                } else {
                    Map<String, String> encryptedMap = new HashMap<>();
                    for (Map.Entry<String, PublicKey> entry : client.getPublicKeys().entrySet()) {
                        String recipient = entry.getKey();
                        if (!recipient.equals(client.getUsername())) {
                            String encrypted = RSAUtils.encrypt(input, entry.getValue());
                            encryptedMap.put(recipient, encrypted);
                        }
                    }
                    String serialized = SerializationUtils.serialize(encryptedMap);
                    client.sendMessage(new ChatMessage(client.getUsername(), null, serialized, ChatMessage.MessageType.ENCRYPTED_TEXT));
                }
            } catch (Exception e) {
                synchronized (System.out) {
                    System.out.println("\r[!] Error: " + e.getMessage());
                    System.out.print("[" + client.getUsername() + "]: ");
                    System.out.flush();
                }
            }
            synchronized (System.out) {
                System.out.print("[" + client.getUsername() + "]: ");
                System.out.flush();
            }
        }
    }
}