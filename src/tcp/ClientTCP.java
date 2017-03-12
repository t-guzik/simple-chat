package tcp;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Tomasz Guzik on 2017-03-10.
 *
 * ZAD 1
 * Klienci łączą się serwerem przez protokół TCP
 * Serwer przyjmuje wiadomości od każdego klienta i rozsyła je do pozostałych (wraz z id/nickiem klienta)
 * Serwer jest wielowątkowy – każde połączenie od klienta powinno mieć swój wątek
 * Proszę zwrócić uwagę na poprawną obsługę wątków
 */
public class ClientTCP{
    /** Socket data */
    protected final String serverHost = "192.168.0.14";
    protected boolean loggedIn = false;
    protected static final int PORTNUM = 9999;
    protected Socket socket;
    protected BufferedReader inputStream;
    protected PrintWriter outputStream;

    /** GUI */
    private String login;
    private TextArea chatArea;
    private TextField loggedUsers;

    public ClientTCP(TextArea chatArea, TextField loggedUsers, String login){
        this.chatArea = chatArea;
        this.login = login;
        this.loggedUsers = loggedUsers;
    }

    public void login() {
        if (loggedIn)
            return;
        try {
            socket = new Socket(serverHost, PORTNUM);
            inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outputStream = new PrintWriter(socket.getOutputStream(), true);
            outputStream.println("L" + login);
            loggedIn = true;
        } catch(IOException e) {
            log("Can't get socket to " + serverHost + "/" + PORTNUM + ": " + e);
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                String msg;
                char option; // M = msg, N = clients number
                try {
                    while (loggedIn && ((msg = inputStream.readLine()) != null)){
                        option = msg.charAt(0);
                        msg = msg.substring(1);
                        switch(option){
                            case 'M':
                                chatArea.appendText(msg + "\n");
                                break;

                            case 'S':
                                loggedUsers.setText("Logged users: " + msg);
                                break;
                        }
                    }
                } catch(IOException e) {
                    log("IOException: " + e);
                    return;
                }
            }
        }).start();
    }

    public void logout() {
        if (!loggedIn)
            return;

        loggedIn = false;
        try {
            if (socket != null) {
                outputStream.println("Q" + login);
                socket.close();
            }
        } catch (IOException e) {
            log("Failure during close to " + serverHost);
        }
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void send(String msg) {
        if(loggedIn){
            outputStream.println("M" + msg);
        }
    }
}