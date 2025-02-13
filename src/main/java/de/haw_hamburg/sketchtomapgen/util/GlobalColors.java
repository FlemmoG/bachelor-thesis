package de.haw_hamburg.sketchtomapgen.util;

import javafx.scene.paint.Color;

public class GlobalColors {

  public static final Color GROUND_COLOR = new Color(0.537, 0.631, 0.447, 1);

  public static final Color SHALLOW_WATER_COLOR = new Color(0.4, 0.8, 0.9, 1);
  public static final Color OCEAN_SURFACE_COLOR = new Color(0.2, 0.5, 0.8, 1);
  public static final Color DEEP_OCEAN_COLOR = new Color(0.05, 0.1, 0.4, 1);
  public static final Color ROAD_COLOR = new Color(0.39, 0.22, 0.07, 1);
  public static final Color MARKER_COLOR = new Color(0.0, 0.0, 0.00, 1);


  private GlobalColors() {
    throw new UnsupportedOperationException("Utility class");
  }
}

