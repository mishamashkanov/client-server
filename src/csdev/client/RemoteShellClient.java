package csdev.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import csdev.*;

public class RemoteShellClient {
    private Socket socket;
    private CommandWriter commandWriter;
    private ResponseReader responseReader;
    private Scanner consoleScanner;
    private boolean running;
    
    public RemoteShellClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.socket.setSoTimeout(Protocol.SOCKET_TIMEOUT);
        this.commandWriter = new CommandWriter(socket);
        this.responseReader = new ResponseReader(socket, this::handleResponse);
        this.consoleScanner = new Scanner(System.in);
        this.running = true;
    }
    
    public void start() {
        printWelcomeMessage();
        
        try {
            while (running && commandWriter.isConnected() && 
                   responseReader.isActive() && consoleScanner.hasNextLine()) {
                
                System.out.print(Protocol.PROMPT);
                String input = consoleScanner.nextLine();
                processCommand(input);
            }
        } finally {
            cleanup();
        }
    }
    
    private void processCommand(String input) {
        CommandParser.ParsedCommand parsed = CommandParser.parse(input);
        
        try {
            switch (parsed.getType()) {
                case EXIT:
                    handleExit();
                    break;
                case CLIENT_CLEAR:
                    clearScreen();
                    break;
                case HELP:
                    printHelp();
                    break;
                case CHANGE_DIR:
                    if (parsed.getArgument().isEmpty()) {
                        System.out.println("Usage: cd <directory>");
                    } else {
                        commandWriter.sendChangeDirectory(parsed.getArgument());
                    }
                    break;
                case PRINT_DIR:
                    commandWriter.sendPwdCommand();
                    break;
                case LIST_FILES:
                    commandWriter.sendLsCommand(parsed.getArgument());
                    break;
                case LIST_USERS:
                    commandWriter.sendListUsers();
                    break;
                case EXECUTE:
                    if (!parsed.getArgument().isEmpty()) {
                        commandWriter.sendCommand(parsed.getArgument());
                    }
                    break;
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            if (e.getMessage().contains("Not connected")) {
                running = false;
            }
        }
    }
    
    private void handleResponse(String response) {
        if (response != null && !response.isEmpty()) {
            System.out.println(response);
        }
    }
    
    private void printWelcomeMessage() {
        System.out.println("REMOTE SHELL CLIENT");
        System.out.println("Special commands enabled:");
        System.out.println("  cd <dir>    - Change directory");
        System.out.println("  pwd         - Show current directory");
        System.out.println("  ls [args]   - List files");
        System.out.println("  clear       - Clear screen");
        System.out.println("  help        - Show help");
        System.out.println("  exit        - Disconnect");
        System.out.println("\nConnected to server. Type commands below:\n");
    }
    
    private void printHelp() {
        System.out.println("\nAVAILABLE COMMANDS:");
        System.out.println("BASIC COMMANDS:");
        System.out.println("  <command>       - Execute on remote server");
        System.out.println("  help            - Show this help");
        System.out.println("  exit            - Disconnect from server");
        System.out.println();
        System.out.println("FILE SYSTEM COMMANDS:");
        System.out.println("  cd <directory>  - Change working directory");
        System.out.println("  pwd             - Print working directory");
        System.out.println("  ls [options]    - List directory contents");
        System.out.println();
        System.out.println("CLIENT COMMANDS:");
        System.out.println("  clear           - Clear terminal screen");
        System.out.println("  list/users      - List connected users");
        System.out.println();
    }
    
    private void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    private void handleExit() {
        try {
            commandWriter.sendExitCommand();
            System.out.println("Disconnecting...");
        } catch (IOException e) {
            System.err.println("Error during disconnect");
        }
        running = false;
    }
    
    private void cleanup() {
        System.out.println("Cleaning up...");
        commandWriter.close();
        responseReader.stop();
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket");
        }
        if (consoleScanner != null) {
            consoleScanner.close();
        }
        System.out.println("Client stopped.");
    }
}
