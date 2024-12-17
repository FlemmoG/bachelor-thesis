package de.haw_hamburg.sketchtomapgen.controller;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import de.haw_hamburg.sketchtomapgen.util.DataReceiver;
import de.haw_hamburg.sketchtomapgen.util.ViewRoutes;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;

public class IconPlacementController extends AbstractController implements DataReceiver {
  @FXML
  private Canvas drawingCanvas;

  @FXML
  private Button buildingIcon;
  @FXML
  private Button roadIcon;
  @FXML
  private Button parkIcon;
  @FXML
  private Button waterIcon;
  @FXML
  private Button finishPlacementButton;
  private SketchModel sketchModel;

  @FXML
  private void initialize() {
    // Setup drag and drop or icon selection logic here
    setupIconDragAndDrop();
    setupFinishPlacementButton();
  }

  private void setupIconDragAndDrop() {
    // Implement drag and drop functionality for icons
    // Example (you'll need to implement the full logic):
    buildingIcon.setOnMousePressed(event -> {
      // Start drag operation
    });

    // Similar setup for other icons
  }

  private void setupFinishPlacementButton() {
    finishPlacementButton.setOnAction(event -> {
      try {
        // Switch back to main view
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ViewRoutes.MAIN_VIEW));
        Parent mainView = loader.load();
        Scene scene = finishPlacementButton.getScene();
        scene.setRoot(mainView);
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  public void receiveData(Object data) {
    if (data instanceof SketchModel) {
      this.sketchModel = (SketchModel) data;
      portrayModel();
    }
  }

  private void portrayModel() {
    if (drawingCanvas == null || sketchModel == null) {
      return;
    }
    var graphicsContext = drawingCanvas.getGraphicsContext2D();
    graphicsContext.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
    graphicsContext.setFill(javafx.scene.paint.Color.BLACK);

    for (Coordinate coordinate : sketchModel.getPoints()) {
      graphicsContext.fillOval(coordinate.getX(), coordinate.getY(), 2, 2); // Punkte zeichnen
    }
  }
}
