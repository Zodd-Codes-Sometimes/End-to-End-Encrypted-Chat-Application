import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 9001;
    private static final ConcurrentHashMap<String, ObjectOutputStream> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[Server] ChatServer started on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
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

                out.writeObject("[*] Connection acknowledged by ChatServer");

                while (true) {
                    String requestedUsername = ((String) in.readObject()).replaceAll("\\s+", "-");
                    if (!clients.containsKey(requestedUsername)) {
                        username = requestedUsername;
                        clients.put(username, out);
                        out.writeObject("[*] Username accepted: " + username);
                        broadcast("[*] User " + username + " has joined the chat.", null);
                        break;
                    } else {
                        out.writeObject("[!] Username taken. Try again or use: " + requestedUsername + new Random().nextInt(100));
                    }
                }

                Object inputObj;
                while ((inputObj = in.readObject()) != null) {
                    ChatMessage msg = (ChatMessage) inputObj;
                    if (msg.getMessage().equalsIgnoreCase("/disconnect")) {
                        out.writeObject("[*] Disconnected from the ChatServer successfully");
                        break;
                    }
                    broadcast("[" + msg.getSender() + "]: " + msg.getMessage(), msg.getSender());
                }
            } catch (Exception e) {
                System.out.println("[Server] Client error: " + e.getMessage());
            } finally {
                try {
                    if (username != null) {
                        clients.remove(username);
                        broadcast("[*] User " + username + " has been disconnected from the Chat Room", null);
                    }
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void broadcast(String message, String excludeUser) {
            for (Map.Entry<String, ObjectOutputStream> entry : clients.entrySet()) {
                if (!entry.getKey().equals(excludeUser)) {
                    try {
                        entry.getValue().writeObject(new ChatMessage("Server", message));
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}
