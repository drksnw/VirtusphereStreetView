import ch.hearc.android.virtuslib.Virtusphere;
import ch.hearc.android.virtuslib.VirtusphereEvent;

/**
 * Created by drksnw on 10/19/16.
 */
public class Main {
    // !!! THIS CLASS IS ONLY FOR DEBUG PURPOSES !!!
    // !!! DO NOT COMPILE IN LIBRARY !!!

    public static void main(String[] args) {
        Virtusphere virtusphere = new Virtusphere();

        virtusphere.onEvent(new VirtusphereEvent() {
            @Override
            public void moved(int x, int y) {
                System.out.printf("Got movement information: X: %d, Y: %d\n", x, y);
            }

            @Override
            public void disconnected() {
                System.err.println("Virtusphere disconnected.");
            }

            @Override
            public void sphereExited(){
                System.err.println("Pointer is out of capture window !");
            }
        });

        virtusphere.discoverAndConnect();
    }
}
