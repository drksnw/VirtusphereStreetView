package ch.hearc.android.virtuspherestreetview;

/**
 * Created by drksnw on 10/20/16.
 */

public class WorldData {

    public static final float[] SKYBOX_COORDS = new float[] {
            -10.0f, -10.0f, 10.0f,
            10.0f, -10.0f, 10.0f,
            10.0f, 10.0f, 10.0f,
            -10.0f, 10.0f, 10.0f,
            -10.0f, -10.0f, -10.0f,
            -10.0f, 10.0f, -10.0f,
            10.0f, 10.0f, -10.0f,
            10.0f, -10.0f, -10.0f,
            -10f, 10f, -10f,
            -10f, 10f, 10f,
            10f, 10f, 10f,
            10f, 10f, -10f,
            -10f, -10f, -10f,
            10f, -10f, -10f,
            10f, -10f, 10f,
            -10f, -10f, 10f,
            10f, -10f, -10f,
            10f, 10f, -10f,
            10f, 10f, 10f,
            10f, -10f, 10f,
            -10f, -10f, -10f,
            -10f, -10f, 10f,
            -10f, 10f, 10f,
            -10f, 10f, -10f
    };

    public static final float[] SKYBOX_NORMALS = new float[] {
            0f, 0f, 1f,
            0f, 0f, -1f,
            0f, 1f, 0f,
            0f, -1f, 0f,
            1f, 0f, 0f,
            -1f, 0f, 0f

    };

    public static final int[] SKYBOX_INDICES = new int[] {0,1,2,0,2,3,4,5,6,4,6,7,8,9,10,8,10,11,12,13,14,12,14,15,16,17,18,16,18,19,20,21,22,20,22,23};

}
