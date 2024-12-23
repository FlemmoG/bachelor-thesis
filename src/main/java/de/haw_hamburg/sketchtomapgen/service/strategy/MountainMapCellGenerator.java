package de.haw_hamburg.sketchtomapgen.service.strategy;

import com.auburn.fastnoiselite.FastNoiseLite;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.VoronoiCellModel;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MountainMapCellGenerator implements MapCellGenerationStrategy {

  @Override
  public void generateMap(VoronoiCellModel voronoiCellModel, GeneratedMapModel generatedMapModel) {
    // Skalierungsvariablen
    final double mountainSizeFactor = 1.0;   // Multipliziert die Größe der Berge
    final double noiseThreshold = 0.7;      // Ab diesem Noise-Wert werden Berge gezeichnet
    final int stepSize = 10;                // Schrittweite für die Iteration (größere Werte = weniger Berge)
    final int minDistanceBetweenAssets = 20; // Minimaler Abstand zwischen zwei Bergen

    // Noise-Generator initialisieren
    FastNoiseLite fastNoiseLite = new FastNoiseLite(new Random().nextInt());
    fastNoiseLite.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
    fastNoiseLite.SetFrequency(0.01f);

    // Voronoi-Polygon und dessen Envelope holen
    Polygon polygon = voronoiCellModel.getPolygon();
    Envelope envelope = polygon.getEnvelopeInternal();

    // Mountain-Asset laden
    URL mountainImageUrl = getClass().getResource(AssetRoutes.MOUNTAINS_ASSET);
    javafx.scene.image.Image mountainImage = new javafx.scene.image.Image(String.valueOf(mountainImageUrl));

    // Liste zur Nachverfolgung der gezeichneten Asset-Positionen
    List<Coordinate> drawnAssets = new ArrayList<>();

    // Schleife über alle Punkte im Envelope mit festem Schritt (stepSize)
    for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x += stepSize) {
      for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y += stepSize) {
        Coordinate point = new Coordinate(x, y);

        // Prüfen, ob der Punkt im Voronoi-Polygon liegt
        if (polygon.contains(new GeometryFactory().createPoint(point))) {
          // Noise-Wert für die aktuelle Position berechnen
          float noiseValue = fastNoiseLite.GetNoise(x, y);

          // Noise-Wert in den Bereich [0, 1] normalisieren
          float normalizedValue = (noiseValue + 1.0f) / 2.0f;

          // Nur zeichnen, wenn der Noise-Wert hoch genug ist
          if (normalizedValue > noiseThreshold) {
            // Prüfen, ob der Punkt zu nahe an einem anderen Asset liegt
            boolean isTooClose = drawnAssets.stream()
                    .anyMatch(existing -> point.distance(existing) < minDistanceBetweenAssets);

            if (!isTooClose) {
              // Größe der Berge berechnen (skalierbar durch mountainSizeFactor)
              int mountainSize = (int) (20 + normalizedValue * 30 * mountainSizeFactor);

              // Berg zeichnen
              generatedMapModel.addAsset(x, y, mountainImage, mountainSize);

              // Gezeichnete Position speichern
              drawnAssets.add(point);
            }
          }
        }
      }
    }
  }



}

