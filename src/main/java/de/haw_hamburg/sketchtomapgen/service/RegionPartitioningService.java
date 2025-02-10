package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.model.collection.CellModelCollection;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

import java.util.*;

import static de.haw_hamburg.sketchtomapgen.util.Icon.OCEAN;
import static de.haw_hamburg.sketchtomapgen.util.Icon.WATER;

public class RegionPartitioningService {
  private SketchModel sketchModel;
  private CellModelCollection voronoiCellModels;
  private int width;
  private int height;

  public RegionPartitioningService(int width, int height){
    this.width = width;
    this.height = height;
  }
  public void initializeService(SketchModel sketchModel){
    this.sketchModel = sketchModel;
  }

  public void addIcon(Coordinate coordinate, Icon icon){
    sketchModel.addIcon(coordinate, icon);
  }

  public void computeVoronoiFromIcons() {
    GeometryCollection concaveHullsForClusters = sketchModel.getConcaveHullsForClusters();
    Map<Coordinate, Icon> icons = sketchModel.getIcons();

    CellModelCollection voronoiCellModels = new CellModelCollection();
    Random random = new Random();

    Geometry nonCoveredArea = getTotalArea();

    for (int i = 0; i < concaveHullsForClusters.getNumGeometries(); i++) {
      Geometry concaveHull = concaveHullsForClusters.getGeometryN(i);

      // Filter points within the current concave hull
      List<Coordinate> pointsWithinHull = new ArrayList<>();
      for (Coordinate coord : icons.keySet()) {
        if (concaveHull.covers(new GeometryFactory().createPoint(coord))) {
          pointsWithinHull.add(coord);
        }
      }

      if (pointsWithinHull.isEmpty()) continue;

      // Handle cases where only one point is within the hull
      if (pointsWithinHull.size() == 1) {
        Coordinate singlePoint = pointsWithinHull.get(0);
        Icon icon = icons.get(singlePoint);
        if (icon != null) {
          Color color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 0.5);
          CellModel cellModel = new CellModel((Polygon) concaveHull, color, icon, singlePoint);
          voronoiCellModels.add(cellModel);
          nonCoveredArea = nonCoveredArea.difference(concaveHull);
        }
        continue;
      }

      VoronoiDiagramBuilder voronoiDiagramBuilder = new VoronoiDiagramBuilder();
      voronoiDiagramBuilder.setSites(pointsWithinHull);
      Geometry voronoiDiagram = voronoiDiagramBuilder.getDiagram(new GeometryFactory());

      for (int j = 0; j < voronoiDiagram.getNumGeometries(); j++) {
        Geometry cell = voronoiDiagram.getGeometryN(j);
        Geometry clippedCell = cell.intersection(concaveHull);

        if (!clippedCell.isEmpty() && clippedCell instanceof Polygon polygon) {
          // Get the centroid and find nearest icon
          Coordinate centroid = polygon.getCentroid().getCoordinate();
          Coordinate nearestIconCoord = findNearestIconCoordinate(centroid, icons);
          Icon icon = icons.get(nearestIconCoord);

          if (icon != null) {
            Color color = switch (icon) {
              case MOUNTAIN -> Color.rgb(139, 69, 19, 0.5); // Braun für Berge
              case TREE -> Color.rgb(34, 139, 34, 0.5); // Dunkelgrün für Bäume
              case WATER -> Color.rgb(0, 191, 255, 0.5); // Hellblau für Wasser
              case VILLAGE -> Color.rgb(184, 134, 11, 0.5); // Goldbraun für Dörfer
              case BLANK -> Color.rgb(200, 200, 200, 0.5); // Grauton für leere Zellen
              case OCEAN -> Color.rgb(0, 0, 139, 0.5); // Dunkelblau für Ozeane
            };

            CellModel cellModel = new CellModel(polygon, color, icon, nearestIconCoord);
            voronoiCellModels.add(cellModel);
          }
        }
      }

      // Entferne den Bereich, der von den Polygonen abgedeckt wird
      for (CellModel cellModel : voronoiCellModels) {
        nonCoveredArea = nonCoveredArea.difference(cellModel.getPolygon());
      }
    }

    // Nachdem alle Hüllen bearbeitet wurden, erstellen wir die "Wasser"-Zelle für den verbleibenden Bereich
    if (!nonCoveredArea.isEmpty()) {
      if (nonCoveredArea instanceof Polygon polygon) {
        Color waterColor = Color.rgb(0, 0, 255, 0.5);
        CellModel waterCellModel = new CellModel(polygon, waterColor, OCEAN, null);
        voronoiCellModels.add(waterCellModel);
      }
    }

