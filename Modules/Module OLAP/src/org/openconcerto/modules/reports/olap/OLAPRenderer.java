package org.openconcerto.modules.reports.olap;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.olap4j.CellSet;
import org.openconcerto.modules.reports.olap.renderer.CellSetRenderer;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.ReloadPanel;


public class OLAPRenderer extends JPanel {
    final CellSetRenderer cellSetRenderer;
    final ReloadPanel reload;

    public OLAPRenderer() {
        this.cellSetRenderer = new CellSetRenderer();

        this.reload = new ReloadPanel();
        this.reload.setOpaque(false);
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        this.add(reload, c);

        final JScrollPane comp = new JScrollPane(cellSetRenderer);
        comp.setViewportBorder(BorderFactory.createLineBorder(Color.WHITE, 5));

        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(comp, c);
        this.setPreferredSize(new Dimension(640, 480));
        this.setPreferredSize(new Dimension(512, 320));
    }

    public void setCellSet(CellSet set) {
        setWaitState(false);
        cellSetRenderer.setCellSet(set);
    }

    public void setWaitState(boolean b) {
        if (b) {
            reload.setMode(ReloadPanel.MODE_ROTATE);
        } else {
            reload.setMode(ReloadPanel.MODE_EMPTY);
        }

    }
}
