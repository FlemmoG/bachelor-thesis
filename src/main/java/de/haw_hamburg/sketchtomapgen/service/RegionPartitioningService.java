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
            Color color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256), 0.5);
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
    GeometryFactory geometryFactory = new GeometryFactory();

    for (CellModel cellModel : voronoiCellModels) {
      Polygon polygon = cellModel.getPolygon();
      Envelope envelope = polygon.getEnvelopeInternal();

      for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x++) {
        for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y++) {
          if (x >= 0 && x < width && y >= 0 && y < height) {
            Coordinate point = new Coordinate(x, y);
            if (polygon.contains(geometryFactory.createPoint(point))) {
              pixelWriter.setColor(x, y, cellModel.getColor());
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
