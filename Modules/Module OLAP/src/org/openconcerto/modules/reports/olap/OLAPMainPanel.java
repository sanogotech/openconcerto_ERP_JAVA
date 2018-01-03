package org.openconcerto.modules.reports.olap;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.olap4j.OlapConnection;
import org.olap4j.metadata.Schema;
import org.openconcerto.ui.JLabelBold;

public class OLAPMainPanel extends JPanel {
    OLAPMainPanel(OlapConnection oConnection, Schema schema, final OLAPPanel olapPanel) {
        this.setLayout(new GridBagLayout());
        this.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 3, 2, 2);
        JLabel label = new JLabel("Sélectionnez au minimum une colonne et une ligne pour visualiser l'hypercube.");
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JLabelBold title = new JLabelBold("Analyse multidimensionnelle des données");
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {

                final JPopupMenu menu = new JPopupMenu();
                final JMenuItem menuItem = new JMenuItem("Recharger la configuration");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        olapPanel.reload();

                    }
                });
                menu.show(title, e.getX(), e.getY());

            }
        });
        this.add(title, c);
        c.gridy++;
        this.add(label, c);
        c.gridy++;
        c.insets = new Insets(2, 0, 0, 0);
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBorder(null);
        final OLAPParametersPanel parameters = new OLAPParametersPanel(schema);
        parameters.setOpaque(false);
        split.setLeftComponent(parameters);
        split.setOpaque(true);

        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BorderLayout());

        final OLAPRenderer renderer = new OLAPRenderer();

        final OLAPMDXPanel mdxPanel = new OLAPMDXPanel(parameters, oConnection, renderer);
        final OLAPConfigurationPanel confPanel = new OLAPConfigurationPanel(parameters, oConnection, renderer, mdxPanel);
        parameters.setConfigurationPanel(confPanel);
        JTabbedPane t = new JTabbedPane();
        t.add("Mode simplifié", confPanel);
        t.add("Requête MDX", mdxPanel);

        renderer.setOpaque(false);
        t.setOpaque(false);
        rightPanel.add(t, BorderLayout.NORTH);
        rightPanel.add(renderer, BorderLayout.CENTER);
        split.setRightComponent(rightPanel);
        c.weightx = 1;
        c.weighty = 1;
        split.setDividerLocation(0.33D);
        this.add(split, c);

    }

}
