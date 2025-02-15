package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.model.collection.CellModelCollection;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

import java.util.*;

import static de.haw_hamburg.sketchtomapgen.util.Icon.OCEAN;

public class RegionPartitioningService {
  private SketchModel sketchModel;
  private CellModelCollection voronoiCellModels;
  private final int width;
  private final int height;
  private GeometryCollection concaveHullsForClusters;
  private final GeometryFactory geometryFactory;
  private final Envelope totalAreaEnvelope;

  public RegionPartitioningService(int width, int height) {
    this.width = width;
    this.height = height;
    geometryFactory = new GeometryFactory();
    totalAreaEnvelope = new Envelope(0, width, 0, height);
  }

  public void initializeService(SketchModel sketchModel) {
    this.sketchModel = sketchModel;
  }

  public void addIcon(Coordinate coordinate, Icon icon) {
    sketchModel.addIcon(coordinate, icon);
  }

  // Erstellt aus N Umrissen N Voronoi Diagramme auf Basis der Icons
  public void computeVoronoiFromIcons() {
    if (concaveHullsForClusters == null) {
      concaveHullsForClusters = sketchModel.getOutlinesForClusters();
    }
    Map<Coordinate, Icon> icons = sketchModel.getIcons();

    CellModelCollection voronoiCellModels = new CellModelCollection();

    Geometry nonCoveredArea = getTotalArea();

    // Über alle Cluster iterieren
    for (int i = 0; i < concaveHullsForClusters.getNumGeometries(); i++) {
      Geometry concaveHull = concaveHullsForClusters.getGeometryN(i);

      // Nur Icons und dessen Koordinaten beachten, wenn innerhalb des aktuellen Clutsers
      List<Coordinate> pointsWithinHull = new ArrayList<>();
      for (Coordinate coord : icons.keySet()) {
        if (concaveHull.covers(geometryFactory.createPoint(coord))) {
          pointsWithinHull.add(coord);
        }
      }

      if (pointsWithinHull.isEmpty()) continue;

      // Kein Voronoi nötig, wenn Umriss nur ein Icon enthält
      if (pointsWithinHull.size() == 1) {
        Coordinate singlePoint = pointsWithinHull.get(0);
        Icon icon = icons.get(singlePoint);
        if (icon != null) {
          CellModel cellModel = new CellModel((Polygon) concaveHull, getColor(icon), icon, singlePoint);
          voronoiCellModels.add(cellModel);
          nonCoveredArea = nonCoveredArea.difference(concaveHull);
        }
        continue;
      }

      // Voronoi aus den Icons im Cluster bauen
      VoronoiDiagramBuilder voronoiDiagramBuilder = new VoronoiDiagramBuilder();
      voronoiDiagramBuilder.setSites(pointsWithinHull);
      voronoiDiagramBuilder.setClipEnvelope(totalAreaEnvelope);
      Geometry voronoiDiagram = voronoiDiagramBuilder.getDiagram(geometryFactory);

      for (int j = 0; j < voronoiDiagram.getNumGeometries(); j++) {
        Geometry cell = voronoiDiagram.getGeometryN(j);

        if (!cell.isEmpty() && cell instanceof Polygon polygon) {
          // Icon zu Centroiden zuordnen
          Coordinate centroid = polygon.getCentroid().getCoordinate();
          Coordinate nearestIconCoord = findNearestIconCoordinate(centroid, icons);
          Icon icon = icons.get(nearestIconCoord);

          // Zelle auf Umriss zuschneiden (falls am Rand)
          if (icon != null) {
            Color color = getColor(icon);
            Geometry clippedCell = polygon.intersection(concaveHull);
            List<Polygon> polygons = new ArrayList<>();
            if (clippedCell instanceof Polygon clippedPolygon) {
              polygons.add(clippedPolygon);

              // Möglicherweise ist die Zelle ein MultiPolygon, da aus nicht direkt angrenzenden Polygonen besteht
            } else if (clippedCell instanceof MultiPolygon clippedMultiPolygon) {
              for (int k = 0; k < clippedMultiPolygon.getNumGeometries(); k++) {
                Geometry geom = clippedMultiPolygon.getGeometryN(k);
                if (geom instanceof Polygon) {
                  polygons.add((Polygon) geom);
                }
              }
            }
            // Über die beschnittenen Polygone iterieren und CellModels erstellen
            for (Polygon clippedPolygon : polygons) {
              CellModel cellModel = new CellModel(clippedPolygon, color, icon, nearestIconCoord);
              voronoiCellModels.add(cellModel);
            }
          }
        }
      }

      // Ocean Polygon berechnen, indem die Polygon differenz genommen wird
      for (CellModel cellModel : voronoiCellModels) {
        nonCoveredArea = nonCoveredArea.difference(cellModel.getPolygon());
      }
    }

    // Nachdem alle Hüllen bearbeitet wurden wird die Ozean Zelle erstellt
    if (!nonCoveredArea.isEmpty()) {
      if (nonCoveredArea instanceof Polygon polygon) {
        Color waterColor = Color.rgb(0, 0, 255, 0.5);
        CellModel waterCellModel = new CellModel(polygon, waterColor, OCEAN, null);
        voronoiCellModels.add(waterCellModel);
      }
    }

    this.voronoiCellModels = voronoiCellModels;
  }

  // Über alle CellModels und dessen Bounding Box iterieren und prüfen, ob Punkt von Polygon abgedeckt, wenn ja, dann dessen Farbe zeichnen
  public WritableImage getImage() {
    WritableImage image = new WritableImage(width, height);
    PixelWriter pixelWriter = image.getPixelWriter();

    for (CellModel cellModel : voronoiCellModels) {
      Polygon polygon = cellModel.getPolygon();
      PreparedGeometry preparedCellPolygon = PreparedGeometryFactory.prepare(polygon);

      Envelope envelope = polygon.getEnvelopeInternal();

      for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x++) {
        for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y++) {
          if (x >= 0 && x < width && y >= 0 && y < height) {
            Coordinate point = new Coordinate(x, y);
            if (preparedCellPolygon.covers(geometryFactory.createPoint(point))) {
              pixelWriter.setColor(x, y, cellModel.getColor());
            }
          }
        }
      }
    }
    return image;
  }

  private static Color getColor(Icon icon) {
    return switch (icon) {
      case MOUNTAIN -> Color.rgb(139, 69, 19, 0.5); // Braun für Berge
      case TREE -> Color.rgb(34, 139, 34, 0.5); // Dunkelgrün für Bäume
      case WATER -> Color.rgb(0, 191, 255, 0.5); // Hellblau für Wasser
      case VILLAGE -> Color.rgb(184, 134, 11, 0.5); // Goldbraun für Dörfer
      case BLANK -> Color.rgb(200, 200, 200, 0.5); // Grauton für leere Zellen
      case OCEAN -> Color.rgb(0, 0, 139, 0.5); // Dunkelblau für Ozeane
    };
  }

  private Geometry getTotalArea() {
    Coordinate[] coordinates = new Coordinate[]{
            new Coordinate(0, 0),
            new Coordinate(width, 0),
            new Coordinate(width, height),
            new Coordinate(0, height),
            new Coordinate(0, 0)
    };

    return geometryFactory.createPolygon(coordinates);
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
