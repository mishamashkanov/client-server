package csdev.client;

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
                System.err.println("Invalid port, using default");
            }
        }
        
        try {
            RemoteShellClient client = new RemoteShellClient(host, port);
            client.start();
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            System.err.println("Check if server is running at " + host + ":" + port);
        } finally {
            scanner.close();
        }
    }
}
