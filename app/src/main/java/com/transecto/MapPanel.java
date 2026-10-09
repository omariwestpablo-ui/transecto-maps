package com.transecto;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.openstreetmap.gui.jmapviewer.GeoPosition;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MapPolygon;

public class MapPanel extends JMapViewer {

    private static final long serialVersionUID = 1L;

    private DrawHandler drawHandler;
    private List<GeoPosition> polygonVertices = new ArrayList<>();
    private List<GeoPosition> lineVertices = new ArrayList<>();
    private final List<OverlayShape> polygonOverlays = new ArrayList<>();
    private final List<OverlayShape> gridOverlays = new ArrayList<>();
    private final List<OverlayShape> transectOverlays = new ArrayList<>();
    private final List<MapMarkerDot> stationMarkers = new ArrayList<>();
    private TileSource tileSource = TileSource.BING_AERIAL;

    public enum TileSource {
        BING_AERIAL,
        OSM,
        HYBRID
    }

    public void setTileSource(TileSource source) {
        this.tileSource = source;
        if (source == TileSource.BING_AERIAL) {
            setTileSource(new org.openstreetmap.gui.jmapviewer.BingAerialTileSource());
        } else if (source == TileSource.OSM) {
            setTileSource(org.openstreetmap.gui.jmapviewer.OsmTileSource.Mapnik);
        } else {
            setTileSource(new org.openstreetmap.gui.jmapviewer.BingAerialTileSource());
        }
        repaint();
    }

    public void setDrawHandler(DrawHandler handler) {
        this.drawHandler = handler;
    }

    public void setPolygonVertices(List<GeoPosition> vertices, Color stroke, Color fill) {
        polygonVertices.clear();
        polygonVertices.addAll(vertices);
        polygonOverlays.clear();
        polygonOverlays.add(new OverlayShape(vertices, stroke, fill));
        repaint();
    }

    public void clearPolygons() {
        polygonVertices.clear();
        polygonOverlays.clear();
        repaint();
    }

    public void setGridCells(List<GridGenerator.GridCell> cells) {
        gridOverlays.clear();
        for (GridGenerator.GridCell cell : cells) {
            gridOverlays.add(new OverlayShape(cell.points, new Color(0xFF00CC00), new Color(0x5000FF00)));
        }
        repaint();
    }

    public void clearGrid() {
        gridOverlays.clear();
        repaint();
    }

    public void setTransects(List<TransectGenerator.Transect> transects) {
        transectOverlays.clear();
        stationMarkers.clear();
        for (TransectGenerator.Transect t : transects) {
            transectOverlays.add(new OverlayShape(t.points, new Color(0xFFFF0000), new Color(0x00000000)));
            addMapMarker(t.stationMarker);
        }
        repaint();
    }

    public void clearTransects() {
        transectOverlays.clear();
        stationMarkers.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (OverlayShape overlay : polygonOverlays) {
            drawOverlay(g2, overlay);
        }
        for (OverlayShape overlay : gridOverlays) {
            drawOverlay(g2, overlay);
        }
        for (OverlayShape overlay : transectOverlays) {
            drawOverlay(g2, overlay);
        }
        g2.dispose();
    }

    private void drawOverlay(Graphics2D g2, OverlayShape overlay) {
        if (overlay == null || overlay.points == null || overlay.points.size() < 2) {
            return;
        }
        Path2D path = new Path2D.Double();
        Point p0 = getMapPosition(overlay.points.get(0));
        path.moveTo(p0.getX(), p0.getY());
        for (int i = 1; i < overlay.points.size(); i++) {
            Point p = getMapPosition(overlay.points.get(i));
            path.lineTo(p.getX(), p.getY());
        }
        if (overlay.points.size() > 2) {
            path.closePath();
        }

        g2.setColor(overlay.fillColor);
        g2.fill(path);
        g2.setColor(overlay.strokeColor);
        g2.setStroke(new BasicStroke(2));
        g2.draw(path);
    }

    public static class OverlayShape {
        public final List<GeoPosition> points;
        public final Color strokeColor;
        public final Color fillColor;

        public OverlayShape(List<GeoPosition> points, Color strokeColor, Color fillColor) {
            this.points = new ArrayList<>(points);
            this.strokeColor = strokeColor;
            this.fillColor = fillColor;
        }
    }
}
