
package clientserverchat;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class DisconnectResponse extends Message {
   
    private boolean success;
    
    DisconnectResponse(boolean success) {
        super("Server", "You have been disconnected");
        this.success = success;
    }

    public boolean getSuccess() {
        return this.success;
    }
    
}
