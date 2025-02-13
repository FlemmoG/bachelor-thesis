package de.haw_hamburg.sketchtomapgen.controller;

import de.haw_hamburg.sketchtomapgen.util.DataReceiver;
import de.haw_hamburg.sketchtomapgen.util.ViewRoutes;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// Agiert als Router
public class NavigationController {

  private Stage stage;

  public void setStage(Stage stage) {
    this.stage = stage;
  }

  // Wechselt die View, indem es aus dem Übergebenen Pfad die neue View holt und diese in die Stage setzt
  public void switchView(String fxmlPath, Object data) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    Parent newView = loader.load();

    // NavigationController (this) dem Controller der neuen View mitgeben
    Object controller = loader.getController();
    if (controller instanceof AbstractController) {
      ((AbstractController) controller).setNavigationController(this);
    }

    // Wenn Daten vorhanden, dann übergeben (kann hier beliebiges Objekt sein)
    if (data != null && controller instanceof DataReceiver) {
      ((DataReceiver) controller).receiveData(data);
    }

    stage.setScene(new Scene(newView));
  }

  public void switchView(String fxmlPath) throws IOException {
    switchView(fxmlPath, null);
  }

  @FXML
  private void openDrawView() throws IOException {
    switchView(ViewRoutes.DRAW_VIEW, null);
  }


}
