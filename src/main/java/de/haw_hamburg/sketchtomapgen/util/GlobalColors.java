package de.haw_hamburg.sketchtomapgen.util;

import javafx.scene.paint.Color;

public class GlobalColors {

  public static final Color TOTALLY_FLAT = new Color(0.537, 0.631, 0.447, 1);
  public static final Color MAINLY_FLAT = new Color(0.592, 0.651, 0.490, 1);
  public static final Color LITTLE_HILLY = new Color(0.776, 0.788, 0.647, 1);
  public static final Color MAINLY_HILLY = new Color(0.976, 0.961, 0.871, 1);

  public static final Color SHALLOW_WATER = new Color(0.439, 0.647, 0.820, 1);
  public static final Color WATER_SURFACE = new Color(0.486, 0.741, 0.804, 1);

  public static final Color DEEP_WATER = new Color(0.057, 0.247, 0.863, 1);

  private GlobalColors() {
    throw new UnsupportedOperationException("Utility class");
  }
}

