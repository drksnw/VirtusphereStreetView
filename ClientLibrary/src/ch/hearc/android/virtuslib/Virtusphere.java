package ch.hearc.android.virtuslib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Enumeration;

/**
 * Created by drksnw on 10/19/16.
 */
public class Virtusphere {

    private VirtusphereEvent ev = null;

    public Virtusphere(){

    }

    public void connect(String ip, int port){
        try{
            NetworkListener nl = new NetworkListener(new Socket(ip, port));
            Thread t = new Thread(nl);
            t.start();
        } catch(IOException ex){
            System.err.println("Can't connect to the Virtusphere !");
        }
    }

    public void discoverAndConnect(){
        try{
            DatagramSocket c = new DatagramSocket();
            c.setBroadcast(true);

            byte[] sendData = "DISCOVER_VIRTUSPHERE_REQ".getBytes();

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while(interfaces.hasMoreElements()){
                NetworkInterface netif = interfaces.nextElement();

                if(netif.isLoopback() || !netif.isUp()){
                    continue; //Inutile d'envoyer un packet sur lo. Ou sur une inteface down.
                }

                for(InterfaceAddress ifAdd : netif.getInterfaceAddresses()){
                    InetAddress broadcast = ifAdd.getBroadcast();
                    System.out.println(broadcast);
                    if(broadcast == null){
                        continue;
                    }

                    System.out.println("Sending broadcast packet to: "+broadcast.getHostAddress());

                    try{
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, 8888);
                        c.send(sendPacket);
                        System.out.println("PACKET SENT TO "+sendPacket.getAddress().getHostAddress());
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }

            byte[] recvBuf = new byte[15000];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            c.receive(receivePacket);

            String message = new String(receivePacket.getData()).trim();
            System.out.println("Message received from "+receivePacket.getAddress().getHostAddress());
            if(message.equals("DISCOVER_VIRTUSPHERE_RES")){
                connect(receivePacket.getAddress().getHostAddress(), 28000);
            }
        } catch(IOException ex){
            ex.printStackTrace();

        }
    }

    public void onEvent(VirtusphereEvent ev){
        this.ev = ev;
    }

    private class NetworkListener implements Runnable{

        private Socket s;
        private PrintWriter out;
        private BufferedReader in;

        public NetworkListener(Socket s){
            try {
                this.s = s;
                out = new PrintWriter(s.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            } catch(IOException ex){
                System.err.println("Can't open streams !");
            }
        }

        @Override
        public void run(){
            String newLine;
            try {
                while ((newLine = in.readLine()) != null) {
                    String[] command = newLine.split("#");
                    switch (command[0]) {
                        case "MOVE":
                            String[] coords = command[1].split(";");
                            try {
                                ev.moved(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                            } catch (NullPointerException ex) {
                                System.err.println("Event not initialized !");
                            }
                            break;
                        case "MEXITED":
                            ev.sphereExited();
                            break;
                        default:
                            System.err.println("Unrecognized command !");
                            break;
                    }
                }
                ev.disconnected();
            } catch(IOException ex){
                System.err.println("Can't read from server !");
            }
        }
    }
}
