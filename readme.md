## Quadtree Grid Service

This service takes an input GeoJSON geometry, and outputs a variably sized grid, with smaller cells towards the edges 
of the original geometry.

This is done by creating a starting grid that covers the bounding box of the input, then repeatedly subdividing any
grids that intersect the boundary of the input geometry. Cells get divided until dividing them would create cells
smaller than the specified minimum size.

### Parameters:
<ul>
<li>clip: A boolean value that determines whether output cells will be clipped to the extent of the input geometry. Defaults to true
<li>maxGridSize: A double that controls the size (in degrees) of the cells in the starting grid. Defaults to 5
<li>minGridSize: A double that controls the minimum size cells can be subdivided to. Defaults to 0.25
</ul>

#### Example
Input
![Input](/images/input.png)
Output
![Output](/images/output.png)


### Notes/ToDo
<ul>
<li>In hindsight, I should have use Java Topology Suite instead of the GDAL bindings, since doing it this way is really 
difficult to set up, and GDAL is a port of JTS anyway.
<li>The exception handling for the REST controller could probably be moved to another class
<li>An easy next step would be to allow this to receive and respond with KML geometry as well
<li>This can take a while for more complex inputs, so some way to deal with that would be needed in a production environment
<li>Functional and unit tests, especially around error handling
</ul>