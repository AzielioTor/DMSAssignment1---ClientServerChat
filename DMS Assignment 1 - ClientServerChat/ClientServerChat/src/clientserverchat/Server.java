package clientserverchat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Adam Campbell - 14847097
 * @author Aziel Shaw - 1311607
 */
public class Server {

    private HashMap<String, ClientData> clients;
    private ArrayList<Message> messages;
    private boolean enabled;

    // Server Credentials
    public static final int SENDING_PORT = 8888;
    public static final int RECEIVING_PORT = 9999;
    private ServerSocket sendingServerSocket = null;
    private ServerSocket receivingServerSocket = null;

    Server() {

        clients = new HashMap<>();
        messages = new ArrayList<>();
        enabled = true;
    }

    private void startServer() {

        try {            
            receivingServerSocket = new ServerSocket(RECEIVING_PORT);
            receivingServerSocket.setSoTimeout(30000); // 30 seconds to accept
            System.out.println("Server started receiving at " + InetAddress.getLocalHost() + " on port " + RECEIVING_PORT);
            
            sendingServerSocket = new ServerSocket(SENDING_PORT);
            sendingServerSocket.setSoTimeout(30000); // 30 seconds to accept
            System.out.println("Server started sending at " + InetAddress.getLocalHost() + " on port " + SENDING_PORT);

            // Begin broadcast of clients
            ClientBroadcaster broadcast = new ClientBroadcaster();
            Thread broadcastThread = new Thread(broadcast);
            broadcastThread.start();
            
        } catch (IOException ex) {
            System.out.println("Server cannot listen on port: " + ex.getMessage());
            System.exit(-1);
        }

        while (enabled) {
            // Attempt to access incoming requests
            acceptReceivingSocketRequest();
        }

        try {
            sendingServerSocket.close();
            receivingServerSocket.close();
        } catch (IOException ex) {
            // Ignore
        }

        System.out.println("Server finished, terminating...");
    }
    
    private void acceptReceivingSocketRequest() {
        
        try {
            Socket receivingSocket = receivingServerSocket.accept();
            System.out.println("Receiving connection made with address: " + receivingSocket.getInetAddress()); // Gets client IP
            
            MessageReceiver receiver = new MessageReceiver(receivingSocket);
            Thread receiverThread = new Thread(receiver);
            receiverThread.start();
            
        } catch (IOException ex) {
            // Timeout, ignore and try again
        }
    }
    
    private void acceptSendingSocketRequest(String username) {
            
        try {
            Socket sendingSocket = sendingServerSocket.accept();
            System.out.println("Sending connection made with address: " + sendingSocket.getInetAddress()); // Gets client IP

            MessageSender sender = new MessageSender(sendingSocket, username);
            Thread senderThread = new Thread(sender);
            senderThread.start();
            
            // Bind sender to client
            clients.get(username).setSender(sender);
            
        } catch (IOException ex) {
            System.out.println("Unable to create connection for AcceptSendingSocketRequest");
        }
    }

    private void addClient(String name, String address, int port, MessageReceiver receiver) {

        clients.put(name, new ClientData(name, address, port, receiver));
    }
    
    public ConnectionResponse processConnectRequest(ConnectionRequest request, Socket socket) {
                    
        ConnectionResponse response = null;
        int port = socket.getPort();
            
        if(clients.containsKey(request.getSenderName()) && !clients.get(request.getSenderName()).isTerminated())
            response = new ConnectionResponse(false, "Username already in use", port);
        else {
            response = new ConnectionResponse(true, "Connection successful", port);
             
            // Notify all users of connection
            for(String key : clients.keySet()) {

                if(!key.equals(request.getSenderName())) {
                    Whisper message = new Whisper("Server", request.getMessage(), key);
                    clients.get(key).getMessages().add(message);
                }
            }
        }

        return response;
    }
    
    public DisconnectResponse processDisconnectRequest(DisconnectRequest request) {
        
        DisconnectResponse response = null;
        
        if(clients.containsKey(request.getSenderName()) && !clients.get(request.getSenderName()).isTerminated()) {
            
            // Notify all users of disconnection
            for(String key : clients.keySet()) {

                if(!key.equals(request.getSenderName())) {
                    Whisper message = new Whisper("Server", request.getMessage(), key);
                    clients.get(key).getMessages().add(message);
                }
            }
            
            // Disconnection successful
            response = new DisconnectResponse(true);
        }

        return response;
    }

    public static void main(String[] args) {

        // Server instantiantion
        Server server = new Server();
        server.startServer();
    }

    public class MessageReceiver implements Runnable {

    private Socket socket;
    private String username;

