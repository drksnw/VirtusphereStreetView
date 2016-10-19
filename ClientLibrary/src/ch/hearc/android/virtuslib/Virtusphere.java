package ch.hearc.android.virtuslib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
