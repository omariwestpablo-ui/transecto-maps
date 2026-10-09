package com.transecto;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MeasurementPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JLabel lengthLabel;
    private final JLabel areaLabel;

    public MeasurementPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(300, 110));

        lengthLabel = new JLabel("Longitud último segmento: 0.0 m");
        areaLabel = new JLabel("Área acumulada: 0.0 m²");
        lengthLabel.setForeground(new Color(30, 120, 255));
        areaLabel.setForeground(new Color(0, 140, 90));

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout(4, 4));
        content.add(lengthLabel, BorderLayout.NORTH);
        content.add(areaLabel, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    public void setValues(double lengthMeters, double areaSquareMeters) {
        lengthLabel.setText(String.format("Longitud último segmento: %.1f m", lengthMeters));
        areaLabel.setText(String.format("Área acumulada: %.2f m²", areaSquareMeters));
        repaint();
    }
}
