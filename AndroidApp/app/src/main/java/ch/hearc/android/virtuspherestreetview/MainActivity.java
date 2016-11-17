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

import javax.microedition.khronos.egl.EGLConfig;

import ch.hearc.android.virtuslib.Virtusphere;
import ch.hearc.android.virtuslib.VirtusphereEvent;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer{
    //App specific attributes
    private Vibrator vibrator;
    private Virtusphere virtusphere;

    //OpenGL scene constants
    private static final float CAMERA_Z = 0.0f;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {0.0f, 0.0f, 0.0f, 0.0f};
    private static final int COORDS_PER_VERTEX = 3;

    private final float[] lightPosInEyeSpace = new float[4];

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
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelBox;
    private float[] headRotation;

    private int skyboxId = 0;
    private int oldSkybox = skyboxId;

    //OpenGL parameters
    private float floorDepth = 20f;

    /**
     * Reads a file stored in R.raw. Used to load shaders.
     * @param resId Resource ID (R.raw.file_name)
     * @return A String with the contents of the file
     */
    private String readRawTextFile(int resId){
        InputStream inputStream = getResources().openRawResource(resId);
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null){
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }

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

    /**
     * Compiles raw text file into OpenGL Shader
     * @param type Shader type (Vertex or Fragment shader)
     * @param resId Resource ID of the shader source code (R.raw.shader_code)
     * @return Pointer to compiled shader
     */
    private int loadGLShader(int type, int resId){
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if(compileStatus[0] == 0){
            Log.e("Error", "Error compiling shader: "+GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if(shader == 0){
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing vibrator
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        //Initialize Cardboard API
        GvrView gvrView = (GvrView)findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8,8,8,8,16,8);
        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);

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
                virtusphere.connect("157.26.106.241", 28000);
            }
        });
        virtusThread.start();
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        headTransform.getHeadView(headView, 0);
        headTransform.getQuaternion(headRotation, 0);
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

        Matrix.setIdentityM(camera, 0);

        if(skyboxId != oldSkybox){
            setSkybox(skyboxId);
            oldSkybox = skyboxId;
        }

        drawBox();
    }

    /**
     * Draw the box
     */
    public void drawBox(){
        //Specifies the program to use
        GLES20.glUseProgram(boxProgram);

        //Sending values to shaders
        GLES20.glUniform3fv(boxLightPosParam, 1, lightPosInEyeSpace, 0);
        checkGLError("boxLightPos");
        //GLES20.glUniformMatrix4fv(boxModelParam, 1, false, modelBox, 0);
        checkGLError("boxModel");
        GLES20.glUniformMatrix4fv(boxModelViewParam, 1, false, modelView, 0);
        checkGLError("boxModelView");
        GLES20.glUniformMatrix4fv(boxModelViewProjectionParam, 1, false, modelViewProjection, 0);
        checkGLError("boxMVP");

        GLES20.glVertexAttribPointer(boxPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, boxVertices);
        checkGLError("boxPos");
        //GLES20.glVertexAttribPointer(boxNormalParam, 3, GLES20.GL_FLOAT, false, 0, boxNormals);
       // Log.d("aaa", ""+boxNormalParam);
        //checkGLError("boxNorm");
  //      GLES20.glVertexAttribPointer(boxColorParam, 4, GLES20.GL_FLOAT, false, 0, boxColors);

        GLES20.glEnableVertexAttribArray(boxPositionParam);
        checkGLError("Enable boxPos");
        //GLES20.glEnableVertexAttribArray(boxNormalParam);
        //checkGLError("Enable boxNorm");
        checkGLError("Enable boxIndices");

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, boxPositionParam);
        checkGLError("Bind elements");
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, WorldData.SKYBOX_INDICES.length, GLES20.GL_UNSIGNED_INT, boxIndices);
        checkGLError("Draw elements");

        checkGLError("Draw skybox");
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

        ByteBuffer bbBoxVertices = ByteBuffer.allocateDirect(WorldData.SKYBOX_COORDS.length*4);
        bbBoxVertices.order(ByteOrder.nativeOrder());
        boxVertices = bbBoxVertices.asFloatBuffer();
        boxVertices.put(WorldData.SKYBOX_COORDS);
        boxVertices.position(0);

        ByteBuffer bbBoxIndices = ByteBuffer.allocateDirect(WorldData.SKYBOX_INDICES.length*4);
        bbBoxIndices.order(ByteOrder.nativeOrder());
        boxIndices = bbBoxIndices.asIntBuffer();
        boxIndices.put(WorldData.SKYBOX_INDICES);
        boxIndices.position(0);

        ByteBuffer bbBoxNormals = ByteBuffer.allocateDirect(WorldData.SKYBOX_NORMALS.length*4);
        bbBoxNormals.order(ByteOrder.nativeOrder());
        boxNormals = bbBoxNormals.asFloatBuffer();
        boxNormals.put(WorldData.SKYBOX_NORMALS);
        boxNormals.position(0);

        setSkybox(0);

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.vertex);
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.fragment);

        boxProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(boxProgram, vertexShader);
        GLES20.glAttachShader(boxProgram, fragmentShader);
        GLES20.glLinkProgram(boxProgram);
        GLES20.glUseProgram(boxProgram);

        checkGLError("Box Program");

        boxModelParam = GLES20.glGetAttribLocation(boxProgram, "aPosition");
        checkGLError("Program attributes");
        boxNormalParam = GLES20.glGetAttribLocation(boxProgram, "aNormal");
        checkGLError("Program attributes");
        boxColorParam = GLES20.glGetAttribLocation(boxProgram, "aColor");
        checkGLError("Program attributes");
        boxLightPosParam = GLES20.glGetUniformLocation(boxProgram, "uLightPos");
        checkGLError("Program attributes");
        boxPositionParam = GLES20.glGetAttribLocation(boxProgram, "aPosition");
        checkGLError("Program attributes");
        boxModelViewParam = GLES20.glGetUniformLocation(boxProgram, "uMVMatrix");
        checkGLError("Program attributes");
        boxModelViewProjectionParam = GLES20.glGetUniformLocation(boxProgram, "uMVP");

        checkGLError("Program attributes");

        Matrix.setIdentityM(modelBox, 0);
        Matrix.translateM(modelBox, 0, 0, 0, 0);

        checkGLError("onSurfaceCreated");
    }

    public void setSkybox(int skyid){

        if(skyid == 0){
            Bitmap[] textures = {getBmpFromRaw(R.raw.sky0_posx), getBmpFromRaw(R.raw.sky0_negx), getBmpFromRaw(R.raw.sky0_posy), getBmpFromRaw(R.raw.sky0_negy), getBmpFromRaw(R.raw.sky0_posz), getBmpFromRaw(R.raw.sky0_negz)};

            loadSkybox(textures);
        } else if(skyid == 1){
            Bitmap[] textures = {getBmpFromRaw(R.raw.sky1_posx), getBmpFromRaw(R.raw.sky1_negx), getBmpFromRaw(R.raw.sky1_posy), getBmpFromRaw(R.raw.sky1_negy), getBmpFromRaw(R.raw.sky1_posz), getBmpFromRaw(R.raw.sky1_negz)};
            loadSkybox(textures);
        }
    }

    private void loadSkybox(Bitmap[] textures){
        if(boxTextureParam == -1) {
            int[] texIds = new int[1];
            GLES20.glGenTextures(1, texIds, 0);
            boxTextureParam = texIds[0];
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, boxTextureParam);



        for(int i=0; i<6; i++){
            GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GLES20.GL_RGBA, textures[i], 0);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_CUBE_MAP);
    }

    /*private ByteBuffer getSkybox(){
        InputStream is = getApplicationContext().getResources().openRawResource(R.raw.test_skybox);
        Bitmap bmp = BitmapFactory.decodeStream(is);
        int bytes = bmp.getByteCount();
        ByteBuffer pixels = ByteBuffer.allocate(bytes);
        bmp.copyPixelsToBuffer(pixels);

        return pixels;
    }*/

    @Override
    public void onRendererShutdown() {

    }

    @Override
    public void onCardboardTrigger(){
        Log.d("Info", "Loading skybox "+((skyboxId+1)%2));
        skyboxId = ++skyboxId%2;
    }
}
