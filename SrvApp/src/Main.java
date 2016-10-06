/**
 * Created by drksnw on 10/6/16.
 */


import com.sun.javafx.robot.FXRobot;
import com.sun.javafx.robot.FXRobotFactory;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import sun.security.util.SecurityConstants;

import java.awt.*;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try{
            primaryStage.setTitle("Hello World !");
            Label lbl = new Label();
            lbl.setText("Capturing mouse. Press Q to quit.");

            StackPane root = new StackPane();
            root.getChildren().add(lbl);
            Scene sc = new Scene(root, 500, 500);
            primaryStage.setScene(sc);

            Robot robot = new Robot();

            sc.setOnMouseMoved(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    System.out.println("DeltaX: "+ ScreenHelper.getDeltaFromCenterX((int)event.getScreenX())+" DeltaY: "+ ScreenHelper.getDeltaFromCenterY((int)event.getScreenY()));
                    robot.mouseMove(ScreenHelper.getCenterX(), ScreenHelper.getCenterY());
                }
            });

            sc.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if(event.getCode() == KeyCode.Q){
                        System.exit(0);
                    }
                }
            });


            primaryStage.show();
        } catch (AWTException e){
            e.printStackTrace();
        }

    }
}
