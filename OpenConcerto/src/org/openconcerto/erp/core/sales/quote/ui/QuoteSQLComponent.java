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
 
 package org.openconcerto.erp.core.sales.quote.ui;

import static org.openconcerto.utils.CollectionUtils.createSet;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.common.ui.AbstractArticleItemTable;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.TotalPanel;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.erp.core.sales.quote.report.DevisXmlSheet;
import org.openconcerto.erp.panel.PanelOOSQLComponent;
import org.openconcerto.map.ui.ITextComboVilleViewer;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.ui.RadioButtons;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.checks.ValidState;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.ibm.icu.math.BigDecimal;

public class QuoteSQLComponent extends GroupSQLComponent {

    public static final String ID = "sales.quote";
    protected DevisItemTable table = new DevisItemTable();

    private JUniqueTextField numeroUniqueDevis;
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final ITextArea infos = new ITextArea();
    private final RadioButtons radioEtat = new RadioButtons("NOM");
    private JTextField textPourcentRemise, textPoidsTotal;
    private DeviseField textRemiseHT;
    private DeviseField fieldHT;
    private PanelOOSQLComponent panelOO;

    // Site d'intervention
    final JTextField telSite = new JTextField(20);
    final ITextComboVilleViewer villeSite = new ITextComboVilleViewer();
    final JTextField faxSite = new JTextField(20);
    final JTextField telPSite = new JTextField(20);
    final JTextField mailSite = new JTextField(20);
    final JTextField contactSite = new JTextField(20);
    final JTextField desSite = new JTextField(20);
    final ITextArea adrSite = new ITextArea();

    // Donneur d'ordre
    final JTextField telDonneur = new JTextField(20);
    final JTextField sirenDonneur = new JTextField(20);
    final ITextComboVilleViewer villeDonneur = new ITextComboVilleViewer();
    final JTextField faxDonneur = new JTextField(20);
    final JTextField telPDonneur = new JTextField(20);
    final JTextField mailDonneur = new JTextField(20);
    final JTextField contactDonneur = new JTextField(20);
    final JTextField desDonneur = new JTextField(20);
    final ITextArea adrDonneur = new ITextArea();

    public QuoteSQLComponent(SQLElement element) {
        super(element);
    }

    @Override
    public Set<String> getPartialResetNames() {
        Set<String> s = new HashSet<String>();
        s.add("OBJET");
        s.add("NUMERO");
        return s;
    }

    // @Override
    // protected Set<String> createRequiredNames() {
    // final Set<String> s = new HashSet<String>();
    // s.add("NOM");
    // s.add("ID_ADRESSE");
    // s.add("ID_MODE_REGLEMENT");
    // return s;
    // }

    @Override
    public JComponent createEditor(String id) {
        if (id.equals("sales.quote.number")) {
            this.numeroUniqueDevis = new JUniqueTextField(15);
            return this.numeroUniqueDevis;
        } else if (id.equals("panel.oo")) {
            this.panelOO = new PanelOOSQLComponent(this);
            return this.panelOO;
        } else if (id.equals("DATE")) {
            return new JDate(true);
        } else if (id.equals("sales.quote.state")) {
            return new RadioButtons("NOM");
        } else if (id.equals("sales.quote.items.list")) {
            return this.table;
        } else if (id.equals("T_POIDS")) {
            return new JTextField(10);
        } else if (id.equals("sales.quote.total.amount")) {
            final AbstractArticleItemTable items = (AbstractArticleItemTable) getEditor("sales.quote.items.list");

            final DeviseField totalHT = (DeviseField) getEditor("T_HT");
            final DeviseField totalService = (DeviseField) getEditor("T_SERVICE");
            final DeviseField totalSupply = (getTable().contains("PREBILAN") ? (DeviseField) getEditor("PREBILAN") : (DeviseField) getEditor("T_HA"));
            final DeviseField totalDevise = (DeviseField) getEditor("T_DEVISE");
            final JTextField totalWeight = (JTextField) getEditor("T_POIDS");
            final DeviseField totalTTC = (DeviseField) getEditor("T_TTC");
            final DeviseField totalTVA = (DeviseField) getEditor("T_TVA");
            final DeviseField totalRemise = (DeviseField) getEditor("REMISE_HT");
            final DeviseField totalPORT = (DeviseField) getEditor("PORT_HT");
            final DeviseField totalEco = (DeviseField) getEditor("T_ECO_CONTRIBUTION");

            return new TotalPanel(items, totalEco, totalHT, totalTVA, totalTTC, totalRemise, totalPORT, totalService, totalSupply, totalDevise, totalWeight, null);
        } else if (id.startsWith("T_")) {
            return new DeviseField();
        } else if (id.equals("REMISE_HT") || id.equals("PORT_HT") || id.equals("PREBILAN")) {
            return new DeviseField();
        } else if (id.equals("sales.quote.info.general")) {
            return new ITextArea(4, 40);
        }
        return super.createEditor(id);
    }

