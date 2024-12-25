package de.haw_hamburg.sketchtomapgen.service;

import com.auburn.fastnoiselite.FastNoiseLite;
import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.VoronoiCellModel;
import de.haw_hamburg.sketchtomapgen.model.collection.VoronoiCellModelCollection;
import de.haw_hamburg.sketchtomapgen.service.strategy.*;
import de.haw_hamburg.sketchtomapgen.util.*;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.apfloat.internal.ImplementationMismatchException;
import org.locationtech.jts.geom.*;

import java.util.*;

public class MapGenerationService {
  private final Map<Icon, MapCellGenerationStrategy> strategyMap;
  private VoronoiCellModelCollection voronoiCellModels;
  private GeneratedMapModel generatedMapModel;
  private final int width;
  private final int height;

  public MapGenerationService(int width, int height){
    this.width = width;
    this.height = height;

    strategyMap = new HashMap<>();
    strategyMap.put(Icon.MOUNTAIN, new MountainMapCellGenerator());
    strategyMap.put(Icon.TREE, new TreeMapCellGenerator());
    strategyMap.put(Icon.WATER, new WaterMapCellGenerator());
    strategyMap.put(Icon.VILLAGE, new VillageMapCellGenerator());
  }

  public void initializeService(VoronoiCellModelCollection voronoiCellModels){
    this.voronoiCellModels = voronoiCellModels;
  }

  public void generateMap(){
    if (voronoiCellModels == null) {
      throw new IllegalStateException("Service not initialized");
    }

    GeneratedMapModel generatedMapModel = new GeneratedMapModel(width, height);
    for (VoronoiCellModel cell : voronoiCellModels) {
      MapCellGenerationStrategy strategy = strategyMap.get(cell.getIcon());
      if (strategy != null) {
        strategy.generateMap(cell, generatedMapModel);
      } else {
        throw new ImplementationMismatchException("No strategy implementation found for icon type: " + cell.getIcon());
      }
    }
    this.generatedMapModel = generatedMapModel;
    addRiversToMap();
  }

  private void addRiversToMap() {
    List<Polygon> polygons = voronoiCellModels.getPolygons();
    if (polygons.isEmpty()) return;

    Random random = new Random();

    for (Polygon polygon : polygons) {
      Point center = polygon.getCentroid();

      Map<Character, List<WeightedRule>> stochasticRules = new HashMap<>();
      stochasticRules.put('I', Arrays.asList(
              new WeightedRule("+F-F-RX", 1),
              new WeightedRule("-F+F-RX", 1),
              new WeightedRule("-F-F+RX", 1)
      ));

      // X rules (equal probability)
      stochasticRules.put('X', Arrays.asList(
              new WeightedRule("+F-F+RI", 1),
              new WeightedRule("-F+F+RI", 1)
      ));

      // R rules (equal probability)
      stochasticRules.put('R', Arrays.asList(
              new WeightedRule("F[SL]F", 1),
              new WeightedRule("F[LS]F", 1)
      ));

      // S rules (equal probability)
      stochasticRules.put('S', Arrays.asList(
              new WeightedRule("F-I+", 1),
              new WeightedRule("F+I-", 1)
      ));

      // L rules (equal probability)
      stochasticRules.put('L', Arrays.asList(
              new WeightedRule("F+I-", 1),
              new WeightedRule("F-I+", 1)
      ));

      int iterations = 8 + random.nextInt(4);
      double stepSize = polygon.getEnvelopeInternal().getWidth() * 0.02;
      double beta = 45 + random.nextDouble() * 45; // Increased angle range [45,90]

      String riverPattern = new StochasticLSystemGenerator("I", stochasticRules, iterations).generate();

      for (int t = 0; t < 3; t++) {
        TurtleRenderer renderer = new TurtleRenderer(
                generatedMapModel,
                center.getX(),
                center.getY(),
                random.nextDouble() * 360, // Random initial direction
                stepSize * (1 - t * 0.15),
                beta,
                polygon
        );

        renderer.render(riverPattern);
      }
    }
  }

  public WritableImage getImage(){
    return generatedMapModel.getWritableImage();
  }

}
