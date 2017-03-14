package app;

import javafx.scene.control.*;
import java.io.*;
import java.net.*;

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

            if(DEBUG) {
                log("User " + login + " correctly logged in.");
                log("TCP " + socket.toString());
            }


            isLoggedIn = true;
        } catch (IOException e) {
            log("Can't get socket to " + serverHost + ":" + PORT + " " + e);
            return;
        }

        /** Thread for TCP channel */
        new Thread(new Runnable() {
            public void run() {
                Thread.currentThread().setName("TCP handler thread");
                if(DEBUG) log("TCP channel running in " + Thread.currentThread().toString());
                String msg;
                char option; // M = msg, S = clients list size
                try {
                    while (isLoggedIn && ((msg = inputStream.readLine()) != null)) {
                        option = msg.charAt(0);
                        msg = msg.substring(1);
                        if (DEBUG) log("TCP message received, size=" + msg.length());
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
                } finally {
                    log("TCP socket closed!");
                }
            }
        }).start();

        if(!multicastActive)
            /** Thread for UDP channel */
            new Thread(new Runnable() {
                public void run() {
                    Thread.currentThread().setName("UDP handler thread");
                    if(DEBUG) log("UDP channel running in " + Thread.currentThread().toString());
                    try {
                        datagramSocket = new DatagramSocket(socket.getLocalPort());
                        if(DEBUG) log("UDP datagram socket localport=" + datagramSocket.getLocalPort());
                        DatagramPacket response = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                        while (!stopped) {
                            datagramSocket.receive(response);
                            if(DEBUG) log("UDP datagram received form " + response.getAddress() + ":" + response.getPort() + ", size=" + response.getLength());
                            String result = new String(response.getData(), 0, response.getLength());
                            chatArea.appendText(result);
                        }
                    } catch (IOException e) {
                        log("IOException " + e);
                    } finally {
                        log("UDP datagram socket closed!");
                    }
                }
            }).start();
        else { // multicast UDP
            /** Thread for UDP multicast channel */
            new Thread(new Runnable() {
                public void run() {
                    Thread.currentThread().setName("UDP multicast handler thread");
                    if(DEBUG) log("UDP multicast channel running in " + Thread.currentThread().toString());
                    try {
                        datagramMulticastSocket = new MulticastSocket(PORT - 1);
                        datagramMulticastSocket.setTimeToLive(TTL);
                        /** Should be set in order to don't receive own messages.*/
                        //datagramMulticastSocket.setLoopbackMode(true);
                        datagramMulticastSocket.joinGroup(InetAddress.getByName(multicastAddress));
                        if(DEBUG) {
                            log("UDP datagram socket [localport=" + datagramMulticastSocket.getLocalPort() +
                                    ", TTL=" + datagramMulticastSocket.getTimeToLive() +"]");
                            log("Client " + login + " joined the multicast group!");
                        }

                        DatagramPacket response = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                        while (!stopped) {
                            if (groupMembershipSelected) {
                                datagramMulticastSocket.receive(response);
                                if(DEBUG) log("UDP datagram received form " + response.getAddress() + ":" + response.getPort() + ", size=" + response.getLength());
                                String result = new String(response.getData(), 0, response.getLength());
                                chatArea.appendText(result);
                            }
                        }
                    } catch (IOException e) {
                        log("IOException " + e);
                    } finally {
                        log("UDP datagram multicast socket closed!");
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
        if(DEBUG) log("UDP datagram sent to " + request.getAddress() + ":" + request.getPort() + ", size=" + request.getLength());
    }

    public void sendMulticastUDP() throws IOException {
        if(groupMembershipSelected && !datagramMulticastSocket.isClosed()) {
            DatagramPacket request = new DatagramPacket(data, data.length, InetAddress.getByName(serverHost), PORT);
            datagramMulticastSocket.send(request);

            /** OR JUST WITHOUT SERVER */
            //DatagramPacket request1 = new DatagramPacket(data, data.length, multicastGroup, PORT-1);
            //datagramMulticastSocket.send(request1);
            if(DEBUG) log("UDP multicast datagram sent to " + request.getAddress() + ":" + request.getPort() + ", size=" + request.getLength());
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
            if (DEBUG) log("TCP message sent, size=" + msg.length());
        }
    }

    public void setGroupMembershipSelected(boolean groupMembershipSelected) throws InterruptedException, IOException {
        this.groupMembershipSelected = groupMembershipSelected;

        if(!groupMembershipSelected) {
            datagramMulticastSocket.close();
            if (DEBUG) log("Client " + login + " left the multicast group!");
        }
    }

    private void log(String message) {
        System.out.println(message);
    }
}