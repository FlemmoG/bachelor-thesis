package de.haw_hamburg.sketchtomapgen.util.l_system;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class StochasticLSystemGenerator {
  private String axiom;
  private Map<Character, List<WeightedRule>> rules;
  private int iterations;
  private Random random;

  public StochasticLSystemGenerator(String axiom, Map<Character, List<WeightedRule>> rules, int iterations) {
    this.axiom = axiom;
    this.rules = rules;
    this.iterations = iterations;
    this.random = new Random();
  }

  // Generiert für ein Axiom, einen Regelsatz (gewichtet) und Anzahl von Iterationen ein L-System String
  public String generate() {
    String current = axiom;

    for (int i = 0; i < iterations; i++) {
      StringBuilder next = new StringBuilder();

      for (char c : current.toCharArray()) {

        if (rules.containsKey(c)) {
          List<WeightedRule> possibleRules = rules.get(c);
          double totalWeight = possibleRules.stream().mapToDouble(r -> r.weight).sum();

          double choice = random.nextDouble() * totalWeight;
          double currentWeight = 0;
          String selectedProduction = String.valueOf(c);

          for (WeightedRule rule : possibleRules) {
            currentWeight += rule.weight;
            if (choice <= currentWeight) {
              selectedProduction = rule.production;
              break;
            }
          }
          next.append(selectedProduction);
        } else {
          next.append(c);
        }
      }

      current = next.toString();
    }

    return current;
  }
}
