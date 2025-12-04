package csdev.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class ResponseReader {
    private DataInputStream in;
    private Thread readerThread;
    private boolean active;
    
    public ResponseReader(Socket socket, Consumer<String> responseHandler) throws IOException {
        this.in = new DataInputStream(socket.getInputStream());
        this.active = true;
        this.readerThread = new Thread(() -> readResponses(responseHandler));
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }
    
    private void readResponses(Consumer<String> responseHandler) {
        try {
            while (active) {
                try {
                    int resultCode = in.readInt();
                    
                    if (resultCode == Protocol.RESULT_OK) {
                        int dataLength = in.readInt();
                        
                        if (dataLength > Protocol.MAX_RESPONSE_LENGTH) {
                            throw new IOException("Response too large");
                        }
                        
                        if (dataLength > 0) {
                            byte[] data = new byte[dataLength];
                            in.readFully(data);
                            String response = new String(data, Protocol.ENCODING);
                            responseHandler.accept(response);
                        }
                    } else if (resultCode == Protocol.RESULT_ERROR) {
                        int errorLength = in.readInt();
                        if (errorLength > 0) {
                            byte[] errorData = new byte[errorLength];
                            in.readFully(errorData);
                            String error = new String(errorData, Protocol.ENCODING);
                            responseHandler.accept("ERROR: " + error);
                        }
                    }
                } catch (java.net.SocketTimeoutException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            if (active) {
                System.err.println("Connection lost");
                responseHandler.accept("\n[Connection lost]");
            }
        }
    }
    
    public void stop() {
        active = false;
        try {
            if (in != null) {
                in.close();
            }
            if (readerThread != null) {
                readerThread.interrupt();
            }
        } catch (IOException e) {
            System.err.println("Error closing input stream");
        }
    }
    
    public boolean isActive() {
        return active;
    }
}
