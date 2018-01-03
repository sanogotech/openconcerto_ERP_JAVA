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
 
 package org.openconcerto.erp.core.customerrelationship.customer.element;

import org.openconcerto.erp.core.common.component.AdresseSQLComponent;
import org.openconcerto.erp.core.common.element.NumerotationAutoSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.ui.AdresseClientItemTable;
import org.openconcerto.erp.preferences.ModeReglementDefautPrefPanel;
import org.openconcerto.erp.utils.TM;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ElementSQLObject;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.sqlobject.JUniqueTextField;
import org.openconcerto.sql.sqlobject.SQLSearchableTextCombo;
import org.openconcerto.sql.ui.textmenu.TextFieldWithMenu;
import org.openconcerto.sql.ui.textmenu.TextFieldWithMenuItemsTableFetcher;
import org.openconcerto.sql.ui.textmenu.TextFieldWithWebBrowsing;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.lowagie.text.Font;

public class CustomerSQLComponent extends GroupSQLComponent {
    private ContactItemTable table;
    private AdresseClientItemTable adresseTable = new AdresseClientItemTable();
    private SQLTable contactTable = Configuration.getInstance().getDirectory().getElement("CONTACT").getTable();
    private final SQLTable tableNum = getTable().getBase().getTable("NUMEROTATION_AUTO");
    private final JUniqueTextField code = new JUniqueTextField(20) {
        @Override
        public String getAutoRefreshNumber() {
            if (getMode() == Mode.INSERTION) {
                return NumerotationAutoSQLElement.getNextNumero(getElement().getClass());
            } else {
                return null;
            }
        }
    };

