package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import de.haw_hamburg.sketchtomapgen.model.VoronoiCellModel;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

import java.util.*;

public class IconPlacementService {
  private SketchModel sketchModel;
  private List<VoronoiCellModel> voronoiCellModels;
  private int width;
  private int height;

  public IconPlacementService(int width, int height){
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

    List<VoronoiCellModel> voronoiCellModels = new ArrayList<>();
    Random random = new Random();

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
            // Convert polygon to coordinates including all internal points
            Set<Coordinate> allPoints = new HashSet<>();
            Envelope envelope = polygon.getEnvelopeInternal();

            // Iterate through the bounding box of the polygon
            for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x++) {
              for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y++) {
                // Check if point is within image bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                  Coordinate point = new Coordinate(x, y);
                  // Check if point is inside the clipped cell
                  if (clippedCell.contains(new GeometryFactory().createPoint(point))) {
                    allPoints.add(point);
                  }
                }
              }
            }

            Color color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            VoronoiCellModel voronoiCellModel = new VoronoiCellModel(allPoints, color, icon);
            voronoiCellModels.add(voronoiCellModel);
          }
        }
      }
    }

    this.voronoiCellModels = voronoiCellModels;
  }

  public WritableImage getImage() {
    WritableImage image = new WritableImage(width, height);
    PixelWriter pixelWriter = image.getPixelWriter();
    for (VoronoiCellModel voronoiCellModel : voronoiCellModels) {
      for (Coordinate point : voronoiCellModel.getPoints()) {
        pixelWriter.setColor((int) point.x, (int) point.y, voronoiCellModel.getColor());
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
}
