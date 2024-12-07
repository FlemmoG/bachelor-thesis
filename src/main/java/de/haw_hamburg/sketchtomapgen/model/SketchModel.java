package de.haw_hamburg.sketchtomapgen.model;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.cycle.HierholzerEulerianCycle;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.interfaces.EulerianCycleAlgorithm;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.awt.Point;
import java.util.List;
import java.util.Set;


public class SketchModel {
  private Graph<Point, DefaultEdge> sketchGraph;
  private int width;
  private int height;

  public SketchModel(int width, int height) {
    this.width = width;
    this.height = height;
    this.sketchGraph = new SimpleGraph<>(DefaultEdge.class);
  }

  public void addPixelAt(int x, int y) {
    Point pixel = new Point(x, y);

    sketchGraph.addVertex(pixel);
    connectToNeighbors(pixel);
  }

  public void removePixelAt(int x, int y) {
    //TODO
  }

  private void connectToNeighbors(Point pixel) {
    // Nachbarn definieren: 8-Nachbarschaft (horizontal, vertikal, diagonal)
    int[][] neighbors = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    for (int[] offset : neighbors) {
      Point neighbor = new Point(pixel.x + offset[0], pixel.y + offset[1]);
      if (sketchGraph.containsVertex(neighbor)) {
        sketchGraph.addEdge(pixel, neighbor);
      }
    }
  }

  public Graph<Point, DefaultEdge> getSketchGraph() {
    return sketchGraph;
  }

  public int getPixelCount() {
    return sketchGraph.vertexSet().size();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isPixelSet(int x, int y) {
    return sketchGraph.containsVertex(new Point(x, y));
  }

  public int getConnectedComponentCount() {
    ConnectivityInspector<Point, DefaultEdge> connectivity = new ConnectivityInspector<>(sketchGraph);
    return connectivity.connectedSets().size();
  }

  public List<Set<Point>> getComponents(){
    ConnectivityInspector<Point, DefaultEdge> connectivity = new ConnectivityInspector<>(sketchGraph);
    return connectivity.connectedSets();
  }

  public boolean isEulerianCycle() {
    return sketchGraph.vertexSet().stream()
            .allMatch(vertex -> sketchGraph.degreeOf(vertex) % 2 == 0);
  }

  public List<Point> findShortestPath(Point start, Point end) {
    // Dijkstra-Algorithmus für den kürzesten Pfad
    DijkstraShortestPath<Point, DefaultEdge> dijkstraAlg =
            new DijkstraShortestPath<>(sketchGraph);

    // Pfad zwischen Start- und Endpunkt finden
    GraphPath<Point, DefaultEdge> path = dijkstraAlg.getPath(start, end);

    // Wenn kein Pfad existiert, null oder leere Liste zurückgeben
    if (path == null) {
      return null;
    }

    // Vertex-Liste des Pfads zurückgeben
    return path.getVertexList();
  }
}
