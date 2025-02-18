package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import com.auburn.fastnoiselite.FastNoiseLite;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MountainMapCellGenerator implements MapCellGenerationStrategy {
  final double MOUNTAIN_SIZE_FACTOR = 2;   // Multipliziert die Größe der Berge
  final double NOISE_THRESHOLD = 0.2;      // Ab diesem Noise-Wert werden Berge gezeichnet
  final int STEP_SIZE = 20;                // Schrittweite für die Iteration (größere Werte = weniger Berge)
  final int MIN_DISTANCE_BETWEEN_ASSETS = 30; // Minimaler Abstand zwischen zwei Bergen
  final int MIN_MOUNTAIN_SIZE = 30; // Minimale Größe der Berge (verhindert zu kleine Berge, die unnatürlich wirken)
  final int MIN_DISTANCE_FROM_BOUNDARY = 30;  // Minimaler Abstand zu den Zellgrenzen
  private final GeometryFactory geometryFactory = new GeometryFactory();

  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    // Noise-Generator initialisieren
    FastNoiseLite fastNoiseLite = new FastNoiseLite(new Random().nextInt());
    fastNoiseLite.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
    fastNoiseLite.SetFrequency(0.01f);

    // Voronoi-Polygon und dessen Envelope holen
    Polygon polygon = cellModel.getPolygon();
    Envelope envelope = polygon.getEnvelopeInternal();
    PreparedGeometry preparedCellPolygon = PreparedGeometryFactory.prepare(polygon);

    // Mountain-Asset laden
    URL mountainImageUrl = getClass().getResource(AssetRoutes.MOUNTAIN_MAP_CELL_ASSET);
    Image mountainImage = new Image(String.valueOf(mountainImageUrl));

    Random random = new Random();

    // Liste zur Nachverfolgung der gezeichneten Asset-Positionen
    List<Coordinate> drawnAssets = new ArrayList<>();
    Random stepRandomizer = new Random();
    // Schleife über alle Punkte im Envelope mit festem Schritt
    for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x += STEP_SIZE + stepRandomizer.nextInt(10)) {
      for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y += STEP_SIZE + stepRandomizer.nextInt(10)) {
        Coordinate point = new Coordinate(x, y);

        // Prüfen, ob der Punkt zu nahe an einem anderen Asset liegt
        boolean isTooClose = drawnAssets.stream()
                .anyMatch(existing -> point.distance(existing) < MIN_DISTANCE_BETWEEN_ASSETS);

        if (isTooClose){
          continue;
        }

        // Noise-Wert für die aktuelle Position berechnen und normalisieren
        float noiseValue = fastNoiseLite.GetNoise(x, y);
        float normalizedValue = (noiseValue + 1.0f) / 2.0f;

        // Nur zeichnen, wenn der Noise-Wert hoch genug ist
        if (normalizedValue > NOISE_THRESHOLD) {

          if (preparedCellPolygon.covers(new GeometryFactory().createPoint(point))) {
            if (distanceToBoundary(point, polygon) < MIN_DISTANCE_FROM_BOUNDARY) {
              continue;
            }
            int mountainSize = (int) (5 + Math.pow(normalizedValue, 3) * 100 * MOUNTAIN_SIZE_FACTOR);
            if (mountainSize >= MIN_MOUNTAIN_SIZE) {
              // Random Bild flips
              Image transformedImage = randomlyTransformImage(mountainImage, random);
              generatedMapModel.addAsset((int) point.getX(), (int) point.getY(), transformedImage, mountainSize);
              // Gezeichnete Position merken
              drawnAssets.add(point);
            }
          }
        }
      }
    }
  }

  private Image randomlyTransformImage(Image original, Random random) {
    Image transformed = original;

    if (random.nextBoolean()) {
      transformed = flipImageHorizontally(transformed);
    }

    // Rotation (-10° bis +10°)
    if (random.nextDouble() > 0.3) { // 70% Chance
      transformed = rotateImage(transformed, random.nextDouble() * 20 - 10);
    }

    return transformed;
  }

  private Image rotateImage(Image image, double degrees) {
    double radians = Math.toRadians(degrees);
    int width = (int) image.getWidth();
    int height = (int) image.getHeight();

    WritableImage output = new WritableImage(width, height);
    PixelWriter writer = output.getPixelWriter();
    PixelReader reader = image.getPixelReader();

    double centerX = width / 2.0;
    double centerY = height / 2.0;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        double dx = x - centerX;
        double dy = y - centerY;

        // Rotationsmatrix
        double rotatedX = dx * Math.cos(radians) - dy * Math.sin(radians) + centerX;
        double rotatedY = dx * Math.sin(radians) + dy * Math.cos(radians) + centerY;

        if (rotatedX >= 0 && rotatedX < width && rotatedY >= 0 && rotatedY < height) {
          Color color = reader.getColor((int) rotatedX, (int) rotatedY);
          writer.setColor(x, y, color);
        }
      }
    }
    return output;
  }

  private Image flipImageHorizontally(Image image) {
    int width = (int) image.getWidth();
    int height = (int) image.getHeight();

    WritableImage output = new WritableImage(width, height);
    PixelWriter writer = output.getPixelWriter();
    PixelReader reader = image.getPixelReader();

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        Color color = reader.getColor(x, y);

        writer.setColor(width - 1 - x, y, color);
      }
    }

    return output;
  }

  private double distanceToBoundary(Coordinate coordinate, Polygon polygon){
    return DistanceOp.distance(
            geometryFactory.createPoint(coordinate),
            polygon.getBoundary()
    );
  }
}