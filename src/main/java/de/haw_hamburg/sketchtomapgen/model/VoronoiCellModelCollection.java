package de.haw_hamburg.sketchtomapgen.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
}

