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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class TransparentPanel extends JPanel {
    private BufferedImage screenShot;

    public TransparentPanel(Frame frame) {
        try {
            final Robot robot = new Robot();
            screenShot = robot.createScreenCapture(frame.getBounds());
            final Graphics2D graphics = screenShot.createGraphics();
            // sets a 65% translucent composite
            final AlphaComposite alpha = AlphaComposite.SrcOver.derive(0.65f);
            graphics.setComposite(alpha);
            // fills the background
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, screenShot.getWidth(), screenShot.getHeight());
            graphics.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (screenShot != null) {
            g.drawImage(screenShot, 0, 0, null);
        }
    }

}
