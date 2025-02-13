package de.haw_hamburg.sketchtomapgen.util.l_system;

// Verlinkt Produktionen mit Gewichten
public class WeightedRule {
  String production;
  double weight;

  public WeightedRule(String production, double weight) {
    this.production = production;
    this.weight = weight;
  }
}
