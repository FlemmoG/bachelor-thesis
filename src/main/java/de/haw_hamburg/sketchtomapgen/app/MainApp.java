package de.haw_hamburg.sketchtomapgen.app;

import de.haw_hamburg.sketchtomapgen.controller.MainController;
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
  public void start(Stage primaryStage) throws Exception {
    // MainView laden
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/haw_hamburg/sketchtomapgen/fxml/MainView.fxml"));
    Parent mainView = loader.load();

    // Controller holen und die Stage setzen
    MainController controller = loader.getController();
    controller.setStage(primaryStage);

    // Scene anzeigen
    primaryStage.setScene(new Scene(mainView, 800, 600));
    primaryStage.setTitle("Sketch To Map Generator");
    primaryStage.show();
  }
}
