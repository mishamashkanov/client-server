package csdev;

import java.io.Serializable;

/**
 * Базовый класс для всех сообщений
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    protected int id;

    public Message(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }
}