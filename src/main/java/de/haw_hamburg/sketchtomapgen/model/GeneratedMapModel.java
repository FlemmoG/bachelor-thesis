package de.haw_hamburg.sketchtomapgen.model;

import de.haw_hamburg.sketchtomapgen.util.AssetRoutes;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class GeneratedMapModel {
  private final int width;
  private final int height;

  // Kerndatenstruktur die Pixel direkt speichert
  private WritableImage writableImage;
  private Font font;

  public GeneratedMapModel(int width, int height) {
    this.width = width;
    this.height = height;
    this.writableImage = new WritableImage(width, height);
    this.font = loadFantasyFont(15);
  }

  // Fügt Pixel zum finalen Bild hinzu, wobei out of bounds pixel ignoriert werden
  public void addPixel(int x, int y, Color color) {
    if (x >= 0 && x < width && y >= 0 && y < height) {
      PixelWriter pixelWriter = writableImage.getPixelWriter();
      pixelWriter.setColor(x, y, color);
    } //else -> ignore
  }

  // Gibt die Farbe für eine Position zurück
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

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }

  // Fügt ein beliebiges Asset an einer übergebenen Position hinzu
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
        sourceX = Math.min(sourceX, (int) asset.getWidth() - 1);
        sourceY = Math.min(sourceY, (int) asset.getHeight() - 1);

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

  // Fügt Text an übergebener Position hinzu
  public void addLabel(int x, int y, String text, Color fxColor) {
    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(this.writableImage, null);

    Graphics2D g2d = bufferedImage.createGraphics();

    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2d.setFont(font);

    java.awt.Color awtColor = new java.awt.Color((float) fxColor.getRed(), (float) fxColor.getGreen(), (float) fxColor.getBlue(), (float) fxColor.getOpacity());
    g2d.setColor(awtColor);
    g2d.drawString(text, x, y);

    g2d.dispose();

    this.writableImage = SwingFXUtils.toFXImage(bufferedImage, this.writableImage);
  }

  // Überschreibt die aktuellen Pixel mit den Pixeln des übergebenen Bildes
  public void setWritableImage(WritableImage finalImage) {
    this.writableImage = finalImage;
  }

  // Lädt Font
  private Font loadFantasyFont(int fontSize) {
    try {
      URL fontUrl = getClass().getResource(AssetRoutes.FONT_ASSET);
      File file = new File(fontUrl.toURI());
      return Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(Font.PLAIN, fontSize);
    } catch (FontFormatException | IOException | URISyntaxException e) {
      e.printStackTrace();
      return new Font("Serif", Font.PLAIN, fontSize);
    }
  }
}