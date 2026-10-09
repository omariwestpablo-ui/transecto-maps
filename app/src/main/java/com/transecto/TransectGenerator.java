package com.transecto;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;

public class TransectGenerator {

    public static class Transect {
        public final String id;
        public final double distanceFromStartM;
        public final double latitude;
        public final double longitude;
        public final List<org.openstreetmap.gui.jmapviewer.GeoPosition> points;
        public final org.openstreetmap.gui.jmapviewer.MapMarkerDot stationMarker;

        public Transect(String id, double distanceFromStartM, double latitude, double longitude,
                       List<org.openstreetmap.gui.jmapviewer.GeoPosition> points,
                       org.openstreetmap.gui.jmapviewer.MapMarkerDot stationMarker) {
            this.id = id;
            this.distanceFromStartM = distanceFromStartM;
            this.latitude = latitude;
            this.longitude = longitude;
            this.points = points;
            this.stationMarker = stationMarker;
        }
    }

    public static List<Transect> generateTransects(Geometry line, double spacingM, double halfLengthM) {
        List<Transect> result = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return result;
        }
        LengthIndexedLine indexedLine = new LengthIndexedLine(line);
        double total = line.getLength();
        GeometryFactory gf = new GeometryFactory();

        for (double d = 0; d <= total; d += spacingM) {
            Coordinate origin = indexedLine.extractPoint(d);
            double angle = computeLineAngle(indexedLine, d, total);
            double leftAngle = angle + Math.PI / 2.0;
            double rightAngle = angle - Math.PI / 2.0;

            Coordinate left = new Coordinate(
                origin.x + Math.cos(leftAngle) * halfLengthM,
                origin.y + Math.sin(leftAngle) * halfLengthM
            );
            Coordinate right = new Coordinate(
                origin.x + Math.cos(rightAngle) * halfLengthM,
                origin.y + Math.sin(rightAngle) * halfLengthM
            );

            LineString ls = gf.createLineString(new Coordinate[] { left, right });
            List<org.openstreetmap.gui.jmapviewer.GeoPosition> points = new ArrayList<>();
            points.add(new org.openstreetmap.gui.jmapviewer.GeoPosition(left.y, left.x));
            points.add(new org.openstreetmap.gui.jmapviewer.GeoPosition(right.y, right.x));

            org.openstreetmap.gui.jmapviewer.MapMarkerDot marker = new org.openstreetmap.gui.jmapviewer.MapMarkerDot(
                new org.openstreetmap.gui.jmapviewer.GeoPosition(origin.y, origin.x)
            );
            result.add(new Transect("T" + (result.size() + 1), d, origin.y, origin.x, points, marker));
        }
        return result;
    }

    private static double computeLineAngle(LengthIndexedLine indexedLine, double distance, double totalLength) {
        if (distance <= 0) {
            Coordinate a = indexedLine.extractPoint(0.0);
            Coordinate b = indexedLine.extractPoint(Math.min(totalLength, 1.0));
            return Math.atan2(b.y - a.y, b.x - a.x);
        }
        Coordinate a = indexedLine.extractPoint(Math.max(0.0, distance - 1.0));
        Coordinate b = indexedLine.extractPoint(Math.min(totalLength, distance + 1.0));
        return Math.atan2(b.y - a.y, b.x - a.x);
    }
}
