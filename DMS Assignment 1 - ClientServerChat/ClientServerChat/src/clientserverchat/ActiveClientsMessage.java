
package clientserverchat;

import java.util.ArrayList;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class ActiveClientsMessage extends Message {
    
    private ArrayList<String> activeClients;
    
    ActiveClientsMessage(ArrayList<String> clients) {
        super("Server", "Active Clients Refreshed");
        activeClients = clients;
    }
    
    public ArrayList<String> getClients() {
        return activeClients;
    }
}
