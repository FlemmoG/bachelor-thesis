package de.haw_hamburg.sketchtomapgen.model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;

public class GeneratedMapModel {
  private final int width;
  private final int height;
  private WritableImage writableImage;

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

  public boolean isWithinBounds(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  public void addAsset(int centerX, int centerY, Image asset, int size) {
    PixelWriter pixelWriter = writableImage.getPixelWriter();

    // Skalierte Version des Assets erstellen ohne URL
    WritableImage scaledAsset = new WritableImage(size, size);

    // Scale the image manually
    double scaleX = asset.getWidth() / size;
    double scaleY = asset.getHeight() / size;

    PixelWriter scaledWriter = scaledAsset.getPixelWriter();
    PixelReader assetReader = asset.getPixelReader();

    // Manually scale the image
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        int sourceX = (int) (x * scaleX);
        int sourceY = (int) (y * scaleY);

        // Ensure we don't exceed the source image boundaries
        sourceX = Math.min(sourceX, (int)asset.getWidth() - 1);
        sourceY = Math.min(sourceY, (int)asset.getHeight() - 1);

        Color color = assetReader.getColor(sourceX, sourceY);
        scaledWriter.setColor(x, y, color);
      }
    }

    int startX = centerX - size / 2;
    int startY = centerY - size / 2;

    // Copy the scaled image to the final position
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        Color assetColor = scaledAsset.getPixelReader().getColor(x, y);

        int worldX = startX + x;
        int worldY = startY + y;

        if (worldX >= 0 && worldX < width && worldY >= 0 && worldY < height && assetColor.getOpacity() > 0) {
          pixelWriter.setColor(worldX, worldY, assetColor);
        }
      }
    }
  }

  public void drawLine(Coordinate start, Coordinate end, Color color) {
    PixelWriter pixelWriter = writableImage.getPixelWriter();

    // Bresenham-Algorithmus oder einfache Line-Interpolation
    int x0 = (int) start.x;
    int y0 = (int) start.y;
    int x1 = (int) end.x;
    int y1 = (int) end.y;

    int dx = Math.abs(x1 - x0);
    int dy = Math.abs(y1 - y0);

    int sx = x0 < x1 ? 1 : -1;
    int sy = y0 < y1 ? 1 : -1;

    int err = dx - dy;

    while (true) {
      pixelWriter.setColor(x0, y0, color); // Zeichne Pixel

      if (x0 == x1 && y0 == y1) break;

      int e2 = 2 * err;

      if (e2 > -dy) {
        err -= dy;
        x0 += sx;
      }
      if (e2 < dx) {
        err += dx;
        y0 += sy;
      }
    }
  }


  public void setWritableImage(WritableImage finalImage) {
    this.writableImage = finalImage;
  }
}
