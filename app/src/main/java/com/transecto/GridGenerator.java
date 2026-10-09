package com.transecto;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public class GridGenerator {

    public static class GridCell {
        public final String id;
        public final int row;
        public final int col;
        public final double areaM2;
        public final List<org.openstreetmap.gui.jmapviewer.GeoPosition> points;

        public GridCell(String id, int row, int col, double areaM2, List<org.openstreetmap.gui.jmapviewer.GeoPosition> points) {
            this.id = id;
            this.row = row;
            this.col = col;
            this.areaM2 = areaM2;
            this.points = points;
        }
    }

    public static List<GridCell> createGrid(Geometry polygon, double cellWidth, double cellHeight) {
        List<GridCell> cells = new ArrayList<>();
        if (polygon == null || polygon.isEmpty()) {
            return cells;
        }

        Envelope env = polygon.getEnvelopeInternal();
        double minX = env.getMinX();
        double maxX = env.getMaxX();
        double minY = env.getMinY();
        double maxY = env.getMaxY();

        GeometryFactory gf = new GeometryFactory();

        int colIndex = 0;
        for (double x = minX; x < maxX; x += cellWidth) {
            int rowIndex = 0;
            for (double y = minY; y < maxY; y += cellHeight) {
                Coordinate[] shell = new Coordinate[] {
                    new Coordinate(x, y),
                    new Coordinate(x + cellWidth, y),
                    new Coordinate(x + cellWidth, y + cellHeight),
                    new Coordinate(x, y + cellHeight),
                    new Coordinate(x, y)
                };
                Polygon rect = gf.createPolygon(shell);
                Geometry intersection = rect.intersection(polygon);
                if (intersection == null || intersection.isEmpty()) {
                    rowIndex++;
                    continue;
                }
                double area = intersection.getArea();
                if (area <= 0.0) {
                    rowIndex++;
                    continue;
                }

                String id = getCellId(rowIndex, colIndex);
                List<org.openstreetmap.gui.jmapviewer.GeoPosition> points = new ArrayList<>();
                for (Coordinate c : intersection.getCoordinates()) {
                    if (Double.isFinite(c.x) && Double.isFinite(c.y)) {
                        points.add(new org.openstreetmap.gui.jmapviewer.GeoPosition(c.y, c.x));
                    }
                }
                cells.add(new GridCell(id, rowIndex, colIndex, area, points));
                rowIndex++;
            }
            colIndex++;
        }
        return cells;
    }

    private static String getCellId(int row, int col) {
        char base = (char) ('A' + (col % 26));
        return base + String.valueOf(row + 1);
    }
}
