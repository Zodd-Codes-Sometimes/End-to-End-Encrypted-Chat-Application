import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    public static void main(String[] args) {
        new ChatClient().start();
    }
    public void start() {
        Scanner scanner = new Scanner(System.in);
        int attempts = 0;
        while (attempts < 3) {
            try {
                System.out.print("Enter Server IP: ");
                String ip = scanner.nextLine();
                socket = new Socket(ip, 9001);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                System.out.println(in.readObject()); // Server acknowledgment

                while (true) {
                    System.out.print("Enter your username: ");
                    username = scanner.nextLine().replaceAll("\\s+", "-");
                    out.writeObject(username);
                    String response = (String) in.readObject();
                    System.out.println(response);
                    if (response.startsWith("[*] Username registered successfully")) break;
                }

                new Thread(this::receiveMessages).start();
                sendMessages(scanner);
                break;
            } catch (Exception e) {
                System.out.println("[!] Connection failed. Try again.");
                attempts++;
            }
        }
        if (attempts == 3) System.out.println("[!] Max attempts reached. Exiting.");
    }
    private void receiveMessages() {
        try {
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof ChatMessage msg) {
                    // Print on new line to avoid overwriting the prompt
                    System.out.print("\r" + msg.getMessage() + "\n" + "[" + username + "]" + ": ");
                }
            }
        } catch (Exception e) {
            System.out.println("[!] Server closed the connection.");
        }
    }
    private void sendMessages(Scanner scanner) {
        try {
            while (true) {
                System.out.print("[" + username + "]" + ": ");
                String msg = scanner.nextLine();
                out.writeObject(new ChatMessage(username, msg));
                out.flush();
                if (msg.equalsIgnoreCase("/disconnect")) break;
            }
        } catch (IOException ignored) {
        }
    }
}
