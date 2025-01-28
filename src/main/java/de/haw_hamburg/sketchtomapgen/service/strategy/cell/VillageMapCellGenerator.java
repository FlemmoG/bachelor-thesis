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

  }

}
