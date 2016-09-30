
package clientserverchat;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class Whisper extends Message {

    private String recipient;

    Whisper(String sender, String message, String recipient) {
        super(sender, message);
        this.recipient = recipient;
    }

    public String getRecipient() {
        return recipient;
    }
}
