package de.haw_hamburg.sketchtomapgen.model.collection;

import de.haw_hamburg.sketchtomapgen.model.CellModel;
import de.haw_hamburg.sketchtomapgen.util.Icon;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CellModelCollection implements Iterable<CellModel> {
  private List<CellModel> cellModels;

  public CellModelCollection() {
    this.cellModels = new ArrayList<>();
  }

  public void add(CellModel cellModel) {
    cellModels.add(cellModel);
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

