package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import com.github.sjcasey21.wavefunctioncollapse.Main;
import com.github.sjcasey21.wavefunctioncollapse.OverlappingModel;
import com.github.sjcasey21.wavefunctioncollapse.SimpleTiledModel;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import javax.imageio.ImageIO;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class VillageMapCellGenerator implements MapCellGenerationStrategy {
  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    try {
      // Lade das Initialisierungsbild
      URL wfcInitPictureUrl = getClass().getResource(AssetRoutes.VILLAGE_TILE_ASSET);
      BufferedImage inputImage = ImageIO.read(wfcInitPictureUrl);

      Polygon cellPolygon = cellModel.getPolygon();
      Envelope envelope = cellPolygon.getEnvelopeInternal();

      int outputWidth = (int) Math.ceil(envelope.getMaxX() - envelope.getMinX());
      int outputHeight = (int) Math.ceil(envelope.getMaxY() - envelope.getMinY());

      // Parameter für das Overlapping Model
      int N = 3;
      boolean periodicInput = true;
      boolean periodicOutput = false;
      int symmetry = 1;
      int ground = 102;

      System.out.println(outputHeight  + " " + outputWidth);

      // Erstelle das OverlappingModel
      OverlappingModel model = new OverlappingModel(
              inputImage,
              N,
              256,
              256,
              periodicInput,
              periodicOutput,
              symmetry,
              ground
      );

      // Starte die WFC-Generierung
      boolean success = model.run(new Random().nextInt(), 0);
      if (success) {
        BufferedImage outputImageOg = model.graphics();
        BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, outputImageOg.getType());
        Graphics2D g2d = outputImage.createGraphics();
        int size = Math.max(outputWidth, outputHeight);
        g2d.drawImage(outputImageOg, 0, 0, size, size, null);
        g2d.dispose();

        // Iteriere über die Koordinaten im Bereich des Polygons
        for (int x = (int) envelope.getMinX(); x <= envelope.getMaxX(); x++) {
          for (int y = (int) envelope.getMinY(); y <= envelope.getMaxY(); y++) {
            // Punkt erstellen und prüfen, ob er im Polygon liegt
            if (cellPolygon.contains(new GeometryFactory().createPoint(new Coordinate(x, y)))) {
              // Farbinformation aus dem generierten Bild holen
              int imgX = Math.floorMod(x, outputImage.getWidth()); // Um sicherzustellen, dass x innerhalb der Bildbreite liegt
              int imgY = Math.floorMod(y, outputImage.getHeight()); // Um sicherzustellen, dass y innerhalb der Bildhöhe liegt
              int rgb = outputImage.getRGB(imgX, imgY);
              Color color = Color.rgb(
                      (rgb >> 16) & 0xFF,
                      (rgb >> 8) & 0xFF,
                      rgb & 0xFF
              );

              // Pixel ins GeneratedMapModel schreiben
              generatedMapModel.addPixel(x, y, color);
            }
          }
        }
      } else {
        System.err.println("Fehler bei der WFC-Generierung.");
      }

    } catch (IOException e) {
      System.err.println("Fehler beim Laden oder Speichern des Bildes: " + e.getMessage());
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

}
