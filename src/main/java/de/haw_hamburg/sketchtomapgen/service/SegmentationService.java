package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.Point;
import java.util.*;
import java.util.stream.Collectors;

public class SegmentationService {
  private SketchModel sketchModel;
  private int width;
  private int height;

  public SegmentationService(int width, int height) {
    this.width = width;
    this.height = height;
    sketchModel = new SketchModel(width, height);
  }

  public WritableImage getImage() {
    WritableImage image = new WritableImage(sketchModel.getWidth(), sketchModel.getHeight());
    PixelWriter pixelWriter = image.getPixelWriter();

    for (int x = 0; x < sketchModel.getWidth(); x++) {
      for (int y = 0; y < sketchModel.getHeight(); y++) {
        if (sketchModel.isPixelSet(x, y)) {
          pixelWriter.setColor(x, y, Color.BLACK);
        } else {
          pixelWriter.setColor(x, y, Color.WHITESMOKE);
        }
      }
    }

    return image;
  }



  public void addPixels(int lastXRounded, int lastYRounded, int currentX, int currentY) {
    int dx = Math.abs(currentX - lastXRounded);
    int dy = Math.abs(currentY - lastYRounded);
    int sx = lastXRounded < currentX ? 1 : -1;
    int sy = lastYRounded < currentY ? 1 : -1;
    int err = dx - dy;

    while (true) {
      sketchModel.addPixelAt(lastXRounded, lastYRounded);

      if (lastXRounded == currentX && lastYRounded == currentY) {
        break;
      }

      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        lastXRounded += sx;
      }
      if (e2 < dx) {
        err += dx;
        lastYRounded += sy;
      }
    }
  }

  public void removePixels(int x, int y){
    //TODO
  }

  public SketchModel getSketchModel() {
    return sketchModel;
  }

  public void cleanSketchModel() {
    List<Set<Point>> components = sketchModel.getComponents();

    //Größte Komponente finden
    Set<Point> largestComponent = components.stream()
            .max(Comparator.comparingInt(Set::size))
            .orElseThrow();

    //Neues SketchModel mit bereinigter Komponente erstellen
    SketchModel cleanedModel = new SketchModel(
            sketchModel.getWidth(),
            sketchModel.getHeight()
    );

    //Pixel der größten Komponente hinzufügen
    for (Point pixel : largestComponent) {
      cleanedModel.addPixelAt(pixel.x, pixel.y);
    }

    sketchModel = cleanedModel;
  }
}