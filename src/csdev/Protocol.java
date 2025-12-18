package csdev;

public class Protocol {
    public static final int PORT = 8888;

    public static final int CMD_CONNECT = 1;
    public static final int CMD_DISCONNECT = 2;
    public static final int CMD_EXECUTE = 3;
    public static final int CMD_LIST_USERS = 4;
    
    public static final int RESULT_OK = 0;
    public static final int RESULT_ERROR = 1;
    
    public static final int SOCKET_TIMEOUT = 1000;
    public static final int COMMAND_TIMEOUT = 30000;

    public static final String ENCODING = "UTF-8";
    public static final String PROMPT = "remote-shell> ";
    
    public static final String SPECIAL_CD = "cd ";
    public static final String SPECIAL_PWD = "pwd";
    public static final String SPECIAL_LS = "ls";
    public static final String SPECIAL_CLEAR = "clear";
    public static final int MAX_COMMAND_LENGTH = 4096;
    public static final int MAX_RESPONSE_LENGTH = 65536;
}
