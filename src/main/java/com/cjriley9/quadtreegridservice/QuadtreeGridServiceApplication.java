package com.cjriley9.quadtreegridservice;

import org.gdal.ogr.ogr;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;

import java.util.Arrays;
import java.util.Optional;

@SpringBootApplication
@RestController
public class QuadtreeGridServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuadtreeGridServiceApplication.class, args);
    }

    @PostMapping("/quadtreeGrid")
    public String hello(@RequestParam(defaultValue = "true") boolean clip,
                        @RequestParam Optional<Double> maxGridSize,
                        @RequestParam Optional<Double> minGridSize,
                        @RequestBody String inFeatures) {
        Geometry inGeom = Geometry.CreateFromJson(inFeatures);
        // try to make it valid before throwing an invalid error
        inGeom.MakeValid();
        // return error on invalid or empty geometry
        if (!inGeom.IsValid() && !inGeom.IsEmpty()) {
            return "Error";
        }
        // only work with 2d geometry, do before checking type to reduce the number of options to check
        inGeom.FlattenTo2D();
        // make sure we have a polygon or multipolygon
        switch (inGeom.GetGeometryType()) {
            case ogrConstants.wkbPolygon:
                break;
            case ogrConstants.wkbMultiPolygon:
                break;
            default:
                return "Invalid input geometry type, must be Polygon or MultiPolygon";
        }
        GridCreator creator = new GridCreator(inGeom, .25);

        // process parameters
        maxGridSize.ifPresent(creator::SetMaxGridSize);
        minGridSize.ifPresent(creator::SetMinGridSize);
        creator.SetClip(clip);

        Geometry outGeom = creator.Process();
        return outGeom.ExportToJson();
    }

}
