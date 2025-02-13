package de.haw_hamburg.sketchtomapgen.controller;

import de.haw_hamburg.sketchtomapgen.service.SketchService;
import de.haw_hamburg.sketchtomapgen.util.ViewRoutes;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.io.IOException;


public class DrawController extends AbstractController {

  private final double ERASER_RADIUS = 50;
  private final double PEN_RADIUS = 1;

  @FXML
  private Canvas drawingCanvas;

  @FXML
  private Button drawButton;

  @FXML
  private Button eraseButton;

  @FXML
  private Button generateButton;

  private boolean isDrawing = true;
  private SketchService sketchService;

  @FXML
  public void initialize() {
    drawingCanvas.setFocusTraversable(true);
    sketchService = new SketchService((int) drawingCanvas.getWidth(), (int) drawingCanvas.getHeight());

    addMouseEventHandlers();
    addKeyboardEventHandlers();
  }

  // Teilt dem navigation Controller mit, dass zur nächsten View gewechselt werden soll
  @FXML
  private void openIconPlacementView() {
    try {
      navigationController.switchView(ViewRoutes.ICON_PLACEMENT_VIEW, sketchService.getSketchModel());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // Generiert einen Umriss (oder mehrere) aus der Skizze und zeichnet ihn
  private void generateOutline() {
    GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
    gc.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());

    sketchService.cleanSketchModel();

    WritableImage processedImage = sketchService.getImage();
    gc.drawImage(processedImage, 0, 0);
    openIconPlacementView();
  }

  // Event Handler für die Benutzerinteraktion
  private void addMouseEventHandlers() {

    drawButton.setOnAction(e -> isDrawing = true);
    eraseButton.setOnAction(e -> isDrawing = false);
    generateButton.setOnAction(e -> generateOutline());

    GraphicsContext gc = drawingCanvas.getGraphicsContext2D();

    drawingCanvas.setOnMouseClicked(event -> drawingCanvas.requestFocus());

    final double[] lastX = {0};
    final double[] lastY = {0};

    drawingCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
      lastX[0] = e.getX();
      lastY[0] = e.getY();
    });

    drawingCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
      int currentX = (int) Math.round(e.getX());
      int currentY = (int) Math.round(e.getY());
      int lastXRounded = (int) Math.round(lastX[0]);
      int lastYRounded = (int) Math.round(lastY[0]);

      if (isDrawing) {
        drawingCanvas.setCursor(Cursor.HAND);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(PEN_RADIUS);
        gc.strokeLine(lastX[0], lastY[0], currentX, currentY);

        sketchService.addPixelsUsingInterpolation(lastXRounded, lastYRounded, currentX, currentY);
      } else {
        drawingCanvas.setCursor(Cursor.CLOSED_HAND);
        gc.setFill(Color.WHITESMOKE);
        gc.fillRect(currentX - ERASER_RADIUS / 2, currentY - ERASER_RADIUS / 2, ERASER_RADIUS, ERASER_RADIUS);

        sketchService.removePixels(currentX, currentY, (int) ERASER_RADIUS);
      }

      lastX[0] = currentX;
      lastY[0] = currentY;
    });

    drawingCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> drawingCanvas.setCursor(Cursor.DEFAULT));
  }

  private void addKeyboardEventHandlers() {
    drawingCanvas.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.D) {
        isDrawing = true;
      } else if (event.getCode() == KeyCode.E) {
        isDrawing = false;
      } else if (event.getCode() == KeyCode.ENTER) {
        generateOutline();
      }
    });
  }
}

