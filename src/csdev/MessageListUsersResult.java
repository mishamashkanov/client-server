package csdev;

import java.util.Arrays;

public class MessageListUsersResult extends Message {
    private static final long serialVersionUID = 1L;
    
    public String[] users;
    public String errorMessage;
    
    public MessageListUsersResult(String[] users) {
        super(Protocol.RESULT_OK);
        this.users = users;
    }
    
    public MessageListUsersResult(String errorMessage) {
        super(Protocol.RESULT_ERROR);
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        if (users != null) {
            return "Connected users: " + Arrays.toString(users);
        } else {
            return errorMessage != null ? errorMessage : "No users";
        }
    }
}
