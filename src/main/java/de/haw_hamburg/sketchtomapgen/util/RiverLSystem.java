package de.haw_hamburg.sketchtomapgen.util;

public class RiverLSystem {
  private String axiom = "F"; // Startregel: ein Fluss beginnt mit "F"
  private int iterations = 3; // Anzahl der Iterationen (kann variiert werden)
  private double angle = 25.7; // Winkel für Abzweigungen

  public String generateSystem() {
    String system = axiom;
    for (int i = 0; i < iterations; i++) {
      system = applyRules(system);
    }
    return system;
  }

  private String applyRules(String input) {
    StringBuilder result = new StringBuilder();
    for (char c : input.toCharArray()) {
      switch (c) {
        case 'F': // Flussabschnitt
          result.append("F[+F][-F]F"); // Hauptfluss mit Abzweigungen
          break;
        default:
          result.append(c); // Unveränderte Zeichen
          break;
      }
    }
    return result.toString();
  }

  public double getAngle() {
    return angle;
  }
}

