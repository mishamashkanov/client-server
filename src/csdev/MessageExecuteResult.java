package csdev;

public class MessageExecuteResult extends Message {
    private static final long serialVersionUID = 1L;
    
    public String result;
    public int exitCode;
    public long executionTime;
    
    public MessageExecuteResult() {
        super(Protocol.RESULT_OK);
    }
    
    public MessageExecuteResult(String result) {
        this();
        this.result = result;
    }
    
    public MessageExecuteResult(String result, int exitCode, long executionTime) {
        this();
        this.result = result;
        this.exitCode = exitCode;
        this.executionTime = executionTime;
    }
}
