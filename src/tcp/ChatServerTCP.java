package tcp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Tomasz Guzik on 2017-03-10.
 *
 * ZAD 1
 * Klienci łączą się serwerem przez protokół TCP
 * Serwer przyjmuje wiadomości od każdego klienta i rozsyła je do pozostałych (wraz z id/nickiem klienta)
 * Serwer jest wielowątkowy – każde połączenie od klienta powinno mieć swój wątek
 * Proszę zwrócić uwagę na poprawną obsługę wątków
 */
public class ChatServerTCP {
    private static final int PORT = 9999;

    private ServerSocket serverSocket;
    private List<ClientHandler> clients;

    public ChatServerTCP() throws IOException {
        clients = new LinkedList<>();
        serverSocket = new ServerSocket(PORT);
    }

    public void runServer() {
        while(true){
            try {
                Socket newClient = serverSocket.accept();
                String hostName = newClient.getInetAddress().getHostName();
                log("New user from " + hostName);

                ClientHandler clientHandler = new ClientHandler(newClient, hostName);

                synchronized (clients) {
                    clients.add(clientHandler);
                }

                clientHandler.start();
                clientHandler.send("", "Welcome to C.H.A.T. !");


            } catch (IOException e){
                log("IO Exception on server: " + e);
            }
        }
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
                log("User from " + clientSocket.getInetAddress().getHostName() + " has been disconnected.");
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
