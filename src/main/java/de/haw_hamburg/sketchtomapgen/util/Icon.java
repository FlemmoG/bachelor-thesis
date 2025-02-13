package de.haw_hamburg.sketchtomapgen.util;

import java.net.URL;

public enum Icon {
  MOUNTAIN(AssetRoutes.MOUNTAINS_ASSET),
  TREE(AssetRoutes.TREES_ASSET),
  WATER(AssetRoutes.WATER_ASSET),
  VILLAGE(AssetRoutes.VILLAGE_ASSET),
  BLANK(null),
  OCEAN(null);

  private final String assetPath;

  Icon(String assetPath) {
    this.assetPath = assetPath;
  }

  public URL getUrl(Class<?> contextClass) {
    return assetPath != null ? contextClass.getResource(assetPath) : null;
  }
}
