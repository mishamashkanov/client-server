package csdev;

/**
 * Результат запроса списка пользователей
 */
public class MessageListUsersResult extends Message {
    private static final long serialVersionUID = 1L;

    public String[] users;
    public String errorMessage;

    public MessageListUsersResult(String[] users) {
        super(Protocol.RESULT_OK);
        this.users = users;
        this.errorMessage = "";
    }

    public MessageListUsersResult(String error) {
        super(Protocol.RESULT_ERROR);
        this.users = new String[0];
        this.errorMessage = error;
    }
}
