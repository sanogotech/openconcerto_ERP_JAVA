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
 
 package org.openconcerto.erp.core.finance.payment.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.erp.core.sales.invoice.report.VenteFactureXmlSheet;
import org.openconcerto.erp.generationDoc.gestcomm.FicheRelanceSheet;
import org.openconcerto.erp.generationDoc.gestcomm.RelanceSheet;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;

public class ListeDesRelancesAction extends CreateFrameAbstractAction implements MouseListener {

    private IListFrame frame;

    public ListeDesRelancesAction() {
        super();
        this.putValue(Action.NAME, "Liste des relances");
    }

    public JFrame createFrame() {
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("RELANCE");
        this.frame = new IListFrame(new ListeAddPanel(elt) {
            @Override
            protected void createUI() {
                super.createUI();

                this.btnMngr.setAdditional(this.buttonModifier, new ITransformer<JButton, String>() {

                    @Override
                    public String transformChecked(JButton input) {

                        SQLRowAccessor row = getListe().fetchSelectedRow();

                        if (row.getForeign("ID_TYPE_LETTRE_RELANCE") == null || row.isForeignEmpty("ID_TYPE_LETTRE_RELANCE")) {
                            return "Vous ne pouvez pas modifier une relance envoyée par mail!";
                        }
                        return null;
                    }
                });
            }
        });

        final SQLTableModelSourceOnline src = (SQLTableModelSourceOnline) this.frame.getPanel().getListe().getModel().getReq();

        this.frame.getPanel().getListe().setSQLEditable(true);

        for (SQLTableModelColumn column : src.getColumns()) {
            if (column.getClass().isAssignableFrom(SQLTableModelColumnPath.class)) {
                ((SQLTableModelColumnPath) column).setEditable(false);
            }
        }

        ((SQLTableModelColumnPath) src.getColumns(elt.getTable().getField("INFOS")).iterator().next()).setEditable(true);

        this.frame.getPanel().getListe().getJTable().addMouseListener(this);
        this.frame.getPanel().setAddVisible(false);

        // Date panel
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.gridy = 4;

        IListFilterDatePanel datePanel = new IListFilterDatePanel(frame.getPanel().getListe(), elt.getTable().getField("DATE"), IListFilterDatePanel.getDefaultMap());
        c.gridy++;
        c.anchor = GridBagConstraints.CENTER;
        datePanel.setFilterOnDefault();
        frame.getPanel().add(datePanel, c);

        return this.frame;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {

        int selectedId = this.frame.getPanel().getListe().getSelectedId();
        if (selectedId > 1 && e.getButton() == MouseEvent.BUTTON3) {

            // String locationRelance =
            // TemplateNXProps.getInstance().getStringProperty("LocationRelanceOO");
            final SQLRow rowRelance = this.frame.getPanel().getListe().fetchSelectedRow();

            boolean isNotMail = !(rowRelance.getForeign("ID_TYPE_LETTRE_RELANCE") == null || rowRelance.isForeignEmpty("ID_TYPE_LETTRE_RELANCE"));
            // final String fileName = "Relance_" + rowRelance.getString("NUMERO");
            // final File fileOutOO = new File(locationRelance, fileName + ".odt");
            JPopupMenu menu = new JPopupMenu();
            final RelanceSheet s = new RelanceSheet(rowRelance);

            // Voir le document
            AbstractAction actionOpen = new AbstractAction("Voir le document") {
                public void actionPerformed(ActionEvent e) {
                    s.generate(false, false, "");
                    s.showDocument();
                }
            };
            actionOpen.setEnabled(isNotMail);
            menu.add(actionOpen);

            // Impression
            AbstractAction actionPrint = new AbstractAction("Imprimer") {
                public void actionPerformed(ActionEvent e) {
                    s.fastPrintDocument();
                }
            };
            actionPrint.setEnabled(isNotMail);
            menu.add(actionPrint);

            // Impression
            AbstractAction actionPrintFact = new AbstractAction("Imprimer la facture") {
                public void actionPerformed(ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                printInvoice(rowRelance);
                            } catch (Exception e) {
                                ExceptionHandler.handle("Impression impossible", e);
                            }
                        }
                    });
                    t.start();

                }
            };
            actionPrintFact.setEnabled(isNotMail);
            menu.add(actionPrintFact);

            // Impression

            AbstractAction actionPrintBoth = new AbstractAction("Imprimer la facture et la relance") {
                public void actionPerformed(ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                s.fastPrintDocument();
                                printInvoice(rowRelance);
                            } catch (Exception e) {
                                ExceptionHandler.handle("Impression impossible", e);
                            }
                        }
                    });
                    t.start();
                }
            };
            actionPrintBoth.setEnabled(isNotMail);
            menu.add(actionPrintBoth);

            // Générer
            final AbstractAction actionGenerate = new AbstractAction("Générer") {
                public void actionPerformed(ActionEvent e) {

                    String printer = PrinterNXProps.getInstance().getStringProperty("RelancePrinter");
                    s.generate(false, true, printer, true);
                }
            };
            actionGenerate.setEnabled(isNotMail);
            menu.add(actionGenerate);

            // Créer la fiche de relance
            menu.add(new AbstractAction("Créer la fiche de relance") {
                public void actionPerformed(ActionEvent e) {
                    try {
                        FicheRelanceSheet sheet = new FicheRelanceSheet(rowRelance);
                        sheet.createDocumentAsynchronous();
                        sheet.showPrintAndExportAsynchronous(true, false, true);
                    } catch (Exception ex) {
                        ExceptionHandler.handle("Impression impossible", ex);
                    }
                }
            });
            menu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    private void printInvoice(final SQLRow rowRelance) throws Exception {
        final VenteFactureXmlSheet sheet = new VenteFactureXmlSheet(rowRelance.getForeignRow("ID_SAISIE_VENTE_FACTURE"));
        sheet.getOrCreateDocumentFile();
        sheet.showPrintAndExport(false, true, true);
    }
}
