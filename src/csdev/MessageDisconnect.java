package csdev;

/**
 * Отключение от сервера
 */
public class MessageDisconnect extends Message {
    private static final long serialVersionUID = 1L;

    public MessageDisconnect() {
        super(Protocol.CMD_DISCONNECT);
    }
}