    this.voronoiCellModels = voronoiCellModels;
  }

  private Geometry getTotalArea() {
    GeometryFactory geometryFactory = new GeometryFactory();
    Coordinate[] coordinates = new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(width, 0),
            new Coordinate(width, height),
            new Coordinate(0, height),
            new Coordinate(0, 0)
    };

    return geometryFactory.createPolygon(coordinates);
  }


  public WritableImage getImage() {
    WritableImage image = new WritableImage(width, height);
    PixelWriter pixelWriter = image.getPixelWriter();

    // Für jedes Zellmodell (jede Zelle mit einem Polygon)
    for (CellModel cellModel : voronoiCellModels) {
      Polygon polygon = cellModel.getPolygon();
      Envelope envelope = polygon.getEnvelopeInternal();

      // Bestimme den y-Bereich (Scanlines), der das Polygon umfasst,
      // dabei wird auf die Bildgrenzen geachtet.
      int yMin = Math.max((int) Math.ceil(envelope.getMinY()), 0);
      int yMax = Math.min((int) Math.floor(envelope.getMaxY()), height - 1);

      // Hole die Koordinaten der äußeren Hülle (Exterior-Ring)
      Coordinate[] outerCoords = polygon.getExteriorRing().getCoordinates();

      // Für jede Scanline
      for (int y = yMin; y <= yMax; y++) {
        List<Double> intersections = new ArrayList<>();

        // --- Außenring verarbeiten ---
        // Iteriere über alle Kanten des Außenrings.
        // (Da der Ring geschlossen ist, enthält outerCoords[outerCoords.length-1] denselben Punkt wie outerCoords[0])
        for (int i = 0; i < outerCoords.length - 1; i++) {
          Coordinate p1 = outerCoords[i];
          Coordinate p2 = outerCoords[i + 1];

          // Wir berücksichtigen eine Kante nur, wenn die Scanline zwischen den y-Koordinaten der Endpunkte liegt.
          // Dabei wird der Fall, dass die Scanline genau einen Endpunkt trifft, nur einmal gezählt.
          if ((p1.y <= y && p2.y > y) || (p2.y <= y && p1.y > y)) {
            // Berechne den Schnittpunkt der horizontalen Linie y mit der Kante (p1,p2)
            double x = p1.x + (y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y);
            intersections.add(x);
          }
        }

        // --- Innenringe (Löcher) verarbeiten ---
        int numHoles = polygon.getNumInteriorRing();
        for (int r = 0; r < numHoles; r++) {
          Coordinate[] holeCoords = polygon.getInteriorRingN(r).getCoordinates();
          for (int i = 0; i < holeCoords.length - 1; i++) {
            Coordinate p1 = holeCoords[i];
            Coordinate p2 = holeCoords[i + 1];

            if ((p1.y <= y && p2.y > y) || (p2.y <= y && p1.y > y)) {
              double x = p1.x + (y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y);
              intersections.add(x);
            }
          }
        }

        // Sortiere die Schnittpunkte von links nach rechts
        Collections.sort(intersections);

        // Fülle die Pixel zwischen jeweils zwei benachbarten Schnittpunkten.
        // Dabei gehen wir davon aus, dass das innere des Polygons durch die gerade Anzahl
        // von Schnittpunkten und das even-odd-Prinzip korrekt abgedeckt ist.
        for (int i = 0; i < intersections.size(); i += 2) {
          if (i + 1 < intersections.size()) {
            int xStart = (int) Math.ceil(intersections.get(i));
            int xEnd   = (int) Math.floor(intersections.get(i + 1));

            // Optional: x-Koordinaten auf den Bereich des Envelopes bzw. Bildes beschränken
            xStart = Math.max(xStart, (int) Math.ceil(envelope.getMinX()));
            xEnd   = Math.min(xEnd, (int) Math.floor(envelope.getMaxX()));

            for (int x = xStart; x <= xEnd; x++) {
              if (x >= 0 && x < width) {
                pixelWriter.setColor(x, y, cellModel.getColor());
              }
            }
          }
        }
      }
    }
    return image;
  }


  private Coordinate findNearestIconCoordinate(Coordinate centroid, Map<Coordinate, Icon> icons) {
    double minDistance = Double.MAX_VALUE;
    Coordinate nearestCoord = null;

    for (Coordinate coord : icons.keySet()) {
      double distance = centroid.distance(coord);
      if (distance < minDistance) {
        minDistance = distance;
        nearestCoord = coord;
      }
    }

    return nearestCoord;
  }

  public CellModelCollection getVoronoiCellModels() {
    return voronoiCellModels;
  }
}
