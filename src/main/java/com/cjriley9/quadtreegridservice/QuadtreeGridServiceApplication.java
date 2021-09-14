package com.cjriley9.quadtreegridservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogrConstants;
import org.springframework.web.server.ResponseStatusException;

@SpringBootApplication
@RestController
public class QuadtreeGridServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuadtreeGridServiceApplication.class, args);
    }

    @PostMapping(value = "/generate-quadtree-grid",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String generateQuadtreeGrid(@RequestParam(defaultValue = "true") boolean clip,
                        @RequestParam(defaultValue = "5.0") double maxGridSize,
                        @RequestParam(defaultValue = "0.25") double minGridSize,
                        @RequestBody String inFeatures) {

        // make sure max size is larger than min size before starting to process the geometry
        if (maxGridSize <= minGridSize) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxGridSize must be larger than minGridsize");
        }

        Geometry inGeom = Geometry.CreateFromJson(inFeatures);

        if (inGeom == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to parse input GeoJSON");
        }

        // try to make it valid before proceeding
        inGeom.MakeValid();

        // return error on empty geometry
        if (inGeom.IsEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid or empty geometry"
            );
        }
        // only work with 2d geometry, do before checking type to reduce the number of options to check
        inGeom.FlattenTo2D();

        // make sure we have a polygon or multipolygon, error out if not
        switch (inGeom.GetGeometryType()) {
            case ogrConstants.wkbPolygon:
            case ogrConstants.wkbMultiPolygon:
                break;
            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid geometry type, must be Polygon or Multipolygon"
                );
        }

        GridCreator creator;
        try {
            creator = new GridCreator(inGeom);
        } catch (InvalidRectangleException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error creating bounding box from input geometry: " + e.getMessage());
        }

        // process parameters
        creator.setMaxGridSize(maxGridSize);
        creator.setMinGridSize(minGridSize);
        creator.setClip(clip);
        try {
            Geometry outGeom = creator.Process();
            return outGeom.ExportToJson();
        } catch (GridCreatorException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while processing input geometry"
            );
        }
    }

}
