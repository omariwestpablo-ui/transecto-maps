package com.transecto;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.openstreetmap.gui.jmapviewer.CoordinateConverter;
import org.openstreetmap.gui.jmapviewer.GeoPosition;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.OsmTileSource;
import org.openstreetmap.gui.jmapviewer.layers.TileLayer;

public class Main extends JFrame {

    private static final long serialVersionUID = 1L;

    private final MapPanel mapPanel;
    private final MeasurementPanel measurementPanel;
    private final DrawHandler drawHandler;
    private final JLabel statusLabel;
    private final JTextField widthField;
    private final JTextField heightField;
    private final JTextField transectSpacingField;
    private final JTextField transectLengthField;
    private final DefaultTableModel gridTableModel;
    private final DefaultTableModel transectTableModel;

    private final List<GeoPosition> polygonVertices = new ArrayList<>();
    private final List<GeoPosition> lineVertices = new ArrayList<>();
    private final List<GridGenerator.GridCell> gridCells = new ArrayList<>();
    private final List<TransectGenerator.Transect> transects = new ArrayList<>();

    public Main() {
        super("Transecto Map");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setSize(1400, 900);
        setLocationRelativeTo(null);

        mapPanel = new MapPanel();
        mapPanel.setTileSource(MapPanel.TileSource.BING_AERIAL);
        mapPanel.setZoom(12);
        mapPanel.setDisplayPosition(new GeoPosition(19.4326, -99.1332), 12);

        drawHandler = new DrawHandler(mapPanel);
        mapPanel.setDrawHandler(drawHandler);

        JTabbedPane tabs = new JTabbedPane();
        JPanel areaPanel = buildAreaPanel();
        JPanel cuadrantsPanel = buildCuadrantesPanel();
        JPanel transectosPanel = buildTransectosPanel();
        JPanel exportPanel = buildExportPanel();

        tabs.addTab("📐 Área", areaPanel);
        tabs.addTab("📊 Cuadrantes", cuadrantsPanel);
        tabs.addTab("📏 Transectos", transectosPanel);
        tabs.addTab("📁 Exportar", exportPanel);

        JPanel side = new JPanel();
        side.setLayout(new BorderLayout());
        side.setPreferredSize(new Dimension(340, 800));
        side.add(new JScrollPane(tabs), BorderLayout.CENTER);

        add(createToolbar(), BorderLayout.NORTH);
        add(mapPanel, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);

        statusLabel = new JLabel("Listo");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(statusLabel, BorderLayout.SOUTH);

        updateMeasurementPanel();
    }

    private JPanel buildAreaPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        measurementPanel = new MeasurementPanel();
        measurementPanel.setBorder(BorderFactory.createTitledBorder("Mediciones en tiempo real"));
        panel.add(measurementPanel);

        panel.add(Box.createVerticalStrut(12));

        JLabel areaInfo = new JLabel("Área y perímetro del polígono activo");
        panel.add(areaInfo);

        JLabel areaValue = new JLabel("- ");
        areaValue.setFont(areaValue.getFont().deriveFont(18f));
        panel.add(areaValue);

        JButton btnCloseArea = new JButton("Cerrar polígono");
        btnCloseArea.addActionListener(e -> closePolygon());

        JButton btnClearArea = new JButton("Limpiar polígono");
        btnClearArea.addActionListener(e -> clearPolygon());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(btnCloseArea);
        buttons.add(btnClearArea);
        panel.add(buttons);

