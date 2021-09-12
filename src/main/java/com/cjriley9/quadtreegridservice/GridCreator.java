package com.cjriley9.quadtreegridservice;

import org.gdal.ogr.Geometry;

import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.gdal.ogr.ogrConstants;

public class GridCreator {
    private double minGridSize = .25;
    private double maxGridSize =   5;
    private final Geometry inGeom;
    private boolean clip = true;
    private final Rectangle bbox;

    GridCreator(Geometry inGeom) {
        this.inGeom = inGeom;
        this.bbox = Rectangle.FromGeometry(inGeom);
    }

    GridCreator(Geometry inGeom, double minGridSize) {
        this.inGeom = inGeom;
        this.bbox = Rectangle.FromGeometry(inGeom);
        this.minGridSize = minGridSize;
    }

    GridCreator(Geometry inGeom, double minGridSize, double maxGridSize) {
        this.inGeom = inGeom;
        this.bbox = Rectangle.FromGeometry(inGeom);
        this.minGridSize = minGridSize;
        this.maxGridSize = maxGridSize;
    }

    void SetClip(boolean clip) {
        this.clip = clip;
    }

    /**
     * Fills a new queue with a set of cells of maxGridSize that cover the input AOI
     */
    Queue<Rectangle> prepQueue() {
        Queue<Rectangle> queue = new ArrayDeque<>();

        // get the number of cells needed to cover the AOI in each direction
        int xSize = (int)((bbox.xMax - bbox.xMin)/maxGridSize) + 1;
        int ySize = (int)((bbox.yMax - bbox.yMin)/maxGridSize) + 1;

        // used to center the new cells over the aoi
        double xRemainder = (bbox.xMax - bbox.xMin) % maxGridSize;
        double yRemainder = (bbox.yMax - bbox.yMin) % maxGridSize;
        double xBasis = bbox.xMin - (xRemainder / 2);
        double yBasis = bbox.yMin - (yRemainder / 2);

        for (int i = 0; i <= xSize; i++) {
            for (int j = 0; j <= ySize; j++) {
                double newXMin = xBasis + (i * maxGridSize);
                double newYMin = yBasis + (j * maxGridSize);
                double newXMax = xBasis + ((i + 1) * maxGridSize);
                double newYMax = yBasis + ((j + 1) * maxGridSize);

                double[] bounds = new double[]{newXMin, newXMax, newYMin, newYMax};
                queue.add(new Rectangle(bounds));
            }
        }

        return queue;
    }

    List<Pair<Boolean, Geometry>> processQueue(Queue<Rectangle> queue) {
        List<Pair<Boolean, Geometry>> outList = new ArrayList<>();
        Geometry boundary = inGeom.Boundary();

        while (!queue.isEmpty()) {
            Rectangle currentRect = queue.remove();
            double rectSize = currentRect.xMax - currentRect.xMin;
            Geometry polygon = currentRect.asGeometry();
            if (polygon.Intersects(boundary)) {
                if ((rectSize/2) >= minGridSize) {
                    List<Rectangle> subdivided = currentRect.subdivide();
                    subdivided.forEach((rect) -> queue.add(rect));
                }
                else {
                    outList.add(Pair.of(true, polygon));
                }
            }
            else if (polygon.Intersects(inGeom)) {
                outList.add(Pair.of(false, polygon));
            }
        }
//        while (!queue.isEmpty()) {
//            Rectangle currentRect = queue.remove();
//            outList.add(Pair.of(false, currentRect.asGeometry()));
//        }
        return outList;


    }

    Geometry Process() {
        Queue<Rectangle> queue = prepQueue();
        Geometry outGeometry = new Geometry(ogrConstants.wkbMultiPolygon);
        System.out.println(outGeometry.GetGeometryType());
        List<Pair<Boolean, Geometry>> cellList = processQueue(queue);
        for (Pair<Boolean, Geometry> cell : cellList) {
            Geometry outCellGeom;
            if (clip && cell.getLeft()) {
//                System.out.println("Getting intersection");
                outCellGeom = cell.getValue().Intersection(inGeom);
//                System.out.println(outCellGeom.GetGeometryType());
            }
            else {
//                System.out.println("No intersection");
                outCellGeom = cell.getValue();
//                System.out.println(test.GetGeometryType());
//                System.out.println(test.GetGeometryName());
            }
            switch (outCellGeom.GetGeometryType()) {
                case ogrConstants.wkbMultiPolygon: {
                    int geomCount = outCellGeom.GetGeometryCount();
                    for (int i = 0; i < geomCount; i++) {
                        Geometry subGeom = outCellGeom.GetGeometryRef(i);
                        if (subGeom.GetGeometryType() == ogrConstants.wkbPolygon) {
                            outGeometry.AddGeometry(subGeom);
                        }
                    }
                    break;
                }
                case ogrConstants.wkbPolygon:
                    outGeometry.AddGeometry(outCellGeom);
                    break;
                default:
                    break;
            }
        }
        System.out.println(outGeometry.GetGeometryCount());
        return outGeometry;
    }
}
