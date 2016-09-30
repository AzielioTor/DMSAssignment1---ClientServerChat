
package clientserverchat;

import clientserverchat.Server.MessageReceiver;
import clientserverchat.Server.MessageSender;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class ClientData {
    
    private final String NAME;
    private final String IP;
    private final int port;
    private Queue<Message> messages;
    private MessageReceiver receiver;
    private MessageSender sender;
    private boolean terminated;
    
    ClientData(String name, String address, int port, MessageReceiver receiver) {
        NAME = name;
        IP = address;
        this.port = port;
        messages = new LinkedList<>();
        this.receiver = receiver;
        terminated = false;
    }
    
    public String getName() {
        return NAME;
    }
    
    public String getAddress() {
        return IP;
    }
    
    public int getPort() {
        return port;
    }
    
    public Message getNextMessage() {
        return messages.remove();
    }
    
    public Queue<Message> getMessages() {
        return messages;
    }

    public MessageReceiver getReceiver() {
        return receiver;
    }

    public MessageSender getSender() {
        return sender;
    }

    public void setSender(MessageSender sender) {
        this.sender = sender;
    }
    
    public void terminate() {
        terminated = true;
    }
    
    public boolean isTerminated() {
        return terminated;
    }
}
