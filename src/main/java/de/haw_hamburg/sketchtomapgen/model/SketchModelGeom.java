package de.haw_hamburg.sketchtomapgen.model;

import de.haw_hamburg.sketchtomapgen.Exception.TriangulationException;
import org.locationtech.jts.algorithm.hull.ConcaveHull;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import java.util.*;

public class SketchModelGeom {
  private Set<Coordinate> points;
  private GeometryFactory geometryFactory;

  public SketchModelGeom() {
    this.points = new HashSet<>();
    this.geometryFactory = new GeometryFactory();
  }

  public void addPixelAt(int x, int y) {
    points.add(new Coordinate(x, y));
  }

  public boolean isPixelSet(int x, int y) {
    return points.contains(new Coordinate(x, y));
  }

  public void removePixelAt(int x, int y) {
    points.remove(new Coordinate(x,y));
  }

  public Set<Coordinate> getPoints() {
    return points;
  }

  public Geometry getConcaveHull(){
    MultiPoint multiPoint = geometryFactory.createMultiPointFromCoords(points.toArray(new Coordinate[0]));
    double maxLength = calculateMaxDistanceBetweenPoints();

    System.out.println(maxLength);

    return ConcaveHull.concaveHullByLength(multiPoint,maxLength,false);
  }

  public double calculateMaxDistanceBetweenPoints() {
    double totalDistance = 0.0;
    int count = 0;

    Iterator<Coordinate> iterator = points.iterator();
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
