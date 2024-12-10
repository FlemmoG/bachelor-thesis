package de.haw_hamburg.sketchtomapgen.model;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import java.awt.Point;
import java.util.*;
import java.util.stream.Collectors;

public class SketchModelGeom {
  private Set<Point> points;
  private GeometryFactory geometryFactory;

  public SketchModelGeom() {
    this.points = new HashSet<>();
    this.geometryFactory = new GeometryFactory();
  }

  public void addPixelAt(int x, int y) {
    points.add(new Point(x, y));
  }

  public boolean isPixelSet(int x, int y) {
    return points.contains(new Point(x, y));
  }

  public void removePixelAt(int x, int y) {
    points.remove(new Point(x,y));
  }

  public List<LineSegment> extractSortedBoundaryEdges() {
    // Validate input
    if (points == null || points.size() < 3) {
      return new ArrayList<>();
    }

    // Create geometry factory
    GeometryFactory geometryFactory = new GeometryFactory();

    // Create Delaunay triangulation
    DelaunayTriangulationBuilder triangulationBuilder = new DelaunayTriangulationBuilder();
    Set<Coordinate> coordinates = new HashSet<>();
    for (Point point : points) {
      coordinates.add(new Coordinate(point.x, point.y));
    }

    triangulationBuilder.setSites(coordinates);
    GeometryCollection triangles = (GeometryCollection) triangulationBuilder.getTriangles(geometryFactory);

    // Set to track unique boundary edges
    Set<LineSegment> boundaryEdges = new HashSet<>();

    // Iterate through triangles to find boundary edges
    for (int i = 0; i < triangles.getNumGeometries(); i++) {
      Polygon triangle = (Polygon) triangles.getGeometryN(i);
      LineString triangleExterior = triangle.getExteriorRing();

      // Check each segment of the triangle
      for (int j = 0; j < triangleExterior.getNumPoints() - 1; j++) {
        Coordinate start = triangleExterior.getCoordinateN(j);
        Coordinate end = triangleExterior.getCoordinateN(j + 1);
        LineSegment edge = new LineSegment(start, end);

        // Count occurrences of this edge across all triangles
        int edgeCount = countEdgeOccurrences(triangles, edge);

        // If edge appears only once, it's a boundary edge
        if (edgeCount == 1) {
          boundaryEdges.add(edge);
        }
      }
    }

    // Convert to list and sort in descending order of length
    List<LineSegment> sortedBoundaryEdges = new ArrayList<>(boundaryEdges);
    Collections.sort(sortedBoundaryEdges, (e1, e2) ->
            Double.compare(e2.getLength(), e1.getLength())
    );

    return sortedBoundaryEdges;
  }

  private int countEdgeOccurrences(GeometryCollection triangles, LineSegment edge) {
    int count = 0;
    for (int i = 0; i < triangles.getNumGeometries(); i++) {
      Polygon triangle = (Polygon) triangles.getGeometryN(i);
      LineString triangleExterior = triangle.getExteriorRing();

      for (int j = 0; j < triangleExterior.getNumPoints() - 1; j++) {
        Coordinate start = triangleExterior.getCoordinateN(j);
        Coordinate end = triangleExterior.getCoordinateN(j + 1);
        LineSegment currentEdge = new LineSegment(start, end);

        if (edge.equalsTopo(currentEdge)) {
          count++;
        }
      }
    }
    return count;
  }


}