        return panel;
    }

    private JPanel buildCuadrantesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        widthField = new JTextField("10");
        heightField = new JTextField("10");
        form.add(new JLabel("Tamaño horizontal (m)"));
        form.add(widthField);
        form.add(new JLabel("Tamaño vertical (m)"));
        form.add(heightField);
        form.add(new JLabel(""));
        form.add(new JLabel(""));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton generate = new JButton("Generar cuadrantes");
        generate.addActionListener(e -> generateGrid());
        JButton clear = new JButton("Limpiar cuadrantes");
        clear.addActionListener(e -> clearGrid());
        actions.add(generate);
        actions.add(clear);

        String[] cols = {"ID", "Fila", "Columna", "Área (m²)"};
        gridTableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(gridTableModel);

        panel.add(form, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.CENTER);
        panel.add(new JScrollPane(table), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildTransectosPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        transectSpacingField = new JTextField("20");
        transectLengthField = new JTextField("10");
        form.add(new JLabel("Intervalo (m)"));
        form.add(transectSpacingField);
        form.add(new JLabel("Longitud cada lado (m)"));
        form.add(transectLengthField);
        form.add(new JLabel(""));
        form.add(new JLabel(""));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton gen = new JButton("Generar transectos");
        gen.addActionListener(e -> generateTransects());
        JButton clear = new JButton("Limpiar transectos");
        clear.addActionListener(e -> clearTransects());
        actions.add(gen);
        actions.add(clear);

        String[] cols = {"ID", "Distancia (m)", "Lat", "Lon"};
        transectTableModel = new DefaultTableModel(cols, 0);
        JTable table = new JTable(transectTableModel);

        panel.add(form, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.CENTER);
        panel.add(new JScrollPane(table), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildExportPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton geoJson = new JButton("Descargar GeoJSON");
        geoJson.addActionListener(e -> downloadGeoJson());
        JButton csv = new JButton("Descargar CSV");
        csv.addActionListener(e -> downloadCsv());
        JButton shp = new JButton("Descargar Shapefile");
        shp.addActionListener(e -> downloadShapefile());

        panel.add(geoJson);
        panel.add(Box.createVerticalStrut(8));
        panel.add(csv);
        panel.add(Box.createVerticalStrut(8));
        panel.add(shp);

        return panel;
    }

    private JPanel createToolbar() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton polygonBtn = new JButton("Dibujar polígono");
        polygonBtn.addActionListener(e -> {
            drawHandler.setMode(DrawHandler.Mode.POLYGON);
            statusLabel.setText("Dibujo de polígono activo");
        });

        JButton lineBtn = new JButton("Dibujar línea");
        lineBtn.addActionListener(e -> {
            drawHandler.setMode(DrawHandler.Mode.LINE);
            statusLabel.setText("Dibujo de transecto activo");
        });

        JButton cancelBtn = new JButton("Cancelar dibujo");
        cancelBtn.addActionListener(e -> {
            drawHandler.cancelDrawing();
            polygonVertices.clear();
            lineVertices.clear();
            statusLabel.setText("Dibujo cancelado");
        });

        JButton toggleTheme = new JButton("Tema");
        toggleTheme.addActionListener(e -> toggleTheme());

        toolbarPanel.add(polygonBtn);
        toolbarPanel.add(lineBtn);
        toolbarPanel.add(cancelBtn);
        toolbarPanel.add(toggleTheme);
        return toolbarPanel;
    }

    private void toggleTheme() {
        if (getContentPane().getBackground().equals(Color.WHITE)) {
            getContentPane().setBackground(new Color(40, 40, 40));
            setForeground(Color.WHITE);
        } else {
            getContentPane().setBackground(Color.WHITE);
            setForeground(Color.BLACK);
        }
        repaint();
    }

    private void updateMeasurementPanel() {
        if (measurementPanel == null) {
            return;
        }
        if (polygonVertices.size() < 2) {
            measurementPanel.setValues(0.0, 0.0);
            return;
        }
        double length = ProjectionUtils.calculatePolylineLengthMeters(polygonVertices);
        double area = ProjectionUtils.calculateAccumulatedAreaSquareMeters(polygonVertices);
        measurementPanel.setValues(length, area);
    }

    private void closePolygon() {
        if (polygonVertices.size() < 3) {
            JOptionPane.showMessageDialog(this, "Se requieren al menos 3 vértices para cerrar el polígono.");
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                List<GeoPosition> vertices = new ArrayList<>(polygonVertices);
                if (vertices.size() > 2) {
                    mapPanel.setPolygonVertices(vertices, Color.BLUE, new Color(64, 0, 255, 80));
                    updateMeasurementPanel();
                }
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("Polígono generado");
            }
        };
        worker.execute();
    }

    private void clearPolygon() {
        polygonVertices.clear();
        mapPanel.clearPolygons();
        statusLabel.setText("Polígono limpiado");
        updateMeasurementPanel();
    }

    private void generateGrid() {
        if (polygonVertices.size() < 3) {
            JOptionPane.showMessageDialog(this, "Primero dibuje un polígono válido.");
            return;
        }

        try {
            double width = Double.parseDouble(widthField.getText());
            double height = Double.parseDouble(heightField.getText());
            if (width <= 0 || height <= 0) {
                throw new NumberFormatException();
            }
            final double w = width;
            final double h = height;

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    Geometry polygon = ProjectionUtils.toProjectedGeometry(polygonVertices);
                    List<GridGenerator.GridCell> cells = GridGenerator.createGrid(polygon, w, h);
                    gridCells.clear();
                    gridCells.addAll(cells);
                    mapPanel.setGridCells(cells);
                    return null;
                }

                @Override
                protected void done() {
                    gridTableModel.setRowCount(0);
                    for (GridGenerator.GridCell cell : gridCells) {
                        gridTableModel.addRow(new Object[]{cell.id, cell.row, cell.col, String.format(Locale.US, "%.2f", cell.areaM2)});
                    }
                    statusLabel.setText("Cuadrantes generados: " + gridCells.size());
                }
            };
            worker.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Revise los valores del tamaño del cuadrante.");
        }
    }

    private void clearGrid() {
        gridCells.clear();
        gridTableModel.setRowCount(0);
        mapPanel.clearGrid();
    }

    private void generateTransects() {
        if (lineVertices.size() < 2) {
            JOptionPane.showMessageDialog(this, "Primero dibuje una línea válida.");
            return;
        }
        try {
            double spacing = Double.parseDouble(transectSpacingField.getText());
            double length = Double.parseDouble(transectLengthField.getText());
            if (spacing <= 0 || length <= 0) {
                throw new NumberFormatException();
            }
            final double s = spacing;
            final double l = length;

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    Geometry line = ProjectionUtils.toProjectedGeometry(lineVertices);
                    List<TransectGenerator.Transect> list = TransectGenerator.generateTransects(line, s, l);
                    transects.clear();
                    transects.addAll(list);
                    mapPanel.setTransects(list);
                    return null;
                }

                @Override
                protected void done() {
                    transectTableModel.setRowCount(0);
                    for (TransectGenerator.Transect t : transects) {
                        transectTableModel.addRow(new Object[]{t.id, String.format(Locale.US, "%.1f", t.distanceFromStartM), String.format(Locale.US, "%.6f", t.latitude), String.format(Locale.US, "%.6f", t.longitude)});
                    }
                    statusLabel.setText("Transectos generados: " + transects.size());
                }
            };
            worker.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Revise los valores del intervalo y la longitud del transecto.");
        }
    }

    private void clearTransects() {
        transects.clear();
        transectTableModel.setRowCount(0);
        mapPanel.clearTransects();
    }

    private void downloadGeoJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar GeoJSON");
        chooser.setSelectedFile(new File("quadrantes.geojson"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                ExportUtils.exportGeoJson(gridCells, chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "GeoJSON guardado correctamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el GeoJSON: " + ex.getMessage());
            }
        }
    }

    private void downloadCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar CSV");
        chooser.setSelectedFile(new File("cuadrantes.csv"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                ExportUtils.exportCsv(gridCells, chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "CSV guardado correctamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el CSV: " + ex.getMessage());
            }
        }
    }

    private void downloadShapefile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar Shapefile");
        chooser.setSelectedFile(new File("cuadrantes.shp"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                ExportUtils.exportShapefile(gridCells, chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Shapefile guardado correctamente.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el Shapefile: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.setVisible(true);
        });
    }
}
