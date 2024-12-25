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

    // Get the envelope (bounding box) of all polygons
    Envelope envelope = polygons.get(0).getEnvelopeInternal();
    for (Polygon polygon : polygons) {
      envelope.expandToInclude(polygon.getEnvelopeInternal());
    }

    // Create stochastic L-system rules
    Map<Character, List<WeightedRule>> stochasticRules = new HashMap<>();

    // I rules (each with equal probability 1/3)
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

    Random random = new Random();

    // Parameters from the stochastic L-system
    int iterations = 8; //+ random.nextInt(4); // [8,11]
    double beta = 15 + random.nextDouble() * 15; // [15,35]
    double r = 11 + random.nextDouble() * 9; // [11,20]
    double startX = 0; // x₀ = 0
    double startY = 300 + random.nextDouble() * 400; // y₀ = [300,700]
    double theta = 20 + random.nextDouble() * 40; // θ ∈ [20,60]

    // Create and render the river
    StochasticLSystemGenerator lSystem = new StochasticLSystemGenerator(
            "I", // Axiom
            stochasticRules,
            iterations
    );
    String riverPattern = lSystem.generate();

    // Scale step size based on envelope size
    double stepSize = Math.min(envelope.getWidth(), envelope.getHeight()) * 0.02;

    // Render with different thicknesses for more natural look
    for (int t = 0; t < 3; t++) {
      TurtleRenderer renderer = new TurtleRenderer(
              generatedMapModel,
              startX + envelope.getMinX(),
              startY + envelope.getMinY(),
              theta,
              stepSize * (1 - t * 0.15),
              beta
      );

      renderer.setMapBounds(
              (int) envelope.getMinX(),
              (int) envelope.getMinY(),
              (int) envelope.getMaxX(),
              (int) envelope.getMaxY()
      );

      renderer.render(riverPattern);
    }
  }

  public WritableImage getImage() {
    if (generatedMapModel == null) {
      throw new IllegalStateException("Map not generated");
    }
    return generatedMapModel.getWritableImage();
  }

}
