package de.haw_hamburg.sketchtomapgen.controller;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import de.haw_hamburg.sketchtomapgen.service.RegionPartitioningService;
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
  private Button undoButton;
  @FXML
  private VBox satisfactionSection;
  private final int CUSTOM_CURSOR_SIZE = 32;
  private RegionPartitioningService regionPartitioningService;
  private Icon activeIcon = Icon.BLANK;
  private Map<Coordinate ,Icon> icons;
  private List<Coordinate> iconHistory;
  private SketchModel sketchModel;

  @FXML
  private void initialize() {
    icons = new HashMap<>();
    regionPartitioningService = new RegionPartitioningService((int) drawingCanvas.getWidth(), (int) drawingCanvas.getHeight());
    iconHistory = new ArrayList<>();

    addMouseEventHandlers();
  }

  // Updatet das Canvas und zeigt das Voronoi Diagramm auf Basis der aktuell gesetzten Icons an
  private void portrayCellModelCollection() {
    GraphicsContext gc = drawingCanvas.getGraphicsContext2D();
    gc.clearRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());

    regionPartitioningService.computeVoronoiFromIcons();

    WritableImage processedImage = regionPartitioningService.getImage();
    gc.drawImage(processedImage, 0, 0);

    for (Map.Entry<Coordinate, Icon> entry : icons.entrySet()) {
      Coordinate coord = entry.getKey();
      Icon icon = entry.getValue();
      URL imageUrl = icon.getUrl(getClass());
      if (imageUrl != null) {
        Image image = new Image(String.valueOf(imageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true);
        gc.drawImage(image, coord.getX() - CUSTOM_CURSOR_SIZE / 2.0, coord.getY() - CUSTOM_CURSOR_SIZE / 2.0);
      }
    }
  }

  @FXML
  private void finishPlacement() {
    satisfactionSection.setVisible(true);
    drawingCanvas.setDisable(true);
    undoButton.setDisable(true);
    finishPlacementButton.setDisable(true);
    treeButton.setDisable(true);
    mountainButton.setDisable(true);
    waterButton.setDisable(true);
    villageButton.setDisable(true);
  }

  @FXML
  private void handleUndo() {
    if (iconHistory.isEmpty()) {
      return; // Nothing to undo
    }
    // Get the last placed coordinate.
    Coordinate lastCoordinate = iconHistory.remove(iconHistory.size() - 1);

    // Remove the icon from the local map.
    icons.remove(lastCoordinate);

    if (sketchModel != null) {
      sketchModel.getIcons().remove(lastCoordinate);
      if (iconHistory.isEmpty()) {
        portraySketchModel();
      } else
        portrayCellModelCollection();
    }
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
    drawingCanvas.setDisable(false);
    satisfactionSection.setVisible(false);
    undoButton.setDisable(false);
    finishPlacementButton.setDisable(false);
    treeButton.setDisable(false);
    mountainButton.setDisable(false);
    waterButton.setDisable(false);
    villageButton.setDisable(false);
  }



  private void addMouseEventHandlers() {

    finishPlacementButton.setOnAction(e -> {
      finishPlacement();
      drawingCanvas.setCursor(Cursor.DEFAULT);
      activeIcon = Icon.BLANK;
    });

    URL mountainImageUrl = Icon.MOUNTAIN.getUrl(getClass());
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

    URL treesImageUrl = Icon.TREE.getUrl(getClass());
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

    URL waterImageUrl = Icon.WATER.getUrl(getClass());
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

    URL villageImageUrl = Icon.VILLAGE.getUrl(getClass());
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
      Coordinate coordinate = new Coordinate(e.getX(), e.getY());
      switch (activeIcon) {
        case TREE -> {
          drawingCanvas.getGraphicsContext2D().drawImage(
                  new Image(String.valueOf(treesImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                  e.getX() - CUSTOM_CURSOR_SIZE / 2.0,
                  e.getY() - CUSTOM_CURSOR_SIZE / 2.0
          );
          icons.put(coordinate, Icon.TREE);
          regionPartitioningService.addIcon(coordinate, Icon.TREE);
        }
        case WATER -> {
          drawingCanvas.getGraphicsContext2D().drawImage(
                  new Image(String.valueOf(waterImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                  e.getX() - CUSTOM_CURSOR_SIZE / 2.0,
                  e.getY() - CUSTOM_CURSOR_SIZE / 2.0
          );
          icons.put(coordinate, Icon.WATER);
          regionPartitioningService.addIcon(coordinate, Icon.WATER);
        }
        case VILLAGE -> {
          drawingCanvas.getGraphicsContext2D().drawImage(
                  new Image(String.valueOf(villageImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                  e.getX() - CUSTOM_CURSOR_SIZE / 2.0,
                  e.getY() - CUSTOM_CURSOR_SIZE / 2.0
          );
          icons.put(coordinate, Icon.VILLAGE);
          regionPartitioningService.addIcon(coordinate, Icon.VILLAGE);
        }
        case MOUNTAIN -> {
          drawingCanvas.getGraphicsContext2D().drawImage(
                  new Image(String.valueOf(mountainImageUrl), CUSTOM_CURSOR_SIZE, CUSTOM_CURSOR_SIZE, true, true),
                  e.getX() - CUSTOM_CURSOR_SIZE / 2.0,
                  e.getY() - CUSTOM_CURSOR_SIZE / 2.0
          );
          icons.put(coordinate, Icon.MOUNTAIN);
          regionPartitioningService.addIcon(coordinate, Icon.MOUNTAIN);
        }
        default -> {
          // ignore
        }
      }

      // Record this placement so it can be undone later.
      iconHistory.add(coordinate);
      portrayCellModelCollection();
    });
  }

  @Override
  public void receiveData(Object data) {
    if (data instanceof SketchModel receivedSketchModel) {
      regionPartitioningService.initializeService(receivedSketchModel);
      this.sketchModel = receivedSketchModel;
      portraySketchModel();
    }
  }

  private void portraySketchModel() {
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