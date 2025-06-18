package src;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;
import utils.SerializationUtils;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class ChatServer {
    private static final int DEFAULT_PORT = 9001;
    private static final ConcurrentHashMap<String, ClientInfo> clients = new ConcurrentHashMap<>();

    private static class ClientInfo {
        ObjectOutputStream out;
        PublicKey publicKey;

        ClientInfo(ObjectOutputStream out, PublicKey publicKey) {
            this.out = out;
            this.publicKey = publicKey;
        }
    }

    private final ServerSocket serverSocket;

    public ChatServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void start() throws IOException {
        System.out.println("[Server] ChatServer started on port " + serverSocket.getLocalPort());
        while (!serverSocket.isClosed()) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
        new ChatServer(serverSocket).start();
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                out.writeObject(new ChatMessage("Server", null, "[*] Connection acknowledged by ChatServer", ChatMessage.MessageType.TEXT));
                out.flush();

                while (true) {
                    String requestedUsername = ((String) in.readObject()).replaceAll("\\s+", "-");
                    if (requestedUsername == null || requestedUsername.trim().isEmpty()) {
                        out.writeObject(new ChatMessage("Server", null, "[!] Username cannot be empty. Try again.", ChatMessage.MessageType.TEXT));
                        out.flush();
                        continue;
                    }
                    if (!clients.containsKey(requestedUsername)) {
                        username = requestedUsername;
                        out.writeObject(new ChatMessage("Server", null, "[*] Username accepted: " + username, ChatMessage.MessageType.TEXT));
                        out.flush();
                        break;
                    } else {
                        out.writeObject(new ChatMessage("Server", null, "[!] Username taken. Try again or use: " + requestedUsername + new Random().nextInt(100), ChatMessage.MessageType.TEXT));
                        out.flush();
                    }
                }

                ChatMessage keyMsg = (ChatMessage) in.readObject();
                if (keyMsg.getType() != ChatMessage.MessageType.PUBLIC_KEY) {
                    throw new IOException("Expected public key");
                }
                clients.put(username, new ClientInfo(out, keyMsg.getPublicKey()));

                // Convert public keys to Base64-encoded strings
                Map<String, String> publicKeyStrings = new HashMap<>();
                for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
                    String keyEncoded = Base64.getEncoder().encodeToString(entry.getValue().publicKey.getEncoded());
                    publicKeyStrings.put(entry.getKey(), keyEncoded);
                }
                String serializedKeys = SerializationUtils.serialize(publicKeyStrings);
                out.writeObject(new ChatMessage("Server", null, serializedKeys, ChatMessage.MessageType.PUBLIC_KEY_MAP));
                out.flush();

                // Broadcast updated public key map to all clients
                broadcast(new ChatMessage("Server", null, serializedKeys, ChatMessage.MessageType.PUBLIC_KEY_MAP), username);
                broadcast(new ChatMessage("Server", null, "[*] User " + username + " has joined the chat.", ChatMessage.MessageType.TEXT), null);

                Object inputObj;
                while ((inputObj = in.readObject()) != null) {
                    ChatMessage msg = (ChatMessage) inputObj;
                    if (msg.getMessage() != null && msg.getMessage().equalsIgnoreCase("/disconnect")) {
                        out.writeObject(new ChatMessage("Server", null, "[*] Disconnected from the ChatServer successfully", ChatMessage.MessageType.TEXT));
                        out.flush();
                        break;
                    } else {
                        broadcast(msg, null);
                    }
                }
            } catch (IOException e) {
                System.err.println("[Server] Client error: " + (username != null ? username : "unknown") + ": " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.err.println("[Server] Deserialization error for " + (username != null ? username : "unknown") + ": " + e.getMessage());
            } finally {
                try {
                    if (username != null) {
                        clients.remove(username);
                        // Update public key map for remaining clients
                        Map<String, String> publicKeyStrings = new HashMap<>();
                        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
                            String keyEncoded = Base64.getEncoder().encodeToString(entry.getValue().publicKey.getEncoded());
                            publicKeyStrings.put(entry.getKey(), keyEncoded);
                        }
                        String serializedKeys = SerializationUtils.serialize(publicKeyStrings);
                        broadcast(new ChatMessage("Server", null, serializedKeys, ChatMessage.MessageType.PUBLIC_KEY_MAP), null);
                        broadcast(new ChatMessage("Server", null, "[*] User " + username + " has left.", ChatMessage.MessageType.TEXT), null);
                    }
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void broadcast(ChatMessage message, String excludeUser) {
            List<String> failedClients = new ArrayList<>();
            synchronized (clients) {
                for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
                    if (excludeUser == null || !entry.getKey().equals(excludeUser)) {
                        try {
                            synchronized (entry.getValue().out) {
                                entry.getValue().out.writeObject(message);
                                entry.getValue().out.flush();
                            }
                        } catch (IOException e) {
                            System.err.println("[Server] Failed to send to " + entry.getKey() + ": " + e.getMessage());
                            failedClients.add(entry.getKey());
                        }
                    }
                }
            }
            for (String client : failedClients) {
                clients.remove(client);
                try {
                    Map<String, String> publicKeyStrings = new HashMap<>();
                    for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
                        String keyEncoded = Base64.getEncoder().encodeToString(entry.getValue().publicKey.getEncoded());
                        publicKeyStrings.put(entry.getKey(), keyEncoded);
                    }
                    String serializedKeys = SerializationUtils.serialize(publicKeyStrings);
                    broadcast(new ChatMessage("Server", null, serializedKeys, ChatMessage.MessageType.PUBLIC_KEY_MAP), null);
                    broadcast(new ChatMessage("Server", null, "[*] User " + client + " has disconnected unexpectedly.", ChatMessage.MessageType.TEXT), null);
                } catch (IOException ignored) {}
            }
        }
    }
}