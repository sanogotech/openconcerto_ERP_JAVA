package org.openconcerto.modules.reports.olap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

import org.olap4j.OlapException;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.MetadataElement;

public class ParameterHolder extends JPanel {
    private int limit = 4;
    private List<MetadataElement> elements = new ArrayList<MetadataElement>();
    private OLAPConfigurationPanel configPanel;

    ParameterHolder(boolean acceptDimension) {
        int h = (int) new JLabel("A").getPreferredSize().getHeight() + 19;
        setMinimumSize(new Dimension(200, h));
        setPreferredSize(new Dimension(200, h));
        this.setLayout(new FlowLayout(FlowLayout.LEADING, 2, 1));
        this.setOpaque(true);
        this.setBackground(Color.WHITE);
        this.setBorder(BorderFactory.createEtchedBorder());
        this.setTransferHandler(new TransferHandler("oo") {
            @Override
            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                // System.out.println("ParameterHolder.canImport()" + comp + " class:" +
                // comp.getClass().getName());

                for (int i = 0; i < transferFlavors.length; i++) {
                    DataFlavor dataFlavor = transferFlavors[i];
                    if (dataFlavor.getRepresentationClass().equals(MetadataElement.class)) {
                        return true;
                    }
                }

                return super.canImport(comp, transferFlavors);
            }

            @Override
            public boolean importData(JComponent comp, Transferable t) {
                System.out.println("ParameterHolder.ParameterHolder().new TransferHandler() {...}.importData():" + t.getClass());

                // if (t instanceof MetadataTransferable) {
                MetadataElement tr;
                try {
                    tr = (MetadataElement) t.getTransferData(MetadataTransferable.getDataFlavor());
                    add(tr);
                    System.out.println("Import ok");
                    return true;
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // }
                System.out.println("..");
                // TODO Auto-generated method stub
                return super.importData(comp, t);
            }

        });
    }

    public void setLimit(int count) {
        this.limit = count;
    }

    public void add(MetadataElement element) {
        if (this.elements.contains(element)) {
            return;
        }

        if (this.elements.size() >= limit) {
            this.elements.remove(this.elements.size() - 1);
        }
        this.elements.add(element);
        this.removeAll();
        this.invalidate();
        for (MetadataElement e : elements) {
            this.add(new MetadataElementPanel(e, this));
        }
        this.revalidate();
        this.repaint();
        this.configPanel.configurationModified();
    }

    public void addListener(OLAPConfigurationPanel olapConfigurationPanel) {
        this.configPanel = olapConfigurationPanel;

    }

    public List<MetadataElement> getElements() {
        return elements;
    }

    public void remove(MetadataElement element) {
        this.elements.remove(element);
        this.removeAll();
        this.invalidate();
        for (MetadataElement e : elements) {
            this.add(new MetadataElementPanel(e, this));
        }
        this.revalidate();
        this.repaint();
        this.configPanel.configurationModified();

    }

    public static boolean isDimension(MetadataElement e) {
        if (e instanceof Measure) {
            return false;
        } else if (e instanceof Level) {
            return true;
        } else if (e instanceof org.olap4j.metadata.Dimension) {
            org.olap4j.metadata.Dimension d = (org.olap4j.metadata.Dimension) e;
            try {
                return (!d.getDimensionType().equals(org.olap4j.metadata.Dimension.Type.MEASURE));
            } catch (OlapException e1) {
                e1.printStackTrace();
            }
        }
        return false;
    }

    public void clearElements() {
        this.elements.clear();
        this.removeAll();
        this.revalidate();
        this.repaint();

    }

    public void addAll(List<MetadataElement> elements) {
        this.elements.addAll(elements);
        this.removeAll();
        this.invalidate();
        for (MetadataElement e : elements) {
            this.add(new MetadataElementPanel(e, this));
        }
        this.revalidate();
        this.repaint();
        this.configPanel.configurationModified();
    }
}