    MessageReceiver(Socket receivingSocket) {
        this.socket = receivingSocket;
    }

    @Override
    public void run() {

        System.out.println("Receiver Thread Started");
        
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;
        
        try {
            // Get input/output streams
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());

            // Connection Requested
            ConnectionRequest request = (ConnectionRequest) ois.readObject();
            // Server Response
            ConnectionResponse response = processConnectRequest(request, socket);
            oos.writeObject(response);
            // Successful connection?
            boolean success = response.getSuccess();
            
            if(success) {
                username = request.getSenderName();
                
                // Add client to active client list
                addClient(username, socket.getInetAddress().toString(), socket.getPort(), this);
                
                // Begin sender thread
                acceptSendingSocketRequest(username);
                
                while (!clients.get(username).isTerminated()) {
                    // Read input object
                    Message message = (Message) ois.readObject();

                    // Disconnection request
                    if(message instanceof DisconnectRequest) {
                        DisconnectResponse disconnectResponse = processDisconnectRequest((DisconnectRequest) message);

                        if(disconnectResponse != null) {
                            oos.writeObject(disconnectResponse);
                            clients.get(message.getSenderName()).terminate();
                        }
                    }
                    else {
                        // Private message
                        if(!clients.get(((Whisper)message).getRecipient()).isTerminated())
                            clients.get(((Whisper)message).getRecipient()).getMessages().add(message);
                    }
                }
            }
        } catch (IOException ex) {
            // Ignore
        } catch (ClassNotFoundException ex) {
            System.out.println("Message class not found");
        } finally {

            // Attempt to close streams
            try {
                if (ois != null) {
                    ois.close();
                }
                if(oos != null)
                    oos.close();
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ex) {
                System.out.println("Client unable to close streams: " + ex.getMessage());
            }
        }
        System.out.println("Receiver Thread Finished");
    }

}
    
    public class MessageSender implements Runnable {

        private Socket socket;
        private String username;

        MessageSender(Socket socket, String username) {
            this.socket = socket;
            this.username = username;
        }

        @Override
        public void run() {
            
            System.out.println("Sender Thread Started");
            
            ObjectOutputStream oos = null;
            
            try {

                oos = new ObjectOutputStream(socket.getOutputStream());
                
                while (!clients.get(username).isTerminated()) {
                    
                    Message message = null;
                    
                    Queue<Message> outgoing = null;
                    ClientData data = clients.get(username);
                    
                    if(data != null && !clients.get(username).isTerminated()) {
                        outgoing = data.getMessages();
                    }
                    
                    // Get next message
                    if(outgoing != null && !outgoing.isEmpty())
                        message = outgoing.remove();
                    
                    // Send message
                    if(message != null) {
                        oos.writeObject(message);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Server error: " + ex.getMessage());
            } finally {
                // Attempt to close streams
                try {
                    if (oos != null) {
                        oos.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException ex) {
                    System.out.println("Client unable to close streams: " + ex.getMessage());
                }
            }
            
            System.out.println("Sender Thread Finished");
        }

    }

    public class ClientBroadcaster implements Runnable {

        @Override
        public void run() {
            
            System.out.println("Broadcaster Thread Started");
            
            DatagramSocket socket = null;
            InetAddress hostAddress = null;
            
            while (enabled) {
                
                if(!clients.isEmpty()) {
                    // Active client storage
                    ArrayList<String> activeClients = new ArrayList<>();

                    // Get active client data
                    for(String key : clients.keySet())
                        if(!clients.get(key).isTerminated())
                            activeClients.add(key);
                    // Create message
                    ActiveClientsMessage actives = new ActiveClientsMessage(activeClients);

                    // For each client, send message via Datagram Packet
                    for(String key : clients.keySet()) {

                        try {
                            // Attempt to get socket and address
                            try {
                                socket = new DatagramSocket();
                                hostAddress = InetAddress.getByName(clients.get(key).getAddress().replace("/", ""));

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ObjectOutputStream os = new ObjectOutputStream(baos);
                                os.writeObject(actives);

                                byte[] data = baos.toByteArray();
                                DatagramPacket packet = new DatagramPacket(data, data.length, hostAddress, clients.get(key).getPort());
                                socket.send(packet);
                            } catch (UnknownHostException ex) {
                                System.out.println("Unknown Host Exception");
                            } catch (SocketException ex) {
                                System.out.println("Socket Exception");
                            }
                        } catch (IOException ex) {
                            System.out.println("Broadcast error: " + ex.getMessage());
                        }
                    }
                }
                
                // Sleep for a while
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    System.out.println("Sleep failed");
                }
            }
            
            System.out.println("Broadcaster Thread Stopped");
        }
    }
}
