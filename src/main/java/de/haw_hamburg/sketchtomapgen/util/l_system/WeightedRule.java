package de.haw_hamburg.sketchtomapgen.util.l_system;

public class WeightedRule {
  String production;
  double weight;

  public WeightedRule(String production, double weight) {
    this.production = production;
    this.weight = weight;
  }
}
