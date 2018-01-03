package org.openconcerto.modules.reports.olap;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.olap4j.metadata.MetadataElement;

public class DimensionTreeCellRenderer extends DefaultTreeCellRenderer {
    DimensionTreeCellRenderer(boolean showDimensions) {
        if (showDimensions) {
            this.setLeafIcon(new ImageIcon(this.getClass().getResource("blue_point.png")));
        } else {
            this.setLeafIcon(new ImageIcon(this.getClass().getResource("red_point.png")));
        }

    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof MetadataElement) {
            value = ((MetadataElement) value).getCaption();
            if (value.equals("(All)")) {
                value = "Tous";
            }
        }
        return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    }
}
