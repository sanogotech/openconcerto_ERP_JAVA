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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class PrintPreviewFrame extends JFrame implements ActionListener, ItemListener {
    private JButton print = new JButton("Imprimer");

    private Pageable pg = null;
    private double scale = 1.0;

    private Page page[] = null;
    private JComboBox jcb = new JComboBox();
    private CardLayout cl = new CardLayout();
    private JPanel p = new JPanel(cl);
    private JButton back = new JButton("<<");
    private JButton forward = new JButton(">>");

    public PrintPreviewFrame(Pageable pg) {
        init(pg);
    }

    public void init(Pageable pg) {
        setTitle("Aperçu avant impression");
        this.pg = pg;
        createPreview();
    }

    public PrintPreviewFrame(final Printable pr, final PageFormat p) {

        final Pageable pageable = new Pageable() {
            public int getNumberOfPages() {
                Graphics g = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB).getGraphics();
                int n = 0;
                try {
                    while (pr.print(g, p, n) == Printable.PAGE_EXISTS) {
                        n++;
                        // Stop infinite loop
                        if (n > 100000) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
                return n;
            }

            public PageFormat getPageFormat(int x) {
                return p;
            }

            public Printable getPrintable(int x) {
                return pr;
            }
        };
        init(pageable);
    }

    private void createPreview() {
        page = new Page[pg.getNumberOfPages()];

        final PageFormat pf = pg.getPageFormat(0);
        Dimension size = new Dimension((int) pf.getPaper().getWidth(), (int) pf.getPaper().getHeight());
        if (pf.getOrientation() != PageFormat.PORTRAIT)
            size = new Dimension(size.height, size.width);

        for (int i = 0; i < page.length; i++) {
            jcb.addItem("" + (i + 1));
            page[i] = new Page(i, size);
            p.add("" + (i + 1), new JScrollPane(page[i]));
        }
        setTopPanel();
        this.getContentPane().add(p, "Center");
        final Dimension d = this.getToolkit().getScreenSize();
        this.setSize(d.width, d.height - 60);

        this.setVisible(true);
        if (page.length > 0) {
            page[jcb.getSelectedIndex()].refreshScale();
        }
    }

    private void setTopPanel() {
        final FlowLayout fl = new FlowLayout();
        final GridBagLayout gbl = new GridBagLayout();
        final GridBagConstraints gbc = new GridBagConstraints();
        final JPanel topPanel = new JPanel(gbl), temp = new JPanel(fl);

        back.addActionListener(this);
        forward.addActionListener(this);
        back.setEnabled(false);
        forward.setEnabled(page.length > 1);
        gbc.gridx = 0;
        gbc.gridwidth = 1;

        temp.add(back);
        temp.add(jcb);
        temp.add(forward);

        temp.add(print);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbl.setConstraints(temp, gbc);
        topPanel.add(temp);
        print.addActionListener(this);

        jcb.addItemListener(this);
        print.setMnemonic('P');

        this.getContentPane().add(topPanel, "North");
    }

    public void itemStateChanged(ItemEvent ie) {
        cl.show(p, (String) jcb.getSelectedItem());
        page[jcb.getSelectedIndex()].refreshScale();
        back.setEnabled(jcb.getSelectedIndex() == 0 ? false : true);
        forward.setEnabled(jcb.getSelectedIndex() == jcb.getItemCount() - 1 ? false : true);
        this.validate();
    }

    public void actionPerformed(ActionEvent ae) {
        Object o = ae.getSource();
        if (o == print) {
            printAllPages();
        } else if (o == back) {
            jcb.setSelectedIndex(jcb.getSelectedIndex() == 0 ? 0 : jcb.getSelectedIndex() - 1);
            if (jcb.getSelectedIndex() == 0)
                back.setEnabled(false);
        } else if (o == forward) {
            jcb.setSelectedIndex(jcb.getSelectedIndex() == jcb.getItemCount() - 1 ? 0 : jcb.getSelectedIndex() + 1);
            if (jcb.getSelectedIndex() == jcb.getItemCount() - 1)
                forward.setEnabled(false);
        }
    }

    public void printAllPages() {
        try {
            final PrinterJob pj = PrinterJob.getPrinterJob();
            pj.defaultPage(pg.getPageFormat(0));
            pj.setPageable(pg);
            if (pj.printDialog())
                pj.print();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.toString(), "Error in Printing", 1);
        }
    }

    public void printCurrentPage(Printable p) {
        try {
            final PrinterJob pj = PrinterJob.getPrinterJob();
            pj.defaultPage(pg.getPageFormat(0));
            pj.setPrintable(p);
            HashPrintRequestAttributeSet pra = new HashPrintRequestAttributeSet();
            if (pj.printDialog(pra))
                pj.print(pra);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.toString(), "Error in Printing", 1);
        }
    }

    public Pageable getPageable() {
        return pg;
    }

    // public void stateChanged(ChangeEvent ce) {
    // double temp = (double) slider.getValue() / 100.0;
    // if (temp == scale) {
    // return;
    // }
    // if (temp == 0) {
    // temp = 0.01;
    // }
    // scale = temp;
    // page[jcb.getSelectedIndex()].refreshScale();
    // this.validate();
    // }

    class Page extends JPanel {

        private final Dimension size;
        private final BufferedImage bi;
        private BufferedImage image;

        public Page(int x, Dimension size) {
            this.size = size;
            this.bi = new BufferedImage(size.width, size.height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            int n = x;
            final PageFormat pf = pg.getPageFormat(n);
            final Graphics g = bi.getGraphics();
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            final Color c = g.getColor();
            g.setColor(Color.white);
            g.fillRect(0, 0, (int) pf.getWidth(), (int) pf.getHeight());
            g.setColor(c);
            try {

                g.setColor(Color.BLACK);
                g.clipRect(0, 0, (int) pf.getWidth(), (int) pf.getHeight());
                final Printable printable = pg.getPrintable(n);
                printable.print(g, pf, n);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            this.image = bi;
            this.setPreferredSize(new Dimension(size.width, size.height));
        }

        public void refreshScale() {
            BufferedImage scaledInstance = bi;
            if (scale != 1.0) {
                scaledInstance = (BufferedImage) bi.getScaledInstance((int) (size.width * scale), (int) (size.height * scale), Image.SCALE_FAST);

            }
            this.image = scaledInstance;
            // this.setIcon(new ImageIcon(scaledInstance));
            this.validate();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            System.out.println(g.getClipBounds());
            g.drawImage(this.image, 0, 0, null);

        }
    }

    class PseudoPrintable implements Printable {
        public int print(Graphics g, PageFormat fmt, int index) {
            if (index > 0) {
                return Printable.NO_SUCH_PAGE;
            }
            final int n = jcb.getSelectedIndex();
            try {
                return pg.getPrintable(n).print(g, fmt, n);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return Printable.PAGE_EXISTS;
        }
    }
}
