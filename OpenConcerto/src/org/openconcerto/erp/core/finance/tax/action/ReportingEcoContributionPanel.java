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
package org.openconcerto.erp.core.finance.tax.action;

import org.openconcerto.erp.generationDoc.gestcomm.ReportingEcoContributionSheetXML;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ReportingEcoContributionPanel extends JPanel {

    public ReportingEcoContributionPanel() {
        super(new GridBagLayout());

        JLabel labelCom = new JLabel("Période du ");

        GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(labelCom, c);
        c.gridx++;
        final JDate dateDeb = new JDate();
        this.add(dateDeb, c);
        c.gridx++;
        JLabel labelYear = new JLabel("au");
        final JDate dateFin = new JDate();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        dateDeb.setValue(cal.getTime());

        this.add(labelYear, c);
        c.gridx++;
        this.add(dateFin, c);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        dateFin.setValue(cal.getTime());

        final JButton buttonValid = new JButton(new AbstractAction("Valider") {

            @Override
            public void actionPerformed(ActionEvent e) {

                new Thread() {
                    public void run() {
                        ReportingEcoContributionSheetXML sheet = new ReportingEcoContributionSheetXML(dateDeb.getValue(), dateFin.getValue());
                        try {
                            sheet.createDocument();
                        } catch (InterruptedException exn) {
                            exn.printStackTrace();
                        } catch (ExecutionException exn) {
                            exn.printStackTrace();
                        }
                        sheet.showPrintAndExport(true, false, false);

                    };
                }.start();

            }

        });
        c.gridx++;
        // buttonValid.setEnabled(false);
        this.add(buttonValid, c);
        dateDeb.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                buttonValid.setEnabled(dateDeb.getValue() != null && dateFin.getValue() != null);
            }
        });
        dateFin.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                buttonValid.setEnabled(dateDeb.getValue() != null && dateFin.getValue() != null);
            }
        });
    }

}
