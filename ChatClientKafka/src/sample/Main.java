package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
	@SuppressWarnings("all")
    @Override
    public void start(Stage primaryStage) throws Exception{

        System.out.println("Client started");
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Chat v1.1 Kafka");
        primaryStage.setScene(new Scene(root, 770, 550));//setting window size
        primaryStage.setResizable(false);//prevent window resizing
        primaryStage.show();
        

    }


    public static void main(String[] args)
    {
        launch(args);
    }
}
