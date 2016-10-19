package ch.hearc.android.virtuspherestreetview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import ch.hearc.android.virtuslib.Virtusphere;
import ch.hearc.android.virtuslib.VirtusphereEvent;

public class MainActivity extends AppCompatActivity {

    TextView lblX;
    TextView lblY;
    Virtusphere vsphere;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblX = (TextView)findViewById(R.id.lblX);
        lblY = (TextView)findViewById(R.id.lblY);
        vsphere = new Virtusphere();

        vsphere.onEvent(new VirtusphereEvent() {
            @Override
            public void moved(int x, int y) {
                changeXLabel(Integer.toString(x));
                changeYLabel(Integer.toString(y));
            }

            @Override
            public void disconnected() {
                Log.i("Virtusphere", "disconnected");
            }
        });

        Thread virtusThread = new Thread(new Runnable() {
            @Override
            public void run() {


                vsphere.connect("157.26.109.198", 28000); //Hardcoded IP, to change  !!
            }
        });

        virtusThread.start();

    }

    public void changeXLabel(final String val){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lblX.setText(val);
            }
        });
    }

    public void changeYLabel(final String val){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lblY.setText(val);
            }
        });
    }
}
