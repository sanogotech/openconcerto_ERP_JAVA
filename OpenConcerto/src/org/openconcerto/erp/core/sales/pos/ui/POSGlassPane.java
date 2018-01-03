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
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class POSGlassPane extends JPanel implements MouseListener, MouseMotionListener, FocusListener {

    JMenuBar menuBar;

    // Container contentPane;

    boolean inDrag = false;

    // trigger for redispatching (allows external control)
    boolean needToRedispatch = false;

    POSGlassPane(JPanel p, int x, int y) {
        this.setLayout(null);
        p.setSize(p.getPreferredSize());
        p.setLocation(x, y);
        this.add(p);
        addMouseListener(this);
        addMouseMotionListener(this);
        addFocusListener(this);
    }

    public void setVisible(boolean v) {
        // Make sure we grab the focus so that key events don't go astray.
        if (v)
            requestFocus();
        super.setVisible(v);
    }

    // Once we have focus, keep it if we're visible
    public void focusLost(FocusEvent fe) {
        if (isVisible())
            requestFocus();
    }

    public void focusGained(FocusEvent fe) {
    }

    // We only need to redispatch if we're not visible, but having full control
    // over this might prove handy.
    public void setNeedToRedispatch(boolean need) {
        needToRedispatch = need;
    }

    /*
     * (Based on code from the Java Tutorial) We must forward at least the mouse drags that started
     * with mouse presses over the check box. Otherwise, when the user presses the check box then
     * drags off, the check box isn't disarmed -- it keeps its dark gray background or whatever its
     * L&F uses to indicate that the button is currently being pressed.
     */
    public void mouseDragged(MouseEvent e) {
        if (needToRedispatch)
            redispatchMouseEvent(e);
    }

    public void mouseMoved(MouseEvent e) {
        if (needToRedispatch)
            redispatchMouseEvent(e);
    }

    public void mouseClicked(MouseEvent e) {
        if (needToRedispatch)
            redispatchMouseEvent(e);
    }

    public void mouseEntered(MouseEvent e) {
        if (needToRedispatch)
            redispatchMouseEvent(e);
    }

    public void mouseExited(MouseEvent e) {
        if (needToRedispatch)
            redispatchMouseEvent(e);
    }

    public void mousePressed(MouseEvent e) {
        if (needToRedispatch)
            redispatchMouseEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
        if (needToRedispatch) {
            redispatchMouseEvent(e);
            inDrag = false;
        }
    }

    private void redispatchMouseEvent(MouseEvent e) {
        boolean inButton = false;
        boolean inMenuBar = false;
        Point glassPanePoint = e.getPoint();
        Component component = null;
        Container container = null;
        Point containerPoint = SwingUtilities.convertPoint(this, glassPanePoint, null);
        int eventID = e.getID();

        if (containerPoint.y < 0) {
            inMenuBar = true;
            container = menuBar;
            containerPoint = SwingUtilities.convertPoint(this, glassPanePoint, menuBar);
            testForDrag(eventID);
        }

        // XXX: If the event is from a component in a popped-up menu,
        // XXX: then the container should probably be the menu's
        // XXX: JPopupMenu, and containerPoint should be adjusted
        // XXX: accordingly.
        component = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);

        if (component == null) {
            return;
        } else {
            inButton = true;
            testForDrag(eventID);
        }

        if (inMenuBar || inButton || inDrag) {
            Point componentPoint = SwingUtilities.convertPoint(this, glassPanePoint, component);
            component.dispatchEvent(new MouseEvent(component, eventID, e.getWhen(), e.getModifiers(), componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
        }
    }

    private void testForDrag(int eventID) {
        if (eventID == MouseEvent.MOUSE_PRESSED) {
            inDrag = true;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // enables anti-aliasing
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // gets the current clipping area
        Rectangle clip = g.getClipBounds();

        // sets a 65% translucent composite
        AlphaComposite alpha = AlphaComposite.SrcOver.derive(0.65f);
        Composite composite = g2.getComposite();
        g2.setComposite(alpha);

        // fills the background
        g2.setColor(Color.BLACK);
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);

        g2.setComposite(composite);
    }
}
