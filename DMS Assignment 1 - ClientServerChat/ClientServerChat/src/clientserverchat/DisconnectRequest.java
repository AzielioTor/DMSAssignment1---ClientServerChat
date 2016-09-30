package clientserverchat;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class DisconnectRequest extends Message {

    DisconnectRequest(String name) {
        super(name, name + " has disconnected.");
    }
}