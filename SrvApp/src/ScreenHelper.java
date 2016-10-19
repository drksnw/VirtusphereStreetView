import java.awt.*;

/**
 * Helper class for Screen related functions.
 * @author Guillaume Petitpierre
 * @version 1.0, 06/10/16
 */
public class ScreenHelper {

    /**
     * Gets the distance from the center on the X-axis
     * @param mousePosX The mouse pointer position on the X axis (On the screen's referential)
     * @return Distance from the center
     */
    public static int getDeltaFromCenterX(int mousePosX){
        return mousePosX - getCenterX();
    }

    /**
     * Gets the distance from the center on the Y-axis
     * @param mousePosY The mouse pointer position on the Y-axis (Screen's referential)
     * @return Distance from the center
     */
    public static int getDeltaFromCenterY(int mousePosY){
        return mousePosY - getCenterY();
    }

    /**
     * Gets the center of the screen on X-axis
     * @return Center of the screen (X-axis)
     */
    public static int getCenterX(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return screenSize.width/2;
    }

    /**
     * Gets the center of the screen on Y-axis
     * @return Center of the screen (Y-axis)
     */
    public static int getCenterY(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return screenSize.height/2;
    }

}
