package ch.hearc.android.virtuspherestreetview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.logging.nano.Vr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by drksnw on 11/17/16.
 */

public class GetStreetViewTask extends AsyncTask<String, Void, Object>{

    private Panoramabox pa;

    public GetStreetViewTask() {}

    public GetStreetViewTask(Panoramabox pa){
        this.pa = pa;
    }

    @Override
    protected void onPostExecute(Object bitmaps) {
        super.onPostExecute(bitmaps);
        if(bitmaps instanceof Bitmap[]){
            pa.finishedDLPics = true;
        }
    }

    @Override
    protected Object doInBackground(String... strings){
        if(strings[0].equals("GET_PANO")){
            try{
                String lat = strings[1];
                String lon = strings[2];
                //Front image
                URL url = new URL(Config.STREET_VIEW_BASE_URL+"&location="+lat+","+lon+"&heading=180&fov=92");
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                Bitmap front = BitmapFactory.decodeStream(in);

                //Back image
                url = new URL(Config.STREET_VIEW_BASE_URL+"&location="+lat+","+lon+"&heading=0&fov=92");
                urlConnection = (HttpsURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                Bitmap back = BitmapFactory.decodeStream(in);

                //Right Image
                url = new URL(Config.STREET_VIEW_BASE_URL+"&location="+lat+","+lon+"&heading=270&fov=92");
                urlConnection = (HttpsURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                Bitmap right = BitmapFactory.decodeStream(in);

                //Left Image
                url = new URL(Config.STREET_VIEW_BASE_URL+"&location="+lat+","+lon+"&heading=90&fov=92");
                urlConnection = (HttpsURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                Bitmap left = BitmapFactory.decodeStream(in);

                //Top Image
                url = new URL(Config.STREET_VIEW_BASE_URL+"&location="+lat+","+lon+"&heading=90&fov=92&pitch=90");
                urlConnection = (HttpsURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                Bitmap top = BitmapFactory.decodeStream(in);

                //Bottom Image
                url = new URL(Config.STREET_VIEW_BASE_URL+"&location="+lat+","+lon+"&heading=90&fov=92&pitch=-90");
                urlConnection = (HttpsURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
                Bitmap bottom = BitmapFactory.decodeStream(in);

                //Log.d("texts", "For pos "+lat+" "+lon+", got: "+(front != null ? "front " : "")+(back != null ? "back " : "")+(top != null ? "top " : "")+(bottom != null ? "bottom " : "")+(left != null ? "left " : "")+(right != null ? "right " : ""));

                Bitmap[] textures = {front, back, top, bottom, left, right};
                return textures;
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        } else if(strings[0].equals("GET_ADJ")){
            String lat = strings[1];
            String lon = strings[2];
            try {
                URL url = new URL(Config.STREET_VIEW_CBK_URL + lat + "," + lon);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = br.readLine()) != null){
                    sb.append(line);
                }
                JSONObject obj = new JSONObject(sb.toString());
                JSONArray adjPanosJSON = obj.getJSONArray("Links");
                String[][] adjPanos = new String[adjPanosJSON.length()][3];
                for(int i=0; i<adjPanosJSON.length(); i++){
                    JSONObject pano = adjPanosJSON.getJSONObject(i);
                    String panoId = pano.getString("panoId");
                    URL metadataURL = new URL(Config.STREET_VIEW_METADATA_URL+panoId);
                    HttpsURLConnection metadataConnection = (HttpsURLConnection)metadataURL.openConnection();
                    BufferedInputStream metadataIn = new BufferedInputStream(metadataConnection.getInputStream());
                    BufferedReader metadataBr = new BufferedReader(new InputStreamReader(metadataIn));
                    StringBuilder metadataSb = new StringBuilder();
                    String metadataLine;
                    while((metadataLine = metadataBr.readLine()) != null){
                        metadataSb.append(metadataLine);
                    }
                    JSONObject metadataObj = new JSONObject(metadataSb.toString());
                    adjPanos[i][0] = metadataObj.getJSONObject("location").getString("lat");
                    adjPanos[i][1] = metadataObj.getJSONObject("location").getString("lng");
                    adjPanos[i][2] = pano.getString("yawDeg");
                }
                return adjPanos;


            } catch(MalformedURLException ex){
                ex.printStackTrace();
            } catch(IOException ex){
                ex.printStackTrace();
            } catch(JSONException ex){
                ex.printStackTrace();
            }
        }

        return null;
    }
}
