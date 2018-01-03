package org.openconcerto.modules.ocr;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class InvoiceViewer extends JPanel {
    private JButton bNext;
    private JButton bPrevious;
    private JLabel label;
    private int currentInvoiceIndex = -1;
    private int currentPageIndex = -1;
    private List<InvoiceOCR> invoices = new ArrayList<InvoiceOCR>();
    private JComponent oldPanel;

    public InvoiceViewer() {
        this.setLayout(new BorderLayout());
        this.bNext = new JButton("->");
        this.bPrevious = new JButton("<-");
        this.label = new JLabel("Analyse en cours...");
        final JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(this.bPrevious);
        toolbar.add(this.bNext);
        toolbar.add(this.label);
        this.add(toolbar, BorderLayout.NORTH);
        this.oldPanel = new JScrollPane(new JPanel());
        this.add(this.oldPanel, BorderLayout.CENTER);
        this.bNext.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                next();
            }
        });
        this.bPrevious.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                previous();
            }
        });

    }

    private void clearPageImage(int invoiceIndex, int pageIndex) {
        this.invoices.get(invoiceIndex).getPage(pageIndex).clearImage();
    }

    protected void next() {
        if (this.invoices.isEmpty()) {
            return;
        }
        final InvoiceOCR invoice = this.invoices.get(this.currentInvoiceIndex);
        final int pageCount = invoice.getPageCount();
        final int lastInvoiceIndex = this.currentInvoiceIndex;
        final int lastPageIndex = this.currentPageIndex;
        if (this.currentPageIndex < pageCount - 1) {
            this.currentPageIndex++;
        } else {
            if (this.currentInvoiceIndex < this.invoices.size() - 1) {
                this.currentPageIndex = 0;
                this.currentInvoiceIndex++;
            }
        }

        try {
            select(this.invoices.get(this.currentInvoiceIndex), this.currentPageIndex);
        } catch (IOException e) {
            // nothing
        }

        clearPageImage(lastInvoiceIndex, lastPageIndex);
    }

    protected void previous() {
        if (this.invoices.isEmpty()) {
            return;
        }
        final int lastInvoiceIndex = this.currentInvoiceIndex;
        final int lastPageIndex = this.currentPageIndex;
        if (this.currentPageIndex > 0) {
            this.currentPageIndex--;
        } else {
            if (this.currentInvoiceIndex > 0) {

                this.currentInvoiceIndex--;
                this.currentPageIndex = this.invoices.get(this.currentInvoiceIndex).getPageCount() - 1;
            }
        }
        try {
            clearPageImage(lastInvoiceIndex, lastPageIndex);
            select(this.invoices.get(this.currentInvoiceIndex), this.currentPageIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void select(InvoiceOCR invoice, int page) throws IOException {
        for (int i = 0; i < this.invoices.size(); i++) {
            if (this.invoices.get(i) == invoice) {
                this.currentInvoiceIndex = i;
            }
        }
        this.currentPageIndex = page;
        this.label.setText("Facture " + (this.currentInvoiceIndex + 1) + " page " + (this.currentPageIndex + 1) + " / " + invoice.getPageCount());
        this.remove(this.oldPanel);
        final JScrollPane scroll = new JScrollPane(new InvoiceRendererComponent(invoice, invoice.getPage(this.currentPageIndex)));
        scroll.setOpaque(false);
        scroll.getHorizontalScrollBar().setUnitIncrement(30);
        scroll.getVerticalScrollBar().setUnitIncrement(30);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.oldPanel = scroll;
        this.add(this.oldPanel, BorderLayout.CENTER);
        this.invalidate();
        this.revalidate();
    }

    public void add(InvoiceOCR invoice) {
        this.invoices.add(invoice);
        if (this.invoices.size() == 1 && invoice.getPageCount() > 0) {
            try {
                select(invoice, 0);
            } catch (IOException e) {
                // nothing
            }
        }
    }

    @Override
    public void removeAll() {
        this.invoices.clear();
        this.remove(this.oldPanel);
        this.invalidate();
        this.revalidate();
    }
}
