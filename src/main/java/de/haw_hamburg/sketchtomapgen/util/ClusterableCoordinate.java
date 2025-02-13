package de.haw_hamburg.sketchtomapgen.util;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.locationtech.jts.geom.Coordinate;

// Adapter um mit JTS, aber auch mit Appache Clustering kompatibel zu sein
public class ClusterableCoordinate extends Coordinate implements Clusterable {
  public ClusterableCoordinate(double x, double y) {
    super(x, y);
  }

  public ClusterableCoordinate(Coordinate coordinate) {
    super(coordinate.x, coordinate.y);
  }

  @Override
  public double[] getPoint() {
    return new double[]{this.x, this.y};
  }
}
