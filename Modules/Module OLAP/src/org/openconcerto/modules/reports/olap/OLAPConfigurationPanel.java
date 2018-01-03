package org.openconcerto.modules.reports.olap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.olap4j.OlapConnection;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.MetadataElement;
import org.openconcerto.ui.DefaultGridBagConstraints;

public class OLAPConfigurationPanel extends JPanel {
    private OLAPParametersPanel parameters;
    private ParameterHolder filters;
    private ParameterHolder rows;
    private ParameterHolder colums;
    private final JCheckBox checkHideEmpty;
    private OLAPMDXPanel mdx;

    OLAPConfigurationPanel(OLAPParametersPanel parameters, OlapConnection olapConnection, OLAPRenderer renderer, OLAPMDXPanel mdx) {
        this.setOpaque(false);
        this.parameters = parameters;

        this.mdx = mdx;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        // Colonnes
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;

        JLabel columnsLabel = new JLabel("Colonnes", SwingConstants.RIGHT);
        this.add(columnsLabel, c);
        colums = new ParameterHolder(true);
        c.gridwidth = 3;
        c.gridx++;
        c.weightx = 1;
        this.add(colums, c);

        // Lignes
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        JLabel rowsLabel = new JLabel("Lignes", SwingConstants.RIGHT);
        this.add(rowsLabel, c);
        rows = new ParameterHolder(true);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 3;
        this.add(rows, c);
        // Filtre
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
        JLabel filtersLabel = new JLabel("Filtre", SwingConstants.RIGHT);
        this.add(filtersLabel, c);

        filters = new ParameterHolder(false);
        filters.setLimit(1);
        c.gridwidth = 3;
        c.gridx++;
        c.weightx = 0.5;
        this.add(filters, c);
        filters.addListener(this);
        colums.addListener(this);
        rows.addListener(this);
        //
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        final JButton inverseButton = new JButton("Inverser lignes et colonnes");
        inverseButton.setOpaque(false);
        this.add(inverseButton, c);
        c.gridx++;

        final JButton resetButton = new JButton("Réinitialiser");
        resetButton.setOpaque(false);
        this.add(resetButton, c);
        c.gridx++;

        checkHideEmpty = new JCheckBox("masquer les lignes et colonnes vides");
        checkHideEmpty.setOpaque(false);
        checkHideEmpty.setSelected(true);
        this.add(checkHideEmpty, c);
        checkHideEmpty.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                configurationModified();

            }
        });

        inverseButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                List<MetadataElement> rElements = new ArrayList<MetadataElement>();
                rElements.addAll(rows.getElements());

                List<MetadataElement> cElements = new ArrayList<MetadataElement>();
                cElements.addAll(colums.getElements());

                rows.clearElements();
                rows.addAll(cElements);

                colums.clearElements();
                colums.addAll(rElements);
            }
        });
        resetButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                rows.clearElements();
                colums.clearElements();
                filters.clearElements();
                configurationModified();
            }
        });
    }

    public void add(MetadataElement element) {
        rows.add(element);
    }

    void configurationModified() {

        String query = "SELECT \n";
        try {

            Cube cube = parameters.getCube();

            List<MetadataElement> eColumns = this.colums.getElements();

            if (this.checkHideEmpty.isSelected()) {
                query += "NON EMPTY ";
            }
            query += "{Hierarchize({";
            for (int i = 0; i < eColumns.size(); i++) {
                MetadataElement metadataElement = eColumns.get(i);
                if (ParameterHolder.isDimension(metadataElement)) {
                    query += "{" + metadataElement.getUniqueName() + ".Members}";
                } else {
                    query += "{" + metadataElement.getUniqueName() + "}";
                }
                if (i < eColumns.size() - 1) {
                    query += ", ";
                }
            }
            query += "})} ON COLUMNS, \n";
            if (this.checkHideEmpty.isSelected()) {
                query += "NON EMPTY ";
            }
            query += "{Hierarchize({";

            List<MetadataElement> eRows = this.rows.getElements();
            for (int i = 0; i < eRows.size(); i++) {
                MetadataElement metadataElement = eRows.get(i);
                if (ParameterHolder.isDimension(metadataElement)) {
                    query += "{" + metadataElement.getUniqueName() + ".Members}";
                } else {
                    query += "{" + metadataElement.getUniqueName() + "}";
                }
                if (i < eRows.size() - 1) {
                    query += ", ";
                }
            }
            query += "})} ON ROWS ";

            query += "\nFROM " + cube.getUniqueName();
            if (filters.getElements().size() > 0) {
                query += " \nWHERE " + filters.getElements().get(0).getUniqueName();
            }
            System.out.println(query);

            // myQuery.getAxis(Axis.ROWS).addDimension(dimension)

            // myQuery.getAxis(Axis.FILTER).addDimension(timeDim);
            //
            // Member year1997 = salesCube.lookupMember(IdentifierNode.ofNames("Time",
            // "1997").getSegmentList());
            // timeDim.include(year1997);
            // productDim.include(Selection.Operator.CHILDREN, IdentifierNode.ofNames("Product",
            // "Drink", "Beverages").getSegmentList());
            //
            // productDim.exclude(IdentifierNode.ofNames("Product", "Drink", "Beverages",
            // "Carbonated Beverages").getSegmentList());

            // MdxParserFactory pFactory = olapConnection.getParserFactory();
            // MdxParser parser = pFactory.createMdxParser(olapConnection);
            // SelectNode parsedObject = parser.parseSelect(query);
            // MdxValidator validator = pFactory.createMdxValidator(olapConnection);
            // try {
            // validator.validateSelect(parsedObject);
            // } catch (OlapException e) {
            // System.out.println(e.getMessage());
            // }

            // myQuery.validate();
            // System.out.println(myQuery.getSelect().toString());

            // CellSet results = myQuery.execute();

            mdx.execute(query);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
