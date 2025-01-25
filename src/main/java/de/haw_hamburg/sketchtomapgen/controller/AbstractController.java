package de.haw_hamburg.sketchtomapgen.controller;

public abstract class AbstractController {
  protected NavigationController navigationController;

  public void setNavigationController(NavigationController navigationController) {
    this.navigationController = navigationController;
  }
}
