package src;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;

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

                while (true) {
                    String requestedUsername = ((String) in.readObject()).replaceAll("\\s+", "-");
                    if (!clients.containsKey(requestedUsername)) {
                        username = requestedUsername;
                        out.writeObject(new ChatMessage("Server", null, "[*] Username accepted: " + username, ChatMessage.MessageType.TEXT));
                        break;
                    } else {
                        out.writeObject(new ChatMessage("Server", null, "[!] Username taken. Try again or use: " + requestedUsername + new Random().nextInt(100), ChatMessage.MessageType.TEXT));
                    }
                }

                ChatMessage keyMsg = (ChatMessage) in.readObject();
                if (keyMsg.getType() != ChatMessage.MessageType.PUBLIC_KEY) {
                    throw new IOException("Expected public key");
                }
                clients.put(username, new ClientInfo(out, keyMsg.getPublicKey()));
                broadcast(new ChatMessage("Server", null, "[*] User " + username + " has joined the chat.", ChatMessage.MessageType.TEXT), null);

                Object inputObj;
                while ((inputObj = in.readObject()) != null) {
                    ChatMessage msg = (ChatMessage) inputObj;
                    if (msg.getMessage() != null && msg.getMessage().equalsIgnoreCase("/disconnect")) {
                        out.writeObject(new ChatMessage("Server", null, "[*] Disconnected from the ChatServer successfully", ChatMessage.MessageType.TEXT));
                        break;
                    } else if (msg.getType() == ChatMessage.MessageType.KEY_REQUEST) {
                        String requestedUser = msg.getRecipient();
                        ClientInfo info = clients.get(requestedUser);
                        if (info != null) {
                            out.writeObject(new ChatMessage(requestedUser, info.publicKey));
                        } else {
                            out.writeObject(new ChatMessage("Server", null, "[!] User not found: " + requestedUser, ChatMessage.MessageType.TEXT));
                        }
                    } else if (msg.getType() == ChatMessage.MessageType.ENCRYPTED_TEXT) {
                        ClientInfo recipientInfo = clients.get(msg.getRecipient());
                        if (recipientInfo != null) {
                            recipientInfo.out.writeObject(msg);
                        }
                    } else {
                        broadcast(msg, msg.getSender());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Server] Client error: " + (username != null ? username : "unknown") + ": " + e.getMessage());
            } finally {
                try {
                    if (username != null) {
                        clients.remove(username);
                        broadcast(new ChatMessage("Server", null, "[*] User " + username + " has left.", ChatMessage.MessageType.TEXT), null);
                    }
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void broadcast(ChatMessage message, String excludeUser) {
            for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
                if (!entry.getKey().equals(excludeUser)) {
                    try {
                        entry.getValue().out.writeObject(message);
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}