package de.haw_hamburg.sketchtomapgen.model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class GeneratedMapModel {
  private final int width;
  private final int height;
  private final WritableImage writableImage;

  public GeneratedMapModel(int width, int height) {
    this.width = width;
    this.height = height;
    this.writableImage = new WritableImage(width, height);
  }

  public void addPixel(int x, int y, Color color) {
    if (x >= 0 && x < width && y >= 0 && y < height) {
      PixelWriter pixelWriter = writableImage.getPixelWriter();
      pixelWriter.setColor(x, y, color);
    } else {
      System.err.println("Tried to draw pixel out of bounds: (" + x + ", " + y + ")");
    }
  }

  public Color getColorAt(int x, int y) {
    if (x >= 0 && x < width && y >= 0 && y < height) {
      return writableImage.getPixelReader().getColor(x, y);
    } else {
      return Color.BLACK; // Return a default color if out of bounds
    }
  }

  public WritableImage getWritableImage() {
    return writableImage;
  }

  public void addAsset(int centerX, int centerY, Image asset, int size) {
    PixelWriter pixelWriter = writableImage.getPixelWriter();

    int assetWidth = (int) asset.getWidth();
    int assetHeight = (int) asset.getHeight();
    int startX = centerX - size / 2;
    int startY = centerY - size / 2;

    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        int assetX = (int) ((x / (double) size) * assetWidth);
        int assetY = (int) ((y / (double) size) * assetHeight);

        Color assetColor = asset.getPixelReader().getColor(assetX, assetY);

        int worldX = startX + x;
        int worldY = startY + y;

        if (worldX >= 0 && worldX < width && worldY >= 0 && worldY < height && assetColor.getOpacity() > 0) {
          pixelWriter.setColor(worldX, worldY, assetColor);
        }
      }
    }
  }
}
