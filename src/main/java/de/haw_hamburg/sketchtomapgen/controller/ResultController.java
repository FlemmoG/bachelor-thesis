package de.haw_hamburg.sketchtomapgen.controller;

import de.haw_hamburg.sketchtomapgen.model.VoronoiCellModel;
import de.haw_hamburg.sketchtomapgen.model.collection.VoronoiCellModelCollection;
import de.haw_hamburg.sketchtomapgen.service.MapGenerationService;
import de.haw_hamburg.sketchtomapgen.util.DataReceiver;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import javax.imageio.ImageIO;
import java.io.File;

import static javafx.embed.swing.SwingFXUtils.fromFXImage;


public class ResultController extends AbstractController implements DataReceiver{

  @FXML
  private Canvas resultCanvas;
  private MapGenerationService mapGenerationService;

  public void initialize() {
    this.mapGenerationService = new MapGenerationService((int) resultCanvas.getWidth(), (int) resultCanvas.getHeight());
  }

  @FXML
  private void handleRestart() {
//    System.out.println("Restarting the application...");
//    try {
//      navigationController.switchView(ViewRoutes.DRAW_VIEW);
//    } catch (IOException e) {
//      throw new RuntimeException(e);
//    }
    GraphicsContext gc = resultCanvas.getGraphicsContext2D();
    gc.clearRect(0, 0, resultCanvas.getWidth(), resultCanvas.getHeight());

    mapGenerationService.generateMap();

    WritableImage processedImage = mapGenerationService.getImage();
    gc.drawImage(processedImage, 0, 0);
  }

  @FXML
  private void handleExport() {
    // Create a FileChooser for the user to select the export location
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export Canvas as Image");
    fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PNG Image", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Image", "*.jpg"),
            new FileChooser.ExtensionFilter("BMP Image", "*.bmp")
    );

    // Show the Save File dialog
    File file = fileChooser.showSaveDialog(resultCanvas.getScene().getWindow());
    if (file != null) {
      try {
        // Create a WritableImage from the canvas
        WritableImage writableImage = new WritableImage((int) resultCanvas.getWidth(), (int) resultCanvas.getHeight());
        resultCanvas.snapshot(null, writableImage);

        // Extract the file format from the file extension
        String format = file.getName().substring(file.getName().lastIndexOf(".") + 1);

        // Save the image
        ImageIO.write(fromFXImage(writableImage, null), format, file);

        // Confirmation dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText(null);
        alert.setContentText("The canvas has been successfully exported to:\n" + file.getAbsolutePath());
        alert.showAndWait();
      } catch (Exception e) {
        // Handle any exceptions that occur during the export
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Export Failed");
        alert.setHeaderText(null);
        alert.setContentText("An error occurred while exporting the canvas: " + e.getMessage());
        alert.showAndWait();
      }
    }
  }


  @Override
  public void receiveData(Object data) {
    if (data instanceof VoronoiCellModelCollection voronoiCellModels) {
      mapGenerationService.initializeService(voronoiCellModels);
      portrayResult();
    }
  }

  private void portrayResult() {
    GraphicsContext gc = resultCanvas.getGraphicsContext2D();
    gc.clearRect(0, 0, resultCanvas.getWidth(), resultCanvas.getHeight());

    mapGenerationService.generateMap();

    WritableImage processedImage = mapGenerationService.getImage();
    gc.drawImage(processedImage, 0, 0);
  }
}
