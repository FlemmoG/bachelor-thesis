module de.haw_hamburg.sketchtomapgen {
  requires javafx.controls;
  requires javafx.fxml;
  requires org.jgrapht.core;
  requires java.desktop;
  requires org.locationtech.jts;
  requires commons.math3;
  requires javafx.swing;

  opens de.haw_hamburg.sketchtomapgen.app to javafx.fxml;
  opens de.haw_hamburg.sketchtomapgen.controller to javafx.fxml;
  exports de.haw_hamburg.sketchtomapgen.app;
  exports de.haw_hamburg.sketchtomapgen.controller;
  exports de.haw_hamburg.sketchtomapgen.model;
  exports de.haw_hamburg.sketchtomapgen.util;
  opens de.haw_hamburg.sketchtomapgen.util to javafx.fxml;
  exports com.auburn.fastnoiselite;
  opens com.auburn.fastnoiselite to javafx.fxml;
  exports de.haw_hamburg.sketchtomapgen.model.collection;
  exports de.haw_hamburg.sketchtomapgen.util.l_system;
  opens de.haw_hamburg.sketchtomapgen.util.l_system to javafx.fxml;
}
