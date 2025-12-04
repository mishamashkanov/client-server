package csdev;

public class MessageExecute extends Message {
    private static final long serialVersionUID = 1L;
    
    public String command;
    public boolean isBackground;
    
    public MessageExecute(String command) {
        super(Protocol.CMD_EXECUTE);
        this.command = command;
        this.isBackground = command.trim().endsWith("&");
    }
}
