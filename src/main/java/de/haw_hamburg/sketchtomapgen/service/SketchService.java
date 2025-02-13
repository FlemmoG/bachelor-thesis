package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineSegment;

import java.util.ArrayList;
import java.util.List;

public class SketchService {
  private SketchModel sketchModel;
  private int width;
  private int height;

  public SketchService(int width, int height) {
    this.width = width;
    this.height = height;
    sketchModel = new SketchModel();
  }

  public WritableImage getImage() {
    WritableImage image = new WritableImage(width, height);
    PixelWriter pixelWriter = image.getPixelWriter();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (sketchModel.isPixelSet(x, y)) {
          pixelWriter.setColor(x, y, Color.BLACK);
        } else {
          pixelWriter.setColor(x, y, Color.WHITESMOKE);
        }
      }
    }

    return image;
  }

  // Bresenham nutzen, um interpolierte Linien zu zeichnen
  public void addPixelsUsingInterpolation(int lastXRounded, int lastYRounded, int currentX, int currentY) {
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

  // Alle Pixel aus dem Model entfernen, die in einem bestimmten Radius liegen
  public void removePixels(int x, int y, int radius) {
    int halfRadius = radius / 2;
    for (int i = x - halfRadius; i <= x + halfRadius; i++) {
      for (int j = y - halfRadius; j <= y + halfRadius; j++) {
        sketchModel.removePixelAt(i, j);
      }
    }
  }

  public SketchModel getSketchModel() {
    return sketchModel;
  }

  // Umrisse für alle Cluster zeichnen
  public void cleanSketchModel() {
    GeometryCollection clusters = sketchModel.getOutlinesForClusters();
    sketchModel = new SketchModel();

    // Umriss Form für alle Cluster approximieren
    for (int i = 0; i < clusters.getNumGeometries(); i++) {
      Geometry clusterGeometry = clusters.getGeometryN(i);

      if (clusterGeometry.getBoundary() != null) {
        List<LineSegment> lineSegments = new ArrayList<>();
        Coordinate[] coordinates = clusterGeometry.getBoundary().getCoordinates();

        // Linien Segmente extrahieren
        for (int j = 0; j < coordinates.length - 1; j++) {
          lineSegments.add(new LineSegment(coordinates[j], coordinates[j + 1]));
        }

        // Koordinaten runden, dann dem neuen SketchModel hinzufügen mithilfe der interpolierungs Methode
        for (LineSegment lineSegment : lineSegments) {
          int x1 = (int) Math.round(lineSegment.p0.x);
          int y1 = (int) Math.round(lineSegment.p0.y);
          int x2 = (int) Math.round(lineSegment.p1.x);
          int y2 = (int) Math.round(lineSegment.p1.y);

          addPixelsUsingInterpolation(x1, y1, x2, y2);
        }
      }
    }
  }
}