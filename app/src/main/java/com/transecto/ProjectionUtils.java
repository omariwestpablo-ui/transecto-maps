package com.transecto;

import java.util.ArrayList;
import java.util.List;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.openstreetmap.gui.jmapviewer.GeoPosition;

public final class ProjectionUtils {

    private ProjectionUtils() {
    }

    public static Geometry toProjectedGeometry(List<GeoPosition> positions) {
        if (positions == null || positions.isEmpty()) {
            return new GeometryFactory().createPoint();
        }
        Coordinate[] coords = new Coordinate[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            GeoPosition gp = positions.get(i);
            coords[i] = new Coordinate(gp.getLon(), gp.getLat());
        }

        GeometryFactory gf = new GeometryFactory();
        if (coords.length == 1) {
            return gf.createPoint(coords[0]);
        }
        if (coords.length == 2) {
            return gf.createLineString(coords);
        }
        return gf.createPolygon(coords);
    }

    public static int detectUtmZone(double latitude, double longitude) {
        return (int) Math.floor((longitude + 180.0) / 6.0) + 1;
    }

    public static int getHemisphere(double latitude) {
        return latitude >= 0 ? 326 : 327;
    }

    public static Coordinate projectToUtm(GeoPosition geo) {
        try {
            int zone = detectUtmZone(geo.getLat(), geo.getLon());
            int epsg = getHemisphere(geo.getLat());
            String code = "EPSG:" + (epsg + zone - 1);
            CoordinateReferenceSystem src = CRS.decode("EPSG:4326");
            CoordinateReferenceSystem dst = CRS.decode(code);
            MathTransform transform = CRS.findMathTransform(src, dst, true);
            GeometryFactory gf = new GeometryFactory();
            Point point = gf.createPoint(new Coordinate(geo.getLon(), geo.getLat()));
            Point projected = (Point) JTS.transform(point, transform);
            return projected.getCoordinate();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo proyectar a UTM: " + ex.getMessage(), ex);
        }
    }

    public static double calculatePolylineLengthMeters(List<GeoPosition> positions) {
        if (positions.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 1; i < positions.size(); i++) {
            Coordinate prev = projectToUtm(positions.get(i - 1));
            Coordinate curr = projectToUtm(positions.get(i));
            total += Math.hypot(curr.x - prev.x, curr.y - prev.y);
        }
        return total;
    }

    public static double calculateAccumulatedAreaSquareMeters(List<GeoPosition> positions) {
        if (positions.size() < 3) {
            return 0.0;
        }
        List<Coordinate> projected = new ArrayList<>();
        for (GeoPosition gp : positions) {
            projected.add(projectToUtm(gp));
        }
        projected.add(projected.get(0));
        GeometryFactory gf = new GeometryFactory();
        Polygon poly = gf.createPolygon(projected.toArray(new Coordinate[0]));
        return poly.getArea();
    }

    public static List<Coordinate> convertToUtm(List<GeoPosition> positions) {
        List<Coordinate> result = new ArrayList<>();
        for (GeoPosition gp : positions) {
            result.add(projectToUtm(gp));
        }
        return result;
    }
}
