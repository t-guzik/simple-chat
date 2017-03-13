package app;

import app.AsciiArt;
import javafx.scene.control.*;
import java.io.*;
import java.net.*;

/**
 * Created by Tomasz Guzik on 2017-03-10.
 *
 * ZAD 1
 * Klienci łączą się serwerem przez protokół TCP
 * Serwer przyjmuje wiadomości od każdego klienta i rozsyła je do pozostałych (wraz z id/nickiem klienta)
 * Serwer jest wielowątkowy – każde połączenie od klienta powinno mieć swój wątek
 * Proszę zwrócić uwagę na poprawną obsługę wątków
 */
public class Client {
    /** Socket data */
    private final String serverHost = "localhost";
    private final int PORTNUM = 9999;
    private final int PACKET_SIZE = 10000;

    private Socket socket;
    private BufferedReader inputStream;
    private PrintWriter outputStream;

    private DatagramSocket datagramSocket;

    private volatile boolean stopped = false;
    private volatile boolean isLoggedIn = false;

    /** GUI */
    private String login;
    private TextArea chatArea;
    private TextField loggedUsers;

    public Client(TextArea chatArea, TextField loggedUsers, String login){
        this.chatArea = chatArea;
        this.login = login;
        this.loggedUsers = loggedUsers;
    }

    public void login() {
        if (isLoggedIn)
            return;
        try {
            /** TCP */
            socket = new Socket(serverHost, PORTNUM);
            inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream = new PrintWriter(socket.getOutputStream(), true);
            outputStream.println("L" + login);

            isLoggedIn = true;
        } catch (IOException e) {
            log("Can't get socket to " + serverHost + "/" + PORTNUM + ": " + e);
            return;
        }

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
                }
            }
        }).start();
    }

    public void sendUDP() throws IOException {
        byte[] data = AsciiArt.getArt().getBytes();
        DatagramPacket request = new DatagramPacket(data,
                data.length, InetAddress.getByName(serverHost), PORTNUM);
        datagramSocket.send(request);
    }

    public void logout() {
        if (!isLoggedIn)
            return;

        isLoggedIn = false;
        stopped = true;
        try {
            if (socket != null) {
                outputStream.println("Q" + login);
                datagramSocket.close();
                socket.close();
            }
        } catch (IOException e) {
            log("Failure during close to " + serverHost);
        }
    }

    public void send(String msg) {
        if(isLoggedIn){
            outputStream.println("M" + msg);
        }
    }

    private void log(String message) {
        System.out.println(message);
    }
}