package de.haw_hamburg.sketchtomapgen.util;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class StochasticLSystemGenerator {
  private String axiom;
  private Map<Character, List<WeightedRule>> rules;
  private int iterations;
  private Random random;
  private static final int MAX_LENGTH = 10000; // Maximum length of the generated string

  public StochasticLSystemGenerator(String axiom, Map<Character, List<WeightedRule>> rules, int iterations) {
    this.axiom = axiom;
    this.rules = rules;
    this.iterations = iterations;
    this.random = new Random();
  }

  public String generate() {
    String current = axiom;

    for (int i = 0; i < iterations && current.length() < MAX_LENGTH; i++) {
      StringBuilder next = new StringBuilder();

      for (char c : current.toCharArray()) {
        // Check if adding more characters would exceed the maximum length
        if (next.length() >= MAX_LENGTH) {
          break;
        }

        if (rules.containsKey(c)) {
          List<WeightedRule> possibleRules = rules.get(c);
          double totalWeight = possibleRules.stream()
                  .mapToDouble(r -> r.weight)
                  .sum();

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

          // Check if adding the selected production would exceed the maximum length
          if (next.length() + selectedProduction.length() <= MAX_LENGTH) {
            next.append(selectedProduction);
          } else {
            break;
          }
        } else {
          next.append(c);
        }
      }

      current = next.toString();
    }

    return current;
  }
}
