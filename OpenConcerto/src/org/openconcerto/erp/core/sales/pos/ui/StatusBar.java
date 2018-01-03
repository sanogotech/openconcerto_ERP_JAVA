/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.erp.core.sales.pos.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

public class StatusBar extends JPanel {

    private String title = "";

    public StatusBar() {
        setFont(new Font("Arial", Font.BOLD, 24));
    }

    public void setTitle(String t) {
        if (this.title == null || !this.title.equals(t)) {
            this.title = t;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(CaissePanel.LIGHT_BLUE);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = (int) g.getFontMetrics().getStringBounds(title, g).getWidth();
        g.setColor(new Color(250, 250, 250));
        g.drawString(title, (this.getWidth() - w) / 2, 30);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(320, 44);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, 44);
    }

}
