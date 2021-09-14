package com.cjriley9.quadtreegridservice;

import org.gdal.ogr.Geometry;

import java.util.List;
import java.util.Queue;
import java.util.ArrayList;
import java.util.ArrayDeque;

import org.apache.commons.lang3.tuple.Pair;
import org.gdal.ogr.ogrConstants;

public class GridCreator {
    private final Geometry inGeom;
    private final Rectangle bbox;

    private double minGridSize = .25;
    private double maxGridSize =   5;
    private boolean clip = true;

    GridCreator(Geometry inGeom) throws InvalidRectangleException {
        this.inGeom = inGeom;
        this.bbox = Rectangle.fromGeometry(inGeom);
    }

    GridCreator(Geometry inGeom, double minGridSize) throws InvalidRectangleException{
        this.inGeom = inGeom;
        this.bbox = Rectangle.fromGeometry(inGeom);
        this.minGridSize = minGridSize;
    }

    GridCreator(Geometry inGeom, double minGridSize, double maxGridSize) throws InvalidRectangleException{
        this.inGeom = inGeom;
        this.bbox = Rectangle.fromGeometry(inGeom);
        this.minGridSize = minGridSize;
        this.maxGridSize = maxGridSize;
    }

    void setMaxGridSize(double size) {
        this.maxGridSize = size;
    }

    void setMinGridSize(double size) {
        this.minGridSize = size;
    }

    void setClip(boolean clip) {
        this.clip = clip;
    }

    /**
     * Fills a new queue with a set of cells of maxGridSize that cover the input geometry
     * @return a queue of rectangles that covers the input geometry, to be used in further processing
     */
    Queue<Rectangle> prepQueue() throws GridCreatorException {
        Queue<Rectangle> queue = new ArrayDeque<>();

        // get the number of cells needed to cover the AOI in each direction
        int xSize = (int)((bbox.xMax - bbox.xMin)/maxGridSize) + 1;
        int ySize = (int)((bbox.yMax - bbox.yMin)/maxGridSize) + 1;

        // center the new cells over the aoi
        double xRemainder = (bbox.xMax - bbox.xMin) % maxGridSize;
        double yRemainder = (bbox.yMax - bbox.yMin) % maxGridSize;
        double xBasis = bbox.xMin - (xRemainder / 2);
        double yBasis = bbox.yMin - (yRemainder / 2);

        // loop through and create starting grid
        for (int i = 0; i <= xSize; i++) {
            for (int j = 0; j <= ySize; j++) {
                double newXMin = xBasis + (i * maxGridSize);
                double newYMin = yBasis + (j * maxGridSize);
                double newXMax = xBasis + ((i + 1) * maxGridSize);
                double newYMax = yBasis + ((j + 1) * maxGridSize);

                double[] bounds = new double[]{newXMin, newXMax, newYMin, newYMax};

                try{
                    queue.add(new Rectangle(bounds));
                } catch (InvalidRectangleException e) {
                    throw new GridCreatorException("Rectangle exception while p ");
                }
            }
        }

        return queue;
    }

    /**
     * Processes a queue of cells, subdividing if a cell intersects the input geometry and if subdividing the cell
     * wouldn't result in cells smaller than the minimum allowed size.
     * @param queue a queue that contains rectangles to be subdivided
     * @return a list of paired boolean and geometry values. The boolean is used to indicate whether the cell
     *          intersects the input geometry boundary. This allows us to more easily decide when to clip cells in the
     *          next step.
     */
    List<Pair<Boolean, Geometry>> processQueue(Queue<Rectangle> queue) throws GridCreatorException{
        // use pairs to indicate whether a cell intersects the boundary, to help us know when to clip it later
        List<Pair<Boolean, Geometry>> outList = new ArrayList<>();
        Geometry boundary = inGeom.Boundary();

        while (!queue.isEmpty()) {
            Rectangle currentRect = queue.remove();
            double rectSize = currentRect.xMax - currentRect.xMin;
            Geometry polygon = currentRect.asGeometry();
            if (polygon.Intersects(boundary)) {
                // make sure that a subdivision won't result in cells that are less than minGridsize
                if ((rectSize/2) >= minGridSize) {
                    try {
                        List<Rectangle> subdivided = currentRect.subdivide();
                        queue.addAll(subdivided);
                    }
                    catch (InvalidRectangleException e) {
                        throw new GridCreatorException("Invalid rectangle created while processing queue");
                    }
                }
                else {
                    outList.add(Pair.of(true, polygon));
                }
            }
            // check if it is within the input geometry
            else if (polygon.Intersects(inGeom)) {
                outList.add(Pair.of(false, polygon));
            }
        }
        return outList;


    }

    /**
     * Returns an output geometry created from subdividing the input according to the provided parameters
     * @return a multipolygon geometry containing the result of the subdivision process over the input geometry
     */
    Geometry Process() throws GridCreatorException{
        Queue<Rectangle> queue = prepQueue();
        Geometry outGeometry = new Geometry(ogrConstants.wkbMultiPolygon);
        List<Pair<Boolean, Geometry>> cellList = processQueue(queue);
        for (Pair<Boolean, Geometry> cell : cellList) {
            Geometry outCellGeom;
            // clip the cell to the original geometry
            if (clip && cell.getLeft()) {
                outCellGeom = cell.getValue().Intersection(inGeom);
            }
            else {
                outCellGeom = cell.getValue();
            }
            switch (outCellGeom.GetGeometryType()) {
                case ogrConstants.wkbMultiPolygon: {
                    int geomCount = outCellGeom.GetGeometryCount();
                    for (int i = 0; i < geomCount; i++) {
                        Geometry subGeom = outCellGeom.GetGeometryRef(i);
                        /*
                        these should always be polygons, but ogr can be weird
                        so it's worth checking and ignoring other geometry types
                         */
                        if (subGeom.GetGeometryType() == ogrConstants.wkbPolygon) {
                            outGeometry.AddGeometry(subGeom);
                        }
                    }
                    break;
                }
                case ogrConstants.wkbPolygon:
                    outGeometry.AddGeometry(outCellGeom);
                    break;
                /*
                This should be unreachable but on more complex inputs
                there is a possibility of lines or points making it in
                which would need to be ignored
                 */
                default:
                    break;
            }
        }
        return outGeometry;
    }
}
