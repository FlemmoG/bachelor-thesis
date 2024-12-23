package de.haw_hamburg.sketchtomapgen.service.strategy;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.VoronoiCellModel;

public interface MapCellGenerationStrategy {
  void generateMap(VoronoiCellModel voronoiCellModel, GeneratedMapModel generatedMapModel);
}
