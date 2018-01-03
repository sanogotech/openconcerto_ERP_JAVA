package org.jopencalendar.ui;

import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrintQuality;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

public class PrintJComponentAction extends AbstractAction {
    private JComponent component;

    private final int pageFormat;
    private final String title;

    public PrintJComponentAction(JComponent view) {
        this(view, PageFormat.PORTRAIT, null);
    }

    public PrintJComponentAction(JComponent view, int pageFormat, String title) {
        this.component = view;
        putValue(Action.NAME, "Print");
        this.pageFormat = pageFormat;
        this.title = title;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PrinterJob pj = PrinterJob.getPrinterJob();

        if (pj.printDialog()) {
            try {

                if (this.pageFormat == PageFormat.LANDSCAPE) {

                    final PageFormat pageFormat = getMinimumMarginPageFormat(pj);
                    pageFormat.setOrientation(PageFormat.LANDSCAPE);

                    pj.setPrintable(new JComponentPrintable(component, title), pageFormat);
                    PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();
                    printAttributes.add(PrintQuality.HIGH);
                    printAttributes.add(OrientationRequested.LANDSCAPE);

                    pj.print(printAttributes);
                } else {
                    pj.setPrintable(new JComponentPrintable(component, title));
                    PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();
                    printAttributes.add(PrintQuality.HIGH);
                    pj.print(printAttributes);
                }
            } catch (PrinterException exc) {
                JOptionPane.showMessageDialog(component, exc.getMessage(), "Printing error", JOptionPane.ERROR_MESSAGE);
                System.err.println(exc);
            }
        }

    }

    private PageFormat getMinimumMarginPageFormat(PrinterJob printJob) {
        PageFormat pf0 = printJob.defaultPage();
        PageFormat pf1 = (PageFormat) pf0.clone();
        Paper p = pf0.getPaper();
        p.setImageableArea(0, 0, pf0.getWidth(), pf0.getHeight());
        pf1.setPaper(p);
        PageFormat pf2 = printJob.validatePage(pf1);
        return pf2;
    }
}
