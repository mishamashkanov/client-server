package csdev.client;

import csdev.Protocol;
import java.util.Scanner;
import csdev.*;

public class ClientLauncher {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Remote Shell Client ===");
        
        System.out.print("Server address [localhost]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "localhost";
        }
        
        System.out.print("Server port [" + Protocol.PORT + "]: ");
        String portInput = scanner.nextLine().trim();
        int port = Protocol.PORT;
        if (!portInput.isEmpty()) {
            try {
                port = Integer.parseInt(portInput);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using default: " + Protocol.PORT);
            }
        }
        
        System.out.print("Enter your username [guest]: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "guest";
        }
        
        System.out.print("Enter your full name [Guest User]: ");
        String fullName = scanner.nextLine().trim();
        if (fullName.isEmpty()) {
            fullName = "Guest User";
        }
        
        try {
            RemoteShellClient client = new RemoteShellClient(host, port, username, fullName);
            client.start();
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            System.err.println("Check if server is running at " + host + ":" + port);
        } finally {
            scanner.close();
        }
    }
}
