package csdev.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Импорты общих классов
import csdev.Protocol;
import csdev.Message;
import csdev.MessageConnect;
import csdev.MessageConnectResult;
import csdev.MessageExecute;
import csdev.MessageExecuteResult;
import csdev.MessageDisconnect;
import csdev.MessageListUsers;
import csdev.MessageListUsersResult;

/**
 * Сервер удалённой консоли
 */
public class Main {

    private static final int MAX_USERS = 50;
    private static final AtomicInteger activeUsers = new AtomicInteger(0);

    // Пулы потоков для выполнения команд
    static final ExecutorService commandExecutor =
            Executors.newCachedThreadPool();

    // Пул для фоновых задач
    static final ExecutorService backgroundExecutor =
            Executors.newFixedThreadPool(10);

    // Карта для отслеживания фоновых задач по пользователю
    private static final Map<String, List<Future<?>>> backgroundTasks =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.err.println("=== Сервер удалённой консоли ===");
        System.err.println("Порт: " + Protocol.PORT);

        try (ServerSocket serv = new ServerSocket(Protocol.PORT)) {
            System.err.println("Сервер удалённой консоли инициализирован на порту " + Protocol.PORT);

            ServerStopThread tester = new ServerStopThread();
            tester.start();

            while (true) {
                Socket sock = accept(serv);
                if (sock != null) {
                    if (activeUsers.get() < MAX_USERS) {
                        System.err.println(sock.getInetAddress().getHostName() + " подключился");
                        ServerThread server = new ServerThread(sock);
                        server.start();
                        activeUsers.incrementAndGet();
                    } else {
                        System.err.println(sock.getInetAddress().getHostName() + " - соединение отклонено (максимум пользователей)");
                        // Отправляем сообщение об ошибке перед закрытием
                        try {
                            ObjectOutputStream tempOs = new ObjectOutputStream(sock.getOutputStream());
                            tempOs.writeObject(new MessageConnectResult("Превышено максимальное количество пользователей"));
                            tempOs.flush();
                        } catch (IOException e) {
                            // Игнорируем ошибку при отправке
                        } finally {
                            sock.close();
                        }
                    }
                }
                if (getStopFlag()) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e);
            e.printStackTrace();
        } finally {
            stopAllUsers();
            shutdownExecutors();
            System.err.println("Сервер остановлен");
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    public static Socket accept(ServerSocket serv) {
        assert(serv != null);
        try {
            serv.setSoTimeout(Protocol.SOCKET_TIMEOUT);
            Socket sock = serv.accept();
            return sock;
        } catch (SocketTimeoutException e) {
            // Таймаут - это нормально
            return null;
        } catch (IOException e) {
            if (!getStopFlag()) {
                System.err.println("Ошибка при accept: " + e.getMessage());
            }
        }
        return null;
    }

    private static void stopAllUsers() {
        String[] users = getUsers();
        for (String user : users) {
            ServerThread ut = getUser(user);
            if (ut != null) {
                ut.disconnect();
            }
        }
    }

    private static void shutdownExecutors() {
        commandExecutor.shutdownNow();
        backgroundExecutor.shutdownNow();

        try {
            if (!commandExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Таймаут завершения commandExecutor");
            }
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Таймаут завершения backgroundExecutor");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Добавление фоновой задачи
    public static void addBackgroundTask(String userNic, Future<?> task) {
        synchronized (backgroundTasks) {
            backgroundTasks.computeIfAbsent(userNic, k -> new ArrayList<>()).add(task);
        }
    }

    // Остановка всех фоновых задач пользователя
    public static void stopBackgroundTasks(String userNic) {
        synchronized (backgroundTasks) {
            List<Future<?>> tasks = backgroundTasks.get(userNic);
            if (tasks != null) {
                for (Future<?> task : tasks) {
                    task.cancel(true);
                }
                backgroundTasks.remove(userNic);
            }
        }
    }

    private static Object syncFlags = new Object();
    private static boolean stopFlag = false;

    public static boolean getStopFlag() {
        synchronized (syncFlags) {
            return stopFlag;
        }
    }

    public static void setStopFlag(boolean value) {
        synchronized (syncFlags) {
            stopFlag = value;
        }
    }

    private static Object syncUsers = new Object();
    private static TreeMap<String, ServerThread> users =
            new TreeMap<String, ServerThread>();

    public static ServerThread getUser(String userNic) {
        synchronized (syncUsers) {
            return users.get(userNic);
        }
    }

    public static ServerThread registerUser(String userNic, ServerThread user) {
        synchronized (syncUsers) {
            ServerThread old = users.get(userNic);
            if (old == null) {
                users.put(userNic, user);
            }
            return old;
        }
    }

    public static ServerThread setUser(String userNic, ServerThread user) {
        synchronized (syncUsers) {
            ServerThread res = users.put(userNic, user);
            if (user == null) {
                users.remove(userNic);
                activeUsers.decrementAndGet();
                stopBackgroundTasks(userNic);
            }
            return res;
        }
    }

    public static String[] getUsers() {
        synchronized (syncUsers) {
            return users.keySet().toArray(new String[0]);
        }
    }

    public static int getNumUsers() {
        synchronized (syncUsers) {
            return users.keySet().size();
        }
    }
}

/**
 * Поток для остановки сервера
 */
class ServerStopThread extends Thread {

    static final String cmd = "q";
    static final String cmdL = "quit";

    Scanner fin;

    public ServerStopThread() {
        fin = new Scanner(System.in);
        Main.setStopFlag(false);
        this.setDaemon(true);
        System.err.println("Введите '" + cmd + "' или '" + cmdL + "' для остановки сервера\n");
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
            if (!fin.hasNextLine())
                continue;
            String str = fin.nextLine();
            if (str.equalsIgnoreCase(cmd) || str.equalsIgnoreCase(cmdL)) {
                break;
            }
        }
        onCmdQuit();
    }

    public boolean onCmdQuit() {
        System.err.print("Остановка сервера...");
        fin.close();
        Main.setStopFlag(true);
        return true;
    }
}

/**
 * Поток обработки клиента
 */
class ServerThread extends Thread {

    private Socket sock;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private InetAddress addr;

    private String userNic = null;
    private String userFullName;

    // Текущая рабочая директория для каждого пользователя
    private String currentDirectory = System.getProperty("user.dir");

    public ServerThread(Socket s) throws IOException {
        sock = s;
        s.setSoTimeout(Protocol.SOCKET_TIMEOUT);
        os = new ObjectOutputStream(s.getOutputStream());
        is = new ObjectInputStream(s.getInputStream());
        addr = s.getInetAddress();
        this.setDaemon(true);
    }

    public void run() {
        try {
            while (true) {
                Message msg = null;
                try {
                    msg = (Message) is.readObject();
                } catch (SocketTimeoutException e) {
                    // Таймаут - продолжаем цикл
                    continue;
                } catch (IOException e) {
                    System.err.println("Ошибка чтения от " + (userNic != null ? userNic : addr.getHostAddress()) + ": " + e.getMessage());
                    break;
                } catch (ClassNotFoundException e) {
                    sendError("Неизвестный тип сообщения");
                    continue;
                }

                if (msg == null) {
                    continue;
                }

                switch (msg.getID()) {
                    case Protocol.CMD_CONNECT:
                        if (!connect((MessageConnect) msg))
                            return;
                        break;

                    case Protocol.CMD_DISCONNECT:
                        disconnect();
                        return;

                    case Protocol.CMD_EXECUTE:
                        executeCommand((MessageExecute) msg);
                        break;

                    case Protocol.CMD_LIST_USERS:
                        listUsers((MessageListUsers) msg);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Разрыв соединения с " + (userNic != null ? userNic : addr.getHostAddress()) + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    boolean connect(MessageConnect msg) throws IOException {
        if (msg.userNic == null || msg.userNic.trim().isEmpty()) {
            os.writeObject(new MessageConnectResult("Имя пользователя не может быть пустым"));
            return false;
        }

        ServerThread old = register(msg.userNic, msg.userFullName);
        if (old == null) {
            // Успешное подключение
            MessageConnectResult result = new MessageConnectResult();
            result.currentDirectory = currentDirectory;
            os.writeObject(result);

            // Отправляем приветственное сообщение
            String welcome = "Добро пожаловать в удалённую консоль!\n" +
                    "Текущая директория: " + currentDirectory + "\n" +
                    "Для справки используйте команду 'help'";
            os.writeObject(new MessageExecuteResult(welcome, 0, 0));
            return true;
        } else {
            os.writeObject(new MessageConnectResult(
                    "Пользователь '" + old.userFullName + "' уже подключен как " + userNic));
            return false;
        }
    }

    void executeCommand(MessageExecute msg) throws IOException {
        if (msg.command == null || msg.command.trim().isEmpty()) {
            sendError("Команда не может быть пустой");
            return;
        }

        String command = msg.command.trim();

        // Обработка специальных команд
        if (command.equalsIgnoreCase("help")) {
            sendHelp();
            return;
        } else if (command.equalsIgnoreCase("pwd")) {
            os.writeObject(new MessageExecuteResult(currentDirectory + "\n", 0, 0));
            return;
        } else if (command.startsWith("cd ")) {
            changeDirectory(command.substring(3).trim());
            return;
        }

        // Проверка безопасности
        if (isDangerousCommand(command)) {
            sendError("Команда запрещена по соображениям безопасности");
            return;
        }

        long startTime = System.currentTimeMillis();

        if (msg.isBackground) {
            // Фоновое выполнение
            executeBackgroundCommand(command);
            os.writeObject(new MessageExecuteResult(
                    "Команда запущена в фоновом режиме\n", 0, 0));
        } else {
            // Синхронное выполнение
            Future<CommandResult> future = Main.commandExecutor.submit(
                    () -> executeShellCommand(command));

            try {
                CommandResult result = future.get(Protocol.COMMAND_TIMEOUT, TimeUnit.MILLISECONDS);
                long executionTime = System.currentTimeMillis() - startTime;

                String output = result.output;
                if (!result.error.isEmpty()) {
                    output += "\nОшибка: " + result.error;
                }
                os.writeObject(new MessageExecuteResult(
                        output, result.exitCode, executionTime));

            } catch (TimeoutException e) {
                future.cancel(true);
                sendError("Превышено время выполнения команды (30 секунд)");
            } catch (Exception e) {
                sendError("Ошибка выполнения: " + e.getMessage());
            }
        }
    }

    private void executeBackgroundCommand(String command) {
        Future<?> future = Main.backgroundExecutor.submit(() -> {
            try {
                executeShellCommand(command);
            } catch (Exception e) {
                System.err.println("Ошибка фоновой задачи для " + userNic + ": " + e.getMessage());
            }
        });

        Main.addBackgroundTask(userNic, future);
    }

    private CommandResult executeShellCommand(String command) {
        ProcessBuilder processBuilder;
        String osName = System.getProperty("os.name").toLowerCase();
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();
        int exitCode = -1;

        try {
            if (osName.contains("win")) {
                // Windows
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                // Unix/Linux/MacOS
                processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            }

            // Устанавливаем рабочую директорию
            processBuilder.directory(new File(currentDirectory));

            // Перенаправляем stderr в stdout
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Чтение вывода
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            exitCode = process.waitFor();

        } catch (IOException | InterruptedException e) {
            error.append("Ошибка выполнения: ").append(e.getMessage());
        }

        return new CommandResult(output.toString(), error.toString(), exitCode);
    }

    private void changeDirectory(String newDir) throws IOException {
        File dir;
        if (newDir.startsWith("/")) {
            // Абсолютный путь
            dir = new File(newDir);
        } else if (newDir.startsWith("~")) {
            // Домашняя директория
            dir = new File(newDir.replaceFirst("^~", System.getProperty("user.home")));
        } else {
            // Относительный путь
            dir = new File(currentDirectory, newDir);
        }

        try {
            dir = dir.getCanonicalFile();
        } catch (IOException e) {
            sendError("Некорректный путь: " + e.getMessage());
            return;
        }

        if (dir.exists() && dir.isDirectory()) {
            currentDirectory = dir.getAbsolutePath();
            os.writeObject(new MessageExecuteResult(
                    "Директория изменена на: " + currentDirectory + "\n", 0, 0));
        } else {
            sendError("Директория не существует: " + newDir);
        }
    }

    private void sendHelp() throws IOException {
        String help = "Доступные команды:\n" +
                "  help - эта справка\n" +
                "  pwd  - показать текущую директорию\n" +
                "  cd [dir] - сменить директорию\n" +
                "  ls, whoami, date, uptime - системные команды\n" +
                "  [command] & - выполнить команду в фоновом режиме\n" +
                "  exit - отключиться от сервера\n" +
                "\nЗапрещённые команды: rm -rf, shutdown, и другие опасные операции";
        os.writeObject(new MessageExecuteResult(help, 0, 0));
    }

    private void listUsers(MessageListUsers msg) throws IOException {
        String[] users = Main.getUsers();
        if (users != null && users.length > 0) {
            os.writeObject(new MessageListUsersResult(users));
        } else {
            os.writeObject(new MessageListUsersResult("Нет подключенных пользователей"));
        }
    }

    private boolean isDangerousCommand(String command) {
        String lowerCommand = command.toLowerCase();

        // Проверка явно опасных команд
        String[] dangerousPatterns = {
                "rm -rf /", "rm -rf /*", "mkfs", "dd if=/dev/random",
                "shutdown", "halt", "reboot", "poweroff",
                "chmod 777 /", "chown root /", "> /dev/sda", "sudo "
        };

        for (String pattern : dangerousPatterns) {
            if (lowerCommand.contains(pattern)) {
                return true;
            }
        }

        // Проверка на fork bomb
        if (lowerCommand.matches(".*:()\\s*\\{\\s*:\\|:&\\s*\\};:.*")) {
            return true;
        }

        return false;
    }

    private void sendError(String message) throws IOException {
        os.writeObject(new MessageExecuteResult(message));
    }

    private ServerThread register(String nic, String name) {
        ServerThread old = Main.registerUser(nic, this);
        if (old == null) {
            if (userNic == null) {
                userNic = nic;
                userFullName = name;
                System.err.println("Пользователь '" + name + "' зарегистрирован как '" + nic + "'");
            }
        }
        return old;
    }

    private boolean disconnected = false;

    public void disconnect() {
        if (!disconnected) {
            try {
                System.err.println(addr.getHostName() + " (" + userNic + ") отключился");
                unregister();
                if (os != null) os.close();
                if (is != null) is.close();
                if (sock != null && !sock.isClosed()) sock.close();
            } catch (IOException e) {
                System.err.println("Ошибка при отключении: " + e.getMessage());
            } finally {
                this.interrupt();
                disconnected = true;
            }
        }
    }

    private void unregister() {
        if (userNic != null) {
            Main.setUser(userNic, null);
            userNic = null;
        }
    }
}

/**
 * Результат выполнения команды
 */
class CommandResult {
    String output;
    String error;
    int exitCode;

    CommandResult(String output, String error, int exitCode) {
        this.output = output;
        this.error = error;
        this.exitCode = exitCode;
    }
}