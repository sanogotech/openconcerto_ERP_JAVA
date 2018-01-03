package org.openconcerto.modules.reports.olap.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.Border;

public class RoundBorder implements Border {
    protected int m_w = 6;
    protected int m_h = 6;
    protected Color color;

    public RoundBorder(Color c) {
        this.color = c;
    }

    public Insets getBorderInsets(Component c) {
        return new Insets(m_h, m_w, m_h, m_w);
    }

    public boolean isBorderOpaque() {
        return true;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        w = w - 3;
        h = h - 3;
        x++;
        y++;

        g.setColor(color);
        g.drawLine(x, y + 2, x, y + h - 2);
        g.drawLine(x + 2, y, x + w - 2, y);
        g.drawLine(x, y + 2, x + 2, y); // Top left diagonal
        g.drawLine(x, y + h - 2, x + 2, y + h); // Bottom left diagonal

        g.drawLine(x + w, y + 2, x + w, y + h - 2);
        g.drawLine(x + 2, y + h, x + w - 2, y + h);
        g.drawLine(x + w - 2, y, x + w, y + 2); // Top right diagonal
        g.drawLine(x + w, y + h - 2, x + w - 2, y + h); // Bottom right diagonal

    }
}
