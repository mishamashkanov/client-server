package csdev;

/**
 * Результат выполнения команды
 */
public class MessageExecuteResult extends Message {
    private static final long serialVersionUID = 1L;

    public boolean success;
    public String output;
    public String error;
    public int exitCode;
    public long executionTime; // Время выполнения в мс

    public MessageExecuteResult(String output, int exitCode, long executionTime) {
        super(Protocol.RESULT_OK);
        this.success = true;
        this.output = output;
        this.error = "";
        this.exitCode = exitCode;
        this.executionTime = executionTime;
    }

    public MessageExecuteResult(String error) {
        super(Protocol.RESULT_ERROR);
        this.success = false;
        this.output = "";
        this.error = error;
        this.exitCode = -1;
        this.executionTime = 0;
    }
}
