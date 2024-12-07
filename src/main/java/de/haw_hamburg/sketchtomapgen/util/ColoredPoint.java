package de.haw_hamburg.sketchtomapgen.util;

import java.awt.*;

public class ColoredPoint extends Point {
  private String hexCode;
  public  ColoredPoint(int x, int y, String hexCode){
    super(x, y);
    this.hexCode = hexCode;
  }

  public String getHexCode() {
    return hexCode;
  }
}
