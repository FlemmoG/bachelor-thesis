package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;

import java.util.*;

public class VillageMapCellGenerator implements MapCellGenerationStrategy {

  private static final String[] PREFIXES = {
          "Alt", "Neu", "Dunkel", "Licht", "Eichen", "Drachen", "Mond", "Stern", "Raben", "Falken"
  };

  private static final String[] ROOTS = {
          "hain", "tal", "furt", "burg", "heim", "winkel", "feld", "dorf", "grund", "stedt", "lord"
  };

  private static final String[] SUFFIXES = {
          "dorf", "furt", "heim", "stedt", "tal", "berg", "au", "winkel", "hafen"
  };

  // Zufallszahlengenerator
  private static final Random RANDOM = new Random();
  private List<Point> nodes = new ArrayList<>();
  Set<Point> setLabels = new HashSet<>();

  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    System.out.println("anfang");
    Polygon polygon = cellModel.getPolygon();
    Envelope envelope = polygon.getEnvelopeInternal();
    GeometryFactory gf = new GeometryFactory();

    // Grundfarbe
    for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x++) {
      for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y++) {
        Coordinate point = new Coordinate(x, y);
        if (polygon.contains(gf.createPoint(point))) {
          generatedMapModel.addPixel(x, y, GlobalColors.TOTALLY_FLAT);
        }
      }
    }

    // Straßennetzwerk relativ zur Polygongröße zeichnen
    int numSeeds = (int) (polygon.getArea() / 10000);
    RoadNetworkData networkData = generateRoadNetwork(polygon, numSeeds);

    for (LineString road : networkData.roads) {
      drawLine(road, generatedMapModel, 2);
    }

    // Markerpunkte versetzt über Knoten zeichnen
    this.nodes.addAll(networkData.nodes);
    System.out.println("fertig");
  }

  // Wrapper für Straßennetz und dessen Knotenpunkte
  private static class RoadNetworkData {
    List<LineString> roads;
    List<Point> nodes;

    RoadNetworkData(List<LineString> roads, List<Point> nodes) {
      this.roads = roads;
      this.nodes = nodes;
    }
  }

  public void drawMarkers(GeneratedMapModel generatedMapModel) {
    int labelRadius = 100; // Mindestabstand zwischen Labels
    for (Point node : nodes) {
      int markerSize = 3;
      int x = (int) Math.round(node.getX());
      int y = (int) Math.round(node.getY()) - 10;

      drawMarker(x, y, markerSize, generatedMapModel, GlobalColors.MARKER);

      boolean canPlaceLabel = setLabels.stream().noneMatch(p -> p.distance(node) < labelRadius);

      if (canPlaceLabel) {
        generatedMapModel.addLabel(x, y, getRandomVillageName(), GlobalColors.MARKER, 10);
        setLabels.add(node);
      }
    }
    nodes.clear();
    setLabels.clear();
  }

  private static RoadNetworkData generateRoadNetwork(Polygon polygon, int numSeeds) {
    GeometryFactory gf = new GeometryFactory();
    List<Point> seedPoints = generateSeedPoints(polygon, numSeeds, gf);
    List<LineString> roads = computeMST(seedPoints, gf);
    return new RoadNetworkData(roads, seedPoints);
  }

  private static List<Point> generateSeedPoints(Polygon polygon, int numSeeds, GeometryFactory gf) {
    List<Point> seedPoints = new ArrayList<>();
    Envelope envelope = polygon.getEnvelopeInternal();
    Random random = new Random();

    // Erzeuge Punkte
    while (seedPoints.size() < numSeeds) {
      double x = envelope.getMinX() + random.nextDouble() * envelope.getWidth();
      double y = envelope.getMinY() + random.nextDouble() * envelope.getHeight();
      Point p = gf.createPoint(new Coordinate(x, y));
      if (polygon.contains(p)) {
        seedPoints.add(p);
      }
    }
    return seedPoints;
  }

  private static List<LineString> computeMST(List<Point> points, GeometryFactory gf) {
    int n = points.size();

    // Wenn keine Punkte in der Liste enthalten sind
    if (n == 0) {
      return new ArrayList<>();
    }

    boolean[] inTree = new boolean[n];
    double[] minDist = new double[n];
    int[] parent = new int[n];
    Arrays.fill(minDist, Double.MAX_VALUE);
    minDist[0] = 0;
    parent[0] = -1;

    // Prim's Algorithmus: Füge Punkt hinzu, der den geringsten Abstand zu einem bereits
    // im Baum befindlichen Punkt hat.
    for (int i = 0; i < n; i++) {
      int u = -1;
      double best = Double.MAX_VALUE;
      for (int j = 0; j < n; j++) {
        if (!inTree[j] && minDist[j] < best) {
          best = minDist[j];
          u = j;
        }
      }
      if (u == -1) {
        break;
      }
      inTree[u] = true;

      for (int v = 0; v < n; v++) {
        if (!inTree[v]) {
          double dist = points.get(u).distance(points.get(v));
          if (dist < minDist[v]) {
            minDist[v] = dist;
            parent[v] = u;
          }
        }
      }
    }

    List<LineString> roadSegments = new ArrayList<>();
    for (int i = 1; i < n; i++) {
      if (parent[i] != -1) {
        Coordinate[] coords = new Coordinate[]{
                points.get(i).getCoordinate(),
                points.get(parent[i]).getCoordinate()
        };
        roadSegments.add(gf.createLineString(coords));
      }
    }
    return roadSegments;
  }

  private void drawLine(LineString line, GeneratedMapModel generatedMapModel, int roadRadius) {
    Coordinate[] coords = line.getCoordinates();
    if (coords.length < 2) {
      return;
    }

    int x0 = (int) Math.round(coords[0].x);
    int y0 = (int) Math.round(coords[0].y);
    int x1 = (int) Math.round(coords[1].x);
    int y1 = (int) Math.round(coords[1].y);
    int dx = Math.abs(x1 - x0);
    int dy = Math.abs(y1 - y0);
    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    Random random = new Random();

    while (true) {
      // Zeichne an der zentralen Position einen Kreis, der die Straße verbreitert und ausgefranste Kanten erzeugt.
      for (int offsetX = -roadRadius; offsetX <= roadRadius; offsetX++) {
        for (int offsetY = -roadRadius; offsetY <= roadRadius; offsetY++) {
          double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
          if (distance <= roadRadius - 0.5) {
            generatedMapModel.addPixel(x0 + offsetX, y0 + offsetY, GlobalColors.ROAD);
          } else if (distance <= roadRadius) {
            if (random.nextDouble() > 0.3) {
              generatedMapModel.addPixel(x0 + offsetX, y0 + offsetY, GlobalColors.ROAD);
            }
          }
        }
      }
      if (x0 == x1 && y0 == y1) {
        break;
      }
      int e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        x0 += sx;
      }
      if (e2 < dx) {
        err += dx;
        y0 += sy;
      }
    }
  }

  private void drawMarker(int centerX, int centerY, int markerRadius, GeneratedMapModel generatedMapModel, Color markerColor) {
    for (int offsetX = -markerRadius; offsetX <= markerRadius; offsetX++) {
      for (int offsetY = -markerRadius; offsetY <= markerRadius; offsetY++) {
        if (Math.sqrt(offsetX * offsetX + offsetY * offsetY) <= markerRadius) {
          generatedMapModel.addPixel(centerX + offsetX, centerY + offsetY, markerColor);
        }
      }
    }
  }

  private String getRandomVillageName() {
    String prefix = PREFIXES[RANDOM.nextInt(PREFIXES.length)];
    String root = ROOTS[RANDOM.nextInt(ROOTS.length)];
    String suffix = SUFFIXES[RANDOM.nextInt(SUFFIXES.length)];
    return prefix + root + suffix;
  }
}
