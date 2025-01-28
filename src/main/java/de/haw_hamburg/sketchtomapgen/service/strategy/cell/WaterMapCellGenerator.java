package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;


public class WaterMapCellGenerator implements MapCellGenerationStrategy {
  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    Polygon voronoiPoly = cellModel.getPolygon();
    Coordinate centroid = cellModel.getCentroid();

    // 1. Maximalen Radius bis zur Polygonkante berechnen
    double maxRadius = calculateMaxRadius(voronoiPoly, centroid);
    System.out.println(maxRadius);

    // 2. Organischen See mit variabler Form erzeugen
    Geometry lakeGeometry = createOrganicLakeShape(
            voronoiPoly,
            centroid,
            maxRadius
    );

    // 3. Pixelweise Rendering
    renderLake(generatedMapModel, voronoiPoly, lakeGeometry);
  }

  private double calculateMaxRadius(Polygon polygon, Coordinate centroid) {
    GeometryFactory geometryFactory = new GeometryFactory();

    Point centroidPoint = geometryFactory.createPoint(centroid);
    LineString boundary = polygon.getExteriorRing();

    DistanceOp distanceOp = new DistanceOp(boundary, centroidPoint);
    Coordinate[] closestPoints = distanceOp.nearestPoints();

    return centroidPoint.distance(geometryFactory.createPoint(closestPoints[0]));
  }

  private Geometry createOrganicLakeShape(Geometry boundary, Coordinate center, double baseRadius) {
    GeometryFactory gf = new GeometryFactory();

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

      circleCoords[i] = new Coordinate(
              center.x + effectiveRadius * Math.cos(angle),
              center.y + effectiveRadius * Math.sin(angle)
      );
    }

    circleCoords[circleSegments] = circleCoords[0];
    Geometry rawLakeShape = gf.createPolygon(gf.createLinearRing(circleCoords), null);

    return rawLakeShape.intersection(boundary);
  }


  private void renderLake(GeneratedMapModel model, Geometry boundary, Geometry lake) {
    Envelope env = boundary.getEnvelopeInternal();
    GeometryFactory gf = new GeometryFactory();

    for(int x = (int)env.getMinX(); x <= env.getMaxX(); x++) {
      for(int y = (int)env.getMinY(); y <= env.getMaxY(); y++) {
        Coordinate c = new Coordinate(x, y);
        Point p = gf.createPoint(c);

        if(boundary.contains(p)) {
          Color color = lake.contains(p)
                  ? GlobalColors.WATER_SURFACE
                  : GlobalColors.TOTALLY_FLAT;
          model.addPixel(x, y, color);
        }
      }
    }
  }
}
