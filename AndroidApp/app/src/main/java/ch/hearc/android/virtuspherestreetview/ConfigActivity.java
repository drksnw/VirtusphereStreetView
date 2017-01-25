package ch.hearc.android.virtuspherestreetview;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;

public class ConfigActivity extends AppCompatActivity {

    Button btnLaunch;
    EditText txtServerIp;
    EditText txtLat;
    EditText txtLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        txtServerIp = (EditText)findViewById(R.id.txt_ip_server);
        btnLaunch = (Button)findViewById(R.id.btn_launch);

        txtLat = (EditText)findViewById(R.id.txt_lat);
        txtLon = (EditText)findViewById(R.id.txt_lon);

        btnLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matcher ipAddrMatcher = Patterns.IP_ADDRESS.matcher(txtServerIp.getText());
                if(ipAddrMatcher.matches()){
                    //Entered text is a valid IP address
                    if(Math.abs(Double.parseDouble(txtLat.getText().toString())) <= 85 && Math.abs(Double.parseDouble(txtLon.getText().toString())) <= 180){
                        //Valid Latitude & Longitude
                        Intent in = new Intent(getApplicationContext(), MainActivity.class);
                        in.putExtra("srv_ip", txtServerIp.getText().toString());
                        in.putExtra("lat", txtLat.getText().toString());
                        in.putExtra("lon", txtLon.getText().toString());
                        startActivity(in);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "La longitude ou/et la longitude n'est pas valide", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "L'addresse entrée n'est pas valide", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }
}
