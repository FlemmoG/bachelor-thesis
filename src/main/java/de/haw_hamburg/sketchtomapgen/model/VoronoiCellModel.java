package de.haw_hamburg.sketchtomapgen.model;

import de.haw_hamburg.sketchtomapgen.util.Icon;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;

import java.util.Set;

public class VoronoiCellModel {

  private Set<Coordinate> points;
  private Color color;
  private Icon icon;

  public VoronoiCellModel(Set<Coordinate> points, Color color, Icon icon) {
    this.points = points;
    this.color = color;
    this.icon = icon;
  }

  public Set<Coordinate> getPoints() {
    return points;
  }

  public Color getColor() {
    return color;
  }

  public Icon getIcon() {
    return icon;
  }

  public boolean isPixelSet(int x, int y) {
    return points.contains(new Coordinate(x, y));
  }
}

