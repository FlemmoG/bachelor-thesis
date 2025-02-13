package de.haw_hamburg.sketchtomapgen.model;

import de.haw_hamburg.sketchtomapgen.util.ClusterableCoordinate;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.locationtech.jts.algorithm.hull.ConcaveHull;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import java.util.*;

public class SketchModel {
  // Koodinaten, die den Umriss repräsentieren
  private Set<ClusterableCoordinate> points;

  // Speichert die platzierten Icons mit Position
  private Map<Coordinate, Icon> icons;
  private GeometryFactory geometryFactory;

  public SketchModel() {
    this.points = new HashSet<>();
    this.geometryFactory = new GeometryFactory();
    this.icons = new HashMap<>();
  }

  public void addPixelAt(int x, int y) {
    points.add(new ClusterableCoordinate(x, y));
  }

  public boolean isPixelSet(int x, int y) {
    return points.contains(new ClusterableCoordinate(x, y));
  }

  public void removePixelAt(int x, int y) {
    points.remove(new ClusterableCoordinate(x, y));
  }

  public Set<ClusterableCoordinate> getPoints() {
    return points;
  }

  public void addIcon(Coordinate coordinate, Icon icon) {
    icons.put(coordinate, icon);
  }

  public Map<Coordinate, Icon> getIcons() {
    return icons;
  }

  // Berechnet die Form des gezeichneten Umrisses oder der gezeichneten Umrisse und gibt GeometryCollection zurück
  public GeometryCollection getOutlinesForClusters() {
    double eps = 5.0; // Radius für Nachbarn
    int minPoints = 3; // Minimale Anzahl an Punkten pro Cluster

    DBSCANClusterer<ClusterableCoordinate> clusterer = new DBSCANClusterer<>(eps, minPoints);
    List<Cluster<ClusterableCoordinate>> clusters = clusterer.cluster(points);

    List<Geometry> hulls = new ArrayList<>();

    GeometryPrecisionReducer precisionReducer = new GeometryPrecisionReducer(new PrecisionModel(PrecisionModel.FLOATING));

    // Cluster einzelnt behandelt und From approximieren
    for (Cluster<ClusterableCoordinate> cluster : clusters) {
      List<Coordinate> clusterCoords = new ArrayList<>();
      for (ClusterableCoordinate point : cluster.getPoints()) {
        clusterCoords.add(new Coordinate(point.x, point.y));
      }

      if (!clusterCoords.isEmpty()) {
        // Berechne maxLength spezifisch für das aktuelle Cluster
        double scaledAverageDistance = calculateScaledAverageDistance(clusterCoords);

        // Erstelle aus den Cluster-Koordinaten einen MultiPoint
        MultiPoint multiPoint = geometryFactory.createMultiPointFromCoords(clusterCoords.toArray(new Coordinate[0]));

        // Erzeuge den Concave-Hull für diesen Cluster
        Geometry concaveHull = ConcaveHull.concaveHullByLength(multiPoint, scaledAverageDistance, false);

        // Reduziere die Geometrie auf maximale Präzision
        concaveHull = precisionReducer.reduce(concaveHull);

        hulls.add(concaveHull);
      }
    }

    return geometryFactory.createGeometryCollection(hulls.toArray(new Geometry[0]));
  }

  // Berechnet eine skalierte Durchschnittsdistanz zwischen aufeinanderfolgenden Punkten
  private double calculateScaledAverageDistance(List<Coordinate> coordinates) {
    if (coordinates.size() < 2) {
      return 0.0;
    }

    double totalDistance = 0.0;
    int count = 0;

    for (int i = 1; i < coordinates.size(); i++) {
      totalDistance += coordinates.get(i - 1).distance(coordinates.get(i));
      count++;
    }

    double averageDistance = totalDistance / count;
    return averageDistance * 0.2;
  }
}
