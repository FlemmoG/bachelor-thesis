package de.haw_hamburg.sketchtomapgen.controller;

public abstract class AbstractController {

  // Übergeordneter Controller für das Routing der Applikation
  protected NavigationController navigationController;

  public void setNavigationController(NavigationController navigationController) {
    this.navigationController = navigationController;
  }
}
