package com.transecto;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

public final class ExportUtils {

    private ExportUtils() {
    }

    public static void exportGeoJson(List<GridGenerator.GridCell> cells, File outFile) throws Exception {
        DefaultFeatureCollection features = new DefaultFeatureCollection();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("grid");
        typeBuilder.add("geometry", Geometry.class);
        typeBuilder.add("id", String.class);
        typeBuilder.add("row", Integer.class);
        typeBuilder.add("col", Integer.class);
        typeBuilder.add("area_m2", Double.class);
        SimpleFeatureType type = typeBuilder.buildFeatureType();

        for (GridGenerator.GridCell cell : cells) {
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            GeometryFactory gf = new GeometryFactory();
            Geometry geometry = buildPolygonFromPoints(cell.points, gf);
            builder.set("geometry", geometry);
            builder.set("id", cell.id);
            builder.set("row", cell.row);
            builder.set("col", cell.col);
            builder.set("area_m2", cell.areaM2);
            features.add(builder.buildFeature(cell.id));
        }

        FeatureJSON json = new FeatureJSON();
        json.writeFeatureCollection(features, outFile);
    }

    public static void exportCsv(List<GridGenerator.GridCell> cells, File outFile) throws Exception {
        try (FileWriter writer = new FileWriter(outFile, StandardCharsets.UTF_8)) {
            writer.write("id,row,col,area_m2\n");
            for (GridGenerator.GridCell cell : cells) {
                writer.write(cell.id + "," + cell.row + "," + cell.col + "," + cell.areaM2 + "\n");
            }
        }
    }

    public static void exportShapefile(List<GridGenerator.GridCell> cells, File outFile) throws Exception {
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("grid");
        typeBuilder.setNamespaceURI("http://transecto.com");
        typeBuilder.add("geometry", Geometry.class);
        typeBuilder.add("id", String.class);
        typeBuilder.add("row", Integer.class);
        typeBuilder.add("col", Integer.class);
        typeBuilder.add("area_m2", Double.class);
        SimpleFeatureType type = typeBuilder.buildFeatureType();

        FileDataStore ds = FileDataStoreFinder.createDataStore(outFile);
        ds.createSchema(type);

        SimpleFeatureStore store = (SimpleFeatureStore) ds.getFeatureSource();
        DefaultFeatureCollection collection = new DefaultFeatureCollection();
        for (GridGenerator.GridCell cell : cells) {
            GeometryFactory gf = new GeometryFactory();
            Geometry geom = buildPolygonFromPoints(cell.points, gf);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            builder.set("geometry", geom);
            builder.set("id", cell.id);
            builder.set("row", cell.row);
            builder.set("col", cell.col);
            builder.set("area_m2", cell.areaM2);
            collection.add(builder.buildFeature(cell.id));
        }

        store.addFeatures(collection);
        ds.dispose();
    }

    private static Geometry buildPolygonFromPoints(List<org.openstreetmap.gui.jmapviewer.GeoPosition> positions, GeometryFactory gf) {
        if (positions == null || positions.isEmpty()) {
            return gf.createPolygon();
        }
        List<org.locationtech.jts.geom.Coordinate> coords = new ArrayList<>();
        for (org.openstreetmap.gui.jmapviewer.GeoPosition gp : positions) {
            coords.add(new org.locationtech.jts.geom.Coordinate(gp.getLon(), gp.getLat()));
        }
        if (coords.size() >= 3) {
            coords.add(coords.get(0));
            return gf.createPolygon(coords.toArray(new org.locationtech.jts.geom.Coordinate[0]));
        }
        return gf.createLineString(coords.toArray(new org.locationtech.jts.geom.Coordinate[0]));
    }
}