    private enum Type_Diff {
        SITE("SITE"), DONNEUR_ORDRE("DONNEUR");
        private final String name;

        private Type_Diff(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    private ValidState validStateContact = ValidState.getTrueInstance();

    @Override
    public synchronized ValidState getValidState() {
        assert SwingUtilities.isEventDispatchThread();
        return super.getValidState().and(this.validStateContact);
    }

    private JPanel createPanelDiff(final Type_Diff type) {

        GridBagConstraints cTabSite = new DefaultGridBagConstraints();
        JPanel tabSite = new JPanel(new GridBagLayout());

        cTabSite.weightx = 1;
        cTabSite.fill = GridBagConstraints.HORIZONTAL;
        cTabSite.gridwidth = 2;

        final String name = type.getName();
        final JCheckBox boxSiteDiff = new JCheckBox(getLabelFor(name + "_DIFF"));

        tabSite.add(boxSiteDiff, cTabSite);
        this.addView(boxSiteDiff, name + "_DIFF");

        final String fieldSiren = "SIREN_" + name;
        if (getTable().contains(fieldSiren)) {
            final JLabel labelSrenSite = new JLabel(getLabelFor(fieldSiren));
            labelSrenSite.setHorizontalAlignment(SwingConstants.RIGHT);
            cTabSite.gridwidth = 1;
            cTabSite.gridx = 2;
            cTabSite.weightx = 0;
            tabSite.add(labelSrenSite, cTabSite);

            cTabSite.gridx++;
            cTabSite.weightx = 1;
            if (type == Type_Diff.SITE) {
                throw new IllegalArgumentException("Le siren n'est pas à renseigné pour le site");
            }
            final JTextField siren = this.sirenDonneur;
            tabSite.add(siren, cTabSite);
            this.addView(siren, fieldSiren);
            DefaultGridBagConstraints.lockMinimumSize(siren);
        }
        cTabSite.gridy++;
        cTabSite.gridx = 0;
        cTabSite.weightx = 0;
        cTabSite.fill = GridBagConstraints.HORIZONTAL;
        cTabSite.gridwidth = 1;
        final JLabel labelSiteDes = new JLabel(getLabelFor("DESIGNATION_" + name));

        labelSiteDes.setHorizontalAlignment(SwingConstants.RIGHT);

        tabSite.add(labelSiteDes, cTabSite);
        cTabSite.gridx++;
        cTabSite.weightx = 1;
        final JTextField designation = type == Type_Diff.SITE ? this.desSite : this.desDonneur;
        tabSite.add(designation, cTabSite);
        this.addView(designation, "DESIGNATION_" + name);
        DefaultGridBagConstraints.lockMinimumSize(designation);

        final JLabel labelTelSite = new JLabel(getLabelFor("TEL_" + name));
        labelTelSite.setHorizontalAlignment(SwingConstants.RIGHT);
        cTabSite.gridx++;
        cTabSite.weightx = 0;
        tabSite.add(labelTelSite, cTabSite);

        cTabSite.gridx++;
        cTabSite.weightx = 1;
        final JTextField tel = type == Type_Diff.SITE ? this.telSite : this.telDonneur;
        tabSite.add(tel, cTabSite);
        this.addView(tel, "TEL_" + name);
        DefaultGridBagConstraints.lockMinimumSize(tel);

        final JLabel labelSiteAdr = new JLabel(getLabelFor("ADRESSE_" + name));
        labelSiteAdr.setHorizontalAlignment(SwingConstants.RIGHT);
        cTabSite.gridy++;
        cTabSite.gridx = 0;
        cTabSite.weightx = 0;
        tabSite.add(labelSiteAdr, cTabSite);

        cTabSite.gridx++;
        cTabSite.weightx = 1;
        final ITextArea adresse = type == Type_Diff.SITE ? this.adrSite : this.adrDonneur;
        tabSite.add(adresse, cTabSite);
        this.addView(adresse, "ADRESSE_" + name);
        DefaultGridBagConstraints.lockMinimumSize(adresse);

        final JLabel labelTelPSite = new JLabel(getLabelFor("TEL_P_" + name));
        labelTelPSite.setHorizontalAlignment(SwingConstants.RIGHT);
        cTabSite.gridx++;
        cTabSite.weightx = 0;
        tabSite.add(labelTelPSite, cTabSite);

        cTabSite.gridx++;
        cTabSite.weightx = 1;
        final JTextField telP = type == Type_Diff.SITE ? this.telPSite : this.telPDonneur;
        tabSite.add(telP, cTabSite);
        this.addView(telP, "TEL_P_" + name);

        cTabSite.gridy++;
        cTabSite.gridx = 0;
        cTabSite.weightx = 0;
        final JLabel labelVilleAdr = new JLabel(getLabelFor("VILLE_" + name));
        labelVilleAdr.setHorizontalAlignment(SwingConstants.RIGHT);
        tabSite.add(labelVilleAdr, cTabSite);

        cTabSite.gridx++;
        cTabSite.weightx = 1;
        final ITextComboVilleViewer ville = type == Type_Diff.SITE ? this.villeSite : this.villeDonneur;
        tabSite.add(ville, cTabSite);
        this.addView(ville, "VILLE_" + name);
        DefaultGridBagConstraints.lockMinimumSize(ville);

        cTabSite.gridx++;
        cTabSite.weightx = 0;

        final JLabel labelFaxSite = new JLabel(getLabelFor("FAX_" + name));
        labelFaxSite.setHorizontalAlignment(SwingConstants.RIGHT);
        tabSite.add(labelFaxSite, cTabSite);

        cTabSite.gridx++;
        cTabSite.weightx = 1;
        final JTextField fax = type == Type_Diff.SITE ? this.faxSite : this.faxDonneur;
        tabSite.add(fax, cTabSite);
        this.addView(fax, "FAX_" + name);
        DefaultGridBagConstraints.lockMinimumSize(fax);

        cTabSite.gridy++;
        cTabSite.gridx = 0;
        cTabSite.weightx = 0;

        final JLabel labelContactSite = new JLabel(getLabelFor("CONTACT_" + name));
        labelContactSite.setHorizontalAlignment(SwingConstants.RIGHT);
        tabSite.add(labelContactSite, cTabSite);

        cTabSite.gridx++;
        cTabSite.weightx = 1;
        final JTextField contact = type == Type_Diff.SITE ? this.contactSite : this.contactDonneur;
        tabSite.add(contact, cTabSite);
        this.addView(contact, "CONTACT_" + name);

        cTabSite.gridx++;
        cTabSite.weightx = 0;

        final JLabel labelMailSite = new JLabel(getLabelFor("MAIL_" + name));
        labelMailSite.setHorizontalAlignment(SwingConstants.RIGHT);
        tabSite.add(labelMailSite, cTabSite);

        cTabSite.gridx++;
        cTabSite.weightx = 1;
        final JTextField mail = type == Type_Diff.SITE ? this.mailSite : this.mailDonneur;
        tabSite.add(mail, cTabSite);
        this.addView(mail, "MAIL_" + name);

        boxSiteDiff.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                final boolean selected = boxSiteDiff.isSelected();
                setSiteEnabled(selected, type);
                if (!selected) {
                    clearFieldDiff(type);
                }
            }
        });
        return tabSite;
    }

    private void clearFieldDiff(Type_Diff type) {
        if (type == Type_Diff.SITE) {
            this.desSite.setText("");
            this.adrSite.setText("");
            this.villeSite.setValue(null);
            this.telPSite.setText("");
            this.telSite.setText("");
            this.mailSite.setText("");
            this.contactSite.setText("");
            this.faxSite.setText("");
        } else {
            this.sirenDonneur.setText("");
            this.desDonneur.setText("");
            this.adrDonneur.setText("");
            this.villeDonneur.setValue(null);
            this.telPDonneur.setText("");
            this.telDonneur.setText("");
            this.mailDonneur.setText("");
            this.contactDonneur.setText("");
            this.faxDonneur.setText("");
        }
    }

    private void setSiteEnabled(boolean b, Type_Diff type) {
        if (type == Type_Diff.SITE) {
            this.desSite.setEditable(b);
            this.adrSite.setEditable(b);
            this.villeSite.setEnabled(b);
            this.telPSite.setEditable(b);
            this.telSite.setEditable(b);
            this.mailSite.setEditable(b);
            this.contactSite.setEditable(b);
            this.faxSite.setEditable(b);
        } else {
            b = false;

            this.sirenDonneur.setEditable(b);
            this.desDonneur.setEditable(b);
            this.adrDonneur.setEditable(b);
            this.villeDonneur.setEnabled(b);
            this.telPDonneur.setEditable(b);
            this.telDonneur.setEditable(b);
            this.mailDonneur.setEditable(b);
            this.contactDonneur.setEditable(b);
            this.faxDonneur.setEditable(b);
        }
    }

    @Override
    protected SQLRowValues createDefaults() {
        System.err.println("Create defaults");

        // setSiteEnabled(false, Type_Diff.DONNEUR_ORDRE);
        // setSiteEnabled(false, Type_Diff.SITE);

        // Numero incremental auto
        final SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(getElement().getClass()));

        // User
        final SQLElement eltComm = Configuration.getInstance().getDirectory().getElement("COMMERCIAL");
        final int idUser = UserManager.getInstance().getCurrentUser().getId();

        SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(eltComm.getTable()).getFirstRowContains(idUser, eltComm.getTable().getField("ID_USER_COMMON"));

        if (rowsComm != null) {
            rowVals.put("ID_COMMERCIAL", rowsComm.getID());
        }

        if (getTable().getUndefinedID() == SQLRow.NONEXISTANT_ID) {
            rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.EN_ATTENTE);
        } else {
            SQLRowValues foreign = UndefinedRowValuesCache.getInstance().getDefaultRowValues(getTable());
            if (foreign != null && !foreign.isUndefined()) {
                rowVals.put("ID_ETAT_DEVIS", foreign.getObject("ID_ETAT_DEVIS"));
            } else {
                rowVals.put("ID_ETAT_DEVIS", EtatDevisSQLElement.EN_ATTENTE);
            }

        }
        rowVals.put("DATE", new Date());
        rowVals.put("T_ECO_CONTRIBUTION", BigDecimal.ZERO);
        rowVals.put("T_HT", Long.valueOf(0));
        rowVals.put("T_TVA", Long.valueOf(0));
        rowVals.put("T_SERVICE", Long.valueOf(0));
        rowVals.put("T_TTC", Long.valueOf(0));

        if (getTable().getFieldsName().contains("DATE_VALIDITE")) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            rowVals.put("DATE_VALIDITE", cal.getTime());
        }
        return rowVals;
    }

    private void calculPourcentage() {
        final String remiseP = this.textPourcentRemise.getText().replace(',', '.');
        Long totalHT = this.fieldHT.getValue();
        Long remiseHT = this.textRemiseHT.getValue();

        totalHT = totalHT == null ? Long.valueOf(0) : totalHT;
        remiseHT = remiseHT == null ? Long.valueOf(0) : remiseHT;

        try {
            final int valueRemise = Integer.valueOf(remiseP);

            final long remise = valueRemise * (totalHT.longValue() + remiseHT.longValue()) / 100;
            if (remiseHT != remise) {
                this.textRemiseHT.setValue(remise);
            }

        } catch (final NumberFormatException e) {
            ExceptionHandler.handle("Erreur durant le calcul de la remise", e);
        }

    }

    @Override
    protected void addViews() {

        super.addViews();

        final DeviseField totalHT = (DeviseField) getEditor("T_HT");
        final DeviseField totalEco = (DeviseField) getEditor("T_ECO_CONTRIBUTION");
        final DeviseField totalService = (DeviseField) getEditor("T_SERVICE");
        final DeviseField totalSupply = (DeviseField) getEditor("T_HA");
        final DeviseField totalDevise = (DeviseField) getEditor("T_DEVISE");
        final JTextField totalWeight = (JTextField) getEditor("T_POIDS");
        final DeviseField totalTTC = (DeviseField) getEditor("T_TTC");
        final DeviseField totalTVA = (DeviseField) getEditor("T_TVA");
        final DeviseField totalRemise = (DeviseField) getEditor("REMISE_HT");
        final DeviseField totalPORT = (DeviseField) getEditor("PORT_HT");

        if (getTable().contains("PREBILAN")) {
            this.addView(getEditor("PREBILAN"), "PREBILAN");
        }

        this.addView(totalPORT, "PORT_HT");
        this.addView(totalEco, "T_ECO_CONTRIBUTION");
        this.addView(totalRemise, "REMISE_HT");
        this.addView(totalTVA, "T_TVA");
        this.addView(totalTTC, "T_TTC");
        this.addView(totalWeight, "T_POIDS");
        this.addView(totalDevise, "T_DEVISE");
        this.addView(totalSupply, "T_HA");
        this.addView(totalService, "T_SERVICE");
        this.addView(totalHT, "T_HT");
    }

    @Override
    public JComponent getLabel(String id) {
        if (id.equals("sales.quote.total.amount")) {
            return new JLabel();
        } else if (id.equals("panel.oo")) {
            return new JLabel();
        } else {
            return super.getLabel(id);
        }
    }

    @Override
    public int insert(final SQLRow order) {

        final int idDevis;
        // on verifie qu'un devis du meme numero n'a pas été inséré entre temps
        if (this.numeroUniqueDevis.checkValidation()) {

            idDevis = super.insert(order);
            this.table.updateField("ID_DEVIS", idDevis);
            // Création des articles
            this.table.createArticle(idDevis, getElement());

            // generation du document
            try {
                final DevisXmlSheet sheet = new DevisXmlSheet(getTable().getRow(idDevis));
                sheet.createDocumentAsynchronous();
                sheet.showPrintAndExportAsynchronous(QuoteSQLComponent.this.panelOO.isVisualisationSelected(), QuoteSQLComponent.this.panelOO.isImpressionSelected(), true);
            } catch (Exception e) {
                ExceptionHandler.handle("Impossible de créer le devis", e);
            }

            // incrémentation du numéro auto
            if (NumerotationAutoSQLElement.getNextNumero(getElement().getClass()).equalsIgnoreCase(this.numeroUniqueDevis.getText().trim())) {
                final SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                int val = this.tableNum.getRow(2).getInt(NumerotationAutoSQLElement.getLabelNumberFor(getElement().getClass()));
                val++;
                rowVals.put(NumerotationAutoSQLElement.getLabelNumberFor(getElement().getClass()), new Integer(val));
                try {
                    rowVals.update(2);
                } catch (final SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            idDevis = getSelectedID();
            ExceptionHandler.handle("Impossible d'ajouter, numéro de devis existant.");
            final Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                final EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        }

        return idDevis;
    }

    @Override
    public void select(final SQLRowAccessor r) {
        if (r == null || r.getIDNumber() == null)
            super.select(r);
        else {
            System.err.println(r);
            final SQLRowValues rVals = r.asRowValues().deepCopy();
            final SQLRowValues vals = new SQLRowValues(r.getTable());
            vals.load(rVals, createSet("ID_CLIENT"));
            vals.setID(rVals.getID());
            System.err.println("Select CLIENT");

            super.select(vals);
            rVals.remove("ID_CLIENT");
            super.select(rVals);
        }

        // super.select(r);
        if (r != null) {
            this.table.insertFrom("ID_DEVIS", r.getID());
            // this.radioEtat.setVisible(r.getID() > getTable().getUndefinedID());
            if (getTable().contains("SITE_DIFF"))
                setSiteEnabled(r.getBoolean("SITE_DIFF"), Type_Diff.SITE);

            if (getTable().contains("DONNEUR_DIFF"))
                setSiteEnabled(r.getBoolean("DONNEUR_DIFF"), Type_Diff.DONNEUR_ORDRE);
        }

    }

    @Override
    public void update() {

        if (!this.numeroUniqueDevis.checkValidation()) {
            ExceptionHandler.handle("Impossible de modifier, numéro de devis existant.");
            final Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                final EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
            return;
        }
        super.update();
        this.table.updateField("ID_DEVIS", getSelectedID());
        this.table.createArticle(getSelectedID(), getElement());

        // generation du document

        try {
            final DevisXmlSheet sheet = new DevisXmlSheet(getTable().getRow(getSelectedID()));
            sheet.createDocumentAsynchronous();
            sheet.showPrintAndExportAsynchronous(QuoteSQLComponent.this.panelOO.isVisualisationSelected(), QuoteSQLComponent.this.panelOO.isImpressionSelected(), true);
        } catch (Exception e) {
            ExceptionHandler.handle("Impossible de créer le devis", e);
        }

    }

    /**
     * Création d'un devis à partir d'un devis existant
     * 
     * @param idDevis
     * 
     */
    public void loadDevisExistant(final int idDevis) {

        final SQLElement devis = Configuration.getInstance().getDirectory().getElement("DEVIS");
        final SQLElement devisElt = Configuration.getInstance().getDirectory().getElement("DEVIS_ELEMENT");

        // On duplique le devis
        if (idDevis > 1) {
            final SQLRow row = devis.getTable().getRow(idDevis);
            final SQLRowValues rowVals = new SQLRowValues(devis.getTable());
            rowVals.put("ID_CLIENT", row.getInt("ID_CLIENT"));
            rowVals.put("NUMERO", NumerotationAutoSQLElement.getNextNumero(getElement().getClass()));


            this.select(rowVals);
        }

        // On duplique les elements de devis
        final List<SQLRow> myListItem = devis.getTable().getRow(idDevis).getReferentRows(devisElt.getTable());

        if (myListItem.size() != 0) {
            this.table.getModel().clearRows();

            for (final SQLRow rowElt : myListItem) {

                final SQLRowValues rowVals = rowElt.createUpdateRow();
                rowVals.clearPrimaryKeys();
                this.table.getModel().addRow(rowVals);
                final int rowIndex = this.table.getModel().getRowCount() - 1;
                this.table.getModel().fireTableModelModified(rowIndex);
            }
        } else {
            this.table.getModel().clearRows();
        }
        this.table.getModel().fireTableDataChanged();
        this.table.repaint();
    }

}
