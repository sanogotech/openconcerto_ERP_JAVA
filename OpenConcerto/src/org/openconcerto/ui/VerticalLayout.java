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
 
 package org.openconcerto.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

public class VerticalLayout implements LayoutManager {

    private int gap;

    public VerticalLayout() {
        this(0);
    }

    public VerticalLayout(int gap) {
        this.gap = gap;
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    public void layoutContainer(Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension size = parent.getSize();
        int width = size.width - insets.left - insets.right;
        int height = insets.top;
        for (int i = 0, c = parent.getComponentCount(); i < c; i++) {
            final Component m = parent.getComponent(i);
            if (m.isVisible()) {
                m.setBounds(insets.left, height, width, m.getPreferredSize().height);
                height += m.getSize().height + gap;
            }
        }
    }

    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    public Dimension preferredLayoutSize(Container parent) {
        final Insets insets = parent.getInsets();
        final Dimension pref = new Dimension(0, 0);
        for (int i = 0, c = parent.getComponentCount(); i < c; i++) {
            final Component m = parent.getComponent(i);
            if (m.isVisible()) {
                final Dimension componentPreferredSize = parent.getComponent(i).getPreferredSize();
                pref.height += componentPreferredSize.height + gap;
                pref.width = Math.max(pref.width, componentPreferredSize.width);
            }
        }
        pref.width += insets.left + insets.right;
        pref.height += insets.top + insets.bottom;
        return pref;
    }

    public void addLayoutComponent(String name, Component c) {
    }

    public void removeLayoutComponent(Component c) {
    }

}
