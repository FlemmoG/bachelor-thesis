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

    GeometryFactory factory = new GeometryFactory();
    Geometry combinedGeometry = factory.createMultiPolygon(
                    polygons.toArray(new Polygon[0]))
            .union();

    Random random = new Random();
    Point center = combinedGeometry.getCentroid();

    Map<Character, List<WeightedRule>> stochasticRules = new HashMap<>();
    stochasticRules.put('I', Arrays.asList(
            new WeightedRule("+F-F-RX", 1),
            new WeightedRule("-F+F-RX", 1),
            new WeightedRule("-F-F+RX", 1)
    ));
    stochasticRules.put('X', Arrays.asList(
            new WeightedRule("+F-F+RI", 1),
            new WeightedRule("-F+F+RI", 1)
    ));
    stochasticRules.put('R', Arrays.asList(
            new WeightedRule("F", 3),        // Favor straight growth
            new WeightedRule("F[SL]F", 1),  // Reduce branching
            new WeightedRule("F[LS]F", 1)
    ));
    stochasticRules.put('S', Arrays.asList(
            new WeightedRule("F-I+", 2),    // Slightly increase simple growth
            new WeightedRule("F", 1)        // Add direct, non-branching rules
    ));
    stochasticRules.put('L', Arrays.asList(
            new WeightedRule("F+I-", 1),
            new WeightedRule("F-I+", 1)
    ));

    int iterations = 15;
    double stepSize = combinedGeometry.getEnvelopeInternal().getWidth() * 0.01;
    double beta = 15;

    String riverPattern = new StochasticLSystemGenerator("I", stochasticRules, iterations).generate();

    for (int t = 0; t < 3; t++) {
      TurtleRenderer renderer = new TurtleRenderer(
              generatedMapModel,
              center.getX(),
              center.getY(),
              random.nextDouble() * 360,
              stepSize * (1 - t * 0.15),
              beta,
              combinedGeometry
      );

      renderer.render(riverPattern);
    }
  }

  public WritableImage getImage(){
    return generatedMapModel.getWritableImage();
  }

}
