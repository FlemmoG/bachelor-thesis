package de.haw_hamburg.sketchtomapgen.model;

import de.haw_hamburg.sketchtomapgen.util.ClusterableCoordinate;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.locationtech.jts.algorithm.hull.ConcaveHull;
import org.locationtech.jts.geom.*;

import java.util.*;

public class SketchModel {
  private Set<ClusterableCoordinate> points;
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
    points.remove(new ClusterableCoordinate(x,y));
  }

  public Set<ClusterableCoordinate> getPoints() {
    return points;
  }

  public void addIcon(Coordinate coordinate, Icon icon){
    icons.put(coordinate, icon);
  }

  public Map<Coordinate, Icon> getIcons() {
    return icons;
  }

  public void getIcon(){

  }

  public Geometry getConcaveHull(){
    MultiPoint multiPoint = geometryFactory.createMultiPointFromCoords(points.toArray(new Coordinate[0]));
    double maxLength = calculateMaxDistanceBetweenPoints();

    System.out.println(maxLength);

    return ConcaveHull.concaveHullByLength(multiPoint,maxLength,false);
  }

  public GeometryCollection getConcaveHullsForClusters() {
    double eps = 5.0; // Radius for neighbors
    int minPoints = 3; // minimal points for cluster

    DBSCANClusterer<ClusterableCoordinate> clusterer = new DBSCANClusterer<>(eps, minPoints);

    // Cluster berechnen
    List<Cluster<ClusterableCoordinate>> clusters = clusterer.cluster(points);

    // Concave Hulls für alle Cluster erstellen
    List<Geometry> hulls = new ArrayList<>();
    for (Cluster<ClusterableCoordinate> cluster : clusters) {
      List<Coordinate> clusterCoords = new ArrayList<>();
      for (ClusterableCoordinate point : cluster.getPoints()) {
        clusterCoords.add(new Coordinate(point.x, point.y));
      }

      if (!clusterCoords.isEmpty()) {
        MultiPoint multiPoint = geometryFactory.createMultiPointFromCoords(clusterCoords.toArray(new Coordinate[0]));
        double maxLength = calculateMaxDistanceBetweenPoints();
        Geometry concaveHull = ConcaveHull.concaveHullByLength(multiPoint, maxLength, false);
        hulls.add(concaveHull);
      }
    }

    // Alle Hulls in eine GeometryCollection zusammenführen
    return geometryFactory.createGeometryCollection(hulls.toArray(new Geometry[0]));
  }

  private double calculateMaxDistanceBetweenPoints() {
    double totalDistance = 0.0;
    int count = 0;

    Iterator<ClusterableCoordinate> iterator = points.iterator();
    Coordinate prev = iterator.next();

    while (iterator.hasNext()) {
      Coordinate curr = iterator.next();

      // Distance between two points
      double distance = prev.distance(curr);
      totalDistance += distance;
      count++;

      prev = curr;
    }

    // Avg distance (scaled)
    double averageDistance = totalDistance / count;

    return averageDistance * 0.2;
  }



}
