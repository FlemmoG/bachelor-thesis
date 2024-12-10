package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.SketchModelGeom;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.LineSegment;

import java.util.List;

public class SegmentationServiceGeom {
  private SketchModelGeom sketchModel;
  private int width;
  private int height;

  public SegmentationServiceGeom(int width, int height) {
    this.width = width;
    this.height = height;
    sketchModel = new SketchModelGeom();
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

  public void removePixels(int x, int y, int radius) {
    int halfRadius = radius / 2;
    for (int i = x - halfRadius; i <= x + halfRadius; i++) {
      for (int j = y - halfRadius; j <= y + halfRadius; j++) {
        sketchModel.removePixelAt(i, j);
      }
    }
  }

  public SketchModelGeom getSketchModel() {
    return sketchModel;
  }

  public void cleanSketchModel() {
    List<LineSegment> lineSegments = sketchModel.extractSortedBoundaryEdges();
    sketchModel = new SketchModelGeom(); // Clear the sketch model

    for (LineSegment lineSegment : lineSegments) {
      // Get integer coordinates of the segment's endpoints
      int x1 = (int) Math.round(lineSegment.p0.x);
      int y1 = (int) Math.round(lineSegment.p0.y);
      int x2 = (int) Math.round(lineSegment.p1.x);
      int y2 = (int) Math.round(lineSegment.p1.y);

      // Use Bresenham's algorithm to draw all pixels along the line
      addPixelsUsingInterpolation(x1, y1, x2, y2);
    }
  }

}