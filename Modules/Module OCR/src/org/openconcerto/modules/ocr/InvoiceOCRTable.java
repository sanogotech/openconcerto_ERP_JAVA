package org.openconcerto.modules.ocr;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.openconcerto.erp.core.common.ui.DeviseTableCellRenderer;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.ReloadPanel;
import org.openconcerto.ui.table.TimestampTableCellEditor;

public class InvoiceOCRTable extends JPanel {
    List<InvoiceOCR> invoices = new ArrayList<InvoiceOCR>();
    final DefaultTableModel dm;
    final ReloadPanel comp = new ReloadPanel();

    public InvoiceOCRTable(final InvoiceViewer viewer) {
        this.dm = new DefaultTableModel() {
            @Override
            public int getRowCount() {
                return getInvoicesSize();
            }

            @Override
            public int getColumnCount() {
                return 7;
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                case 0:
                    return "Date";
                case 1:
                    return "Fournisseur";
                case 2:
                    return "Numéro de facture";
                case 3:
                    return "Montant HT";
                case 4:
                    return "Montant TVA";
                case 5:
                    return "Montant TTC";
                case 6:
                    return "Valider";
                }
                return "" + column;
            }

            @Override
            public Object getValueAt(int row, int column) {
                if (!SwingUtilities.isEventDispatchThread()) {
                    throw new IllegalStateException();
                }
                final InvoiceOCR i = getInvoice(row);
                if (column == 0) {
                    return i.getDate();
                } else if (column == 1) {
                    return i.getSupplierName();
                } else if (column == 2) {
                    return i.getInvoiceNumber();
                } else if (column == 3) {
                    return i.getAmount();
                } else if (column == 4) {
                    return i.getTax();
                } else if (column == 5) {
                    return i.getAmountWithTax();
                } else if (column == 6) {
                    return i.getValid();
                }
                return "?";
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (!SwingUtilities.isEventDispatchThread()) {
                    throw new IllegalStateException();
                }
                final InvoiceOCR i = getInvoice(row);
                if (column == 0) {
                    i.setDate((Date) aValue);
                } else if (column == 1) {
                    i.setSupplierName((String) aValue);
                } else if (column == 2) {
                    i.setInvoiceNumber((String) aValue);
                } else if (column == 3) {
                    i.setAmount((BigDecimal) aValue);
                } else if (column == 4) {
                    i.setTax((BigDecimal) aValue);
                } else if (column == 5) {
                    i.setAmountWithTax((BigDecimal) aValue);
                } else if (column == 6) {
                    boolean valid = ((Boolean) aValue).booleanValue();
                    if (valid) {
                        i.setTaxId();
                        
                        if (!i.checkNullValue()) {
                            JOptionPane.showMessageDialog(null, "Tous les champs ne sont pas renseignés", "alert", JOptionPane.ERROR_MESSAGE);
                        } else if (!i.checkAmounts()) {
                            JOptionPane.showMessageDialog(null, "Les montants saisis ne sont pas corrects", "alert", JOptionPane.ERROR_MESSAGE);
                        } else if (i.getTaxId() == -1) {
                            JOptionPane.showMessageDialog(null, "Le taux de TVA n'est pas pris en charge", "alert", JOptionPane.ERROR_MESSAGE);
                        } else {
                            i.setValid(true);
                        }
                    } else {
                        i.setValid(false);
                    }
                }
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Date.class;
                } else if (columnIndex == 1) {
                    return String.class;
                } else if (columnIndex == 2) {
                    return String.class;
                } else if (columnIndex == 3) {
                    return BigDecimal.class;
                } else if (columnIndex == 4) {
                    return BigDecimal.class;
                } else if (columnIndex == 5) {
                    return BigDecimal.class;
                } else if (columnIndex == 6) {
                    return Boolean.class;
                }

                return super.getColumnClass(columnIndex);
            }

        };

        final JTable t = new JTable(this.dm);
        t.setAutoCreateRowSorter(true);

        t.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value != null) {
                    final DateFormat f = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
                    value = f.format(value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        t.getColumnModel().getColumn(0).setCellEditor(new TimestampTableCellEditor(false));
        t.getColumnModel().getColumn(3).setCellRenderer(new DeviseTableCellRenderer());
        
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        this.add(new JLabelBold("Factures analysées"), c);
        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(new JScrollPane(t), c);
        c.gridy++;
        c.weighty = 0;
        this.comp.setMode(ReloadPanel.MODE_ROTATE);
        this.add(this.comp, c);

        t.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int viewRow = t.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = t.convertRowIndexToModel(viewRow);
                        if (modelRow >= 0) {
                            try {
                                viewer.select(getInvoice(modelRow), 0);
                            } catch (IOException e1) {
                                // nothing
                            }
                        }
                    }
                }
            }
        });
    }

    protected int getInvoicesSize() {
        return this.invoices.size();
    }

    public InvoiceOCR getInvoice(int index) {
        return this.invoices.get(index);
    }

    public void add(InvoiceOCR invoice) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }
        this.invoices.add(invoice);
        this.dm.fireTableDataChanged();
    }

    @Override
    public void removeAll() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException();
        }
        this.invoices.clear();
        this.dm.fireTableDataChanged();
    }

    public void setModelCompleted() {
        this.comp.setMode(ReloadPanel.MODE_EMPTY);
    }
}
