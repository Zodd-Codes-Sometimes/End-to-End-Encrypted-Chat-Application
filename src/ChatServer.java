import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    private static final int PORT = 9001;


    private static final ConcurrentHashMap<String, ClientInfo> clients = new ConcurrentHashMap<>();

    // Helper class to keep track of each client's output stream and public key.
    private static class ClientInfo {
        ObjectOutputStream out; // Used to send messages to this client.
        PublicKey publicKey;    // The client's public key for encryption.

        ClientInfo(ObjectOutputStream out, PublicKey publicKey) {
            this.out = out;
            this.publicKey = publicKey;
        }
    }

    // Main method: starts the server and listens for incoming client connections.
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[Server] ChatServer started on port " + PORT);

        // Continuously accept new client connections.
        while (true) {
            Socket socket = serverSocket.accept();
            // Each client is handled in a separate thread.
            new Thread(new ClientHandler(socket)).start();
        }
    }

    // Handles communication with a single client.
    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String username;

        // Constructor: stores the client's socket.
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        // Main logic for handling a connected client.
        public void run() {
            try {
                // Set up streams for sending/receiving objects.
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Notify client that connection is successful.
                out.writeObject(new ChatMessage("Server", null, "[*] Connection acknowledged by ChatServer", ChatMessage.MessageType.TEXT));

                // Handle username registration: loop until a unique username is provided.
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

                // Receive and register the client's public key.
                ChatMessage keyMsg = (ChatMessage) in.readObject();
                if (keyMsg.getType() != ChatMessage.MessageType.PUBLIC_KEY) {
                    throw new IOException("Expected public key");
                }
                clients.put(username, new ClientInfo(out, keyMsg.getPublicKey()));

                // Announce to all clients that a new user has joined.
                broadcast(new ChatMessage("Server", null, "[*] User " + username + " has joined the chat.", ChatMessage.MessageType.TEXT), null);

                // Main loop: handle messages from this client.
                Object inputObj;
                while ((inputObj = in.readObject()) != null) {
                    ChatMessage msg = (ChatMessage) inputObj;

                    // Handle client disconnect command.
                    if (msg.getMessage() != null && msg.getMessage().equalsIgnoreCase("/disconnect")) {
                        out.writeObject(new ChatMessage("Server", null, "[*] Disconnected from the ChatServer successfully", ChatMessage.MessageType.TEXT));
                        break;
                    }
                    // Handle requests for another user's public key.
                    else if (msg.getType() == ChatMessage.MessageType.KEY_REQUEST) {
                        String requestedUser = msg.getRecipient();
                        ClientInfo info = clients.get(requestedUser);
                        if (info != null) {
                            out.writeObject(new ChatMessage(requestedUser, info.publicKey));
                        } else {
                            out.writeObject(new ChatMessage("Server", null, "[!] User not found: " + requestedUser, ChatMessage.MessageType.TEXT));
                        }
                    }
                    // Forward encrypted messages to the intended recipient.
                    else if (msg.getType() == ChatMessage.MessageType.ENCRYPTED_TEXT) {
                        ClientInfo recipientInfo = clients.get(msg.getRecipient());
                        if (recipientInfo != null) {
                            recipientInfo.out.writeObject(msg);
                        }
                    }
                    // Broadcast all other messages to all clients (except the sender).
                    else {
                        broadcast(msg, msg.getSender());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Server] Client error: " + username + ": " + e.getMessage());
            } finally {
                try {
                    // Remove the client from the list and notify others if they disconnect.
                    if (username != null) {
                        clients.remove(username);
                        broadcast(new ChatMessage("Server", null, "[*] User " + username + " has left the chat.", ChatMessage.MessageType.TEXT));
                    }
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        // Sends a message to all connected clients except the one specified by excludeUser.
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
