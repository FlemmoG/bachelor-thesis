# Sketch-basierte Generierung von fiktionalen Landkarten

Dieser praktische Teil meiner Bachelorarbeit beschäftigt sich mit der **sketch-basierten Generierung von fiktionalen Landkarten**.


Das folgende Beispiel zeigt einen WIP-Umriss von **Hamburg und seinen Bezirken**, generiert durch Voronoi-Diagramme:

![Bezirke Hamburg](src/main/resources/de/haw_hamburg/sketchtomapgen/rendered/HAM_MAP_1.png)

Das nächste Beispiel zeigt einen WIP-Umriss von **Hamburg und seinen Stadtteilen**, ebenfalls generiert durch Voronoi-Diagramme:

![Stadtteile Hamburg](src/main/resources/de/haw_hamburg/sketchtomapgen/rendered/HAM_MAP_2.png)

Sketch -> Icon Placement -> 
Voronoi-Diagramm -> Prozedural generierte Karte: 

![Prozess](src/main/resources/de/haw_hamburg/sketchtomapgen/rendered/PROCESS.jpg)

Die Gebirgszellen wurden mit Perlin-Noise generiert, der Ozean mit Simplex-Noise, die Seeregion durch zufällige Variationen, das Waldgebiet mithilfe des WFC-Algorithmus und die Flussstrukturen durch L-Systeme.

![HQFinal](src/main/resources/de/haw_hamburg/sketchtomapgen/rendered/HQGEN.png)

Kombination aus allen Landschaftstypen in höherer Qualität.