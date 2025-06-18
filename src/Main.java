package src;

import java.util.Scanner;
import java.security.PublicKey;
import java.io.IOException;
import utils.RSAUtils;

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
            client.start(); // Connect and set username
            System.out.print("[" + client.getUsername() + "]: "); // Initial prompt after start
            new Thread(this::receiveMessages).start();
            handleConsoleInput();
        } catch (Exception e) {
            System.out.println("[!] Error: " + e.getMessage());
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
                if (msg.getType() == ChatMessage.MessageType.ENCRYPTED_TEXT) {
                    String decrypted = RSAUtils.decrypt(msg.getMessage(), client.getPrivateKey());
                    System.out.println("\r[" + msg.getSender() + "]: " + decrypted);
                    System.out.print("[" + client.getUsername() + "]: ");
                } else if (msg.getType() == ChatMessage.MessageType.TEXT) {
                    System.out.println("\r" + msg.getMessage());
                    System.out.print("[" + client.getUsername() + "]: ");
                }
            }
        } catch (Exception e) {
            System.out.println("\r[!] Connection lost: " + e.getMessage());
        }
    }

    private void handleConsoleInput() {
        while (true) {
            String input = scanner.nextLine();
            try {
                if (input.startsWith("/send")) {
                    handleSendCommand(input);
                } else if (input.equalsIgnoreCase("/exit")) {
                    client.sendMessage(new ChatMessage(client.getUsername(), null, "/disconnect", ChatMessage.MessageType.TEXT));
                    break;
                } else {
                    System.out.println("[!] Unknown command. Use /send <recipient> <message> or /exit");
                }
            } catch (Exception e) {
                System.out.println("[!] Error: " + e.getMessage());
            }
            System.out.print("[" + client.getUsername() + "]: ");
        }
    }

    private void handleSendCommand(String input) throws Exception {
        String[] parts = input.split("\\s+", 3);
        if (parts.length < 3) {
            System.out.println("[!] Usage: /send <recipient> <message>");
            return;
        }
        String recipient = parts[1];
        String message = parts[2];

        PublicKey publicKey = client.requestPublicKey(recipient);
        if (publicKey != null) {
            String encrypted = RSAUtils.encrypt(message, publicKey);
            client.sendMessage(new ChatMessage(client.getUsername(), recipient, encrypted, ChatMessage.MessageType.ENCRYPTED_TEXT));
            System.out.println("[+] Message sent to " + recipient);
        } else {
            System.out.println("[!] Failed to send message: User " + recipient + " not found or public key unavailable");
        }
    }
}