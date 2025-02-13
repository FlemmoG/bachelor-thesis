package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import com.github.sjcasey21.wavefunctioncollapse.OverlappingModel;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import de.haw_hamburg.sketchtomapgen.util.GlobalColors;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.distance.DistanceOp;

public class ForestMapCellGenerator implements MapCellGenerationStrategy {
  private static final double MAX_BLEND_DISTANCE = 30.0; // Abstand zum Rand
  private static final double NOISE_STRENGTH = 0.3; // Bestimmt die Stärke des Weichzeichnens
  private static final int WFC_TILE_SIZE = 128; // Größe des WFC Outputs
  private final GeometryFactory geometryFactory = new GeometryFactory();

  // generiert die Kartenabschnitte für Waldregionen
  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    try {
      URL wfcInitPictureUrl = getClass().getResource(AssetRoutes.FOREST_MAP_CELL_ASSET);
      BufferedImage inputImage = ImageIO.read(wfcInitPictureUrl);

      Polygon cellPolygon = cellModel.getPolygon();
      Envelope envelope = cellPolygon.getEnvelopeInternal();

      int minX = (int) Math.floor(envelope.getMinX());
      int maxX = (int) Math.floor(envelope.getMaxX());
      int minY = (int) Math.floor(envelope.getMinY());
      int maxY = (int) Math.floor(envelope.getMaxY());

      int outputWidth = maxX - minX + 1;
      int outputHeight = maxY - minY + 1;

      Geometry cellBoundary = cellPolygon.getBoundary();
      PreparedGeometry preparedCellPolygon = PreparedGeometryFactory.prepare(cellPolygon);

      // WFC Model initialisieren
      OverlappingModel model = new OverlappingModel(
              inputImage,
              3,
              WFC_TILE_SIZE,
              WFC_TILE_SIZE,
              true,
              false,
              2,
              -1
      );

      // wiederholt versuchen, bis erfolgreich
      while (!model.run(new Random().nextInt(), 0)) {
        System.out.println("WFC Fehler, retrying...");
      }

      BufferedImage wfcTile = model.graphics();
      BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);

      Graphics2D g2d = outputImage.createGraphics();
      TexturePaint texture = new TexturePaint(wfcTile, new Rectangle(0, 0, WFC_TILE_SIZE, WFC_TILE_SIZE));
      g2d.setPaint(texture);
      g2d.fillRect(0, 0, outputWidth, outputHeight);
      g2d.dispose();

      for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
          Coordinate coord = new Coordinate(x, y);
          if (preparedCellPolygon.covers(geometryFactory.createPoint(coord))) {
            int imgX = x - minX;
            int imgY = y - minY;

            double distance = DistanceOp.distance(
                    geometryFactory.createPoint(coord),
                    cellBoundary
            );

            int rgb = outputImage.getRGB(imgX, imgY);
            Color color = applyEdgeEffects(
                    Color.rgb(
                            (rgb >> 16) & 0xFF,
                            (rgb >> 8) & 0xFF,
                            rgb & 0xFF
                    ),
                    distance
            );

            generatedMapModel.addPixel(x, y, color);
          }
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Rand weichzeichnen
  private Color applyEdgeEffects(Color baseColor, double distance) {
    if (distance < MAX_BLEND_DISTANCE) {
      double blendFactor = distance / MAX_BLEND_DISTANCE;
      return blendColors(baseColor, blendFactor);
    }
    return baseColor;
  }

  private Color blendColors(Color source, double blendFactor) {
    double easedFactor = Math.pow(blendFactor, 2);
    Random random = new Random((long) (blendFactor * 1000));
    double noise = random.nextDouble() * NOISE_STRENGTH;
    double finalFactor = Math.min(1, Math.max(0, easedFactor + noise - NOISE_STRENGTH / 2));

    return source.interpolate(GlobalColors.GROUND_COLOR, 1 - finalFactor);
  }
}