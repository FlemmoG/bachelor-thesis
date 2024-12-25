package de.haw_hamburg.sketchtomapgen.util;

import java.util.HashMap;
import java.util.Map;

public class LSystemGenerator {
  private String axiom;
  private Map<Character, String> rules;
  private int iterations;

  public LSystemGenerator(String axiom, Map<Character, String> rules, int iterations) {
    this.axiom = axiom;
    this.rules = rules;
    this.iterations = iterations;
  }

  public String generate() {
    String current = axiom;

    for (int i = 0; i < iterations; i++) {
      StringBuilder next = new StringBuilder();

      for (char c : current.toCharArray()) {
        if (rules.containsKey(c)) {
          next.append(rules.get(c));
        } else {
          next.append(c);
        }
      }

      current = next.toString();
    }

    return current;
  }
}

