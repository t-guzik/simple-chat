package app;

import tcp.ChatServerTCP;
import udp.ChatServerUDP;

import java.io.IOException;

/**
 * Created by Tomasz Guzik on 2017-03-12.
 */
public class Server {

    public static void main(String[] argv) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    log("Chat TCP server running...");
                    ChatServerTCP chatServerTCP = new ChatServerTCP();
                    chatServerTCP.runServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                log("Chat UDP server running...");
                ChatServerUDP chatServerUDP = new ChatServerUDP();
                chatServerUDP.runServer();
            }
        }).start();
    }

    private static void log(String s) {
        System.out.println(s);
    }
}
