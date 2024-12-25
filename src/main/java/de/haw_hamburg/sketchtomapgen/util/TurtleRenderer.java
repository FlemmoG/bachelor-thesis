package de.haw_hamburg.sketchtomapgen.util;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import javafx.scene.paint.Color;

import java.util.Stack;

public class TurtleRenderer {
  private final GeneratedMapModel mapModel;
  private int mapMinX, mapMinY, mapMaxX, mapMaxY;
  private double x, y;
  private double angle;
  private final double stepSize;
  private final double angleIncrement;
  private final Stack<TurtleState> stack;

  public TurtleRenderer(GeneratedMapModel mapModel, double startX, double startY, double startAngle, double stepSize, double angleIncrement) {
    this.mapModel = mapModel;
    this.x = startX;
    this.y = startY;
    this.angle = startAngle;
    this.stepSize = stepSize;
    this.angleIncrement = angleIncrement;
    this.stack = new Stack<>();
  }

  public void setMapBounds(int minX, int minY, int maxX, int maxY) {
    this.mapMinX = minX;
    this.mapMinY = minY;
    this.mapMaxX = maxX;
    this.mapMaxY = maxY;
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

  private void drawLine(int startX, int startY, int endX, int endY, Color color) {
    int dx = Math.abs(endX - startX);
    int dy = Math.abs(endY - startY);
    int sx = startX < endX ? 1 : -1;
    int sy = startY < endY ? 1 : -1;
    int err = dx - dy;

    while (true) {
      mapModel.addPixel(startX, startY, color);

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
