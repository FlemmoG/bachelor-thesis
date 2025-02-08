package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.model.collection.CellModelCollection;
import de.haw_hamburg.sketchtomapgen.service.strategy.cell.*;
import de.haw_hamburg.sketchtomapgen.util.*;
import de.haw_hamburg.sketchtomapgen.util.l_system.StochasticLSystemGenerator;
import de.haw_hamburg.sketchtomapgen.util.l_system.RiverTurtleRenderer;
import de.haw_hamburg.sketchtomapgen.util.l_system.WeightedRule;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import org.apfloat.internal.ImplementationMismatchException;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.linearref.LengthIndexedLine;

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
    strategyMap.put(Icon.TREE, new ForestMapCellGenerator());
    strategyMap.put(Icon.WATER, new LakeMapCellGenerator());
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
    for (CellModel cell : voronoiCellModels) {
      MapCellGenerationStrategy strategy = strategyMap.get(cell.getIcon());
      if (strategy != null) {
        strategy.generateMap(cell, generatedMapModel);
      } else {
        throw new ImplementationMismatchException("No strategy implementation found for icon type: " + cell.getIcon());
      }
    }
    addRiversToMap();
    addLabelsToMap();
  }

  public void addFiltersToImage() {
    WritableImage noisyImage = addNoise(generatedMapModel.getWritableImage(), 0.1);

    ColorAdjust colorAdjust = new ColorAdjust();
    colorAdjust.setHue(-0.025);  // Leichter Gelbstich
    colorAdjust.setSaturation(-0.65); // Entsättigung
    colorAdjust.setBrightness(0.075); // Aufhellung
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

  private void addLabelsToMap() {
    MapCellGenerationStrategy villageStrategy = strategyMap.get(Icon.VILLAGE);
    if (villageStrategy instanceof VillageMapCellGenerator villageGenerator) {
      villageGenerator.drawMarkers(generatedMapModel);
    }
  }

  private void addRiversToMap() {
    List<Polygon> polygons = voronoiCellModels.getPolygonsExcludingOcean();
    if (polygons.isEmpty()) return;

    GeometryFactory factory = new GeometryFactory();
    Geometry combinedGeometry = factory.createMultiPolygon(
                    polygons.toArray(new Polygon[0]))
            .union();

    Random random = new Random();

    Map<Character, List<WeightedRule>> stochasticRules = new HashMap<>();
    stochasticRules.put('I', Arrays.asList(
            new WeightedRule("+FF-FF-RX", 1),
            new WeightedRule("-FF+FF-RX", 1),
            new WeightedRule("+FF[-F]F-RX", 1),
            new WeightedRule("-FF[+F]F-RX", 1)
    ));
    stochasticRules.put('X', Arrays.asList(
            new WeightedRule("+FF-FF+RI", 1),
            new WeightedRule("-FF+FF+RI", 1),
            new WeightedRule("+FF[-F]F+RI", 1),
            new WeightedRule("-FF[+F]F+RI", 1)
    ));
    stochasticRules.put('R', Arrays.asList(
            new WeightedRule("F[+F]F[-F]F", 3), // Mehr Verzweigungen
            new WeightedRule("FF", 1)
    ));

    int iterations = 12;
    double baseStepSize = combinedGeometry.getEnvelopeInternal().getWidth() * 0.008;
    double beta = 35; // Winkeländerung

    // Flussparameter
    int numberOfRivers = (int) combinedGeometry.getArea() / 10000;
    System.out.println(numberOfRivers);
    for (int t = 0; t < numberOfRivers; t++) {
      // Zufälligen Startpunkt am Rand wählen
      Coordinate startCoord = getRandomEdgePoint(combinedGeometry, random);
      Point center = combinedGeometry.getCentroid();

      // Initiale Richtung zum Zentrum
      double dx = center.getX() - startCoord.x;
      double dy = center.getY() - startCoord.y;
      double initialAngle = Math.toDegrees(Math.atan2(dy, dx));

      // Schrittgröße mit zufälliger Variation
      double stepSize = baseStepSize * (0.8 + random.nextDouble() * 0.4);

      RiverTurtleRenderer renderer = new RiverTurtleRenderer(
              generatedMapModel,
              startCoord.x,
              startCoord.y,
              initialAngle,
              stepSize,
              beta,
              combinedGeometry,
              voronoiCellModels
      );

      String riverPattern = new StochasticLSystemGenerator("I", stochasticRules, iterations).generate();
      renderer.render(riverPattern);
    }
  }

  private Coordinate getRandomEdgePoint(Geometry geometry, Random random) {
    Geometry boundary = geometry.getBoundary();
    if (boundary instanceof LineString) {
      LengthIndexedLine lil = new LengthIndexedLine(boundary);
      double length = lil.getEndIndex();
      return lil.extractPoint(random.nextDouble() * length);
    }
    return geometry.getCentroid().getCoordinate();
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
