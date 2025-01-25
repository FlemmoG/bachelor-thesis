package de.haw_hamburg.sketchtomapgen.service.strategy.cell;

import de.haw_hamburg.sketchtomapgen.model.GeneratedMapModel;
import de.haw_hamburg.sketchtomapgen.model.CellModel;

public interface MapCellGenerationStrategy {
  void generateMap(CellModel cellModel, GeneratedMapModel generatedMapModel);
}
