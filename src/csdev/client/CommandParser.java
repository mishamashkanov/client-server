package csdev.client;

import csdev.*;

public class CommandParser {
    
    public static ParsedCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ParsedCommand(CommandType.EMPTY, "", "");
        }
        
        String trimmed = input.trim();
        String lowerTrimmed = trimmed.toLowerCase();
        
        if (lowerTrimmed.equals("exit") || lowerTrimmed.equals("quit")) {
            return new ParsedCommand(CommandType.EXIT, "", "");
        }
        
        if (lowerTrimmed.equals("help") || lowerTrimmed.equals("?")) {
            return new ParsedCommand(CommandType.HELP, "", "");
        }
        
        if (lowerTrimmed.equals("clear") || lowerTrimmed.equals("cls")) {
            return new ParsedCommand(CommandType.CLIENT_CLEAR, "", "");
        }
        
        if (lowerTrimmed.startsWith("cd ")) {
            String path = trimmed.substring(3).trim();
            return new ParsedCommand(CommandType.CHANGE_DIR, Protocol.SPECIAL_CD, path);
        }
        
        if (lowerTrimmed.equals("pwd")) {
            return new ParsedCommand(CommandType.PRINT_DIR, Protocol.SPECIAL_PWD, "");
        }
        
        if (lowerTrimmed.equals("ls") || lowerTrimmed.startsWith("ls ")) {
            String args = trimmed.substring(2).trim();
            return new ParsedCommand(CommandType.LIST_FILES, Protocol.SPECIAL_LS, args);
        }
        
        if (lowerTrimmed.equals("list") || lowerTrimmed.equals("users")) {
            return new ParsedCommand(CommandType.LIST_USERS, "", "");
        }
        
        return new ParsedCommand(CommandType.EXECUTE, "", trimmed);
    }
    
    public enum CommandType {
        EXECUTE,
        EXIT,
        CHANGE_DIR,
        PRINT_DIR,
        LIST_FILES,
        LIST_USERS,
        CLIENT_CLEAR,
        HELP,
        EMPTY
    }
    
    public static class ParsedCommand {
        private final CommandType type;
        private final String specialPrefix;
        private final String argument;
        
        public ParsedCommand(CommandType type, String specialPrefix, String argument) {
            this.type = type;
            this.specialPrefix = specialPrefix;
            this.argument = argument;
        }
        
        public CommandType getType() { 
            return type; 
        }
        public String getSpecialPrefix() { 
            return specialPrefix; 
        }
        public String getArgument() { 
            return argument; 
        }
        public String getFullCommand() {
            if (specialPrefix.isEmpty()) {
                return argument;
            }
            return specialPrefix + (argument.isEmpty() ? "" : " " + argument);
        }
    }
}
