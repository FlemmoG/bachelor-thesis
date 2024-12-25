package de.haw_hamburg.sketchtomapgen.util;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.Stack;

public class TurtleRenderer {
  private final GeneratedMapModel mapModel;
  private double x, y;
  private double angle;
  private final double stepSize;
  private final double angleIncrement;
  private final Stack<TurtleState> stack;
  private Polygon boundaryPolygon;
  private GeometryFactory geometryFactory;


  public TurtleRenderer(GeneratedMapModel mapModel, double startX, double startY,
                        double startAngle, double stepSize, double angleIncrement, Polygon boundary) {
    this.mapModel = mapModel;
    this.x = startX;
    this.y = startY;
    this.angle = startAngle;
    this.stepSize = stepSize;
    this.angleIncrement = angleIncrement;
    this.stack = new Stack<>();
    this.boundaryPolygon = boundary;
    this.geometryFactory = new GeometryFactory();
  }


  public void render(String lSystem) {
    for (char command : lSystem.toCharArray()) {
      switch (command) {
        case 'F': // Zeichne eine Linie in die aktuelle Richtung
          double newX = x + stepSize * Math.cos(Math.toRadians(angle));
          double newY = y - stepSize * Math.sin(Math.toRadians(angle)); // -Y für Canvas-Koordinaten

          drawLine((int) x, (int) y, (int) newX, (int) newY, Color.BLACK);

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

  protected void drawLine(int startX, int startY, int endX, int endY, Color color) {
    int dx = Math.abs(endX - startX);
    int dy = Math.abs(endY - startY);
    int sx = startX < endX ? 1 : -1;
    int sy = startY < endY ? 1 : -1;
    int err = dx - dy;

    while (true) {
      // Check if current point is inside polygon
      Point point = geometryFactory.createPoint(new Coordinate(startX, startY));
      if (boundaryPolygon.contains(point)) {
        mapModel.addPixel(startX, startY, color);
      }

      if (startX == endX && startY == endY) break;

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

  private static class TurtleState {
    double x, y, angle;

    TurtleState(double x, double y, double angle) {
      this.x = x;
      this.y = y;
      this.angle = angle;
    }
  }
}
