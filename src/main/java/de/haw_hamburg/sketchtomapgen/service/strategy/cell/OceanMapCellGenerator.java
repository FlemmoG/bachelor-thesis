package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import com.auburn.fastnoiselite.FastNoiseLite;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.Random;

public class OceanMapCellGenerator implements MapCellGenerationStrategy{
  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    FastNoiseLite noiseGenerator = new FastNoiseLite();
    noiseGenerator.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
    noiseGenerator.SetSeed(new Random().nextInt());

    Polygon cellPolygon = cellModel.getPolygon();
    Envelope envelope = cellPolygon.getEnvelopeInternal();
    double minX = cellPolygon.getEnvelopeInternal().getMinX();
    double minY = cellPolygon.getEnvelopeInternal().getMinY();
    double width = cellPolygon.getEnvelopeInternal().getWidth();
    double height = cellPolygon.getEnvelopeInternal().getHeight();

    for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x += 1) {
      for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y += 1) {
        double normalizedX = (double) x / generatedMapModel.getWidth();
        double normalizedY = (double) y / generatedMapModel.getHeight();
        Coordinate coordinate = new Coordinate(x, y);

        if (cellPolygon.contains(new GeometryFactory().createPoint(coordinate))) {

          double distanceToEdge = calculateSmoothDistanceToEdge(coordinate, cellPolygon, width, height);


          double noiseX = normalizedX * width + minX;
          double noiseY = normalizedY * height + minY;


          float primaryNoiseValue = noiseGenerator.GetNoise((float) noiseX, (float) noiseY);

          // Weicher Übergang durch Gewichtung mit Abstand zum Rand
          double smoothedHeight = interpolateHeight(
                  primaryNoiseValue,
                  distanceToEdge
          );

          // Bestimme die Höhe basierend auf dem smoothedHeight
          Color heightColor = getColorForHeight(smoothedHeight);

          // Setze die Pixel im generierten Kartenmodell
          generatedMapModel.addPixel(x, y, heightColor);
        }
      }
    }
  }

  private double calculateSmoothDistanceToEdge(Coordinate point, Polygon polygon, double width, double height) {
    double maxDistance = Math.min(width, height);
    double minDistanceToEdge = polygon.getBoundary().distance(new GeometryFactory().createPoint(point));
    double normalizedDistance = Math.min(1.0, minDistanceToEdge / maxDistance);
    return Math.pow(normalizedDistance, 0.5);
  }

  private double interpolateHeight(float primaryNoise, double edgeDistance) {
    double combinedNoise = primaryNoise * (1 - edgeDistance) * edgeDistance;
    return combinedNoise * edgeDistance;
  }

  private Color getColorForHeight(double height) {
    if (height == 0.0) {
      return GlobalColors.WATER_SURFACE.interpolate(GlobalColors.TOTALLY_FLAT, 0.5);
    } else if (height < 0.05) {
      return GlobalColors.SHALLOW_WATER.interpolate(GlobalColors.WATER_SURFACE, height / 0.05);
    } else if (height < 0.18) {
      return GlobalColors.WATER_SURFACE.interpolate(GlobalColors.DEEP_WATER, (height - 0.05) / 0.05);
    } else {
      return GlobalColors.DEEP_WATER.interpolate(Color.WHITE, (height - 0.1) / 0.9);
    }
  }
}
