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
import org.locationtech.jts.operation.distance.DistanceOp;

public class TreeMapCellGenerator implements MapCellGenerationStrategy {
  private static final double MAX_BLEND_DISTANCE = 30.0;
  private static final double NOISE_STRENGTH = 0.3;
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final int WFC_GEN_SIZE = 256;

  @Override
  public void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel) {
    try {
      URL wfcInitPictureUrl = getClass().getResource(AssetRoutes.PIXE_TREE_ASSET);
      BufferedImage inputImage = ImageIO.read(wfcInitPictureUrl);

      Polygon cellPolygon = cellModel.getPolygon();
      Envelope envelope = cellPolygon.getEnvelopeInternal();

      int outputWidth = (int) Math.ceil(envelope.getMaxX() - envelope.getMinX());
      int outputHeight = (int) Math.ceil(envelope.getMaxY() - envelope.getMinY());

      // Precompute polygon boundary for distance calculations
      Geometry cellBoundary = cellPolygon.getBoundary();

      // WFC setup (unchanged)
      OverlappingModel model = new OverlappingModel(
              inputImage, 3, WFC_GEN_SIZE, WFC_GEN_SIZE, true, false, 1, 102
      );

      if (model.run(new Random().nextInt(), 0)) {
        BufferedImage outputImageOg = model.graphics();
        BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, outputImageOg.getType());
        Graphics2D g2d = outputImage.createGraphics();
        int size = Math.max(outputWidth, outputHeight);
        g2d.drawImage(outputImageOg, 0, 0, size, size, null);
        g2d.dispose();

        int minX = (int) envelope.getMinX();
        int minY = (int) envelope.getMinY();

        for (int x = minX; x <= envelope.getMaxX(); x++) {
          for (int y = minY; y <= envelope.getMaxY(); y++) {
            Coordinate coord = new Coordinate(x, y);
            if (cellPolygon.contains(GEOMETRY_FACTORY.createPoint(coord))) {
              int imgX = x - minX;
              int imgY = y - minY;

              if (imgX < 0 || imgX >= outputWidth || imgY < 0 || imgY >= outputHeight) continue;

              // Calculate distance to nearest cell edge
              double distance = DistanceOp.distance(
                      GEOMETRY_FACTORY.createPoint(coord),
                      cellBoundary
              );

              // Get tree pixel color
              int rgb = outputImage.getRGB(imgX, imgY);
              Color treeColor = Color.rgb(
                      (rgb >> 16) & 0xFF,
                      (rgb >> 8) & 0xFF,
                      rgb & 0xFF
              );

              // Apply blending near edges
              if (distance < MAX_BLEND_DISTANCE) {
                double blendFactor = distance / MAX_BLEND_DISTANCE;
                treeColor = blendColors(treeColor, blendFactor);
              }

              generatedMapModel.addPixel(x, y, treeColor);
            }
          }
        }
      } else {
        System.out.println("Fehler");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Color blendColors(Color source, double blendFactor) {
    double easedFactor = blendFactor * blendFactor;
    Random positionRandom = new Random((long) (blendFactor * 1000));
    double noise = positionRandom.nextDouble() * NOISE_STRENGTH;
    double finalFactor = Math.min(1, Math.max(0, easedFactor + noise - NOISE_STRENGTH/2));

    return source.interpolate(GlobalColors.TOTALLY_FLAT, 1 - finalFactor);
  }
}

