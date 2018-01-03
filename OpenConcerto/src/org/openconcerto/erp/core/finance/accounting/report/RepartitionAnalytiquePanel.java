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
 
 /*
 * Créé le 23 avr. 2012
 */
package org.openconcerto.erp.core.finance.accounting.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class RepartitionAnalytiquePanel extends JPanel {

    private final JDate dateDeb, dateEnd;

    public RepartitionAnalytiquePanel() {
        super(new GridBagLayout());

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));

        this.dateDeb = new JDate();
        this.dateEnd = new JDate();

        GridBagConstraints c = new DefaultGridBagConstraints();

        this.add(new JLabel("Période du", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.dateDeb, c);
        this.dateDeb.setValue((Date) rowExercice.getObject("DATE_DEB"));

        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("Au"), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.dateEnd, c);

        final JButton buttonValid = new JButton(new AbstractAction("Valider") {

            @Override
            public void actionPerformed(ActionEvent e) {

                new Thread() {
                    public void run() {
                        RepartitionAnalytiqueSheetXML sheet = new RepartitionAnalytiqueSheetXML(dateDeb.getDate(), dateEnd.getDate());
                        try {
                            sheet.createDocument();
                            sheet.showPrintAndExport(true, false, false);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    };
                }.start();

            }
        });
        c.gridx++;
        this.add(buttonValid, c);

        // Check validity
        buttonValid.setEnabled(false);
        final PropertyChangeListener listener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                buttonValid.setEnabled(dateDeb.getValue() != null && dateEnd.getValue() != null);

            }
        };
        dateEnd.addValueListener(listener);
        dateDeb.addValueListener(listener);
    }

}
