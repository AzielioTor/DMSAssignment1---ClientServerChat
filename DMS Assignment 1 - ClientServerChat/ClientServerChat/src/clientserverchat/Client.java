package clientserverchat;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class Client extends JFrame {

    private String username = "";
    private int port;
    private boolean activeSession;
    private ArrayList<String> activeClients;
    private Queue<Message> outgoing;
    private Queue<String> incoming;
    
    private MessageSender sender;
    private Thread senderThread;
    private Thread receiverThread;

    // Server Credentials
    public static String hostname = "";
    public static final int HOST_SENDING_PORT = 8888;
    public static final int HOST_RECEIVING_PORT = 9999;

    // GUI 
    JPanel mainPanel;
    ChatPanel chatPanel;
    ActiveClientPanel clientPanel;
    MessagePanel messagePanel;
    JList clientList;
    JTextArea messageDisplay, messageInput;
    JButton whisperButton, broadcastButton, disconnectButton;
    // Inital popup
    JOptionPane popUpDialog;
    
    Client() {
        super("Messaging Client");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(600, 600);
        this.setResizable(false);
        mainPanel = new JPanel(null);
        mainPanel.setSize(600, 600);
        mainPanel.setLayout(null);
        this.add(mainPanel);
        addComponentsToPane(mainPanel);
        this.setLocationRelativeTo(null);
        getDetails();
        this.setVisible(true);
        
        activeSession = false;
        activeClients = new ArrayList<>();
        outgoing = new LinkedList<>();
        incoming = new LinkedList<>();
        
        this.addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                
                if(activeSession)
                    sender.disconnect();
            }
        });
    }

    private void start() {
        
        // Start Sender
        sender = new MessageSender();
        senderThread = new Thread(sender);
        senderThread.start();    
    }
    
    public void beginReceiverThreads() {
            
        // Start Receiver
        MessageReceiver receiver = new MessageReceiver();
        receiverThread = new Thread(receiver);
        receiverThread.start();

        // Start Broadcaster
        BroadcastReceiver broadcast = new BroadcastReceiver();
        Thread broadcastThread = new Thread(broadcast);
        broadcastThread.start();
    }
    
    private void sendMessage(boolean broadcast) {
        
        // Do we have a message typed?
        if(!messageInput.getText().isEmpty()) {
                 
            // Get message
            String message = messageInput.getText();
            Whisper mail = null;

            // Singular message
            if(!broadcast) {
                
                // Recipient selected?
                if(!clientList.isSelectionEmpty()) {   
                    // Get recipient
                    String recipient = (String) clientList.getSelectedValue();
                    // Create private message
                    mail = new Whisper(username, username + " whispers: " + message, recipient);
                    // Add to outbox
                    synchronized(outgoing) {
                        outgoing.add(mail);                
                        incoming.add("You whisper say to " + recipient + ": " + message);
                    }
                                
                    // Clear message entry
                    messageInput.setText("");
                }
                else
                    JOptionPane.showMessageDialog(null, "Please ensure you have a client selected before you click \'Whisper\'");
            }
            // Broadcast message
            else {
                if(!activeClients.isEmpty()) {
                    // For each active client
                    for(String recipient : activeClients) {

                        mail = new Whisper(username, username + " broadcasts: " + message, recipient);

                        synchronized(outgoing) {
                            outgoing.add(mail);
                        }
                    }
                    incoming.add("You broadcast to everyone: " + message);
                    // Clear message entry
                    messageInput.setText("");
                }
                else
                    JOptionPane.showMessageDialog(null, "There are no active clients to receive your Broadcast");
            }
            
            messagePanel.repaint();
            chatPanel.repaint();
        }
        else
            JOptionPane.showMessageDialog(null, "You haven\'t supplied a message to send!");
    }
    
    public void processMessage(Message message) {
        
        if(message instanceof Whisper) {
            
            synchronized(incoming) {
                incoming.add(message.getMessage());
                messagePanel.repaint();
            }
        }
        else if(message instanceof ActiveClientsMessage) {
            activeClients = ((ActiveClientsMessage) message).getClients();
            clientPanel.repaint();
        }
    }
    
    public static void main(String[] args) {

        Client client = new Client();
        client.start();
    }

    public class MessageReceiver implements Runnable {

        @Override
        public void run() {

            System.out.println("Receiver Started");
            
            // Create socket
            Socket socket = null;

            // Attempt socket connection
            try {
                socket = new Socket(hostname, HOST_SENDING_PORT);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "You have entered an invalid server, or the server is unreachable. Terminating client.");
                System.exit(0);
            }
            
            // Socket connection successful
            if(socket != null) {
                
                ObjectInputStream ois = null;

                try {

                    ois = new ObjectInputStream(socket.getInputStream());

                    while (activeSession) {

                        Message message = (Message) ois.readObject();
                        processMessage(message);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "You have been disconnected from the server. Terminating client.");
                    System.exit(0);
                } catch (ClassNotFoundException ex) {
                    System.out.println("Message class not found");
                } finally {

                    // Attempt to close streams
                    try {
                        if (ois != null) {
                            ois.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException ex) {
                        System.out.println("Client unable to close streams: " + ex.getMessage());
                    }
                }
            }
            
            System.out.println("Receiver Ended");
        }
    }

    public class MessageSender implements Runnable {

        public ObjectInputStream ois = null;
        public ObjectOutputStream oos = null;
        
        @Override
        public void run() {

            System.out.println("Sender Started");
            
            // Create socket
            Socket socket = null;

            // Attempt socket connection
            try {
                socket = new Socket(hostname, HOST_RECEIVING_PORT);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "You have entered an invalid server, or the server is unreachable. Terminating client.");
                System.exit(0);
            }

            // Socket connection successful
            if(socket != null) {
                try {

                    oos = new ObjectOutputStream(socket.getOutputStream());
                    ois = new ObjectInputStream(socket.getInputStream());

                    // Connect to server
                    connect();

                    while (activeSession) {

                        Message message = null;

                        synchronized(outgoing) {

                            if(!outgoing.isEmpty())
                                message = outgoing.remove();
                        }

                        // We have a message to send
                        if (message != null) {

                            oos.writeObject(message);
                            System.out.println("Outgoing mail sent! Remaining items: " + outgoing.size());
                        }
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "You have been disconnected from the server. Terminating client.");
                    System.exit(0);
                } finally {

                    // Attempt to close streams
                    try {
                        if (oos != null) {
                            oos.close();
                        }
                        if(ois != null) {
                            ois.close();
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException ex) {
                        System.out.println("Client unable to close streams: " + ex.getMessage());
                    }
                }
            }

            System.out.println("Sender Stopped");
        }
     
        public void connect() {

            try {
                // Request connection
                ConnectionRequest request = new ConnectionRequest(username);
                oos.writeObject(request);
                // Read response
                ConnectionResponse response = (ConnectionResponse) ois.readObject();

                if(response.getSuccess()) {
                    activeSession = true;
                    port = response.getPort();
                    JOptionPane.showMessageDialog(null, "Connection established");
                    beginReceiverThreads();
                }
                else {
                    JOptionPane.showMessageDialog(null, "Client already logged in with given username: " + username + 
                            ", please close application and try a different username.");
                }
            } catch (IOException ex) {
                System.out.println("Unable to send connection request to server");
            } catch (ClassNotFoundException ex) {
                System.out.println("Unable to locate ConnectionResponse class");
            }
        }
        
        public void disconnect() {
            
            try {
                // Request disconnection
                DisconnectRequest request = new DisconnectRequest(username);
                oos.writeObject(request);
                // Read response
                DisconnectResponse response = (DisconnectResponse) ois.readObject();
                
                if(response.getSuccess()) {
                    JOptionPane.showMessageDialog(null, "Disconnect successful");
                    activeSession = false;
                }
                else
                    JOptionPane.showMessageDialog(null, "Disconnect unsuccessful");
            } catch (IOException ex) {
                // Ignore
            } catch (ClassNotFoundException ex) {
                System.out.println("Unable to locate DisconnectResponse class");
            }
        }
    }

    public class BroadcastReceiver implements Runnable {
        
        @Override
        public void run() {
            
            System.out.println("Broadcaster Started");
            
            DatagramSocket socket = null;
            
            try {
                socket = new DatagramSocket(port);
            } catch (SocketException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            byte[] incomingData = new byte[1024];
            DatagramPacket receiveDatagram = new DatagramPacket(incomingData, incomingData.length);         
            
            while(activeSession) {
                
                try {
                    socket.receive(receiveDatagram);
                    byte[] data = receiveDatagram.getData();
                    
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    ObjectInputStream is = new ObjectInputStream(bais);
                    
                    try {
                        ActiveClientsMessage message = (ActiveClientsMessage) is.readObject();
                        processMessage(message);
                    } catch (ClassNotFoundException ex) {
                        System.out.println("ActiveClientsMessage class not found");
                    }
                    
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Unable to update active client list");
                }
            }
                System.out.println("Broadcaster Stopped");
        }
    }
    
    private void getDetails() {

        JTextField usernameField = new JTextField(10);
        JTextField hostField = new JTextField(15);

        JPanel myPanel = new JPanel();
        myPanel.add(new JLabel("Username:"));
        myPanel.add(usernameField);
        myPanel.add(new JLabel("Host/IP:"));
        myPanel.add(hostField);

        do {                    
            int result = JOptionPane.showConfirmDialog(null, myPanel, 
                      "Please enter a Username and Host/IP address", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                username = usernameField.getText();
                hostname = hostField.getText();
            }
            else {
                JOptionPane.showMessageDialog(null, "You have decided not to connect. Terminating client.");
                System.exit(0);
            }
        } while (username.isEmpty() && hostname.isEmpty());
    }
    
    private void addComponentsToPane(Container pane) {
                
        // Chat section
        chatPanel = new ChatPanel();
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.setSize(420, 420);
        chatPanel.setLocation(10, 10);
        chatPanel.setLayout(null);
        
        messageDisplay = new JTextArea();
        messageDisplay.setSize(390, 380);
        messageDisplay.setEditable(false);
        messageDisplay.setLocation(15, 25);
        messageDisplay.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        messageDisplay.setLineWrap(true);
        messageDisplay.setWrapStyleWord(true);
        
        DefaultCaret messageDisplayCaret = (DefaultCaret) messageDisplay.getCaret();
        messageDisplayCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane chatScroll = new JScrollPane(messageDisplay,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.setSize(390, 380);
        chatScroll.setLocation(15, 25);
        
        chatPanel.add(chatScroll);
        mainPanel.add(chatPanel);
        
        // Client section
        clientPanel = new ActiveClientPanel();
        clientPanel.setBorder(BorderFactory.createTitledBorder("Active Clients"));
        clientPanel.setSize(150, 420);
        clientPanel.setLocation(chatPanel.getWidth() + 20, 10);
        clientPanel.setLayout(null);
        mainPanel.add(clientPanel);
        
        clientList = new JList();
        clientList.setSize(115, 385);
        clientList.setLocation(15, 25);
        clientList.setListData(new String[0]);
        clientList.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        clientPanel.add(clientList);

        // Message Section
        messagePanel = new MessagePanel();
        messagePanel.setBorder(BorderFactory.createTitledBorder("Message"));
        messagePanel.setSize(580, 140);
        messagePanel.setLocation(10, chatPanel.getHeight() + 10);
        messagePanel.setLayout(null);
        mainPanel.add(messagePanel);
        
        messageInput = new JTextArea();
        messageInput.setSize(420, 100);
        messageInput.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);
        
        DefaultCaret messageInputCaret = (DefaultCaret) messageInput.getCaret();
        messageInputCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
                
        JScrollPane messageScroll = new JScrollPane(messageInput,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        messageScroll.setSize(420, 100);
        messageScroll.setLocation(20, 25);
        messagePanel.add(messageScroll);

        whisperButton = new JButton();
        whisperButton.setSize(100, 20);
        whisperButton.setText("Whisper");
        whisperButton.setLocation(messageScroll.getWidth()+ 40, messageScroll.getY());
        whisperButton.addActionListener((ActionEvent e) -> {
            sendMessage(false);
        });
        messagePanel.add(whisperButton);
        
        broadcastButton = new JButton();
        broadcastButton.setSize(100, 20);
        broadcastButton.setText("Broadcast");
        broadcastButton.setLocation(messageScroll.getWidth()+ 40, whisperButton.getY() + 40);
        broadcastButton.addActionListener((ActionEvent e) -> {
            sendMessage(true);
        });
        messagePanel.add(broadcastButton);
        
        disconnectButton = new JButton();
        disconnectButton.setSize(100, 20);
        disconnectButton.setText("Disconnect");
        disconnectButton.setLocation(messageScroll.getWidth()+ 40, broadcastButton.getY() + 40);
        disconnectButton.addActionListener((ActionEvent e) -> {
            sender.disconnect();
        });
        messagePanel.add(disconnectButton);
    }
    
    public class ChatPanel extends JPanel {
    }
    
    public class ActiveClientPanel extends JPanel {
        
        @Override
        public void repaint() {
            
            String selected = null;
            
            if(clientList != null && !clientList.isSelectionEmpty()) {
                // Get currently selected client name
                selected = (String) clientList.getSelectedValue();
                // Sets client list
                activeClients.remove(username);
            }
            
            if(clientList != null) {
                activeClients.remove(username);
                clientList.setListData(activeClients.toArray());
            }
            
            if(selected != null)
                // Sets previously selected client name
                clientList.setSelectedValue(selected, true);
        }
    }
    
    public class MessagePanel extends JPanel {
        
        @Override
        public void repaint() {

            // Messages
            messageDisplay.setText(null);
            
            if(incoming != null) {
                synchronized(this) {
                    Iterator<String> received = incoming.iterator();

                    while(received.hasNext())
                        messageDisplay.append(received.next() + "\n\n");
                }
            }
        }
    }
}