    private SQLRowValues defaultContactRowVals = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(this.contactTable));
    private JCheckBox checkAdrLivraison, checkAdrFacturation;

    public CustomerSQLComponent(SQLElement element) {
        super(element);
        this.table = new ContactItemTable(this.defaultContactRowVals);
        this.table.setPreferredSize(new Dimension(this.table.getSize().width, 150));
    }

    @Override
    protected Set<String> createRequiredNames() {
        final Set<String> s = new HashSet<String>();
        s.add("NOM");
        s.add("ID_ADRESSE");
        s.add("ID_MODE_REGLEMENT");
        return s;
    }

    @Override
    protected SQLRowValues createDefaults() {

        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("CODE", NumerotationAutoSQLElement.getNextNumero(getElement().getClass()));
        // Mode de règlement par defaut
        try {
            SQLRow r = ModeReglementDefautPrefPanel.getDefaultRow(true);
            SQLElement eltModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
            if (r.getID() > 1) {
                SQLRowValues rowValsMR = eltModeReglement.createCopy(r, null);
                rowVals.put("ID_MODE_REGLEMENT", rowValsMR);
            }
        } catch (SQLException e) {
            System.err.println("Impossible de sélectionner le mode de règlement par défaut du client.");
            e.printStackTrace();
        }
        return rowVals;
    }

    @Override
    public JComponent createEditor(String id) {
        if (id.equals("CATEGORIES")) {
            TextFieldWithMenuItemsTableFetcher itemsFetcher = new TextFieldWithMenuItemsTableFetcher(getTable().getTable("CATEGORIE_CLIENT").getField("NOM"));
            TextFieldWithMenu t = new TextFieldWithMenu(itemsFetcher, false);
            t.addAction(new AbstractAction(TM.tr("add")) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    EditFrame frame = new EditFrame(getElement().getDirectory().getElement("CATEGORIE_CLIENT"), EditMode.CREATION);
                    FrameUtil.showPacked(frame);

                }
            });
            t.addAction(new AbstractAction("Modifier/Supprimer") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    final SQLElement element = getElement().getDirectory().getElement("CATEGORIE_CLIENT");

                    ListeAddPanel p = new ListeAddPanel(element);
                    // EditFrame frame = new EditFrame(element, EditMode.CREATION);
                    IListFrame frame = new IListFrame(p);
                    FrameUtil.showPacked(frame);

                }
            });
            return t;
        } else if (id.equals("GROUPE") || id.equals("FORME_JURIDIQUE") || id.equals("CENTRE_GESTION") || id.equals("METHODE_RELANCE")) {
            return new SQLSearchableTextCombo(ComboLockedMode.UNLOCKED, 1, 20, false);
        } else if (id.equals("SITE_INTERNET")) {
            return new TextFieldWithWebBrowsing();
        } else if (id.equals("DATE")) {
            return new JDate(true);
        } else if (id.equals("customerrelationship.customer.contacts")) {
            return this.table;
        } else if (id.equals("customerrelationship.customer.addresses")) {
            return createAdressesComponent();
        } else if (id.equals("NOM")) {
            return super.createEditor(id);
        }
        if (id.equals("CODE")) {

            return this.code;
        } else if (id.equals("INFOS")) {
            return new ITextArea(4, 40);
        } else if (id.equals("COMMENTAIRES")) {
            return new ITextArea(10, 40);
        } else if (id.equals("TEL")) {
            final JTextField textTel = new JTextField(25);
            textTel.getDocument().addDocumentListener(new DocumentListener() {

                public void changedUpdate(DocumentEvent e) {
                    defaultContactRowVals.put("TEL_DIRECT", textTel.getText());
                }

                public void insertUpdate(DocumentEvent e) {
                    defaultContactRowVals.put("TEL_DIRECT", textTel.getText());
                }

                public void removeUpdate(DocumentEvent e) {
                    defaultContactRowVals.put("TEL_DIRECT", textTel.getText());
                }

            });
            return textTel;
        } else if (id.equals("FAX")) {
            final JTextField textFax = new JTextField(25);
            textFax.getDocument().addDocumentListener(new DocumentListener() {

                public void changedUpdate(DocumentEvent e) {
                    defaultContactRowVals.put("FAX", textFax.getText());
                }

                public void insertUpdate(DocumentEvent e) {
                    defaultContactRowVals.put("FAX", textFax.getText());
                }

                public void removeUpdate(DocumentEvent e) {
                    defaultContactRowVals.put("FAX", textFax.getText());
                }

            });
            return textFax;
        }
        JComponent c = super.createEditor(id);
        return c;
    }

    @Override
    public JComponent getLabel(String id) {
        if (id.equals("ID_MODE_REGLEMENT") || id.equals("INFOS") || id.startsWith("ID_ADRESSE")) {
            JLabel l = (JLabel) super.getLabel(id);
            l.setFont(l.getFont().deriveFont(Font.BOLD));
            return l;
        }
        if (id.equals("customerrelationship.customer.contact")) {
            return new JLabelBold("Contacts");
        } else if (id.equals("customerrelationship.customer.payment")) {
            return new JLabelBold("Mode de règlement");
        } else if (id.equals("customerrelationship.customer.address")) {
            return new JLabelBold("Adresses du client");
        }
        JComponent c = super.getLabel(id);
        return c;
    }

    @Override
    public void update() {
        super.update();
        final int selectedID = getSelectedID();
        this.table.updateField("ID_CLIENT", selectedID);
        this.adresseTable.updateField("ID_CLIENT", selectedID);
    }

    @Override
    public void select(SQLRowAccessor r) {
        super.select(r);
        this.checkAdrLivraison.setSelected(r == null || !r.getFields().contains("ID_ADRESSE_L") || r.isForeignEmpty("ID_ADRESSE_L"));
        this.checkAdrFacturation.setSelected(r == null || !r.getFields().contains("ID_ADRESSE_F") || r.isForeignEmpty("ID_ADRESSE_F"));
        if (r != null) {
            this.table.insertFrom("ID_CLIENT", r.asRowValues());
            this.adresseTable.insertFrom("ID_CLIENT", r.asRowValues());
        }
    }

    @Override
    public int insert(SQLRow order) {
        int id;

        int attempt = 0;
        // on verifie qu'un client du meme numero n'a pas été inséré entre temps
        if (!this.code.checkValidation(false)) {
            while (attempt < JUniqueTextField.RETRY_COUNT) {
                String num = NumerotationAutoSQLElement.getNextNumero(getElement().getClass());
                this.code.setText(num);
                attempt++;
                if (this.code.checkValidation(false)) {
                    System.err.println("ATEMPT " + attempt + " SUCCESS WITH NUMERO " + num);
                    break;
                }
                try {
                    Thread.sleep(JUniqueTextField.SLEEP_WAIT_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        final String num = this.code.getText();
        if (attempt == JUniqueTextField.RETRY_COUNT) {
            id = getSelectedID();
            ExceptionHandler.handle("Impossible d'ajouter, numéro de client existant.");
            final Object root = SwingUtilities.getRoot(this);
            if (root instanceof EditFrame) {
                final EditFrame frame = (EditFrame) root;
                frame.getPanel().setAlwaysVisible(true);
            }
        } else {
            id = super.insert(order);
            this.table.updateField("ID_CLIENT", id);
            this.adresseTable.updateField("ID_CLIENT", id);
            if (NumerotationAutoSQLElement.getNextNumero(getElement().getClass()).equalsIgnoreCase(this.code.getText().trim())) {
                SQLRowValues rowVals = new SQLRowValues(this.tableNum);
                final SQLRow rowNumAuto = this.tableNum.getRow(2);
                if (rowNumAuto.getObject("CLIENT_START") != null) {
                    int val = rowNumAuto.getInt("CLIENT_START");
                    val++;
                    rowVals.put("CLIENT_START", new Integer(val));

                    try {
                        rowVals.update(2);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (attempt > 0) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(null, "Le numéro a été actualisé en " + num);
                    }
                });
            }
        }
        return id;
    }

    private JComponent createAdressesComponent() {
        final JTabbedPane tabbedAdresse = new JTabbedPane() {
            public void insertTab(String title, Icon icon, Component component, String tip, int index) {
                if (component instanceof JComponent) {
                    ((JComponent) component).setOpaque(false);
                }
                super.insertTab(title, icon, component, tip, index);
            }

        };

        // Adr principale

        final JPanel panelAdressePrincipale = new JPanel(new GridBagLayout());
        GridBagConstraints cPanel = new DefaultGridBagConstraints();
        cPanel.weightx = 1;
        cPanel.weighty = 1;
        cPanel.anchor = GridBagConstraints.NORTH;
        this.addView("ID_ADRESSE", REQ + ";" + DEC + ";" + SEP);

        final ElementSQLObject componentPrincipale = (ElementSQLObject) this.getView("ID_ADRESSE");
        ((AdresseSQLComponent) componentPrincipale.getSQLChild()).setDestinataireVisible(true);
        componentPrincipale.setOpaque(false);
        panelAdressePrincipale.add(componentPrincipale, cPanel);
        tabbedAdresse.add(getLabelFor("ID_ADRESSE"), panelAdressePrincipale);
        tabbedAdresse.setOpaque(false);

        // Adresse de facturation

        tabbedAdresse.add(getLabelFor("ID_ADRESSE_F"), createFacturationPanel());

        // Adresse de livraison

        tabbedAdresse.add(getLabelFor("ID_ADRESSE_L"), createLivraisonPanel());

        // Adresses supplémentaires
        String labelAdrSuppl = TM.tr("additional.address");
        tabbedAdresse.add(labelAdrSuppl, this.adresseTable);

        return tabbedAdresse;
    }

    private Component createLivraisonPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        final String field = "ID_ADRESSE_L";

        this.addView(field, DEC + ";" + SEP);
        final ElementSQLObject component = (ElementSQLObject) this.getView(field);
        final AdresseSQLComponent adresseSQLComponent = (AdresseSQLComponent) component.getSQLChild();
        adresseSQLComponent.setDestinataireVisible(true);
        System.err.println("CustomerSQLComponent.createLivraisonPanel()" + component + " " + System.identityHashCode(component));
        System.err.println("CustomerSQLComponent.createLivraisonPanel()" + adresseSQLComponent + " " + System.identityHashCode(adresseSQLComponent));
        component.setOpaque(false);
        c.weightx = 1;
        this.checkAdrLivraison = new JCheckBox(TM.tr("delivery.address.same.main.address"));
        this.checkAdrLivraison.setOpaque(false);
        panel.add(component, c);

        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTH;
        panel.add(this.checkAdrLivraison, c);
        // Listener
        this.checkAdrLivraison.addActionListener(new ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                boolean b = checkAdrLivraison.isSelected();
                System.err.println("CustomerSQLComponent.createAdressesComponent().new ActionListener() {...}.actionPerformed() checkAdrLivraison " + b);
                component.setEditable((!b) ? InteractionMode.READ_WRITE : InteractionMode.DISABLED);
                component.setCreated(!b);
            };
        });
        panel.setOpaque(false);
        return panel;
    }

    private Component createFacturationPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        final String field = "ID_ADRESSE_F";

        this.addView(field, DEC + ";" + SEP);
        final ElementSQLObject component = (ElementSQLObject) this.getView(field);
        final AdresseSQLComponent adresseSQLComponent = (AdresseSQLComponent) component.getSQLChild();
        System.err.println("CustomerSQLComponent.createFacturationPanel()" + component + " " + System.identityHashCode(component));
        System.err.println("CustomerSQLComponent.createFacturationPanel()" + adresseSQLComponent + " " + System.identityHashCode(adresseSQLComponent));
        adresseSQLComponent.setDestinataireVisible(true);
        component.setOpaque(false);
        c.weightx = 1;
        this.checkAdrFacturation = new JCheckBox(TM.tr("invoice.address.same.main.address"));
        this.checkAdrFacturation.setOpaque(false);
        c.anchor = GridBagConstraints.NORTH;
        panel.add(component, c);

        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTH;
        panel.add(this.checkAdrFacturation, c);
        // Listener
        this.checkAdrFacturation.addActionListener(new ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                boolean b = checkAdrFacturation.isSelected();
                System.err.println("CustomerSQLComponent.createAdressesComponent().new ActionListener() {...}.actionPerformed() checkAdrFacturation " + b);
                component.setEditable((!b) ? InteractionMode.READ_WRITE : InteractionMode.DISABLED);
                component.setCreated(!b);
            };
        });
        panel.setOpaque(false);
        return panel;
    }

}
