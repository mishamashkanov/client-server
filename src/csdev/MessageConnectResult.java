package csdev;

public class MessageConnectResult extends Message {
    private static final long serialVersionUID = 1L;
    
    public String errorMessage;
    public String currentDirectory;
    
    public MessageConnectResult() {
        super(Protocol.RESULT_OK);
        this.errorMessage = null;
    }
    
    public MessageConnectResult(String errorMessage) {
        super(Protocol.RESULT_ERROR);
        this.errorMessage = errorMessage;
    }
}
