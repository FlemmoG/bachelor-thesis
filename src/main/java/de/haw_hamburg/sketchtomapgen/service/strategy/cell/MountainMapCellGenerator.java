package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import com.auburn.fastnoiselite.FastNoiseLite;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.service.strategy.cell.MapCellGenerationStrategy;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
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

  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    // Skalierungsvariablen
    final double mountainSizeFactor = 2;   // Multipliziert die Größe der Berge
    final double noiseThreshold = 0.2;      // Ab diesem Noise-Wert werden Berge gezeichnet
    final int stepSize = 20;                // Schrittweite für die Iteration (größere Werte = weniger Berge)
    final int minDistanceBetweenAssets = 30; // Minimaler Abstand zwischen zwei Bergen
    final int minMountainSize = 30; // Minimale Größe der Berge (verhindert zu kleine Berge, die unnatürlich wirken)
    final int minDistanceFromBoundary = 30;  // Minimaler Abstand zu den Zellgrenzen

    // Noise-Generator initialisieren
    FastNoiseLite fastNoiseLite = new FastNoiseLite(new Random().nextInt());
    fastNoiseLite.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
    fastNoiseLite.SetFrequency(0.01f);

    // Voronoi-Polygon und dessen Envelope holen
    Polygon polygon = cellModel.getPolygon();
    Envelope envelope = polygon.getEnvelopeInternal();
    PreparedGeometry preparedCellPolygon = PreparedGeometryFactory.prepare(polygon);

    // Mountain-Asset laden
    URL mountainImageUrl = getClass().getResource(AssetRoutes.MOUNTAINS_ASSET);
    Image mountainImage = new Image(String.valueOf(mountainImageUrl));

    Random random = new Random();

    // Liste zur Nachverfolgung der gezeichneten Asset-Positionen
    List<Coordinate> drawnAssets = new ArrayList<>();
    Random stepRandomizer = new Random();
    // Schleife über alle Punkte im Envelope mit festem Schritt (stepSize)
    for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x += stepSize + stepRandomizer.nextInt(10)) {
      for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y += stepSize + stepRandomizer.nextInt(10)) {
        Coordinate point = new Coordinate(x, y);

        // Prüfen, ob der Punkt zu nahe an einem anderen Asset liegt
        boolean isTooClose = drawnAssets.stream()
                .anyMatch(existing -> point.distance(existing) < minDistanceBetweenAssets);

        if (isTooClose){
          continue;
        }

        // Noise-Wert für die aktuelle Position berechnen
        float noiseValue = fastNoiseLite.GetNoise(x, y);

        // Noise-Wert in den Bereich [0, 1] normalisieren
        float normalizedValue = (noiseValue + 1.0f) / 2.0f;

        // Nur zeichnen, wenn der Noise-Wert hoch genug ist
        if (normalizedValue > noiseThreshold) {

          if (preparedCellPolygon.covers(new GeometryFactory().createPoint(point))) {
            if (distanceToBoundary(point, polygon) < minDistanceFromBoundary) {
              continue;
            }
            int mountainSize = (int) (5 + Math.pow(normalizedValue, 3) * 100 * mountainSizeFactor);
            System.out.println(mountainSize);
            if (mountainSize >= minMountainSize) {
              // Randomly flip the image
              Image finalMountainImage = random.nextBoolean() ? flipImageHorizontally(mountainImage) : mountainImage;

              // Berg zeichnen
              generatedMapModel.addAsset((int) point.getX(), (int) point.getY(), finalMountainImage, mountainSize);

              // Gezeichnete Position speichern
              drawnAssets.add(point);
            }
          }
        }
      }
    }
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
            new GeometryFactory().createPoint(coordinate),
            polygon.getBoundary()
    );
  }
}