package udp;

import java.io.*;
import java.net.*;

/**
 * Created by Tomasz Guzik on 2017-03-12.
 *
 * ZAD 2
 * Symulacja przesyłu danych multimedialnych
 * Serwer oraz każdy klient otwierają dodatkowy kanał UDP (ten sam numer portu jak przy TCP)
 * Po wpisaniu komendy ‘M’ u klienta przesyłana jest wiadomość przez UDP, która symuluje multimedia (np. ASCII Art)
 */
public class ChatServerUDP {
    private static final int PORT = 9999;
    private final int PACKET_SIZE = 1024;
    private final int TIMEOUT = 10000;
    private volatile boolean isShutDown = false;

    public void runServer() {
        byte buffer[] = new byte[PACKET_SIZE];
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            serverSocket.setSoTimeout(TIMEOUT);
            while(true){
                try{
                    if (isShutDown) return;

                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(request);
                    String requestData = new String(request.getData(), 0 , request.getLength());

                    String answer = "Got it.";
                    DatagramPacket response = new DatagramPacket(answer.getBytes(),
                            answer.length(), request.getAddress(), request.getPort());
                    serverSocket.send(response);
                } catch (SocketTimeoutException e){
                    if (isShutDown) return;
                    log("SocketTimeoutException " + e);
                } catch (IOException e){
                    log("IOException " + e);
                }
            }
        } catch (SocketException e) {
            log("SocketException " + e);
        }
    }

    public void shutDown(){
        isShutDown = true;
    }

    private static void log(String s) {
        System.out.println(s);
    }
}
