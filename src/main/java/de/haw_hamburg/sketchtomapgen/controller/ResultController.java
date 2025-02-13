package de.haw_hamburg.sketchtomapgen.controller;

import de.haw_hamburg.sketchtomapgen.model.collection.CellModelCollection;
import de.haw_hamburg.sketchtomapgen.service.MapGenerationService;
import de.haw_hamburg.sketchtomapgen.util.DataReceiver;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import de.haw_hamburg.sketchtomapgen.util.ViewRoutes;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

import static javafx.embed.swing.SwingFXUtils.fromFXImage;


public class ResultController extends AbstractController implements DataReceiver {
  @FXML
  private Canvas resultCanvas;
  private MapGenerationService mapGenerationService;

  public void initialize() {
    this.mapGenerationService = new MapGenerationService((int) resultCanvas.getWidth(), (int) resultCanvas.getHeight());
  }

  // einzelne Regionen erneut generieren (neues Model wird instanziiert)
  @FXML
  private void handleRegenerate() {
    mapGenerationService.resetService();
    portrayResult();
  }

  // gesamte Applikation wird zurückgesetzt und der Nutzer springt zur Draw View zurück
  @FXML
  private void handleRestart() {
    System.out.println("Restarting the application...");
    try {
      navigationController.switchView(ViewRoutes.DRAW_VIEW);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // Service fügt Filter hinzu
  @FXML
  private void addFilters() {
    GraphicsContext gc = resultCanvas.getGraphicsContext2D();
    gc.clearRect(0, 0, resultCanvas.getWidth(), resultCanvas.getHeight());

    mapGenerationService.addFiltersToImage();

    WritableImage processedImage = mapGenerationService.getImage();
    gc.drawImage(processedImage, 0, 0);
  }

  // Export der Datei über FileSystem
  @FXML
  private void handleExport() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Export Canvas as Image");
    fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PNG Image", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Image", "*.jpg"),
            new FileChooser.ExtensionFilter("BMP Image", "*.bmp")
    );

    File file = fileChooser.showSaveDialog(resultCanvas.getScene().getWindow());
    if (file != null) {
      try {
        WritableImage writableImage = new WritableImage((int) resultCanvas.getWidth(), (int) resultCanvas.getHeight());
        resultCanvas.snapshot(null, writableImage);

        String format = file.getName().substring(file.getName().lastIndexOf(".") + 1);

        ImageIO.write(fromFXImage(writableImage, null), format, file);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Successful");
        alert.setHeaderText(null);
        alert.setContentText("The canvas has been successfully exported to:\n" + file.getAbsolutePath());
        alert.showAndWait();
      } catch (Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Export Failed");
        alert.setHeaderText(null);
        alert.setContentText("An error occurred while exporting the canvas: " + e.getMessage());
        alert.showAndWait();
      }
    }
  }

  // Jedes Regionsicon nacheinander der drawStep Methode übergeben zusammen mit dem nächsten Schritt (Runnable)
  private void portrayResult() {
    drawStep(Icon.BLANK, () ->
            drawStep(Icon.OCEAN, () ->
                    drawStep(Icon.MOUNTAIN, () ->
                            drawStep(Icon.VILLAGE, () ->
                                    drawStep(Icon.TREE, () ->
                                            drawStep(Icon.WATER, () -> {
                                              PauseTransition pause = new PauseTransition(Duration.millis(50));
                                              pause.setOnFinished(e -> {
                                                mapGenerationService.addDetails();
                                                clearCanvasAndDrawImage();
                                              });
                                              pause.play();
                                            })
                                    )
                            )
                    )
            )
    );
  }

  // Übergebenes Icon der generateMap Methode im Service übergeben, der für die jeweilige Region dann im JavaFX Thread die Berechnungen startet.
  // So kann JavaFX die Regionsgenerierung visualisieren und die Karte wird sequentiell aufgebaut
  // (Pausen sind nötig, damit die Karte nach und nach aufgebaut wird)
  private void drawStep(Icon icon, Runnable nextStep) {
    Platform.runLater(() -> {
      if (icon == Icon.BLANK) {
        mapGenerationService.drawLandBase();
      } else if (icon != null) {
        mapGenerationService.generateMap(icon);
      }
      clearCanvasAndDrawImage();
      PauseTransition pause = new PauseTransition(Duration.millis(50));
      pause.setOnFinished(e -> {
        if (nextStep != null) {
          nextStep.run();
        }
      });
      pause.play();
    });
  }

  private void clearCanvasAndDrawImage() {
    GraphicsContext gc = resultCanvas.getGraphicsContext2D();
    gc.clearRect(0, 0, resultCanvas.getWidth(), resultCanvas.getHeight());

    WritableImage processedImage = mapGenerationService.getImage();
    gc.drawImage(processedImage, 0, 0);
  }

  @Override
  public void receiveData(Object data) {
    if (data instanceof CellModelCollection voronoiCellModels) {
      mapGenerationService.initializeService(voronoiCellModels);
      portrayResult();
    }
  }
}
