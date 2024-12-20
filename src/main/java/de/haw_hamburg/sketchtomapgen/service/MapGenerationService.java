package de.haw_hamburg.sketchtomapgen.service;

import de.haw_hamburg.sketchtomapgen.model.SketchModel;
import de.haw_hamburg.sketchtomapgen.model.VoronoiCellModelCollection;

public class MapGenerationService {

  private VoronoiCellModelCollection voronoiCellModels;
  public MapGenerationService(){

  }

  public void initializeService(VoronoiCellModelCollection voronoiCellModels){
    this.voronoiCellModels = voronoiCellModels;
  }
}
