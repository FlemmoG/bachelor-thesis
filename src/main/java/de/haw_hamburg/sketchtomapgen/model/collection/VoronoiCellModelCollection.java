package de.haw_hamburg.sketchtomapgen.model.collection;

import de.haw_hamburg.sketchtomapgen.model.VoronoiCellModel;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class VoronoiCellModelCollection implements Iterable<VoronoiCellModel> {
  private List<VoronoiCellModel> voronoiCellModels;

  public VoronoiCellModelCollection() {
    this.voronoiCellModels = new ArrayList<>();
  }

  public void add(VoronoiCellModel voronoiCellModel) {
    voronoiCellModels.add(voronoiCellModel);
  }

  public void remove(VoronoiCellModel voronoiCellModel) {
    voronoiCellModels.remove(voronoiCellModel);
  }


  @Override
  public Iterator<VoronoiCellModel> iterator() {
    return voronoiCellModels.iterator();
  }

  public List<Polygon> getPolygons() {
    return voronoiCellModels.stream()
            .map(VoronoiCellModel::getPolygon) // Annahme: VoronoiCellModel hat eine getPolygon()-Methode
            .toList();
  }

  public GeometryCollection toGeometryCollection() {
    GeometryFactory geometryFactory = new GeometryFactory();
    Polygon[] polygons = getPolygons().toArray(new Polygon[0]);
    return new GeometryCollection(polygons, geometryFactory);
  }
}

