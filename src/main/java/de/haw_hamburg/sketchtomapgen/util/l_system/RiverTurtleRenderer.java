package de.haw_hamburg.sketchtomapgen.util.l_system;

import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.collection.CellModelCollection;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;

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

  private Coordinate getClosestLake(Coordinate coordinate) {
    CellModel closestLake = null;
    double minDistance = Double.MAX_VALUE;

    for (CellModel cellModel : cellModels) {
      if (cellModel.getIcon() == Icon.WATER) {
        double distance = coordinate.distance(cellModel.getCentroid());
        if (distance < minDistance) {
          minDistance = distance;
          closestLake = cellModel;
        }
      }
    }

    return closestLake != null ? closestLake.getCentroid() : null;
  }

  private boolean isLake(Coordinate pixelCoord) {
    int ix = (int) pixelCoord.x;
    int iy = (int) pixelCoord.y;
    Color pixelColor = mapModel.getColorAt(ix, iy);

    // Interpolationsbereich.
    Color lowerBound = GlobalColors.DEEP_WATER;
    Color upperBound = GlobalColors.DEEP_WATER.interpolate(GlobalColors.WATER_SURFACE, 0.5);

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



  protected void drawLine(int startX, int startY, int endX, int endY) {
    // Bresenham mit Breite und Kollisionsprüfung
    int dx = Math.abs(endX - startX);
    int dy = Math.abs(endY - startY);
    int sx = startX < endX ? 1 : -1;
    int sy = startY < endY ? 1 : -1;
    int err = dx - dy;

    while (true) {
      // Für jeden Pixel in der aktuellen Breite
      for (int w = -currentWidth; w <= currentWidth; w++) {
        for (int h = -currentWidth; h <= currentWidth; h++) {
          int px = startX + w;
          int py = startY + h;
          if (isValidPosition(px, py)) {
            // Erzeuge Koordinate des Pixels
            Coordinate pixelCoord = new Coordinate(px, py);

            // Berechne die Distanz vom Pixel zur Quelle
            double distFromBoundary = distanceToBoundary(pixelCoord);

            // Hole den nächstgelegenen See (als Koordinate)
            Coordinate lakeCoord = getClosestLake(pixelCoord);
            // Falls kein See gefunden wird, verwende einen großen Wert, sodass die Interpolation
            // dann in Richtung SHALLOW_WATER tendiert
            double distToLake = (lakeCoord == null) ? 1e9 : pixelCoord.distance(lakeCoord);


            double t = distFromBoundary / (distFromBoundary + distToLake);

            // Interpolieren zwischen den Farben
            Color color = GlobalColors.SHALLOW_WATER.interpolate(GlobalColors.DEEP_WATER, t);

            // Füge den Pixel mit der berechneten Farbe hinzu, wenn nicht innerhalb eines Sees
            if (!isLake(pixelCoord)) {
              mapModel.addPixel(px, py, color);
            }
          }
        }
      }

      if (startX == endX && startY == endY) break;

      // Kollisionsprüfung für nächsten Schritt
      if (!isValidPosition(startX + sx, startY) && !isValidPosition(startX, startY + sy)) {
        break; // Blockiert, Abbruch
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
    // Prüfe auf Kartenbegrenzung und Geländetyp
    Point p = geometryFactory.createPoint(new Coordinate(x, y));
    return boundaryPolygon.contains(p) && !isMountain(x, y);
  }

  private boolean isMountain(int x, int y) {
    // Finde die Zelle, die diese Position enthält
    for (CellModel cell : cellModels) {
      if (cell.getPolygon().contains(geometryFactory.createPoint(new Coordinate(x, y)))) {
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
