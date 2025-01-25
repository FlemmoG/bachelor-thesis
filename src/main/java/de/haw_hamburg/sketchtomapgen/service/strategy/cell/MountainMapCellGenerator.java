package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import com.auburn.fastnoiselite.FastNoiseLite;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.Random;

public class MountainMapCellGenerator implements MapCellGenerationStrategy {
  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    FastNoiseLite noiseGenerator = new FastNoiseLite();
    noiseGenerator.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
    noiseGenerator.SetSeed(new Random().nextInt());

    // Zusätzlicher Noise für Blending
    FastNoiseLite blendNoiseGenerator = new FastNoiseLite();
    blendNoiseGenerator.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
    blendNoiseGenerator.SetSeed(new Random().nextInt());
    blendNoiseGenerator.SetFrequency(0.06f);

    Polygon cellPolygon = cellModel.getPolygon();
    Envelope envelope = cellPolygon.getEnvelopeInternal();
    double minX = cellPolygon.getEnvelopeInternal().getMinX();
    double minY = cellPolygon.getEnvelopeInternal().getMinY();
    double width = cellPolygon.getEnvelopeInternal().getWidth();
    double height = cellPolygon.getEnvelopeInternal().getHeight();

    int resolution = generatedMapModel.getWidth();

    for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x += 1) {
      for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y += 1) {
        double normalizedX = (double) x / resolution;
        double normalizedY = (double) y / resolution;
        Coordinate coordinate = new Coordinate(x, y);

        if (cellPolygon.contains(new GeometryFactory().createPoint(coordinate))) {

          double distanceToEdge = calculateSmoothDistanceToEdge(coordinate, cellPolygon, width, height);


          double noiseX = normalizedX * width + minX;
          double noiseY = normalizedY * height + minY;


          float primaryNoiseValue = noiseGenerator.GetNoise((float) noiseX, (float) noiseY);
          float blendNoiseValue = blendNoiseGenerator.GetNoise((float) noiseX, (float) noiseY);

          // Weicher Übergang durch Gewichtung mit Abstand zum Rand
          double smoothedHeight = interpolateHeight(
                  primaryNoiseValue,
                  blendNoiseValue,
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

  private double interpolateHeight(float primaryNoise, float blendNoise, double edgeDistance) {
    // Kombination von primärem Noise und blendendem Noise
    double combinedNoise = primaryNoise * (1 - edgeDistance) + blendNoise * edgeDistance;

    // Endgültige Höhe basierend auf der Gewichtung und der Distanz
    return combinedNoise * edgeDistance;
  }

  // Farbzuordnung basierend auf der Höhe
  private Color getColorForHeight(double height) {
    if (height == 0) { // Flachland (nur 0)
      System.out.println("Flach");
      return GlobalColors.TOTALLY_FLAT;
    } else if (height < 0.1) { // Hügelige Landschaft (mehr braun)
      return GlobalColors.MAINLY_FLAT.interpolate(GlobalColors.LITTLE_HILLY, height / 0.1);
    } else if (height < 0.3) { // Berge (weniger weiß, mehr braun)
      System.out.println("hoch");
      return GlobalColors.LITTLE_HILLY.interpolate(GlobalColors.MAINLY_HILLY, (height - 0.1) / 0.2);
    } else { // Schnee-bedeckte Gipfel
      System.out.println("sehr hoch");
      return GlobalColors.MAINLY_HILLY.interpolate(Color.WHITE, (height - 0.3) / 0.7);
    }
  }
}

