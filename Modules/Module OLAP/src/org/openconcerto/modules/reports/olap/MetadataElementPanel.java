package org.openconcerto.modules.reports.olap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;

import org.olap4j.metadata.MetadataElement;
import org.openconcerto.modules.reports.olap.renderer.RoundBorder;

public class MetadataElementPanel extends JLabel implements MouseListener {

    private Color color2;
    private ParameterHolder parameterHolder;
    private MetadataElement element;

    public MetadataElementPanel(MetadataElement e, ParameterHolder parameterHolder) {
        this.element = e;
        this.parameterHolder = parameterHolder;
        this.setOpaque(false);
        this.setText(e.getCaption());
        this.setToolTipText(e.getUniqueName());

        if (ParameterHolder.isDimension(e)) {
            setDimensionColor();
        } else {
            setMeasureColor();
        }

        this.addMouseListener(this);

    }

    private void setDimensionColor() {
        this.setBackground(new Color(162, 221, 250));
        this.setBackground2(new Color(217, 239, 250));
        this.setBorder(new RoundBorder(new Color(89, 154, 186)));
    }

    private void setMeasureColor() {
        this.setBackground(new Color(242, 192, 211));
        this.setBackground2(new Color(242, 220, 229));
        this.setBorder(new RoundBorder(new Color(214, 146, 174)));
    }

    private void setBackground2(Color color) {
        this.color2 = color;

    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(this.getBackground());
        g.fillRect(2, 2, this.getWidth() - 4, this.getHeight() - 4);
        g.setColor(this.color2);
        g.fillRect(2, 2, this.getWidth() - 4, this.getHeight() / 2);
        super.paintComponent(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            this.parameterHolder.remove(this.element);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

}
