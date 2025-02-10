package de.haw_hamburg.sketchtomapgen.util.l_system;

import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.collection.CellModelCollection;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import java.util.Comparator;
import java.util.Stack;

public class RiverTurtleRenderer {
  private final GeneratedMapModel mapModel;
  private double x, y;
  private double angle;
  private final double stepSize;
  private final double angleIncrement;
  private final Stack<TurtleState> stack;
  private Geometry boundaryPolygon;
  private PreparedGeometry preparedPolygon;
  private GeometryFactory geometryFactory;
  private final double initialX, initialY;
  private final CellModelCollection cellModels;
  private int currentWidth = 1;


  public RiverTurtleRenderer(GeneratedMapModel mapModel, double startX, double startY,
                             double startAngle, double stepSize, double angleIncrement, Geometry boundary, CellModelCollection cellModels) {
    this.mapModel = mapModel;
    this.x = startX;
    this.y = startY;
    this.angle = startAngle;
    this.stepSize = stepSize;
    this.angleIncrement = angleIncrement;
    this.stack = new Stack<>();
    this.boundaryPolygon = boundary;
    this.preparedPolygon = PreparedGeometryFactory.prepare(boundary);
    this.geometryFactory = new GeometryFactory();
    this.initialX = startX;
    this.initialY = startY;
    this.cellModels = cellModels;
  }


  public void render(String lSystem) {
    int maxWidth = 5;
    int minWidth = 1;
    double taperingFactor = 50.0;

    for (char command : lSystem.toCharArray()) {
      double distance = distanceFromSource();
      // Abnehmende Breite: Je größer die Distanz, desto kleiner wird die Breite.
      // Wir verwenden Math.max, um sicherzustellen, dass wir nicht unter minWidth fallen.
      currentWidth = Math.max(minWidth, maxWidth - (int)(distance / taperingFactor));

      switch (command) {
        case 'F':
          double newX = x + stepSize * Math.cos(Math.toRadians(angle));
          double newY = y - stepSize * Math.sin(Math.toRadians(angle));

          drawLine((int) x, (int) y, (int) newX, (int) newY);

          x = newX;
          y = newY;
          break;
        case '+': // Drehe nach rechts
          angle += angleIncrement;
          break;
        case '-': // Drehe nach links
          angle -= angleIncrement;
          break;
        case '[': // Speichere aktuellen Zustand
          stack.push(new TurtleState(x, y, angle));
          break;
        case ']': // Stelle gespeicherten Zustand wieder her
          TurtleState state = stack.pop();
          x = state.x;
          y = state.y;
          angle = state.angle;
          break;
      }
    }
  }


  private double distanceFromSource() {
    return Math.hypot(x - initialX, y - initialY);
  }

  private double distanceToBoundary(Coordinate pixelCoord){
    return geometryFactory.createPoint(pixelCoord).distance(boundaryPolygon.getBoundary());
  }

  private boolean isLake(Coordinate pixelCoord) {
    int ix = (int) pixelCoord.x;
    int iy = (int) pixelCoord.y;
    Color pixelColor = mapModel.getColorAt(ix, iy);

    // Interpolationsbereich.
    Color lowerBound = GlobalColors.DEEP_WATER;
    Color upperBound = GlobalColors.WATER_SURFACE;

    double tolerance = 0.01;

    boolean redInRange = pixelColor.getRed() >= lowerBound.getRed() - tolerance &&
            pixelColor.getRed() <= upperBound.getRed() + tolerance;
    boolean greenInRange = pixelColor.getGreen() >= lowerBound.getGreen() - tolerance &&
            pixelColor.getGreen() <= upperBound.getGreen() + tolerance;
    boolean blueInRange = pixelColor.getBlue() >= lowerBound.getBlue() - tolerance &&
            pixelColor.getBlue() <= upperBound.getBlue() + tolerance;
    boolean opacityInRange = pixelColor.getOpacity() >= lowerBound.getOpacity() - tolerance &&
            pixelColor.getOpacity() <= upperBound.getOpacity() + tolerance;

    return redInRange && greenInRange && blueInRange && opacityInRange;
  }



  private void drawLine(int startX, int startY, int endX, int endY) {
    // Bresenham mit Breite und Kollisionsprüfung
    int dx = Math.abs(endX - startX);
    int dy = Math.abs(endY - startY);
    int sx = startX < endX ? 1 : -1;
    int sy = startY < endY ? 1 : -1;
    int err = dx - dy;

    // Küstenübergang
    double smallDistance = 20.0;

    while (true) {
      // Für jeden Pixel in der aktuellen Breite
      for (int w = -currentWidth; w <= currentWidth; w++) {
        for (int h = -currentWidth; h <= currentWidth; h++) {
          int px = startX + w;
          int py = startY + h;
          Coordinate pixelCoord = new Coordinate(px, py);
          if (isValidPosition(px, py) && !isLake(pixelCoord)) {
            // Berechne die Distanz von diesem Pixel zur Begrenzung
            double distFromBoundary = distanceToBoundary(pixelCoord);

            // Berechne den Interpolationswert t anhand der kleinen Referenzdistanz.
            // t = 0 (auf der Grenze) -> SHALLOW_WATER,
            // t = 1 (bei smallDistance oder mehr) -> DEEP_WATER.
            double t = Math.min(distFromBoundary / smallDistance, 1.0);

            // Interpolieren zwischen den Farben
            Color color = GlobalColors.SHALLOW_WATER.interpolate(GlobalColors.WATER_SURFACE, t);

            mapModel.addPixel(px, py, color);

          }
        }
      }

      if (startX == endX && startY == endY) break;

      // Kollisionsprüfung für den nächsten Schritt
      if (!isValidPosition(startX + sx, startY) && !isValidPosition(startX, startY + sy)) {
        break;
      }

      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        startX += sx;
      }
      if (e2 < dx) {
        err += dx;
        startY += sy;
      }
    }
  }



  private boolean isValidPosition(int x, int y) {
    Point p = geometryFactory.createPoint(new Coordinate(x, y));
    return preparedPolygon.contains(p) && !isMountain(x, y);
  }

  private boolean isMountain(int x, int y) {
    for (CellModel cell : cellModels) {
      PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(cell.getPolygon());
      if (preparedGeometry.contains(geometryFactory.createPoint(new Coordinate(x, y)))) {
        return cell.getIcon() == Icon.MOUNTAIN;
      }
    }
    return false;
  }

  protected static class TurtleState {
    double x, y, angle;

    TurtleState(double x, double y, double angle) {
      this.x = x;
      this.y = y;
      this.angle = angle;
    }
  }
}
