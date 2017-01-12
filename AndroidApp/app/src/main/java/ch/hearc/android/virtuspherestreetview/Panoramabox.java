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

    private MainActivity myParent;
    private float posX,posY;

    private String lat, lon;
    private int textureId;

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

    /**
     * Reads a file stored in R.raw. Used to load shaders.
     * @param resId Resource ID (R.raw.file_name)
     * @return A String with the contents of the file
     */
    private String readRawTextFile(int resId){
        InputStream inputStream = myParent.getResources().openRawResource(resId);
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

    private static void checkGLError(String label){
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR){
            Log.e(label, "glerror: "+error);
        }
    }

    public Panoramabox(String lat, String lon, int textureId, float posX, float posY, MainActivity myParent){
        this.lat = lat;
        this.lon = lon;
        this.textureId = textureId;
        this.posX = posX;
        this.posY = posY;
        this.myParent = myParent;

        Matrix.setIdentityM(modelView, 0);
        Matrix.translateM(modelView, 0, posX, 0, posY);


        create();

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



        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.vertex);
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.fragment);

        myProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(myProgram, vertexShader);
        GLES20.glAttachShader(myProgram, fragmentShader);
        GLES20.glLinkProgram(myProgram);
        GLES20.glUseProgram(myProgram);

        myLightPosParam = GLES20.glGetUniformLocation(myProgram, "uLightPos");
        myPositionParam = GLES20.glGetAttribLocation(myProgram, "aPosition");
        myModelViewParam = GLES20.glGetUniformLocation(myProgram, "uMVMatrix");
        myModelViewProjectionParam = GLES20.glGetUniformLocation(myProgram, "uMVP");
        mySamplerParam = GLES20.glGetUniformLocation(myProgram, "uSampler");

        try {
            myTextures = (Bitmap[])new GetStreetViewTask(this).execute("GET_PANO",lat, lon).get();
            while(!finishedDLPics);

            int[] texIds = new int[1];
            GLES20.glGenTextures(1, texIds, 0);
            myTextureParam = texIds[0];

            GLES20.glActiveTexture(textureId);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, myTextureParam);

            for(int i=0; i<6; i++){
                GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GLES20.GL_RGBA, myTextures[i], 0);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            }

            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_CUBE_MAP);
            GLES20.glUniform1i(mySamplerParam, textureId-GLES20.GL_TEXTURE0);

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void draw(){
        GLES20.glUseProgram(myProgram);
        GLES20.glActiveTexture(textureId);

        GLES20.glUniform3fv(myLightPosParam, 1, myParent.lightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(myModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(myModelViewProjectionParam, 1, false, myParent.modelViewProjection, 0);

        GLES20.glVertexAttribPointer(myPositionParam, MainActivity.COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, myVertices);
        GLES20.glEnableVertexAttribArray(myPositionParam);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, myPositionParam);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, WorldData.SKYBOX_INDICES.length, GLES20.GL_UNSIGNED_INT, myIndices);
    }
}
