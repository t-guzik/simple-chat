package app;

import javafx.scene.control.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.locks.Lock;

/**
 * Created by Tomasz Guzik on 2017-03-10.
 * New chat user. It has UDP and TCP channels to send messages.
 */
public class Client {
    private static boolean DEBUG = true;

    /** Socket data */
    private final String serverHost = "localhost";
    private final String multicastAddress = "228.5.6.7";
    private InetAddress multicastGroup = InetAddress.getByName(multicastAddress);
    private final int PORT = 9999;
    private final int PACKET_SIZE = 10000;
    private final int TTL = 1;

    private Socket socket;
    private BufferedReader inputStream;
    private PrintWriter outputStream;

    private DatagramSocket datagramSocket;
    private MulticastSocket datagramMulticastSocket;

    private volatile boolean stopped = false;
    private volatile boolean isLoggedIn = false;
    private boolean multicastActive = false;
    private boolean groupMembershipSelected = true;
    private boolean groupReady = false;
    private Thread multicastUDPThread;

    private byte[] data = AsciiArt.getArt().getBytes();

    /** GUI */
    private String login;
    private TextArea chatArea;
    private TextField loggedUsers;

    public Client(TextArea chatArea, TextField loggedUsers, String login, boolean multicastActive) throws IOException {
        this.chatArea = chatArea;
        this.login = login;
        this.loggedUsers = loggedUsers;
        this.multicastActive = multicastActive;
    }

    public void login() {
        if (isLoggedIn)
            return;
        try {
            /** TCP */
            socket = new Socket(serverHost, PORT);
            inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream = new PrintWriter(socket.getOutputStream(), true);
            outputStream.println("L" + login);

            isLoggedIn = true;
        } catch (IOException e) {
            log("Can't get socket to " + serverHost + ":" + PORT + " " + e);
            return;
        }

        /** Thred for TCP channel */
        new Thread(new Runnable() {
            public void run() {
                String msg;
                char option; // M = msg, S = clients list size
                try {
                    while (isLoggedIn && ((msg = inputStream.readLine()) != null)) {
                        option = msg.charAt(0);
                        msg = msg.substring(1);
                        switch (option) {
                            case 'M':
                                chatArea.appendText(msg + "\n");
                                break;

                            case 'S':
                                loggedUsers.setText("Logged users: " + msg);
                                break;
                        }
                    }
                } catch (IOException e) {
                    log("IOException: " + e);
                    return;
                }
            }
        }).start();

        if(!multicastActive)
            /** Thread for UDP channel */
            new Thread(new Runnable() {
                public void run() {
                    try {
                        datagramSocket = new DatagramSocket(socket.getLocalPort());

                        DatagramPacket response = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                        while (!stopped) {
                            datagramSocket.receive(response);
                            String result = new String(response.getData(), 0, response.getLength());
                            chatArea.appendText(result);
                        }
                    } catch (IOException e) {
                        log("IOException " + e);
                    } finally {
                        datagramSocket.close();
                    }
                }
            }).start();
        else { // multicast UDP
            /** Thread for UDP multicast channel */
            new Thread(new Runnable() {
                public void run() {
                    multicastUDPThread = new Thread(Thread.currentThread().getName());
                    try {
                        DatagramPacket response = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                        while (!stopped) {
                            if (groupMembershipSelected) {
                                if(!groupReady) {
                                    datagramMulticastSocket = new MulticastSocket(PORT - 1);
                                    datagramMulticastSocket.setTimeToLive(TTL);
                                    /** Should be set in order to don't receive own messages.*/
                                    //datagramMulticastSocket.setLoopbackMode(true);
                                    datagramMulticastSocket.joinGroup(InetAddress.getByName(multicastAddress));
                                    groupReady = true;
                                }

                                if (DEBUG) log("Multicast membership: JOINED");
                                datagramMulticastSocket.receive(response);
                                String result = new String(response.getData(), 0, response.getLength());
                                chatArea.appendText(result);
                            } else {
                                if(groupReady) {
                                    datagramMulticastSocket.leaveGroup(InetAddress.getByName(multicastAddress));
                                    groupReady = false;
                                }
                                if (DEBUG) log("Multicast membership: LEFT");
                            }
                        }
                    } catch (IOException e) {
                        log("IOException " + e);
                    } finally {
                        try {
                            datagramMulticastSocket.leaveGroup(InetAddress.getByName(multicastAddress));
                            datagramMulticastSocket.close();
                        } catch (IOException e) {
                            log("IOException " + e);
                        }
                    }
                }
            }).start();
        }
    }

    /**
     * Sending UDP media message (AsciArt) to server.
     * @throws IOException
     */
    public void sendUDP() throws IOException {
            DatagramPacket request = new DatagramPacket(data, data.length, InetAddress.getByName(serverHost), PORT);
            datagramSocket.send(request);
    }

    public void sendMulticastUDP() throws IOException {
        if(groupMembershipSelected) {
            DatagramPacket request = new DatagramPacket(data, data.length, multicastGroup, PORT - 1);
            datagramMulticastSocket.send(request);
        }
    }

    /**
     * Logging out and closing chat session.
     * Clossing TCP and UDP sockets.
     */
    public void logout() {
        if (!isLoggedIn)
            return;

        isLoggedIn = false;
        stopped = true;

        try {
            if (socket != null) {
                outputStream.println("Q" + login);
                if(datagramSocket != null)
                    datagramSocket.close();
                if(datagramMulticastSocket != null)
                    datagramMulticastSocket.close();
                socket.close();
            }
        } catch (IOException e) {
            log("Failure during close to " + serverHost);
        }
    }

    /**
     * Sending TCP text message to server.
     * @param msg
     */
    public void send(String msg) {
        if(isLoggedIn){
            outputStream.println("M" + msg);
        }
    }

    public void setGroupMembershipSelected(boolean groupMembershipSelected) throws InterruptedException, IOException {
        this.groupMembershipSelected = groupMembershipSelected;
    }

    private void log(String message) {
        System.out.println(message);
    }
}