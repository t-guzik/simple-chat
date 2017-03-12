package udp;

import asciiart.AsciiArt;
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
public class ClientUDP {
    /** Socket data */
    private final int PORTNUM = 9999;
    private final int PACKET_SIZE = 1024;
    private final int TIMEOUT = 10000;
    private volatile boolean stopped = false;

    public void send() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(TIMEOUT);
            datagramSocket.connect(InetAddress.getLocalHost(), PORTNUM);

            /** SENDER */
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            if (stopped) return;

                            byte[] data = AsciiArt.getArt().getBytes();
                            DatagramPacket request = new DatagramPacket(data,
                                    data.length, InetAddress.getLocalHost(), PORTNUM);
                            datagramSocket.send(request);
                            Thread.yield();
                        }
                    } catch (IOException e) {
                        log("IOException " + e);
                    }
                }
            }).start();

            /** RECEIVER */
            new Thread(new Runnable() {
                public void run() {
                    byte[] buffer = new byte[PACKET_SIZE];

                    while (true) {
                        if (stopped) return;

                        DatagramPacket response = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
                        try {
                            datagramSocket.receive(response);
                            String result = new String(response.getData(), 0, response.getLength());
                            log(result);
                            Thread.yield();
                        } catch(IOException e){
                            log("IOException " + e);
                        }
                    }
                }
            }).start();
        } catch (UnknownHostException e) {
            log("UnknownHostException " + e);
        } catch (IOException e) {
            log("IOException " + e);
        }
    }

    public void logout() {
        stopped = true;
    }

    private void log(String message) {
        System.out.println(message);
    }
}
