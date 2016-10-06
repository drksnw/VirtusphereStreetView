import java.awt.*;

/**
 * Created by drksnw on 10/6/16.
 */
public class ScreenHelper {

    public static int getDeltaFromCenterX(int mousePosX){
        return mousePosX - getCenterX();
    }

    public static int getDeltaFromCenterY(int mousePosY){
        return mousePosY - getCenterY();
    }

    public static int getCenterX(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return screenSize.width/2;
    }

    public static int getCenterY(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return screenSize.height/2;
    }

}
