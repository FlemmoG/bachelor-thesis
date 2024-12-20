package de.haw_hamburg.sketchtomapgen.controller;

public abstract class AbstractController {
  protected MainController mainController;

  public void setMainController(MainController mainController) {
    this.mainController = mainController;
  }
}
