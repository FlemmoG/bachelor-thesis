package de.haw_hamburg.sketchtomapgen.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/haw_hamburg/sketchtomapgen/fxml/MainView.fxml"));
      Scene scene = new Scene(loader.load());
      primaryStage.setTitle("Sketch-to-Map Generator");
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
