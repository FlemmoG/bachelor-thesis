package de.haw_hamburg.sketchtomapgen.util;

import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

import java.util.*;

public class Test {

  /**
   * Extracts and sorts boundary edges from a set of points in descending order of length
   *
   * @param points Array of Coordinate points
   * @return List of LineSegment boundary edges, sorted in descending order of length
   */
  public static List<LineSegment> extractSortedBoundaryEdges(List<Coordinate> points) {
    // Validate input
    if (points == null || points.size() < 3) {
      return new ArrayList<>();
    }

    // Create geometry factory
    GeometryFactory geometryFactory = new GeometryFactory();

    // Create Delaunay triangulation
    DelaunayTriangulationBuilder triangulationBuilder = new DelaunayTriangulationBuilder();
    triangulationBuilder.setSites(points);
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

  /**
   * Counts the number of times an edge appears in the triangulation
   *
   * @param triangles GeometryCollection of triangles
   * @param edge LineSegment to check
   * @return Number of times the edge appears
   */
  private static int countEdgeOccurrences(GeometryCollection triangles, LineSegment edge) {
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

  // Example usage method
  public static void main(String[] args) {
    List<Coordinate> points = new ArrayList<>(List.of(new Coordinate[]{
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(0, 1),
            new Coordinate(1, 1),
            new Coordinate(0.5, 0.5)
    }));

    List<LineSegment> boundaryEdges = extractSortedBoundaryEdges(points);

    System.out.println("Boundary Edges (sorted by length):");
    for (LineSegment edge : boundaryEdges) {
      System.out.println("Edge: " + edge + ", Length: " + edge.getLength());
    }
  }
}