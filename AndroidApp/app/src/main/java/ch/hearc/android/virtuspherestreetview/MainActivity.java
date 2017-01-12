package ch.hearc.android.virtuspherestreetview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.common.logging.nano.Vr;
import com.google.vr.cardboard.AndroidNCompat;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.microedition.khronos.egl.EGLConfig;

import ch.hearc.android.virtuslib.Virtusphere;
import ch.hearc.android.virtuslib.VirtusphereEvent;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer{
    //App specific attributes
    private Vibrator vibrator;
    private Virtusphere virtusphere;

    //OpenGL scene constants
    private static final float CAMERA_Z = 0.0f;
    private static final float Z_NEAR = .1f;
    private static final float Z_FAR = 100f;
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {0.0f, 0.0f, 0.0f, 0.0f};
    public static final int COORDS_PER_VERTEX = 3;

    public final float[] lightPosInEyeSpace = new float[4];

    //OpenGL buffers
    private FloatBuffer boxVertices;
    private FloatBuffer boxColors;
    private FloatBuffer boxNormals;
    private IntBuffer boxIndices;
    private ByteBuffer boxTexture;

    //OpenGL pointers
    private int boxProgram;
    private int boxPositionParam;
    private int boxNormalParam;
    private int boxColorParam;
    private int boxIndicesParam;
    private int boxTextureParam = -1;
    private int boxModelParam;
    private int boxModelViewParam;
    private int boxModelViewProjectionParam;
    private int boxLightPosParam;

    //OpenGL matrices
    private float[] camera;
    private float[] view;
    private float[] headView;
    public float[] modelViewProjection;
    public float[] modelView;
    private float[] modelBox;
    private float[] headRotation;

    private int skyboxId = 0;
    private int oldSkybox = skyboxId;

    //OpenGL parameters
    private float floorDepth = 20f;

    public boolean imgLoaded = false;
    private boolean skyboxSet = false;

    private Bitmap[] textures;
    private HashMap<String, Panoramabox> loadedPanoramas;
    private HashSet<String> adjLoaded;



    private Bitmap getBmpFromRaw(int resid){
        InputStream is = getResources().openRawResource(resid);
        Bitmap bmp = BitmapFactory.decodeStream(is);
        return bmp;
    }

    private static void checkGLError(String label){
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR){
            Log.e(label, "glerror: "+error);
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadedPanoramas = new HashMap<>();
        adjLoaded = new HashSet<>();
        //Initializing vibrator
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        //Initialize Cardboard API
        GvrView gvrView = (GvrView)findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8,8,8,8,16,8);
        gvrView.setRenderer(this);

        if(gvrView.setAsyncReprojectionEnabled(true)){
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }
        setGvrView(gvrView);

        //Initializing matrices
        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        modelBox = new float[16];
        headRotation = new float[4];
        headView = new float[16];

        //Putting the camera at the right place
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        //Initializing Virtusphere
        virtusphere = new Virtusphere();

        virtusphere.onEvent(new VirtusphereEvent() {
            @Override
            public void moved(int i, int i1) {
                //TODO: Implement with StreetView
                Matrix.translateM(camera, 0, (float)i/10, 0, (float)i1/10);

                float userX = -camera[12];
                float userY = -camera[14];

                Log.d("pos user", ""+userX+" "+userY);

                for(Panoramabox pb : loadedPanoramas.values()){

                    if(Math.abs(pb.getPosX() - userX) < 10 && Math.abs(pb.getPosY() - userY) < 10){
                        loadPanorama(pb.getLat(), pb.getLon(), pb.getPosX(), pb.getPosY());
                    }
                }
            }

            @Override
            public void disconnected() {
                Log.i("Virtusphere", "Disconnected");
            }

            @Override
            public void sphereExited() {
                Log.i("Virtusphere", "Exited from capture window");
                vibrator.vibrate(500);
            }
        });

        //Connection method in another thread
        //Android doesn't allow network operations on UI thread
        Thread virtusThread = new Thread(new Runnable() {
            @Override
            public void run() {
                virtusphere.connect("157.26.106.89", 28000);
            }
        });
        virtusThread.start();


    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        headTransform.getHeadView(headView, 0);
        headTransform.getQuaternion(headRotation, 0);
    }

    public String beautifyArray(float[] array){
        String s = "{";
        for(float f : array){
            s += " "+f;
        }
        s += " }";
        return s;
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError("colorParam");

        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);
        float perspective[] = eye.getPerspective(Z_NEAR, Z_FAR);


        Matrix.multiplyMM(modelView, 0, view, 0, modelBox, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);


        for(Panoramabox pb : loadedPanoramas.values()){
            pb.draw();
        }

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        GLES20.glClearColor(.1f, .1f, .1f, .5f);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        loadPanorama("47.1508206","6.9934307", 0, 0);

        checkGLError("Program attributes");

        Matrix.setIdentityM(modelBox, 0);
        Matrix.translateM(modelBox, 0, 0, 0, 0);

        Matrix.setIdentityM(camera, 0);

        checkGLError("onSurfaceCreated");
    }


    private void loadPanorama(String lat, String lon, float posX, float posY){
        loadPanorama(lat, lon, true, posX, posY);
    }

    private void loadPanorama(String lat, String lon, boolean loadAdj, float posX, float posY){
        if(!adjLoaded.contains(lat+","+lon)) {
            try {
                String[][] adj = (String[][]) new GetStreetViewTask().execute("GET_ADJ", lat, lon).get();
                if (!loadedPanoramas.containsKey(lat + "," + lon)) {

                    int textureId = GLES20.GL_TEXTURE0 + (loadedPanoramas.size() % 32);
                    loadedPanoramas.put(lat + "," + lon, new Panoramabox(lat, lon, textureId, posX, posY, this));
                }
                if (loadAdj) {
                    for (int i = 0; i < adj.length; i++) {
                        float[] newPos = getNewCenterWithAngle(posX, posY, Double.parseDouble(adj[i][2]));
                        loadPanorama(adj[i][0], adj[i][1], false, newPos[0], newPos[1]);
                    }
                    adjLoaded.add(lat + "," + lon);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private float[] getNewCenterWithAngle(float oldX, float oldY, double angle){
        float[] result = new float[2];
        float newX, newY;

        // 90 + (360 - angle) -> si >= 360 => -360
        double angleFromXAxis = 90 + (360 - angle) >= 360 ? 90 + (360 - angle) - 360 : 90 + (360 - angle);
        float slope = -((float)Math.tan(Math.toRadians(angleFromXAxis)));
        // oldY = slope*oldX + h (y=mx+h)
        // oldX = (oldY - h) / slope
        float h = oldY - slope*oldX;

        // Angle > 45 && Angle <= 135 -> top side (y = oldY + 10)
        // Angle > 135 && Angle <= 225 -> left side (x = oldX - 10)
        // Angle > 225 && Angle <= 315 -> bottom side (y = oldY - 10)
        // Else => Angle between 45 and -45 -> left side (x = oldX + 10)

        if(angleFromXAxis > 45 && angleFromXAxis <= 135){
            // slope*newX + h = oldY + 10
            newX = (oldY - 20.1f - h)/slope;
            newY = oldY - 20.1f;
        } else if(angleFromXAxis > 135 && angleFromXAxis <= 225){
            // (newY - h) / slope = oldX - 10
            newY = (oldX - 20.1f) * slope + h;
            newX = oldX - 20.1f;
        } else if(angleFromXAxis > 225 && angleFromXAxis <= 315){
            // slope*newX + h = oldY - 10
            newX = (oldY + 20.1f - h)/slope;
            newY = oldY + 20.1f;
        } else {
            // (newY - h) / slope = oldX + 10
            newY = (oldX + 20.1f) * slope + h;
            newX = oldX + 20.1f;
        }

        result[0] = newX;
        result[1] = newY;
        return result;
    }

    @Override
    public void onRendererShutdown() {

    }

    @Override
    public void onCardboardTrigger(){
    }
}
