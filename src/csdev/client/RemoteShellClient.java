package csdev.client;

import csdev.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import csdev.*;

public class RemoteShellClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Scanner consoleScanner;
    private boolean connected;
    private String username;
    
    public RemoteShellClient(String host, int port, String username, String fullName) 
            throws IOException, ClassNotFoundException {
        this.username = username;
        this.consoleScanner = new Scanner(System.in);
        
        System.out.println("Connecting to " + host + ":" + port + "...");
        this.socket = new Socket(host, port);
        this.socket.setSoTimeout(Protocol.SOCKET_TIMEOUT);
        
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        
        // Подключаемся к серверу
        MessageConnect connectMsg = new MessageConnect(username, fullName);
        out.writeObject(connectMsg);
        out.flush();
        
        // Получаем ответ
        MessageConnectResult connectResult = (MessageConnectResult) in.readObject();
        if (connectResult.getID() == Protocol.RESULT_ERROR) {
            throw new IOException("Connection rejected: " + connectResult.errorMessage);
        }
        
        this.connected = true;
        System.out.println("Connected as: " + username + " (" + fullName + ")");
        
        // Получаем приветственное сообщение
        readServerResponse();
    }
    
    public void start() {
        printWelcomeMessage();
        
        try {
            while (connected && consoleScanner.hasNextLine()) {
                System.out.print(Protocol.PROMPT);
                String input = consoleScanner.nextLine();
                processCommand(input);
            }
        } finally {
            disconnect();
        }
    }
    
    private void processCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        
        String command = input.trim();
        String lowerCommand = command.toLowerCase();
        
        try {
            if (lowerCommand.equals("exit") || lowerCommand.equals("quit")) {
                sendDisconnect();
                return;
            } else if (lowerCommand.equals("help") || lowerCommand.equals("?")) {
                sendHelpCommand();
                return;
            } else if (lowerCommand.equals("clear") || lowerCommand.equals("cls")) {
                clearScreen();
                return;
            } else if (lowerCommand.equals("list") || lowerCommand.equals("users")) {
                sendListUsers();
                return;
            } else if (lowerCommand.startsWith("cd ")) {
                String path = command.substring(3).trim();
                if (path.isEmpty()) {
                    System.out.println("Usage: cd <directory>");
                } else {
                    sendExecuteCommand("cd " + path);
                }
                return;
            } else if (lowerCommand.equals("pwd")) {
                sendExecuteCommand("pwd");
                return;
            } else if (lowerCommand.startsWith("ls")) {
                String args = command.length() > 2 ? command.substring(2).trim() : "";
                sendExecuteCommand("ls " + args);
                return;
            } else {
                // Обычная команда
                sendExecuteCommand(command);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            connected = false;
        }
    }
    
    private void sendExecuteCommand(String command) throws IOException, ClassNotFoundException {
        MessageExecute msg = new MessageExecute(command);
        out.writeObject(msg);
        out.flush();
        readServerResponse();
    }
    
    private void sendListUsers() throws IOException, ClassNotFoundException {
        MessageListUsers msg = new MessageListUsers();
        out.writeObject(msg);
        out.flush();
        readServerResponse();
    }
    
    private void sendHelpCommand() throws IOException, ClassNotFoundException {
        sendExecuteCommand("help");
    }
    
    private void sendDisconnect() throws IOException {
        MessageDisconnect msg = new MessageDisconnect();
        out.writeObject(msg);
        out.flush();
        connected = false;
        System.out.println("Disconnecting...");
    }
    
    private void readServerResponse() throws IOException, ClassNotFoundException {
        try {
            Object response = in.readObject();
            
            if (response instanceof MessageExecuteResult) {
                MessageExecuteResult result = (MessageExecuteResult) response;
                if (result.result != null && !result.result.isEmpty()) {
                    System.out.print(result.result);
                    if (!result.result.endsWith("\n")) {
                        System.out.println();
                    }
                }
            } else if (response instanceof MessageListUsersResult) {
                MessageListUsersResult result = (MessageListUsersResult) response;
                System.out.println(result.toString());
            } else if (response instanceof MessageConnectResult) {
                // Игнорируем, так как уже обработали при подключении
            }
            
        } catch (SocketTimeoutException e) {
            // Таймаут - игнорируем
        }
    }
    
    private void printWelcomeMessage() {
        System.out.println("\n=== Remote Shell Client ===");
        System.out.println("Connected as: " + username);
        System.out.println("\nAvailable commands:");
        System.out.println("  <command>      - Execute command on server");
        System.out.println("  cd <dir>       - Change directory");
        System.out.println("  pwd            - Show current directory");
        System.out.println("  ls [args]      - List files");
        System.out.println("  list/users     - List connected users");
        System.out.println("  help           - Show help");
        System.out.println("  clear          - Clear screen");
        System.out.println("  exit           - Disconnect");
        System.out.println("\nType commands below:\n");
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
            // Fallback
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
    
    private void disconnect() {
        if (connected) {
            try {
                sendDisconnect();
            } catch (IOException e) {
                // Игнорируем ошибки при отключении
            }
        }
        
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        
        if (consoleScanner != null) {
            consoleScanner.close();
        }
        
        System.out.println("Client stopped.");
    }
}
