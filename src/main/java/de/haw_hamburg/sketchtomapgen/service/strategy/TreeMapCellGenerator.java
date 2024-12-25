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
    Polygon polygon = voronoiCellModel.getPolygon();
    Envelope envelope = polygon.getEnvelopeInternal();

    int treeCount = (int) (Math.random() * 5) + 3; // 3 bis 7 Bäume

    for (int i = 0; i < treeCount; i++) {
      double startX = envelope.getMinX() + Math.random() * envelope.getWidth();
      double startY = envelope.getMinY() + Math.random() * envelope.getHeight();

      // Zufällige L-System-Regeln
      String axiom = "F";
      Map<Character, String> rules = new HashMap<>();
      rules.put('F', Math.random() < 0.5 ? "F[+F]F[-F]F" : "F[-F][+F]F");

      int iterations = (int) (Math.random() * 2) + 2; // 2 bis 3 Iterationen
      LSystemGenerator lSystemGenerator = new LSystemGenerator(axiom, rules, iterations);
      String lSystem = lSystemGenerator.generate();

      // Zufällige Parameter für die Turtle
      double stepSize = 3 + Math.random() * 4; // Schrittgröße
      double angleIncrement = 20 + Math.random() * 20; // Winkeländerung

      TurtleRenderer renderer = new TurtleRenderer(
              generatedMapModel,
              startX,
              startY,
              90,
              stepSize,
              angleIncrement
      );

      renderer.render(lSystem);
    }
  }

}

