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
        this.xMin = llCorner[0];
        this.yMin = urCorner[1];
        this.xMax = urCorner[0];
        this.yMax = urCorner[1];
    }

    Rectangle(double[] coords) {
        this.xMin = coords[0];
        this.xMax = coords[1];
        this.yMin = coords[2];
        this.yMax = coords[3];
    }

    static Rectangle FromGeometry(Geometry geom) {
        double[] bboxArr = new double[4];
        geom.GetEnvelope(bboxArr);
        System.out.println("Input array: " +Arrays.toString(bboxArr));

        return new Rectangle(bboxArr);
    }

    Geometry asGeometry() {
        Geometry lineString = new Geometry(ogrConstants.wkbLinearRing);
//        System.out.println("Linestring Type: " + lineString.GetGeometryType());
//        System.out.println("xMax: " + xMax + " yMax: " + yMax);

        // lower right
        lineString.AddPoint_2D(xMax, yMin);
        // upper right
        lineString.AddPoint_2D(xMax, yMax);
        // upper left
        lineString.AddPoint_2D(xMin, yMax);
        // lower left
        lineString.AddPoint_2D(xMin, yMin);
//        System.out.println("Linestring Type: " + lineString.GetGeometryType());
        // adds the first point to the end to close the ring
//        System.out.println("Ring Type: " + ring.GetGeometryType());
        lineString.CloseRings();

        Geometry outGeometry = new Geometry(ogrConstants.wkbPolygon);

//        System.out.println("From asGeometry before adding ring: " + outGeometry.GetGeometryType());
        outGeometry.AddGeometry(lineString);
//        System.out.println("From asGeometry: " + outGeometry.GetGeometryType());

        return outGeometry;
    }

    List<Rectangle> subdivide() {
        List<Rectangle> outList = new ArrayList<>();
        double xMid = (xMax + xMin) / 2;
        double yMid = (yMax + yMin) / 2;

        Rectangle nw = new Rectangle(new double[] {xMin, xMid, yMid, yMax});
        outList.add(nw);
        Rectangle ne = new Rectangle(new double[] {xMid, xMax, yMid, yMax});
        outList.add(ne);
        Rectangle sw = new Rectangle(new double[] {xMin, xMid, yMin, yMid});
        outList.add(sw);
        Rectangle se = new Rectangle(new double[] {xMid, xMax, yMin, yMid});
        outList.add(se);

        return outList;
    }
}
