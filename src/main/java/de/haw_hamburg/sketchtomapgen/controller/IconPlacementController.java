package de.haw_hamburg.sketchtomapgen.controller;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import de.haw_hamburg.sketchtomapgen.service.RegionPartitioningService;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import de.haw_hamburg.sketchtomapgen.util.DataReceiver;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import de.haw_hamburg.sketchtomapgen.util.ViewRoutes;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconPlacementController extends AbstractController implements DataReceiver {
  @FXML
  private Canvas drawingCanvas;

  @FXML
  private Button villageButton;
  @FXML
  private Button mountainButton;
  @FXML
  private Button treeButton;
  @FXML
  private Button waterButton;
  @FXML
  private Button finishPlacementButton;
  @FXML
  private VBox satisfactionSection; // Section for satisfaction question
  @FXML
  private Button yesButton;
  @FXML
  private Button noButton;
  private final int CUSTOM_CURSOR_SIZE = 32;
  private RegionPartitioningService regionPartitioningService;
  private Icon activeIcon = Icon.BLANK;
  private Map<Coordinate ,Icon> icons;

  @FXML
  private void initialize() {
    icons = new HashMap<>();
    regionPartitioningService = new RegionPartitioningService((int) drawingCanvas.getWidth(), (int) drawingCanvas.getHeight());

    addMouseEventHandlers();
  }

  private void updateCanvas() {
    GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
    gc.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());

    regionPartitioningService.computeVoronoiFromIcons();

    WritableImage processedImage = regionPartitioningService.getImage();
    gc.drawImage(processedImage, 0, 0);

    for (Map.Entry<Coordinate, Icon> entry : icons.entrySet()) {
      Coordinate coord = entry.getKey();
      Icon icon = entry.getValue();
      URL imageUrl = getImageUrlForIcon(icon); // Methode zur Bestimmung des Bildpfads

      if (imageUrl != null) {
        Image image = new Image(String.valueOf(imageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true);
        gc.drawImage(image, coord.getX() - CUSTOM_CURSOR_SIZE / 2.0, coord.getY() - CUSTOM_CURSOR_SIZE / 2.0);
      }
    }
  }

  private URL getImageUrlForIcon(Icon icon) {
    return switch (icon) {
      case MOUNTAIN -> getClass().getResource(AssetRoutes.MOUNTAINS_ASSET);
      case TREE -> getClass().getResource(AssetRoutes.TREES_ASSET);
      case WATER -> getClass().getResource(AssetRoutes.WATER_ASSET);
      case VILLAGE -> getClass().getResource(AssetRoutes.VILLAGE_ASSET);
      case BLANK -> null;
      case OCEAN -> null;
    };
  }

  private void finishPlacement() {
    satisfactionSection.setVisible(true);
  }

  @FXML
  private void openResultView() {
    try {
      navigationController.switchView(ViewRoutes.RESULT_VIEW, regionPartitioningService.getVoronoiCellModels());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @FXML
  private void handleNo() {
    System.out.println("User is not satisfied with the placement.");
    // Add logic to allow further changes or restart the placement process
  }

  private void addMouseEventHandlers() {

    finishPlacementButton.setOnAction(e -> {
      finishPlacement();
      drawingCanvas.setCursor(Cursor.DEFAULT);
      activeIcon = Icon.BLANK;
    });

    URL mountainImageUrl = getClass().getResource(AssetRoutes.MOUNTAINS_ASSET);
    mountainButton.setOnAction(e -> {
              drawingCanvas.setCursor(
                      new ImageCursor(
                              new Image(String.valueOf(mountainImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                              CUSTOM_CURSOR_SIZE / 2.0,
                              CUSTOM_CURSOR_SIZE / 2.0
                      )
              );
              activeIcon = Icon.MOUNTAIN;
            }
    );

    URL treesImageUrl = getClass().getResource(AssetRoutes.TREES_ASSET);
    treeButton.setOnAction(e -> {
              drawingCanvas.setCursor(
                      new ImageCursor(
                              new Image(String.valueOf(treesImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                              CUSTOM_CURSOR_SIZE / 2.0,
                              CUSTOM_CURSOR_SIZE / 2.0
                      )
              );
              activeIcon = Icon.TREE;
            }
    );

    URL waterImageUrl = getClass().getResource(AssetRoutes.WATER_ASSET);
    waterButton.setOnAction(e -> {
              drawingCanvas.setCursor(
                      new ImageCursor(
                              new Image(String.valueOf(waterImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                              CUSTOM_CURSOR_SIZE / 2.0,
                              CUSTOM_CURSOR_SIZE / 2.0
                      )
              );
              activeIcon = Icon.WATER;
            }
    );

    URL villageImageUrl = getClass().getResource(AssetRoutes.VILLAGE_ASSET);
    villageButton.setOnAction(e -> {
              drawingCanvas.setCursor(
                      new ImageCursor(
                              new Image(String.valueOf(villageImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                              CUSTOM_CURSOR_SIZE / 2.0,
                              CUSTOM_CURSOR_SIZE / 2.0
                      )
              );
              activeIcon = Icon.VILLAGE;
            }
    );

    drawingCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
      switch (activeIcon) {
        case TREE -> {
          // Draw tree asset on the canvas at the mouse click position
          drawingCanvas.getGraphicsContext2D().drawImage(
                  new Image(String.valueOf(treesImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                  e.getX() - CUSTOM_CURSOR_SIZE / 2.0,
                  e.getY() - CUSTOM_CURSOR_SIZE / 2.0
          );
          Coordinate coordinate = new Coordinate(e.getX(), e.getY());
          icons.put(coordinate, Icon.TREE);
          regionPartitioningService.addIcon(new Coordinate(e.getX(), e.getY()), Icon.TREE);
        }
        case WATER -> {
          // Draw water asset on the canvas at the mouse click position
          drawingCanvas.getGraphicsContext2D().drawImage(
                  new Image(String.valueOf(waterImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                  e.getX() - CUSTOM_CURSOR_SIZE / 2.0,
                  e.getY() - CUSTOM_CURSOR_SIZE / 2.0
          );
          Coordinate coordinate = new Coordinate(e.getX(), e.getY());
          icons.put(coordinate, Icon.WATER);
          regionPartitioningService.addIcon(coordinate, Icon.WATER);
        }
        case VILLAGE -> {
          // Draw village asset on the canvas at the mouse click position
          drawingCanvas.getGraphicsContext2D().drawImage(
                  new Image(String.valueOf(villageImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                  e.getX() - CUSTOM_CURSOR_SIZE / 2.0,
                  e.getY() - CUSTOM_CURSOR_SIZE / 2.0
          );
          Coordinate coordinate = new Coordinate(e.getX(), e.getY());
          icons.put(coordinate, Icon.VILLAGE);
          regionPartitioningService.addIcon(new Coordinate(e.getX(), e.getY()), Icon.VILLAGE);
        }
        case MOUNTAIN -> {
          // Draw mountain asset on the canvas at the mouse click position
          drawingCanvas.getGraphicsContext2D().drawImage(
                  new Image(String.valueOf(mountainImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                  e.getX() - CUSTOM_CURSOR_SIZE / 2.0,
                  e.getY() - CUSTOM_CURSOR_SIZE / 2.0
          );
          Coordinate coordinate = new Coordinate(e.getX(), e.getY());
          icons.put(coordinate, Icon.MOUNTAIN);
          regionPartitioningService.addIcon(new Coordinate(e.getX(), e.getY()), Icon.MOUNTAIN);
        }
        default -> {
          //ignore
        }
      }
      updateCanvas();
    });
  }

  @Override
  public void receiveData(Object data) {
    if (data instanceof SketchModel sketchModel) {
      regionPartitioningService.initializeService(sketchModel);
      portrayModel(sketchModel);
    }
  }

  private void portrayModel(SketchModel sketchModel) {
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