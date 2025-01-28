package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.model.collection.CellModelCollection;
import de.haw_hamburg.sketchtomapgen.service.strategy.cell.*;
import de.haw_hamburg.sketchtomapgen.util.*;
import de.haw_hamburg.sketchtomapgen.util.l_system.StochasticLSystemGenerator;
import de.haw_hamburg.sketchtomapgen.util.l_system.TurtleRenderer;
import de.haw_hamburg.sketchtomapgen.util.l_system.WeightedRule;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import org.apfloat.internal.ImplementationMismatchException;
import org.locationtech.jts.geom.*;

import java.net.URL;
import java.util.*;

public class MapGenerationService {
  private final Map<Icon, MapCellGenerationStrategy> strategyMap;
  private CellModelCollection voronoiCellModels;
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
    strategyMap.put(Icon.OCEAN, new OceanMapCellGenerator());
  }

  public void initializeService(CellModelCollection voronoiCellModels){
    this.voronoiCellModels = voronoiCellModels;
  }

  public void generateMap(){
    if (voronoiCellModels == null) {
      throw new IllegalStateException("Service not initialized");
    }
    generatedMapModel = new GeneratedMapModel(width, height);
    //addRiversToMap();
    for (CellModel cell : voronoiCellModels) {
      MapCellGenerationStrategy strategy = strategyMap.get(cell.getIcon());
      if (strategy != null) {
        strategy.generateMap(cell, generatedMapModel);
      } else {
        throw new ImplementationMismatchException("No strategy implementation found for icon type: " + cell.getIcon());
      }
    }

    addFiltersToImage(generatedMapModel);
  }

  private void addFiltersToImage(GeneratedMapModel generatedMapModel) {
    WritableImage noisyImage = addNoise(generatedMapModel.getWritableImage(), 0.1);

    ColorAdjust colorAdjust = new ColorAdjust();
    colorAdjust.setHue(-0.05);  // Leichter Gelbstich
    colorAdjust.setSaturation(-0.7); // Entsättigung
    colorAdjust.setBrightness(0.15); // Aufhellung
    ImageView imageView = new ImageView(noisyImage);
    imageView.setEffect(colorAdjust);

    URL textureUrl = getClass().getResource(AssetRoutes.PARCHMENT_TEXTURE);
    Image parchmentTexture = new Image(String.valueOf(textureUrl));
    ImageView textureView = new ImageView(parchmentTexture);
    textureView.setBlendMode(BlendMode.MULTIPLY); // Farben interagieren
    textureView.setOpacity(0.4); // Transparenz anpassen

    // Vignette-Effekt
    Rectangle vignette = new Rectangle(generatedMapModel.getWidth(), generatedMapModel.getHeight());
    RadialGradient gradient = new RadialGradient(
            0, 0,
            0.5, 0.5,
            0.8,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.TRANSPARENT),
            new Stop(1, Color.rgb(0, 0, 0, 0.5)) // Rand dunkel
    );
    vignette.setFill(gradient);
    vignette.setBlendMode(BlendMode.MULTIPLY);

    Group processingGroup = new Group();
    processingGroup.getChildren().addAll(
            imageView,
            textureView,
            vignette
    );


    WritableImage finalImage = processingGroup.snapshot(null, null);
    generatedMapModel.setWritableImage(finalImage);
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
            new WeightedRule("+FF-FF-RX", 1),
            new WeightedRule("-FF+FF-RX", 1),
            new WeightedRule("-FF-FF+RX", 1)
    ));
    stochasticRules.put('X', Arrays.asList(
            new WeightedRule("+FF-FF+RI", 1),
            new WeightedRule("-FF+FF+RI", 1)
    ));
    stochasticRules.put('R', Arrays.asList(
            new WeightedRule("F", 3),        // Favor straight growth
            new WeightedRule("FF[SL]FF", 1),  // Reduce branching
            new WeightedRule("FF[LS]FF", 1)
    ));
    stochasticRules.put('S', Arrays.asList(
            new WeightedRule("FF-I+", 2),
            new WeightedRule("F", 1)        // Add direct, non-branching rules
    ));
    stochasticRules.put('L', Arrays.asList(
            new WeightedRule("FF+I-", 1),
            new WeightedRule("FF-I+", 1)
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

  private WritableImage addNoise(WritableImage image, double intensity) {
    PixelReader reader = image.getPixelReader();
    WritableImage noisyImage = new WritableImage(reader, (int)image.getWidth(), (int)image.getHeight());
    PixelWriter writer = noisyImage.getPixelWriter();

    Random rand = new Random();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        Color color = reader.getColor(x, y);
        // Füge zufälliges Rauschen hinzu
        double noise = (rand.nextDouble() - 0.5) * intensity;
        writer.setColor(x, y, Color.color(
                clamp(color.getRed() + noise),
                clamp(color.getGreen() + noise),
                clamp(color.getBlue() + noise)
        ));
      }
    }
    return noisyImage;
  }

  private double clamp(double value) {
    return Math.max(0, Math.min(1, value));
  }

  public WritableImage getImage(){
    return generatedMapModel.getWritableImage();
  }

}
