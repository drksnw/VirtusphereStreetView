/**
 * Created by drksnw on 10/6/16.
 */


import com.sun.javafx.binding.StringFormatter;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.awt.*;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        //Starting the server
        TCPServer server = new TCPServer();

        //Server contains an infinite loop, so putting it into a thread.
        Thread serverThread = new Thread(server);
        serverThread.start();

        //Discovery server thread
        Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
        discoveryThread.start();

        try{
            primaryStage.setTitle("Virtusphere Server App");
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
                    //Getting deltas
                    int dx = ScreenHelper.getDeltaFromCenterX((int)event.getScreenX());
                    int dy = ScreenHelper.getDeltaFromCenterY((int)event.getScreenY());


                    server.sendToAll(String.format("MOVE#%d;%d",dx,dy));

                    //Sets the mouse pointer on the center of the screen
                    robot.mouseMove(ScreenHelper.getCenterX(), ScreenHelper.getCenterY());
                }
            });

            sc.setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    server.sendToAll("MEXITED#0");
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
