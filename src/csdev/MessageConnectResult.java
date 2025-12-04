package csdev;

/**
 * Результат подключения
 */
public class MessageConnectResult extends Message {
    private static final long serialVersionUID = 1L;

    public boolean success;
    public String errorMessage;
    public String currentDirectory;

    public MessageConnectResult() {
        super(Protocol.RESULT_OK);
        this.success = true;
        this.errorMessage = "";
        this.currentDirectory = System.getProperty("user.dir");
    }

    public MessageConnectResult(String error) {
        super(Protocol.RESULT_ERROR);
        this.success = false;
        this.errorMessage = error;
        this.currentDirectory = "";
    }
}
