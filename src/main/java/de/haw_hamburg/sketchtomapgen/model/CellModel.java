package de.haw_hamburg.sketchtomapgen.model;

import de.haw_hamburg.sketchtomapgen.util.Icon;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

public class CellModel {

  // Kerndatenstruktur die die Form der Zelle repräsentiert
  private Polygon polygon;

  // Farbe für Visualisierung in der Regionsaufteilung
  private Color color;

  // Definiert das Biom der Zelle
  private Icon icon;
  private Coordinate centroid;

  public CellModel(Polygon polygon, Color color, Icon icon, Coordinate centroid) {
    this.polygon = polygon;
    this.color = color;
    this.icon = icon;
    this.centroid = centroid;
  }

  public Polygon getPolygon() {
    return polygon;
  }

  public Color getColor() {
    return color;
  }

  public Icon getIcon() {
    return icon;
  }

  public Coordinate getCentroid() {
    return centroid;
  }
}

