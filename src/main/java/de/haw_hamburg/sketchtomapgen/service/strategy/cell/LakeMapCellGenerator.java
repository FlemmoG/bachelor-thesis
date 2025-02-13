package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.distance.DistanceOp;


public class LakeMapCellGenerator implements MapCellGenerationStrategy {
  private final GeometryFactory geometryFactory = new GeometryFactory();
  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    Polygon voronoiPoly = cellModel.getPolygon();
    Coordinate centroid = cellModel.getCentroid();

    double maxRadius = calculateMaxRadius(voronoiPoly, centroid);
    System.out.println(maxRadius);

    Geometry lakeGeometry = createOrganicLakeShape(voronoiPoly, centroid, maxRadius);

    renderLake(generatedMapModel, voronoiPoly, lakeGeometry);
  }

  // Maximalen Radius bis zur Polygonkante berechnen
  private double calculateMaxRadius(Polygon polygon, Coordinate centroid) {
    Point centroidPoint = geometryFactory.createPoint(centroid);
    LineString boundary = polygon.getExteriorRing();

    DistanceOp distanceOp = new DistanceOp(boundary, centroidPoint);
    Coordinate[] closestPoints = distanceOp.nearestPoints();

    return centroidPoint.distance(geometryFactory.createPoint(closestPoints[0]));
  }

  // Organischen See mit variabler Form erzeugen
  private Geometry createOrganicLakeShape(Geometry boundary, Coordinate center, double baseRadius) {
    // Adjusted parameters for smoother variation
    final double minRadiusFactor = 0.7;  // Increased minimal radius factor
    final double maxRadiusFactor = 0.3;  // Reduced maximum variation
    final int circleSegments = 64;       // Increased segments for smoother outline

    Coordinate[] circleCoords = new Coordinate[circleSegments + 1];

    // Calculate circle coordinates with smoother variation
    for (int i = 0; i < circleSegments; i++) {
      double angle = 2 * Math.PI * i / circleSegments;

      // Apply smoother radius variation
      double radiusVariation = minRadiusFactor + maxRadiusFactor * Math.random();
      double effectiveRadius = baseRadius * radiusVariation;

      circleCoords[i] = new Coordinate(center.x + effectiveRadius * Math.cos(angle), center.y + effectiveRadius * Math.sin(angle));
    }

    circleCoords[circleSegments] = circleCoords[0];
    Geometry rawLakeShape = geometryFactory.createPolygon(geometryFactory.createLinearRing(circleCoords), null);

    return rawLakeShape.intersection(boundary);
  }

  // Pixelweise Rendering
  private void renderLake(GeneratedMapModel model, Geometry boundary, Geometry lake) {
    Envelope env = boundary.getEnvelopeInternal();
    PreparedGeometry preparedCellPolygonLake = PreparedGeometryFactory.prepare(lake);
    PreparedGeometry preparedCellPolygonBoundary = PreparedGeometryFactory.prepare(boundary);


    for (int x = (int) env.getMinX(); x <= env.getMaxX(); x++) {
      for (int y = (int) env.getMinY(); y <= env.getMaxY(); y++) {
        Coordinate c = new Coordinate(x, y);
        Point p = geometryFactory.createPoint(c);

        if (preparedCellPolygonBoundary.covers(p)) {
          Color color = preparedCellPolygonLake.covers(p) ? lakeColorForPosition(p, lake.getCentroid(), p.distance(lake.getBoundary())) : GlobalColors.GROUND_COLOR;
          model.addPixel(x, y, color);
        }
      }
    }
  }

  // Farbinterpolation
  private Color lakeColorForPosition(Point position, Point center, double distanceToBoundary) {
    double t = Math.min(position.distance(center) / distanceToBoundary, 1.0);
    return GlobalColors.DEEP_OCEAN_COLOR.interpolate(GlobalColors.OCEAN_SURFACE_COLOR, t);
  }
}
