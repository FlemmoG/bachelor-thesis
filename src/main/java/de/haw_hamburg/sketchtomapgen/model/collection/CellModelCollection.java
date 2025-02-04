package de.haw_hamburg.sketchtomapgen.model.collection;

import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CellModelCollection implements Iterable<CellModel> {
  private List<CellModel> cellModels;

  public CellModelCollection() {
    this.cellModels = new ArrayList<>();
  }

  public void add(CellModel newCell) {
    List<CellModel> adjacentCells = cellModels.stream()
            .filter(existingCell -> existingCell.getIcon().equals(newCell.getIcon()))
            .filter(existingCell -> existingCell.getPolygon().intersects(newCell.getPolygon()))
            .toList();

    if (!adjacentCells.isEmpty()) {
      // Alle angrenzenden Polygone in eine neue Geometrie zusammenführen
      List<Geometry> geometries = new ArrayList<>();
      for (CellModel cell : adjacentCells) {
        geometries.add(cell.getPolygon());
      }
      geometries.add(newCell.getPolygon());

      Geometry mergedPolygon = CascadedPolygonUnion.union(geometries);

      // Alte CellModels entfernen
      cellModels.removeAll(adjacentCells);

      cellModels.add(new CellModel((Polygon) mergedPolygon, newCell.getColor(), newCell.getIcon(), mergedPolygon.getCentroid().getCoordinate()));
    } else {
      cellModels.add(newCell);
    }
  }

  public void remove(CellModel cellModel) {
    cellModels.remove(cellModel);
  }


  @Override
  public Iterator<CellModel> iterator() {
    return cellModels.iterator();
  }

  public List<Polygon> getPolygons() {
    return cellModels.stream()
            .map(CellModel::getPolygon)
            .toList();
  }

  public List<Polygon> getPolygonsExcludingOcean() {
    return cellModels.stream()
            .filter(cellModel -> !cellModel.getIcon().equals(Icon.OCEAN))
            .map(CellModel::getPolygon)
            .toList();
  }


  public GeometryCollection toGeometryCollection() {
    GeometryFactory geometryFactory = new GeometryFactory();
    Polygon[] polygons = getPolygons().toArray(new Polygon[0]);
    return new GeometryCollection(polygons, geometryFactory);
  }
}

