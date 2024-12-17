package de.haw_hamburg.sketchtomapgen.controller;

import de.haw_hamburg.sketchtomapgen.util.DataReceiver;
import de.haw_hamburg.sketchtomapgen.util.ViewRoutes;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {

  private Stage stage;
  public void setStage(Stage stage) {
    this.stage = stage;
  }

  public void switchView(String fxmlPath, Object data) throws IOException {
    // View wechseln: Neue FXML-Datei laden und Root der Scene setzen
    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    Parent newView = loader.load();

    // Controller für die neue View optional abrufen
    Object controller = loader.getController();
    if (controller instanceof AbstractController) {
      ((AbstractController) controller).setMainController(this); // MainController weitergeben
    }

    if (data != null && controller instanceof DataReceiver) {
      ((DataReceiver) controller).receiveData(data);
    }

    stage.setScene(new Scene(newView));
  }

  public void switchView(String fxmlPath) throws IOException{
    switchView(fxmlPath, null);
  }

  @FXML
  private void openDrawView() throws IOException {
    switchView(ViewRoutes.DRAW_VIEW, null);
  }


}
