package de.haw_hamburg.sketchtomapgen.service.strategy.background;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.collection.CellModelCollection;

public interface MapBackgroundGenerationStrategy {
  void generateBackground(CellModelCollection voronoiCellModels, GeneratedMapModel generatedMapModel);
}
