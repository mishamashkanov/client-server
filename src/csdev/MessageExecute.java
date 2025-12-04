package csdev;

/**
 * Выполнение команды
 */
public class MessageExecute extends Message {
    private static final long serialVersionUID = 1L;

    public String command;
    public boolean isBackground; // Фоновая задача

    public MessageExecute(String command, boolean isBackground) {
        super(Protocol.CMD_EXECUTE);
        this.command = command;
        this.isBackground = isBackground;
    }

    public MessageExecute(String command) {
        this(command, false);
    }
}
