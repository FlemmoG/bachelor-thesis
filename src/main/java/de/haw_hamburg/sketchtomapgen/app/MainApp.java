package de.haw_hamburg.sketchtomapgen.app;

import de.haw_hamburg.sketchtomapgen.controller.NavigationController;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    // Main view laden
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/haw_hamburg/sketchtomapgen/fxml/MainView.fxml"));
    Parent mainView = loader.load();

    // Controller holen und die Stage setzen
    NavigationController controller = loader.getController();
    controller.setStage(primaryStage);

    URL appIconUrl = getClass().getResource(AssetRoutes.APP_ICON_ASSET);
    primaryStage.getIcons().add(new Image(String.valueOf(appIconUrl)));

    // Scene anzeigen
    primaryStage.setScene(new Scene(mainView, 800, 600));
    primaryStage.setTitle("Sketch To Map Generator");
    primaryStage.show();
  }
}
