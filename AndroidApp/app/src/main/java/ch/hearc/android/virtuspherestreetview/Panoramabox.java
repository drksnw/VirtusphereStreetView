package ch.hearc.android.virtuspherestreetview;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Created by drksnw on 12/2/16.
 */

public class Panoramabox {

    public static final int BOX_NOT_CREATED = -1;
    public static final int BOX_BEING_CREATED = 0;
    public static final int BOX_CREATED = 1;

    private MainActivity myParent;
    private float posX,posY;

    private String lat, lon;
    private int textureId;

    private int created = BOX_NOT_CREATED;

    private FloatBuffer myVertices;
    private FloatBuffer myNormals;
    private IntBuffer myIndices;

    private int myProgram;
    private int mySamplerParam;
    private int myPositionParam;
    private int myTextureParam;
    private int myModelViewParam;
    private int myModelViewProjectionParam;
    private int myLightPosParam;

    private Bitmap[] myTextures;

    private float[] modelView = new float[16];

    public boolean finishedDLPics = false;

    public int isCreated(){
        return created;
    }

    public float getPosX() {
        return posX;
    }

    public float getPosY() {
        return posY;
    }

    public String getLat() {
        return lat;
    }

    public String getLon() {
        return lon;
    }

    private static void checkGLError(String label){
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR){
            Log.e(label, "glerror: "+error);
        }
    }

    public Panoramabox(String lat, String lon, int textureId, float posX, float posY, MainActivity myParent, boolean createNow){
        this.lat = lat;
        this.lon = lon;
        this.textureId = textureId;
        this.posX = posX;
        this.posY = posY;
        this.myParent = myParent;

        Matrix.setIdentityM(modelView, 0);
        Matrix.translateM(modelView, 0, posX, 0, posY);

        try {
            myTextures = (Bitmap[]) new GetStreetViewTask(this).execute("GET_PANO", lat, lon).get();
        } catch(Exception ex){
            ex.printStackTrace();
        }
        if(createNow){
            create();
        }


        Log.d("Panoramabox", "New panorama box created for position "+lat+", "+lon+" at virtuals X: "+posX+" and Y: "+posY);
    }

    public void create(){
        ByteBuffer bbMyVertices = ByteBuffer.allocateDirect(WorldData.SKYBOX_COORDS.length*4);
        bbMyVertices.order(ByteOrder.nativeOrder());
        myVertices = bbMyVertices.asFloatBuffer();
        myVertices.put(WorldData.SKYBOX_COORDS);
        myVertices.position(0);

        ByteBuffer bbMyIndices = ByteBuffer.allocateDirect(WorldData.SKYBOX_INDICES.length*4);
        bbMyIndices.order(ByteOrder.nativeOrder());
        myIndices = bbMyIndices.asIntBuffer();
        myIndices.put(WorldData.SKYBOX_INDICES);
        myIndices.position(0);

        ByteBuffer bbMyNormals = ByteBuffer.allocateDirect(WorldData.SKYBOX_NORMALS.length*4);
        bbMyNormals.order(ByteOrder.nativeOrder());
        myNormals = bbMyNormals.asFloatBuffer();
        myNormals.put(WorldData.SKYBOX_NORMALS);
        myNormals.position(0);





        myProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(myProgram, myParent.vertexShader);
        GLES20.glAttachShader(myProgram, myParent.fragmentShader);
        GLES20.glLinkProgram(myProgram);
        GLES20.glUseProgram(myProgram);

        MainActivity.checkGLError("Create Program");

        myLightPosParam = GLES20.glGetUniformLocation(myProgram, "uLightPos");
        myPositionParam = GLES20.glGetAttribLocation(myProgram, "aPosition");
        myModelViewParam = GLES20.glGetUniformLocation(myProgram, "uMVMatrix");
        myModelViewProjectionParam = GLES20.glGetUniformLocation(myProgram, "uMVP");
        mySamplerParam = GLES20.glGetUniformLocation(myProgram, "uSampler");

        MainActivity.checkGLError("Get Locations");


        //while(!finishedDLPics);

        int[] texIds = new int[1];
        GLES20.glGenTextures(1, texIds, 0);
        myTextureParam = texIds[0];

        MainActivity.checkGLError("Gen Textures");

        GLES20.glActiveTexture(textureId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, myTextureParam);

        for(int i=0; i<6; i++){
            GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GLES20.GL_RGBA, myTextures[i], 0);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }

        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_CUBE_MAP);
        GLES20.glUniform1i(mySamplerParam, textureId-GLES20.GL_TEXTURE0);
        MainActivity.checkGLError("Created Mipmap + set sampler");

        created = BOX_CREATED;
    }

    public void draw(){
        GLES20.glUseProgram(myProgram);
        MainActivity.checkGLError("Use program");
        GLES20.glActiveTexture(textureId);
        MainActivity.checkGLError("Active texture");

        GLES20.glUniform3fv(myLightPosParam, 1, myParent.lightPosInEyeSpace, 0);
        MainActivity.checkGLError("Set lightpos");
        GLES20.glUniformMatrix4fv(myModelViewParam, 1, false, modelView, 0);
        MainActivity.checkGLError("Set modelview");
        GLES20.glUniformMatrix4fv(myModelViewProjectionParam, 1, false, myParent.modelViewProjection, 0);
        MainActivity.checkGLError("Set projection");

        GLES20.glVertexAttribPointer(myPositionParam, MainActivity.COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, myVertices);
        GLES20.glEnableVertexAttribArray(myPositionParam);
        MainActivity.checkGLError("Set vertices");

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, myPositionParam);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, WorldData.SKYBOX_INDICES.length, GLES20.GL_UNSIGNED_INT, myIndices);
        MainActivity.checkGLError("draw elements");
    }
}
