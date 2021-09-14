package com.cjriley9.quadtreegridservice;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;


public class Rectangle {
    final double xMin;
    final double yMin;
    final double xMax;
    final double yMax;

    Rectangle(double[] llCorner, double[] urCorner) throws InvalidRectangleException {
        if (llCorner.length != 2 || urCorner.length != 2) {
            throw new InvalidRectangleException("The lower left corner and upper right corner must each be defined by an array of 2 coordinates");
        }
        if (llCorner[0] >= urCorner[0] || llCorner[1] >= urCorner[1]) {
            throw new InvalidRectangleException("lower left x and y values must be less than their corresponding upper right values");
        }
        this.xMin = llCorner[0];
        this.yMin = urCorner[1];
        this.xMax = urCorner[0];
        this.yMax = urCorner[1];
    }

    Rectangle(double[] coords) throws InvalidRectangleException {
        if (coords[0] >= coords[1] || coords[2] >= coords[3]) {
            throw new InvalidRectangleException("lower left x and y values must be less than their corresponding upper right values");
        }
        this.xMin = coords[0];
        this.xMax = coords[1];
        this.yMin = coords[2];
        this.yMax = coords[3];
    }

    static Rectangle fromGeometry(Geometry geom) throws InvalidRectangleException{
        double[] bboxArr = new double[4];
        geom.GetEnvelope(bboxArr);
        return new Rectangle(bboxArr);
    }


    /**
     * @return an OGR geometry representation of the rectangle
     */
    Geometry asGeometry() {
        // need to start as a linear ring since points can't be added to a polygon directly
        Geometry ring = new Geometry(ogrConstants.wkbLinearRing);

        // lower right
        ring.AddPoint_2D(xMax, yMin);
        // upper right
        ring.AddPoint_2D(xMax, yMax);
        // upper left
        ring.AddPoint_2D(xMin, yMax);
        // lower left
        ring.AddPoint_2D(xMin, yMin);

        // adds the first point to the end to close the ring
        ring.CloseRings();

        Geometry outGeometry = new Geometry(ogrConstants.wkbPolygon);

        outGeometry.AddGeometry(ring);
        return outGeometry;
    }

    /**
     * The only way this is likely to throw an exception is because of floating point inaccuracies causing the min and max
     * to become equal
     * @return a list of 4 rectangles that are the result of subdividing the original rectangle
     */
    List<Rectangle> subdivide() throws InvalidRectangleException {
        List<Rectangle> outList = new ArrayList<>();
        double xMid = (xMax + xMin) / 2;
        double yMid = (yMax + yMin) / 2;

        Rectangle nw = new Rectangle(new double[]{xMin, xMid, yMid, yMax});
        outList.add(nw);
        Rectangle ne = new Rectangle(new double[]{xMid, xMax, yMid, yMax});
        outList.add(ne);
        Rectangle sw = new Rectangle(new double[]{xMin, xMid, yMin, yMid});
        outList.add(sw);
        Rectangle se = new Rectangle(new double[]{xMid, xMax, yMin, yMid});
        outList.add(se);

        return outList;
    }
}
