package de.haw_hamburg.sketchtomapgen.util;

import javafx.scene.paint.Color;

public class GlobalColors {

  public static final Color TOTALLY_FLAT = new Color(0.537, 0.631, 0.447, 1);
  public static final Color MAINLY_FLAT = new Color(0.592, 0.651, 0.490, 1);
  public static final Color LITTLE_HILLY = new Color(0.776, 0.788, 0.647, 1);
  public static final Color MAINLY_HILLY = new Color(0.976, 0.961, 0.871, 1);

  public static final Color SHALLOW_WATER = new Color(0.4, 0.8, 0.9, 1);    // Helltürkis (Flachwasser)
  public static final Color WATER_SURFACE = new Color(0.2, 0.5, 0.8, 1);    // Klassisches Meerblau
  public static final Color DEEP_WATER = new Color(0.05, 0.1, 0.4, 1);      // Dunkles Nachtblau
  public static final Color ROAD = new Color(0.39,0.22,0.07, 1);
  public static final Color MARKER = new Color(0.0,0.0,0.00, 1);


  private GlobalColors() {
    throw new UnsupportedOperationException("Utility class");
  }
}

