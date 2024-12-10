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

  public void cleanSketchModel() {
    List<Set<Point>> components = sketchModel.getComponents();
    SketchModel newSketchModel = new SketchModel(width, height);
    for (Set<Point> component : components) {
      List<Point> closedContour = closeContour(component, 0.01);
      closedContour.forEach((pixel) -> newSketchModel.addPixelAt(pixel.x, pixel.y));
    }

    sketchModel = newSketchModel;
  }

  public List<Point> closeContour(Set<Point> points, double epsilon) {
    if (points.size() < 3) {
      return new ArrayList<>(points);
    }

    // Konvertiere Set zu Liste und sortiere
    List<Point> sortedPoints = new ArrayList<>(points);
    sortedPoints.sort(Comparator.comparingInt((Point p) -> p.y).thenComparingInt(p -> p.x));

    // Finde Startpunkt (unterster, linkester Punkt)
    Point start = sortedPoints.get(0);

    // Convex Hull Algorithmus (Graham Scan)
    List<Point> convexHull = new ArrayList<>();
    convexHull.add(start);

    // Sortiere Punkte nach Polarwinkel
    sortedPoints.sort((p1, p2) -> {
      double angle1 = Math.atan2(p1.y - start.y, p1.x - start.x);
      double angle2 = Math.atan2(p2.y - start.y, p2.x - start.x);
      return Double.compare(angle1, angle2);
    });

    for (Point p : sortedPoints) {
      while (convexHull.size() > 1) {
        Point top = convexHull.get(convexHull.size() - 1);
        Point secondTop = convexHull.get(convexHull.size() - 2);

        // Kreuzprodukt zur Orientierungsbestimmung
        double crossProduct = (top.x - secondTop.x) * (p.y - secondTop.y) -
                (top.y - secondTop.y) * (p.x - secondTop.x);

        if (crossProduct <= 0) {
          convexHull.remove(convexHull.size() - 1);
        } else {
          break;
        }
      }
      convexHull.add(p);
    }

    // Douglas-Peucker Vereinfachung
    return douglasPeucker(convexHull, epsilon);
  }

  private List<Point> douglasPeucker(List<Point> points, double epsilon) {
    if (points.size() <= 2) {
      return new ArrayList<>(points);
    }

    // Finde Punkt mit maximaler Distanz zur Linie
    double maxDistance = 0;
    int index = 0;
    int end = points.size() - 1;

    for (int i = 1; i < end; i++) {
      double distance = perpendicularDistance(points.get(i), points.get(0), points.get(end));
      if (distance > maxDistance) {
        index = i;
        maxDistance = distance;
      }
    }

    // Wenn maximale Distanz kleiner epsilon, reduziere auf Start und Endpunkt
    if (maxDistance < epsilon) {
      return new ArrayList<>(Arrays.asList(points.get(0), points.get(end)));
    }

    // Rekursiv teilen
    List<Point> results1 = douglasPeucker(points.subList(0, index + 1), epsilon);
    List<Point> results2 = douglasPeucker(points.subList(index, points.size()), epsilon);

    // Kombiniere Ergebnisse mit neuer ArrayList
    List<Point> combinedResults = new ArrayList<>(results1);
    combinedResults.addAll(results2.subList(1, results2.size()));
    return combinedResults;
  }

  private double perpendicularDistance(Point point, Point lineStart, Point lineEnd) {
    double area = Math.abs(
            (lineStart.x * lineEnd.y + lineEnd.x * point.y + point.x * lineStart.y) -
                    (lineEnd.x * lineStart.y + point.x * lineEnd.y + lineStart.x * point.y)
    ) / 2.0;

    double bottom = Math.sqrt(
            Math.pow(lineStart.x - lineEnd.x, 2) +
                    Math.pow(lineStart.y - lineEnd.y, 2)
    );

    return 2 * area / bottom;
  }

}