
package clientserverchat;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class ConnectionResponse extends Message {
    
    private boolean success;
    private int port;
    
    ConnectionResponse(boolean success, String message, int port) {
        super("Server", message);
        this.success = success;
        this.port = port;
    }
    
    public boolean getSuccess() {
        return this.success;
    }
    
    public int getPort() {
        return this.port;
    }
}
