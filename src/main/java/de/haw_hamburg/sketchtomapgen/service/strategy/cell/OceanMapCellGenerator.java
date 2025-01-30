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
  private static final float NOISE_FREQUENCY = 0.004f;
  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    FastNoiseLite noiseGenerator = new FastNoiseLite();
    noiseGenerator.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
    noiseGenerator.SetSeed(new Random().nextInt());
    noiseGenerator.SetFrequency(NOISE_FREQUENCY);

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

        if (cellPolygon.covers(new GeometryFactory().createPoint(coordinate))
                && coordinate.x > 0 && coordinate.x < generatedMapModel.getWidth()
                && coordinate.y > 0 && coordinate.y < generatedMapModel.getHeight()
        ) {

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
    final double SHALLOW_MAX = 0.15;
    final double SURFACE_MAX = 0.3;

    if (height <= SHALLOW_MAX) {
      return GlobalColors.SHALLOW_WATER.interpolate(
              GlobalColors.WATER_SURFACE,
              height / SHALLOW_MAX
      );
    } else if (height <= SURFACE_MAX) {
      double blend = (height - SHALLOW_MAX) / (SURFACE_MAX - SHALLOW_MAX);
      return GlobalColors.WATER_SURFACE.interpolate(
              GlobalColors.DEEP_WATER,
              blend
      );
    } else {
      return GlobalColors.DEEP_WATER;
    }
  }
}
