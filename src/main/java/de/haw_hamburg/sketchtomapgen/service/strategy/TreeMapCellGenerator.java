package de.haw_hamburg.sketchtomapgen.service.strategy;

import com.auburn.fastnoiselite.FastNoiseLite;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.VoronoiCellModel;
import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import de.haw_hamburg.sketchtomapgen.util.LSystemGenerator;
import de.haw_hamburg.sketchtomapgen.util.TurtleRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.net.URL;
import java.util.*;

public class TreeMapCellGenerator implements MapCellGenerationStrategy {
  @Override
  public void generateMap(VoronoiCellModel voronoiCellModel, GeneratedMapModel generatedMapModel) {

  }
}

