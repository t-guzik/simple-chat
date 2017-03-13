package app;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by Tomasz Guzik on 2017-03-10.
 * Server TCP and UDP
 */
public class ChatServer {
    private static final int PORT = 9999;
    private final int PACKET_SIZE = 10000;
    private volatile boolean isShutDown = false;

    private ServerSocket serverSocket;
    private List<ClientHandler> clients;

    public ChatServer() throws IOException {
        clients = new LinkedList<>();
        serverSocket = new ServerSocket(PORT);
    }

    public static void main(String []argv) throws IOException {
        ChatServer chatServer = new ChatServer();
        log("Server running...");
        chatServer.runServer();
    }

    public void runServer() {
        /** Run UDP channel */
        new Thread(new Runnable() {
            public void run() {
                runUDP();
            }
        }).start();

        log("TCP ready!");
        while(true){
            try {
                Socket newClient = serverSocket.accept();
                String hostName = newClient.getInetAddress().getHostAddress();
                log("New user from " + hostName + ":" + newClient.getPort());

                ClientHandler clientHandler = new ClientHandler(newClient, hostName);

                synchronized (clients) {
                    clients.add(clientHandler);
                }

                clientHandler.start();
                clientHandler.send("M", "Welcome to C.H.A.T. !");
            } catch (IOException e){
                log("IO Exception on server: " + e);
            }
        }
    }

    private void runUDP() {
        log("UDP ready!");
        byte buffer[] = new byte[PACKET_SIZE];
        try(DatagramSocket serverSocketUDP = new DatagramSocket(PORT)) {
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            while (!isShutDown) {
                serverSocketUDP.receive(request);
                log("UDP request from " + request.getAddress() + ":" + request.getPort());
                broadcastUDP(serverSocketUDP, request);
            }
        } catch (IOException e){
            log("IOException " + e);
        }
    }

    private void broadcastUDP(DatagramSocket serverSocketUDP, DatagramPacket request) {
        clients.forEach(c -> {
            /** Should be:
             *  if( c.clientSocket.getInetAdress ... )
             *  */
            if (c.clientSocket.getPort() != request.getPort()) {
                try {
                    DatagramPacket response = new DatagramPacket(request.getData(),
                        request.getLength(), c.clientSocket.getInetAddress(), c.clientSocket.getPort());

                    serverSocketUDP.send(response);
                } catch (IOException e) {
                    log("IOException " + e);
                }
            }
        });
    }

    private static void log(String s) {
        System.out.println(s);
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader inputStream;
        private PrintWriter outputStream;
        private String clientIP;
        private String login;

        private ClientHandler(Socket socket, String ip) throws IOException{
            this.clientSocket = socket;
            this.clientIP = ip;
            this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.outputStream = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            String msg;
            char option; // L = login, M = msg, Q = quit
            try {
                while( (msg = inputStream.readLine()) != null){
                    option = msg.charAt(0);
                    msg = msg.substring(1);

                    switch(option){
                        case 'M':
                            if (login != null)
                                broadcast(login, msg, "M");
                            break;

                        case 'L':
                            /** Assume login is valid. */
                            login = msg;
                            broadcast(login, " has just been logged in. Hello!", "L");
                            broadcast("", Integer.toString(clients.size()), "S");
                            break;

                        case 'Q':
                            broadcast(login, " has just been logged out. Bye!", "Q");
                            broadcast("", Integer.toString(clients.size()-1), "S");
                            close();
                            return;
                    }
                }
            } catch(IOException e){
                log("IO Exception on client thread: " + e);
            } finally {
                synchronized (clients){
                    clients.remove(this);
                }
            }
        }

        private void close() {
            if (clientSocket == null) {
                log("Socket has not been opened.");
                return;
            }
            try {
                log("User from " + clientSocket.getInetAddress().getHostName() + ":" + clientSocket.getPort() + " has been disconnected.");
                clientSocket.close();
                clientSocket = null;
            } catch (IOException e) {
                log("Failure during close to " + clientIP);
            }
        }

        private void broadcast(String senderLogin, String msg, String option) {
            clients.forEach(c -> {
                if (!c.login.equals(senderLogin)){
                    if(option.equals("M"))
                        c.send("M" + senderLogin + ": ", msg);
                    else if(option.equals("S"))
                        c.send("S" + senderLogin, msg);
                    else
                        c.send("M" + senderLogin, msg);
                }
            });
        }

        private void send(String senderLogin, String msg) {
            outputStream.println(senderLogin + msg);
        }
    }
}