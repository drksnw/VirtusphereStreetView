import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by drksnw on 10/27/16.
 */
public class DiscoveryThread implements Runnable {

    DatagramSocket socket;

    @Override
    public void run(){
        try{
            socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            System.out.println("[DISCOVER] Discovery Server running on port 8888");

            while(true){
                System.out.println("[DISCOVER] Ready to receive packets.");

                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                System.out.println("[DISCOVER] Discovery packet received from: "+packet.getAddress().getHostAddress());

                String message = new String(packet.getData()).trim();

                if(message.equals("DISCOVER_VIRTUSPHERE_REQ")){
                    byte[] sendData = "DISCOVER_VIRTUSPHERE_RES".getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                    socket.send(sendPacket);
                    System.out.println("[DISCOVER] Packet sent.");
                }
            }
        } catch(IOException ex){

        }
    }

    public static DiscoveryThread getInstance(){
        return DiscoveryThreadHolder.INSTANCE;
    }

    private static class DiscoveryThreadHolder {
        private static final DiscoveryThread INSTANCE = new DiscoveryThread();
    }
}
