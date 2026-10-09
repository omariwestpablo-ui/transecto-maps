package com.transecto;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import org.openstreetmap.gui.jmapviewer.GeoPosition;

public class DrawHandler {

    public enum Mode {
        NONE,
        POLYGON,
        LINE
    }

    private final MapPanel mapPanel;
    private Mode mode = Mode.NONE;
    private final List<GeoPosition> polygonVertices = new ArrayList<>();
    private final List<GeoPosition> lineVertices = new ArrayList<>();

    public DrawHandler(MapPanel panel) {
        this.mapPanel = panel;
        attachMouseListener();
    }

    public void setMode(Mode newMode) {
        this.mode = newMode;
        polygonVertices.clear();
        lineVertices.clear();
    }

    public void cancelDrawing() {
        mode = Mode.NONE;
        polygonVertices.clear();
        lineVertices.clear();
    }

    private void attachMouseListener() {
        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                GeoPosition pos = mapPanel.getPosition(e.getPoint());
                if (pos == null) {
                    return;
                }

                if (mode == Mode.POLYGON) {
                    polygonVertices.add(pos);
                    if (polygonVertices.size() > 0) {
                        // todo: guardado del polígono activo en la UI principal
                    }
                    // Double click ends polygon in this simplified implementation.
                    if (e.getClickCount() == 2) {
                        mode = Mode.NONE;
                    }
                }

                if (mode == Mode.LINE) {
                    lineVertices.add(pos);
                    if (e.getClickCount() == 2) {
                        mode = Mode.NONE;
                    }
                }
            }
        });
    }

    public List<GeoPosition> getPolygonVertices() {
        return new ArrayList<>(polygonVertices);
    }

    public List<GeoPosition> getLineVertices() {
        return new ArrayList<>(lineVertices);
    }
}
