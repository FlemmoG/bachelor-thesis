# Sketch-based Generation of Fictional Maps

This practical part of my bachelor's thesis deals with the **sketch-based generation of fictional maps**.

*(All examples are WIP)*

The following example shows a WIP outline of **Hamburg and its districts**, generated using Voronoi diagrams:

![Districts of Hamburg](src/main/resources/de/haw_hamburg/sketchtomapgen/rendered/HAM_MAP_1.png)

The next example shows a WIP outline of **Hamburg and its neighborhoods**, also generated using Voronoi diagrams:

![Neighborhoods of Hamburg](src/main/resources/de/haw_hamburg/sketchtomapgen/rendered/HAM_MAP_2.png)

Sketch -> Icon Placement ->
Voronoi Diagram -> Procedurally generated map:

![Process](src/main/resources/de/haw_hamburg/sketchtomapgen/rendered/PROCESS.jpg)

The mountain cells were generated using Perlin noise, the ocean using Simplex noise, the lake region through random variations, the forest area using the WFC algorithm, and the river structures using L-systems.

![HQFinal](src/main/resources/de/haw_hamburg/sketchtomapgen/rendered/HQGEN.png)

Combination of all landscape types in higher quality.