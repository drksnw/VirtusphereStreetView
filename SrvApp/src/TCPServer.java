import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by drksnw on 10/19/16.
 */
public class TCPServer implements Runnable{

    ServerSocket ss;
    boolean stayAwake = true;
    ArrayList<ClientHandler> clients;

    public TCPServer(){

    }

    @Override
    public void run(){
        try{
            clients = new ArrayList<>();
            ss = new ServerSocket(28000);
            while(stayAwake){
                Socket newClient = ss.accept();
                clients.add(new ClientHandler(newClient));
                Thread t = new Thread(clients.get(clients.size()-1));
                t.start();
            }
        } catch(IOException ex){
            System.err.println("Couldn't open port 28000 !");
        } finally {
            try{
                ss.close();
            } catch(IOException ex){
                System.err.println("Can't close server.");
            }
        }
    }

    public void sendToAll(String data){
        for(ClientHandler c : clients){
            c.sendToClient(data);
        }
    }

    private class ClientHandler implements Runnable{

        PrintWriter out;
        BufferedReader in;

        public ClientHandler(Socket sc){
            try {
                out = new PrintWriter(sc.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(sc.getInputStream()));
            }catch (IOException ex){
                System.err.println("Can't open streams !");
            }
        }

        @Override
        public void run() {
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null) {
                    switch (inputLine){
                        case "PING":
                            sendToClient("PONG");
                            break;
                        default:
                            System.out.println("Got: "+inputLine);
                            break;
                    }
                }
            }catch (IOException ex){
                System.err.println("Can't read data !");
            }

        }

        public void sendToClient(String data){
            out.println(data);
        }
    }
}
