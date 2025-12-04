package csdev;

/**
 * Запрос списка пользователей
 */
public class MessageListUsers extends Message {
    private static final long serialVersionUID = 1L;

    public MessageListUsers() {
        super(Protocol.CMD_LIST_USERS);
    }
}
