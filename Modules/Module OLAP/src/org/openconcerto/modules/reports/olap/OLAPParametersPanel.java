package org.openconcerto.modules.reports.olap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;

import org.olap4j.OlapException;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.MetadataElement;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Schema;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;

public class OLAPParametersPanel extends JPanel {

    private final JTree treeDimension = new JTree();
    private final JTree treeMeasures = new JTree();
    private Cube cube;
    private OLAPConfigurationPanel configurationPanel;

    public OLAPParametersPanel(final Schema schema) {
        this.setLayout(new GridBagLayout());

        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        JLabel lCube = new JLabel("Cube");
        this.add(lCube, c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        final JComboBox cCubes = new JComboBox(getCubeLabels(schema));
        cCubes.setOpaque(false);
        this.add(cCubes, c);
        //
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.weighty = 0;
        this.add(new JLabelBold("Dimensions"), c);
        c.gridy++;
        c.weighty = 0.5;
        final JScrollPane scrollDimension = new JScrollPane(treeDimension);
        scrollDimension.setBorder(BorderFactory.createEtchedBorder());
        this.add(scrollDimension, c);

        c.gridy++;
        c.weighty = 0;
        this.add(new JLabelBold("Mesures"), c);
        c.weighty = 0.5;
        c.gridy++;

        final JScrollPane scrollMeasures = new JScrollPane(treeMeasures);
        scrollMeasures.setBorder(BorderFactory.createEtchedBorder());
        this.add(scrollMeasures, c);

        this.treeDimension.setRootVisible(false);
        this.treeDimension.setCellRenderer(new DimensionTreeCellRenderer(true));
        this.treeDimension.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.treeMeasures.setRootVisible(false);
        this.treeMeasures.setCellRenderer(new DimensionTreeCellRenderer(false));
        this.treeMeasures.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        // D&D
        this.treeDimension.setDragEnabled(true);
        this.treeMeasures.setDragEnabled(true);
        this.treeDimension.setTransferHandler(new MetadataTransfertHandler());
        this.treeMeasures.setTransferHandler(new MetadataTransfertHandler());
        //
        updateFromComboItem(schema, cCubes);
        cCubes.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                updateFromComboItem(schema, cCubes);

            }

        });

        this.treeDimension.addMouseListener(new MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent e) {
                int selRow = treeDimension.getRowForLocation(e.getX(), e.getY());

                if (selRow != -1 && e.getClickCount() > 1) {
                    MetadataElement element = (MetadataElement) treeDimension.getPathForLocation(e.getX(), e.getY()).getLastPathComponent();
                    configurationPanel.add(element);
                }
            };

        });

    }

    private void updateFromComboItem(final Schema schema, final JComboBox cCubes) {

        try {
            this.cube = schema.getCubes().get(cCubes.getSelectedItem().toString());// Model
            treeDimension.setModel(new DimensionTreeModel("Dimensions", cube, true));
            final int rowCount = treeDimension.getRowCount();
            // Need reverse order to avoid collision with expanded children
            for (int i = rowCount - 1; i >= 0; i--) {
                treeDimension.expandRow(i);
            }

            treeMeasures.setModel(new DimensionTreeModel("Mesures", cube, false));
            treeMeasures.expandRow(0);
        } catch (OlapException e) {
            e.printStackTrace();
        }
    }

    private String[] getCubeLabels(Schema schema) {
        try {
            final NamedList<Cube> cubes = schema.getCubes();
            final int size = cubes.size();
            final List<String> list = new ArrayList<String>(size);
            for (int i = 0; i < size; i++) {
                final String name = cubes.get(i).getName();
                list.add(name);
            }
            Collections.sort(list);
            return list.toArray(new String[size]);
        } catch (OlapException e) {
            return new String[0];
        }

    }

    public Cube getCube() {
        return cube;
    }

    public void setConfigurationPanel(OLAPConfigurationPanel confPanel) {
        this.configurationPanel = confPanel;

    }

}
