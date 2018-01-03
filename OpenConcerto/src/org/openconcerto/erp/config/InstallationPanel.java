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
 
 package org.openconcerto.erp.config;

import org.openconcerto.erp.config.update.Updater_1_5;
import org.openconcerto.erp.core.sales.order.ui.EtatCommandeClient;
import org.openconcerto.erp.core.sales.quote.element.EtatDevisSQLElement;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModuleReference;
import org.openconcerto.sql.changer.convert.AddFK;
import org.openconcerto.sql.changer.convert.ChangeIDToInt;
import org.openconcerto.sql.changer.correct.CorrectOrder;
import org.openconcerto.sql.changer.correct.FixSerial;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.request.Inserter;
import org.openconcerto.sql.request.Inserter.Insertion;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;
import org.openconcerto.sql.utils.ChangeTable.DeferredClause;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLCreateMoveableTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class InstallationPanel extends JPanel {
    private static final boolean DEBUG_FK = false;

    static public void insertUndef(final SQLCreateTable ct) throws SQLException {
        // check that we can use insertReturnFirstField()
        if (ct.getPrimaryKey().size() != 1)
            throw new IllegalStateException("Not one and only one field in the PK : " + ct.getPrimaryKey());
        final Insertion<?> insertion = new Inserter(ct).insertReturnFirstField("(" + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") VALUES(" + ReOrder.MIN_ORDER + ")", false);
        assert insertion.getCount() == 1;
        if (insertion.getRows().size() != 1)
            throw new IllegalStateException("Missing ID " + insertion.getRows());
        SQLTable.setUndefID(ct.getRoot().getSchema(), ct.getName(), ((Number) insertion.getRows().get(0)).intValue());
    }

    static private SQLName getTableName(final SQLCreateTable ct) {
        return new SQLName(ct.getRoot().getName(), ct.getName());
    }

    JProgressBar bar = new JProgressBar();
    boolean error;

    public InstallationPanel(final ServerFinderPanel finderPanel) {
        super(new GridBagLayout());
        setOpaque(false);
        GridBagConstraints c = new DefaultGridBagConstraints();
        JButton user = new JButton("Créer l'utilisateur");
        user.setOpaque(false);
        // JButton bd = new JButton("Créer la base de données");
        final JButton up = new JButton("Mise à niveau de la base");
        up.setOpaque(false);
        up.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                finderPanel.saveConfigFile();
                bar.setIndeterminate(true);
                up.setEnabled(false);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        System.setProperty(SQLSchema.NOAUTO_CREATE_METADATA, "false");
                        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create(true);

                        try {
                            final SQLDataSource ds = conf.getSystemRoot().getDataSource();
                            System.err.println("SystemRoot:" + conf.getSystemRoot());
                            System.err.println("Root:" + conf.getRoot());

                            // FixUnbounded varchar
                            fixUnboundedVarchar(conf.getRoot());

                            // FIXME DROP CONSTRAINT UNIQUE ORDRE ON
                            // CONTACT_FOURNISSEUR

                            checkCompteDefault(conf.getRoot());

                            updateSocieteTable(conf.getRoot());
                            updateVille(conf.getRoot().getTable("ADRESSE"));

                            // Champ user 1.4.1
                            final SQLTable tableUser = conf.getRoot().getTable("USER_COMMON");
                            if (tableUser != null) {
                                final AlterTable alter = new AlterTable(tableUser);
                                boolean alterUser = false;
                                if (!tableUser.contains("DISABLED")) {
                                    alter.addBooleanColumn("DISABLED", Boolean.FALSE, false);
                                    alterUser = true;
                                }
                                if (!tableUser.contains("TEL")) {
                                    alter.addVarCharColumn("TEL", 128);
                                    alterUser = true;
                                }

                                if (alterUser) {
                                    final String req = alter.asString();
                                    conf.getRoot().getDBSystemRoot().getDataSource().execute(req);
                                    conf.getRoot().refetchTable(tableUser.getName());
                                    conf.getRoot().getSchema().updateVersion();
                                }
                            }

                            // Champ Paye
                            final SQLTable tableCaisse = conf.getRoot().getTable("CAISSE_COTISATION");
                            if (!tableCaisse.contains("NUMERO_COMPTE_PCE")) {
                                final AlterTable alter = new AlterTable(tableCaisse);
                                alter.addVarCharColumn("NUMERO_COMPTE_PCE", 128);
                                alter.addVarCharColumn("NUMERO_COMPTE_PCE_CHARGES", 128);
                                final String req = alter.asString();
                                conf.getRoot().getDBSystemRoot().getDataSource().execute(req);
                                conf.getRoot().refetchTable(tableCaisse.getName());
                                conf.getRoot().getSchema().updateVersion();
                            }

                            final SQLTable tableRC = conf.getRoot().getTable("RUBRIQUE_COTISATION");
                            if (!tableRC.contains("PART_CSG_SANS_ABATTEMENT")) {
                                final AlterTable alter = new AlterTable(tableRC);
                                alter.addBooleanColumn("PART_CSG_SANS_ABATTEMENT", Boolean.FALSE, false);
                                final String req = alter.asString();
                                conf.getRoot().getDBSystemRoot().getDataSource().execute(req);
                                conf.getRoot().refetchTable(tableRC.getName());
                                conf.getRoot().getSchema().updateVersion();
                            }

                            final SQLTable tableRnet = conf.getRoot().getTable("RUBRIQUE_NET");
                            if (!tableRnet.contains("NUMERO_COMPTE_PCE_CHARGES")) {
                                final AlterTable alter = new AlterTable(tableRnet);
                                alter.addVarCharColumn("NUMERO_COMPTE_PCE_CHARGES", 128);
                                final String req = alter.asString();
                                conf.getRoot().getDBSystemRoot().getDataSource().execute(req);
                                conf.getRoot().refetchTable(tableCaisse.getName());
                                conf.getRoot().getSchema().updateVersion();
                            }

                            if (!tableRC.contains("PART_PAT_IMPOSABLE")) {
                                final AlterTable alter = new AlterTable(tableRC);
                                alter.addBooleanColumn("PART_PAT_IMPOSABLE", Boolean.FALSE, false);
                                final String req = alter.asString();
                                conf.getRoot().getDBSystemRoot().getDataSource().execute(req);
                                conf.getRoot().refetchTable(tableRC.getName());
                                conf.getRoot().getSchema().updateVersion();
                            }

                            if (!tableRC.contains("REDUCTION_FILLON")) {
                                final AlterTable alter = new AlterTable(tableRC);
                                alter.addBooleanColumn("REDUCTION_FILLON", Boolean.FALSE, false);
                                final String req = alter.asString();
                                conf.getRoot().getDBSystemRoot().getDataSource().execute(req);
                                conf.getRoot().refetchTable(tableRC.getName());
                                conf.getRoot().getSchema().updateVersion();
                            }

                            // Vérification des droits existants
                            checkRights(conf.getRoot());

                            if (!conf.getRoot().contains("DEVISE")) {
                                System.out.println("InstallationPanel.InstallationPanel() ADD DEVISE");
                                try {
                                    SQLUtils.executeAtomic(ds, new SQLUtils.SQLFactory<Object>() {
                                        @Override
                                        public Object create() throws SQLException {
                                            final SQLCreateTable createDevise = new SQLCreateTable(conf.getRoot(), "DEVISE");
                                            createDevise.addVarCharColumn("CODE", 128);
                                            createDevise.addVarCharColumn("NOM", 128);
                                            createDevise.addVarCharColumn("LIBELLE", 128);
                                            createDevise.addVarCharColumn("LIBELLE_CENT", 128);
                                            createDevise.addDecimalColumn("TAUX", 16, 8, BigDecimal.ONE, false);
                                            createDevise.addDecimalColumn("TAUX_COMMERCIAL", 16, 8, BigDecimal.ONE, false);
                                            ds.execute(createDevise.asString());
                                            insertUndef(createDevise);
                                            conf.getRoot().getSchema().updateVersion();
                                            return null;
                                        }
                                    });
                                } catch (Exception ex) {
                                    throw new IllegalStateException("Erreur lors de la création de la table DEVISE", ex);
                                }
                            } else if (!conf.getRoot().getTable("DEVISE").contains("TAUX_COMMERCIAL")) {
                                final SQLTable tDevise = conf.getRoot().getTable("DEVISE");
                                final AlterTable alterDevise = new AlterTable(tDevise);
                                alterDevise.addDecimalColumn("TAUX_COMMERCIAL", 16, 8, BigDecimal.ONE, false);
                                ds.execute(alterDevise.asString());
                                tDevise.getSchema().updateVersion();
                            }

                            if (!conf.getRoot().contains("TYPE_MODELE")) {
                                System.out.println("InstallationPanel.InstallationPanel() ADD TYPE_MODELE");
                                try {
                                    SQLUtils.executeAtomic(ds, new SQLUtils.SQLFactory<Object>() {
                                        @Override
                                        public Object create() throws SQLException {
                                            final SQLCreateTable createTypeModele = new SQLCreateTable(conf.getRoot(), "TYPE_MODELE");
                                            createTypeModele.addVarCharColumn("NOM", 128);
                                            createTypeModele.addVarCharColumn("TABLE", 128);
                                            createTypeModele.addVarCharColumn("DEFAULT_MODELE", 128);
                                            ds.execute(createTypeModele.asString());

                                            insertUndef(createTypeModele);

                                            conf.getRoot().getSchema().updateVersion();

                                            conf.getRoot().refetch();

                                            return null;
                                        }
                                    });
                                    final String[] type = new String[] { "Avoir client", "AVOIR_CLIENT", "Avoir", "Bon de livraison", "BON_DE_LIVRAISON", "BonLivraison", "Commande Client",
                                            "COMMANDE_CLIENT", "CommandeClient", "Devis", "DEVIS", "Devis", "Facture", "SAISIE_VENTE_FACTURE", "VenteFacture" };
                                    // ('FR', 'Français', 1.000), ('EN',
                                    // 'Anglais', 2.000)
                                    final List<String> values = new ArrayList<String>();
                                    final SQLBase base = conf.getRoot().getBase();

                                    for (int i = 0; i < type.length; i += 3) {
                                        final int order = values.size() + 1;
                                        values.add("(" + base.quoteString(type[i]) + ", " + base.quoteString(type[i + 1]) + ", " + base.quoteString(type[i + 2]) + ", " + order + ")");
                                    }
                                    final String valuesStr = CollectionUtils.join(values, ", ");
                                    final String insertVals = "INSERT INTO " + conf.getRoot().getTable("TYPE_MODELE").getSQLName().quote() + "(" + SQLBase.quoteIdentifier("NOM") + ", "
                                            + SQLBase.quoteIdentifier("TABLE") + ", " + SQLBase.quoteIdentifier("DEFAULT_MODELE") + ", " + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") VALUES"
                                            + valuesStr;

                                    ds.execute(insertVals);
                                } catch (Exception ex) {
                                    throw new IllegalStateException("Erreur lors de la création de la table TYPE_MODELE", ex);
                                }
                            }

                            SQLTable.setUndefID(conf.getRoot().getSchema(), "DEVISE", 1);
                            SQLTable.setUndefID(conf.getRoot().getSchema(), "TYPE_MODELE", 1);

                            // DSN
                            final DSNInstallationUtils dsnUtils = new DSNInstallationUtils();
                            dsnUtils.updateDSNCommonTable(conf.getRoot());

                            // ECo contribution
                            final EcoContributionInstallationUtils ecoUtils = new EcoContributionInstallationUtils();
                            ecoUtils.updateEcoCommonTable(conf.getRoot());

                            // we need to upgrade all roots
                            // ///////////////////////////
                            conf.getSystemRoot().mapAllRoots();
                            conf.getSystemRoot().refetch();

                            final Set<String> childrenNames = conf.getSystemRoot().getChildrenNames();

                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    bar.setIndeterminate(false);
                                    bar.setMaximum(childrenNames.size() + 1);
                                }
                            });
                            int i = 1;
                            for (final String childName : childrenNames) {
                                System.out.println("InstallationPanel.InstallationPanel() UPDATE SCHEMA " + childName);
                                final int barValue = i;

                                SwingUtilities.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        bar.setValue(barValue);
                                    }
                                });
                                i++;
                                final DBRoot root = conf.getSystemRoot().getRoot(childName);
                                try {
                                    conf.getSystemRoot().getDataSource().execute("CREATE LANGUAGE plpgsql;");
                                } catch (Exception e) {
                                    System.err.println("Warning: cannot add language plpgsql" + e.getMessage());
                                }
                                final SQLTable tableUndef = root.getTable(SQLTable.undefTable);
                                if (tableUndef != null && tableUndef.getField("UNDEFINED_ID").isNullable() == Boolean.FALSE) {
                                    final AlterTable alterUndef = new AlterTable(tableUndef);
                                    alterUndef.alterColumn("TABLENAME", EnumSet.allOf(Properties.class), "varchar(250)", "''", false);
                                    alterUndef.alterColumn("UNDEFINED_ID", EnumSet.allOf(Properties.class), "int", null, true);
                                    try {
                                        ds.execute(alterUndef.asString());
                                        tableUndef.getSchema().updateVersion();
                                    } catch (SQLException ex) {
                                        throw new IllegalStateException("Erreur lors de la modification de UNDEFINED_ID", ex);
                                    }
                                }

                                if (DEBUG_FK) {
                                    findBadForeignKey(root);
                                }

                                if (childName.equalsIgnoreCase("Common")) {
                                    updateCommon(root);
                                } else if (childName.startsWith(conf.getAppName()) || childName.equalsIgnoreCase("Default")
                                ) {
                                    SQLUtils.executeAtomic(ds, new SQLUtils.SQLFactory<Object>() {
                                        @Override
                                        public Object create() throws SQLException {
                                            fixUnboundedVarchar(root);
                                            fixUnboundedNumeric(root);
                                            fixCompletion(root);
                                            try {
                                                updateSocieteSchema(root);
                                            } catch (Exception e) {
                                                throw new SQLException(e);
                                            }
                                            updateToV1Dot2(root);
                                            updateToV1Dot3(root);
                                            updateToV1Dot4(root);
                                            // DSN
                                            dsnUtils.updateDSN(root);
                                            // Eco contribution
                                            ecoUtils.updateEco(root);
                                            updateStyle(root);
                                            createBanque(root);
                                            createAssocAnalytique(root, conf);
                                            updateStock(root);
                                            updateVille(root.getTable("ADRESSE"));

                                            Updater_1_5.update(root);
                                            return null;
                                        }

                                    });
                                }

                            }
                            error = false;
                        } catch (Throwable e1) {
                            ExceptionHandler.handle("Echec de mise à jour", e1);
                            error = true;
                        }

                        conf.destroy();
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                up.setEnabled(true);
                                bar.setValue(bar.getMaximum());
                                if (!error) {
                                    JOptionPane.showMessageDialog(InstallationPanel.this, "Mise à niveau réussie");
                                }
                            }
                        });
                        System.setProperty(SQLSchema.NOAUTO_CREATE_METADATA, "true");
                    }
                }, "Database structure updater").start();

            }

        });
        if (finderPanel.getToken() == null) {
            c.weightx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            this.add(new JLabelBold("Création de l'utilisateur openconcerto dans la base"), c);
            c.gridy++;
            c.weightx = 1;
            this.add(new JLabel("Identifiant de connexion de votre base "), c);
            c.gridy++;
            c.gridwidth = 1;
            c.weightx = 0;
            this.add(new JLabel("Login"), c);
            c.gridx++;

            final JTextField login = new JTextField();
            c.weightx = 1;
            this.add(login, c);

            c.gridx++;
            c.weightx = 0;
            this.add(new JLabel("Mot de passe"), c);
            c.gridx++;
            final JTextField mdp = new JTextField();
            c.weightx = 1;
            this.add(mdp, c);

            c.gridx = 0;
            c.gridy++;
            c.weightx = 0;
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            this.add(user, c);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = 1;
            user.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    // TODO Auto-generated method stub
                    try {
                        if (finderPanel.getServerConfig().createUserIfNeeded(login.getText(), mdp.getText())) {
                            JOptionPane.showMessageDialog(InstallationPanel.this, "L'utilisateur openconcerto a été correctement ajouté.");
                        } else {
                            JOptionPane.showMessageDialog(InstallationPanel.this, "L'utilisateur openconcerto existe déjà dans la base.");
                        }
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(InstallationPanel.this, "Une erreur est survenue pendant la connexion au serveur, vérifiez vos paramètres de connexion.");
                    }
                }
            });

            // Injection SQL
            // c.gridy++;
            // c.weightx = 1;
            // c.gridwidth = GridBagConstraints.REMAINDER;
            // c.insets = new Insets(10, 3, 2, 2);
            // this.add(new TitledSeparator("Injecter la base", true), c);
            //
            // c.gridy++;
            // c.weightx = 0;
            // c.gridwidth = 1;
            // c.insets = DefaultGridBagConstraints.getDefaultInsets();
            // this.add(new JLabel("Fichier"), c);
            //
            // final JTextField chemin = new JTextField();
            // c.gridx++;
            // c.weightx = 1;
            // this.add(chemin, c);
            //
            // c.gridx++;
            // c.weightx = 0;
            // JButton browse = new JButton("...");
            // browse.addActionListener(new ActionListener() {
            //
            // @Override
            // public void actionPerformed(ActionEvent e) {
            // JFileChooser choose = new JFileChooser();
            // if (choose.showOpenDialog(InstallationPanel.this) ==
            // JFileChooser.APPROVE_OPTION) {
            // chemin.setText(choose.getSelectedFile().getAbsolutePath());
            // }
            // }
            // });
            // this.add(browse, c);
            //
            // c.gridy++;
            // c.gridx = 0;
            // JButton inject = new JButton("Injecter");
            // this.add(inject, c);
            // inject.addActionListener(new ActionListener() {
            //
            // @Override
            // public void actionPerformed(ActionEvent e) {
            // File f = new File(chemin.getText());
            // if (!f.exists()) {
            // JOptionPane.showMessageDialog(InstallationPanel.this,
            // "Impossible de trouver le fichier "
            // + chemin.getText());
            // return;
            // }
            // BufferedReader input = null;
            // try {
            //
            // input = new BufferedReader(new FileReader(f));
            // StringBuffer sql = new StringBuffer();
            // String s;
            // while ((s = input.readLine()) != null) {
            // sql.append(s + "\n");
            // }
            // input.close();
            //
            // try {
            // final SQLServer sqlServer =
            // finderPanel.getServerConfig().createSQLServer();
            // Number n = (Number)
            // sqlServer.getBase("postgres").getDataSource().executeScalar("select COUNT(*) from
            // pg_database WHERE datname='OpenConcerto'");
            // if (n.intValue() > 0) {
            // JOptionPane.showMessageDialog(InstallationPanel.this,
            // "La base OpenConcerto est déjà présente sur le serveur!");
            // return;
            // }
            // // System.err.println(sqlServer.getBase("OpenConcerto"));
            // sqlServer.getBase("postgres").getDataSource()
            // .execute("CREATE DATABASE \"OpenConcerto\" WITH TEMPLATE = template0 ENCODING =
            // 'UTF8' LC_COLLATE = 'fr_FR.UTF-8' LC_CTYPE = 'fr_FR.UTF-8';");
            //
            // sqlServer.getBase("postgres").getDataSource().execute("ALTER DATABASE
            // \"OpenConcerto\" OWNER TO openconcerto;");
            //
            // SQLUtils.executeScript(sql.toString(),
            // sqlServer.getSystemRoot("OpenConcerto"));
            // sqlServer.destroy();
            // JOptionPane.showMessageDialog(InstallationPanel.this,
            // "Création de la base OpenConerto terminée.");
            // System.err.println("Création de la base OpenConerto terminée.");
            //
            // } catch (SQLException e1) {
            // // TODO Auto-generated catch block
            //
            // e1.printStackTrace();
            // JOptionPane.showMessageDialog(InstallationPanel.this,
            // "Une erreur s'est produite pendant l'injection du script, vérifier la connexion au
            // serveur et le script.");
            // }
            //
            // } catch (FileNotFoundException ex) {
            // // TODO Auto-generated catch block
            // ex.printStackTrace();
            // } catch (IOException ex) {
            // // TODO Auto-generated catch block
            // ex.printStackTrace();
            // } finally {
            // if (input != null) {
            // try {
            // input.close();
            // } catch (IOException ex) {
            // // TODO Auto-generated catch block
            // ex.printStackTrace();
            // }
            // }
            // }
            //
            // }
            // });

            // c.gridy++;
            // this.add(bd, c);

            c.gridy++;
            c.weightx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets(10, 3, 2, 2);
            this.add(new JLabelBold("Paramètrages de la base de données"), c);
            c.gridy++;
            c.insets = DefaultGridBagConstraints.getDefaultInsets();
            this.add(new JLabel("Création des fonctions SQL nécessaires (plpgsql)."), c);
            c.gridy++;
            c.weightx = 0;
            c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;

            JButton buttonPL = new JButton("Lancer");
            buttonPL.setOpaque(false);
            buttonPL.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!finderPanel.getServerConfig().getType().equals(ServerFinderConfig.POSTGRESQL)) {

                    } else {
                        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create(true);
                        try {
                            final SQLDataSource ds = conf.getSystemRoot().getDataSource();
                            // ds.execute("CREATE FUNCTION plpgsql_call_handler() RETURNS
                            // language_handler AS '$libdir/plpgsql' LANGUAGE C;"
                            // + "\n"
                            // +
                            // "CREATE FUNCTION plpgsql_validator(oid) RETURNS void AS
                            // '$libdir/plpgsql' LANGUAGE C;"
                            // + "\n"
                            // +
                            // "CREATE TRUSTED PROCEDURAL LANGUAGE plpgsql HANDLER
                            // plpgsql_call_handler VALIDATOR plpgsql_validator;");
                            ds.execute("CREATE LANGUAGE plpgsql;");

                        } catch (Exception ex) {
                            System.err.println("Impossible d'ajouter le langage PLPGSQL. Peut etre est il déjà installé.");
                        }
                    }
                    JOptionPane.showMessageDialog(null, "Paramètrage terminé.");
                }
            });
            this.add(buttonPL, c);
        }
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(10, 3, 2, 2);
        this.add(new JLabelBold("Mise à niveau de la base OpenConcerto"), c);
        c.gridy++;
        c.insets = DefaultGridBagConstraints.getDefaultInsets();
        this.add(new JLabel("Cette opération est nécessaire à chaque mise à jour du logiciel."), c);
        c.gridy++;
        this.add(new JLabel("La mise à niveau peut prendre plusieurs minutes."), c);
        c.gridy++;
        this.add(this.bar, c);
        c.gridy++;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;

        this.add(up, c);

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        c.gridy++;
        final JPanel comp = new JPanel();
        comp.setOpaque(false);
        this.add(comp, c);
    }


    private void addArticleFournisseur(DBRoot root) {
        if (!root.contains("ARTICLE_FOURNISSEUR")) {

            SQLCreateTable createBaseFamille = new SQLCreateTable(root, "FAMILLE_ARTICLE_FOURNISSEUR");
            createBaseFamille.addVarCharColumn("CODE", 45);
            createBaseFamille.addVarCharColumn("NOM", 2048);
            createBaseFamille.addForeignColumn("ID_FAMILLE_ARTICLE_FOURNISSEUR_PERE", createBaseFamille);
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            try {
                ds.execute(createBaseFamille.asString());

                insertUndef(createBaseFamille);
                root.refetchTable("FAMILLE_ARTICLE_FOURNISSEUR");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table FAMILLE_ARTICLE_FOURNISSEUR", ex);
            }

            SQLCreateTable createBase = new SQLCreateTable(root, "ARTICLE_FOURNISSEUR");
            createBase.addVarCharColumn("CODE", 45);
            createBase.addVarCharColumn("CODE_BARRE", 45);
            createBase.addVarCharColumn("CODE_DOUANIER", 45);
            createBase.addVarCharColumn("NOM", 2048);
            createBase.addVarCharColumn("DESCRIPTIF", 2048);
            createBase.addVarCharColumn("INFOS", 2048);

            createBase.addColumn("PRIX_METRIQUE_HA_1", "numeric (16,6) DEFAULT 0");
            createBase.addColumn("PRIX_METRIQUE_HA_2", "numeric (16,8) DEFAULT 0");
            createBase.addColumn("PRIX_METRIQUE_HA_3", "numeric (16,8) DEFAULT 0");

            createBase.addColumn("PRIX_METRIQUE_VT_1", "numeric (16,8) DEFAULT 0");
            createBase.addColumn("PRIX_METRIQUE_VT_2", "numeric (16,8) DEFAULT 0");
            createBase.addColumn("PRIX_METRIQUE_VT_3", "numeric (16,8) DEFAULT 0");

            createBase.addForeignColumn("ID_METRIQUE_1", root.findTable("METRIQUE", true));
            createBase.addForeignColumn("ID_METRIQUE_2", root.findTable("METRIQUE", true));
            createBase.addForeignColumn("ID_METRIQUE_3", root.findTable("METRIQUE", true));

            createBase.addColumn("PA_DEVISE", "numeric (16,8) DEFAULT 0");
            createBase.addColumn("PV_U_DEVISE", "numeric (16,8) DEFAULT 0");
            createBase.addColumn("PA_HT", "numeric (16,8) DEFAULT 0");
            createBase.addColumn("PV_HT", "numeric (16,8) DEFAULT 0");
            createBase.addColumn("PV_TTC", "numeric (16,2) DEFAULT 0");

            createBase.addForeignColumn("ID_TAXE", root.findTable("TAXE", true));
            createBase.addForeignColumn("ID_FAMILLE_ARTICLE_FOURNISSEUR", root.findTable("FAMILLE_ARTICLE_FOURNISSEUR", true));
            createBase.addForeignColumn("ID_MODE_VENTE_ARTICLE", root.findTable("MODE_VENTE_ARTICLE", true));
            createBase.addForeignColumn("ID_FOURNISSEUR", root.findTable("FOURNISSEUR", true));
            createBase.addForeignColumn("ID_PAYS", root.findTable("PAYS", true));
            createBase.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            createBase.addForeignColumn("ID_DEVISE_HA", root.findTable("DEVISE", true));
            createBase.addForeignColumn("ID_UNITE_VENTE", root.findTable("UNITE_VENTE", true));
            createBase.addForeignColumn("ID_COMPTE_PCE", root.findTable("COMPTE_PCE", true));
            createBase.addForeignColumn("ID_COMPTE_PCE_ACHAT", root.findTable("COMPTE_PCE", true));
            createBase.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));

            createBase.addColumn("POIDS", "real DEFAULT 0");
            createBase.addColumn("VALEUR_METRIQUE_1", "real DEFAULT 0");
            createBase.addColumn("VALEUR_METRIQUE_2", "real DEFAULT 0");
            createBase.addColumn("VALEUR_METRIQUE_3", "real DEFAULT 0");
            createBase.addBooleanColumn("SERVICE", Boolean.FALSE, false);
            createBase.addBooleanColumn("OBSOLETE", Boolean.FALSE, false);
            createBase.addBooleanColumn("GESTION_STOCK", Boolean.FALSE, false);
            createBase.addIntegerColumn("QTE_ACHAT", 1);
            createBase.addIntegerColumn("QTE_MIN", 1);

            try {
                ds.execute(createBase.asString());

                insertUndef(createBase);
                root.refetchTable("ARTICLE_FOURNISSEUR");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table ARTICLE_FOURNISSEUR", ex);
            }
        }
    }

    private void addContact(DBRoot root) throws SQLException {

        List<String> tables = Arrays.asList("AVOIR_CLIENT", "DEVIS", "BON_DE_LIVRAISON", "COMMANDE_CLIENT", "SAISIE_VENTE_FACTURE");
        final SQLTable tableContact = root.findTable("CONTACT");
        final SQLTable tableDpt = root.findTable("CLIENT_DEPARTEMENT");
        final SQLDataSource dataSource = root.getDBSystemRoot().getDataSource();
        for (String tableName : tables) {
            boolean update = false;
            final SQLTable table = root.getTable(tableName);
            final AlterTable alterEcheance = new AlterTable(table);

            if (!table.contains("ID_CONTACT")) {
                alterEcheance.addForeignColumn("ID_CONTACT", tableContact);
                update = true;
            }

            if (!table.contains("ID_CLIENT_DEPARTEMENT")) {
                alterEcheance.addForeignColumn("ID_CLIENT_DEPARTEMENT", tableDpt);
                update = true;
            }
            if (update) {
                dataSource.execute(alterEcheance.asString());
                table.getSchema().updateVersion();
            }

        }
    }

    private void addForeignKeyFactureOnEcheance(DBRoot root) {

        final SQLTable tableEch = root.getTable("ECHEANCE_CLIENT");
        if (!tableEch.contains("ID_SAISIE_VENTE_FACTURE")) {
            final SQLTable tableFacture = root.getTable("SAISIE_VENTE_FACTURE");
            final SQLTable tableMvt = root.getTable("MOUVEMENT");
            String query = "no query";
            try {
                final SQLDataSource dataSource = root.getDBSystemRoot().getDataSource();
                final AlterTable alterEcheance = new AlterTable(tableEch);
                alterEcheance.addForeignColumn("ID_SAISIE_VENTE_FACTURE", tableFacture);
                dataSource.execute(alterEcheance.asString());
                tableEch.getSchema().updateVersion();
                tableEch.fetchFields();

                // select MOUVEMENT whose parent has a source FACTURE
                final SQLSelect selMvt = new SQLSelect();
                final AliasedTable refChild = new AliasedTable(tableMvt, "m1");
                final AliasedTable refParent = new AliasedTable(tableMvt, "m2");
                selMvt.addSelect(refParent.getField("IDSOURCE"));
                selMvt.addBackwardJoin("INNER", refChild.getField("ID_MOUVEMENT_PERE"), refParent.getAlias());
                selMvt.addSelect(refChild.getKey());
                selMvt.setWhere(new Where(refParent.getField("SOURCE"), "=", tableFacture.getName()));

                final UpdateBuilder build = new UpdateBuilder(tableEch);
                build.addVirtualJoin("( " + selMvt.asString() + " )", "mvt", false, tableMvt.getKey().getName(), "ID_MOUVEMENT");
                build.setFromVirtualJoinField("ID_SAISIE_VENTE_FACTURE", "mvt", "IDSOURCE");
                query = build.asString();
                dataSource.execute(query);
            } catch (SQLException ex) {
                Log.get().severe("Error on query :" + query);
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table ECHEANCE_CLIENT", ex);
            }
        }
    }

    private void addFieldForPartialInvoice(DBRoot root) throws SQLException {
        final SQLTable tableVF = root.getTable("SAISIE_VENTE_FACTURE");
        AlterTable alter = new AlterTable(tableVF);
        boolean doAlter = false;
        if (!tableVF.contains("POURCENT_FACTURABLE")) {
            alter.addColumn("POURCENT_FACTURABLE", "numeric (16,8)");
            doAlter = true;
        }
        if (!tableVF.contains("MONTANT_FACTURABLE")) {
            alter.addColumn("MONTANT_FACTURABLE", "numeric (16,8)");
            doAlter = true;
        }

        if (!tableVF.contains("SOLDE")) {
            alter.addColumn("SOLDE", "boolean DEFAULT false");
            doAlter = true;
        }

        if (!tableVF.contains("PARTIAL")) {
            alter.addColumn("PARTIAL", "boolean DEFAULT false");
            doAlter = true;
        }

        if (doAlter) {
            root.getDBSystemRoot().getDataSource().execute(alter.asString());
            root.refetchTable(tableVF.getName());
        }

        // ELT
        final SQLTable tableVFElt = root.getTable("SAISIE_VENTE_FACTURE_ELEMENT");
        AlterTable alterElt = new AlterTable(tableVFElt);
        boolean doAlterElt = false;
        if (!tableVFElt.contains("MONTANT_FACTURABLE")) {
            alterElt.addColumn("MONTANT_FACTURABLE", "numeric (16,8)");
            doAlterElt = true;
        }

        if (!tableVFElt.contains("POURCENT_FACTURABLE")) {
            alterElt.addColumn("POURCENT_FACTURABLE", "numeric (16,8)");
            doAlterElt = true;
        }

        if (doAlterElt) {
            root.getDBSystemRoot().getDataSource().execute(alterElt.asString());
            root.refetchTable(tableVFElt.getName());
        }

    }

    private void createAssocAnalytique(DBRoot root, ComptaPropsConfiguration conf) {

        if (!root.contains("ASSOCIATION_ANALYTIQUE")) {

            SQLCreateTable createAssoc = new SQLCreateTable(root, "ASSOCIATION_ANALYTIQUE");
            createAssoc.addForeignColumn("ID_ECRITURE", root.findTable("ECRITURE", true));
            createAssoc.addForeignColumn("ID_SAISIE_KM_ELEMENT", root.findTable("SAISIE_KM_ELEMENT", true));
            createAssoc.addForeignColumn("ID_POSTE_ANALYTIQUE", root.findTable("POSTE_ANALYTIQUE", true));
            createAssoc.addColumn("POURCENT", "numeric (16,8) DEFAULT 100");
            createAssoc.addColumn("MONTANT", "bigInt DEFAULT 0");
            createAssoc.addBooleanColumn("GESTION_AUTO", false, false);

            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            try {
                ds.execute(createAssoc.asString());

                insertUndef(createAssoc);
                root.refetchTable("ASSOCIATION_ANALYTIQUE");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table ASSOCIATION_ANALYTIQUE", ex);
            }

        }
        SQLTable tablePoste = root.getTable("POSTE_ANALYTIQUE");
        if (!tablePoste.contains("DEFAULT")) {
            AlterTable a = new AlterTable(tablePoste);
            a.addBooleanColumn("DEFAULT", Boolean.FALSE, false);
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();
            try {
                ds.execute(a.asString());
                root.refetchTable("POSTE_ANALYTIQUE");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création du DEFAULT sur la table POSTE_ANALYTIQUE", ex);
            }
        }
    }

    private void updateStock(DBRoot root) throws SQLException {

        final SQLTable tableStock = root.getTable("STOCK");
        final SQLDataSource ds = root.getDBSystemRoot().getDataSource();
        if (!tableStock.contains("QTE_RECEPT_ATTENTE")) {

            try {

                AlterTable alterElt = new AlterTable(root.getTable("STOCK"));
                alterElt.addColumn("QTE_RECEPT_ATTENTE", "real DEFAULT 0");
                alterElt.addColumn("QTE_LIV_ATTENTE", "real DEFAULT 0");
                ds.execute(alterElt.asString());

                AlterTable alterMvt = new AlterTable(root.getTable("MOUVEMENT_STOCK"));
                alterMvt.addBooleanColumn("REEL", Boolean.TRUE, false);

                ds.execute(alterMvt.asString());

                root.getSchema().updateVersion();

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la mise à jour des tables de stock", ex);
            }

        }
        // if (!root.contains("ARTICLE_ELEMENT")) {
        // final SQLCreateTable createTable = new SQLCreateTable(root, "ARTICLE_ELEMENT");
        // createTable.addForeignColumn("ARTICLE");
        // createTable.addForeignColumn("ID_ARTICLE_PARENT", root.getTable("ARTICLE"));
        // createTable.addIntegerColumn("QTE", 1);
        // createTable.addDecimalColumn("QTE_UNITAIRE", 16, 6, BigDecimal.valueOf(1), false);
        // createTable.addForeignColumn("UNITE_VENTE");
        // insertUndef(createTable);
        // ds.execute(createTable.asString());
        //
        // root.getSchema().updateVersion();
        // root.refetchTable("ARTICLE_ELEMENT");
        //
        // }
        //
        // if (!root.getTable("ARTICLE").contains("COMPOSED")) {
        // AlterTable alterMvt = new AlterTable(root.getTable("ARTICLE"));
        // alterMvt.addBooleanColumn("COMPOSED", Boolean.FALSE, false);
        //
        // ds.execute(alterMvt.asString());
        //
        // root.getSchema().updateVersion();
        // }

    }

    private void createBanque(DBRoot root) throws SQLException {

        // Création de la table
        if (!root.contains("BANQUE") && !root.contains("BANQUE_POLE_PRODUIT")) {

            SQLCreateTable createBanque = new SQLCreateTable(root, "BANQUE");
            createBanque.addForeignColumn("ID_JOURNAL", root.findTable("JOURNAL", true));
            createBanque.addVarCharColumn("INFOS", 2048);
            createBanque.addVarCharColumn("NUMERO_RUE", 45);
            createBanque.addVarCharColumn("RUE", 256);
            createBanque.addVarCharColumn("IBAN", 256);
            createBanque.addVarCharColumn("BIC", 256);
            createBanque.addVarCharColumn("VOIE", 256);
            createBanque.addVarCharColumn("VILLE", 256);
            createBanque.addVarCharColumn("NOM", 256);
            createBanque.addVarCharColumn("DOMICILIATION", 256);
            createBanque.addVarCharColumn("CODE", 256);
            createBanque.addBooleanColumn("AFFACTURAGE", Boolean.FALSE, false);
            createBanque.addForeignColumn("ID_COMPTE_PCE", root.findTable("COMPTE_PCE", true));

            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            try {
                ds.execute(createBanque.asString());

                insertUndef(createBanque);
                root.refetchTable("BANQUE");
                root.getSchema().updateVersion();

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table BANQUE", ex);
            }
        }

        // Création des foreignkeys
        {
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();
            List<String> tablesWithBanque = Arrays.asList("MODE_REGLEMENT", "CHEQUE_A_ENCAISSER", "CHEQUE_FOURNISSEUR");

            for (String string : tablesWithBanque) {

                final SQLTable table = root.getTable(string);
                if (!table.contains("ID_BANQUE") && !table.contains("ID_BANQUE_POLE_PRODUIT")) {
                    String tableName = (table.contains("ID_BANQUE_PRODUIT")) ? "BANQUE_POLE_PRODUIT" : "BANQUE";
                    AlterTable alterElt = new AlterTable(table);
                    alterElt.addForeignColumn("ID_" + tableName, root.getTable(tableName));
                    ds.execute(alterElt.asString());
                    root.refetchTable(string);
                    root.getSchema().updateVersion();
                }
            }
        }

    }

    private void createFactureFournisseur(DBRoot root) throws SQLException {
        boolean refetchRoot = false;
        if (!root.contains("FACTURE_FOURNISSEUR")) {

            SQLCreateTable createFactureF = new SQLCreateTable(root, "FACTURE_FOURNISSEUR");
            createFactureF.addVarCharColumn("NOM", 256);
            createFactureF.addVarCharColumn("NUMERO", 45);
            createFactureF.addVarCharColumn("INFOS", 2048);
            createFactureF.addColumn("DATE", "date");
            createFactureF.addForeignColumn("FOURNISSEUR");
            createFactureF.addForeignColumn("AVOIR_FOURNISSEUR");
            createFactureF.addForeignColumn("COMPTE_PCE");
            createFactureF.addForeignColumn("COMMERCIAL");
            createFactureF.addForeignColumn("MODE_REGLEMENT");
            createFactureF.addForeignColumn("MOUVEMENT");
            createFactureF.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            createFactureF.addColumn("T_HT", "bigint DEFAULT 0");
            createFactureF.addColumn("T_TVA", "bigint DEFAULT 0");
            createFactureF.addColumn("T_TTC", "bigint DEFAULT 0");
            createFactureF.addColumn("T_SERVICE", "bigint DEFAULT 0");
            createFactureF.addColumn("T_DEVISE", "bigint DEFAULT 0");
            createFactureF.addColumn("T_POIDS", "real DEFAULT 0");

            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            try {
                ds.execute(createFactureF.asString());
                insertUndef(createFactureF);
                root.getSchema().updateVersion();
                root.refetchTable("FACTURE_FOURNISSEUR");
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table FACTURE_FOURNISSEUR", ex);
            }

        }

        if (!root.contains("FACTURE_FOURNISSEUR_ELEMENT")) {

            SQLCreateTable createFactureF = new SQLCreateTable(root, "FACTURE_FOURNISSEUR_ELEMENT");

            createFactureF.addDecimalColumn("PRIX_METRIQUE_HA_1", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PRIX_METRIQUE_HA_2", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PRIX_METRIQUE_HA_3", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PRIX_METRIQUE_VT_1", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PRIX_METRIQUE_VT_2", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PRIX_METRIQUE_VT_3", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("T_PV_HT", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("T_PA_HT", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PV_HT", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PA_HT", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("T_PV_TTC", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("T_PA_TTC", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("QTE_UNITAIRE", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PA_DEVISE_T", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addDecimalColumn("PA_DEVISE", 16, 6, BigDecimal.ZERO, false);
            createFactureF.addIntegerColumn("QTE", 1);
            createFactureF.addIntegerColumn("QTE_ACHAT", 1);

            createFactureF.addColumn("VALEUR_METRIQUE_1", "real DEFAULT 0");
            createFactureF.addColumn("VALEUR_METRIQUE_2", "real DEFAULT 0");
            createFactureF.addColumn("VALEUR_METRIQUE_3", "real DEFAULT 0");
            createFactureF.addColumn("T_POIDS", "real DEFAULT 0");
            createFactureF.addColumn("POIDS", "real DEFAULT 0");
            createFactureF.addBooleanColumn("SERVICE", Boolean.FALSE, true);
            createFactureF.addVarCharColumn("CODE", 45);
            createFactureF.addVarCharColumn("NOM", 256);
            createFactureF.addColumn("DATE", "date");
            createFactureF.addForeignColumn("STYLE");
            createFactureF.addForeignColumn("METRIQUE", "3");
            createFactureF.addForeignColumn("METRIQUE", "2");
            createFactureF.addForeignColumn("METRIQUE", "1");
            createFactureF.addForeignColumn("FACTURE_FOURNISSEUR");
            createFactureF.addForeignColumn("TAXE");
            createFactureF.addForeignColumn("ID_MODE_VENTE_ARTICLE", root.findTable("MODE_VENTE_ARTICLE").getSQLName(), "ID", "5");
            createFactureF.addForeignColumn("UNITE_VENTE");
            createFactureF.addForeignColumn("ARTICLE");
            createFactureF.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            createFactureF.addForeignColumn("CODE_FOURNISSEUR");

            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            try {
                ds.execute(createFactureF.asString());
                insertUndef(createFactureF);
                root.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table FACTURE_FOURNISSEUR_ELEMENT", ex);
            }

        }

        if (root.contains("FACTURE_FOURNISSEUR_ELEMENT")) {
            addHAElementField(root.getTable("FACTURE_FOURNISSEUR_ELEMENT"), root);
        }

        if (!root.contains("REGLER_MONTANT_ELEMENT")) {

            SQLCreateTable createReglerElt = new SQLCreateTable(root, "REGLER_MONTANT_ELEMENT");

            createReglerElt.addForeignColumn("ECHEANCE_FOURNISSEUR");
            createReglerElt.addForeignColumn("REGLER_MONTANT");
            createReglerElt.addForeignColumn("MOUVEMENT", "ECHEANCE");
            createReglerElt.addColumn("DATE", "date");
            createReglerElt.addColumn("MONTANT_REGLE", "bigint DEFAULT 0");
            createReglerElt.addColumn("MONTANT_A_REGLER", "bigint DEFAULT 0");

            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            try {
                ds.execute(createReglerElt.asString());
                insertUndef(createReglerElt);
                root.getSchema().updateVersion();
                root.refetchTable("REGLER_MONTANT_ELEMENT");
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table REGLER_MONTANT_ELEMENT", ex);
            }

        }

        final SQLTable tableReglerMontant = root.getTable("REGLER_MONTANT");

        boolean updateRegler = false;

        AlterTable alterElt = new AlterTable(tableReglerMontant);
        if (!tableReglerMontant.contains("ID_FOURNISSEUR")) {
            alterElt.addForeignColumn("ID_FOURNISSEUR", root.getTable("FOURNISSEUR"));
            updateRegler = true;
        }

        if (!tableReglerMontant.contains("NOM")) {
            alterElt.addVarCharColumn("NOM", 256);
            updateRegler = true;
        }

        if (updateRegler) {
            root.getDBSystemRoot().getDataSource().execute(alterElt.asString());
            root.refetchTable(tableReglerMontant.getName());
        }

        if (refetchRoot) {
            root.refetch();
        }
    }

    private void fixUnboundedNumeric(DBRoot root) throws SQLException {

        final List<AlterTable> alters = new ArrayList<AlterTable>();
        final List<UpdateBuilder> builds = new ArrayList<UpdateBuilder>();
        {
            SQLTable tableAvoir = root.getTable("AVOIR_CLIENT_ELEMENT");
            final AlterTable alter = new AlterTable(tableAvoir);
            if (tableAvoir.contains("POURCENT_ACOMPTE")) {
                SQLField fieldAcompteAvoir = tableAvoir.getField("POURCENT_ACOMPTE");
                if (fieldAcompteAvoir.getType().getSize() > 500) {
                    final String fName = fieldAcompteAvoir.getName();
                    alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "100", false);

                    UpdateBuilder build = new UpdateBuilder(tableAvoir);
                    build.set(fieldAcompteAvoir.getName(), "100");
                    build.setWhere(new Where(fieldAcompteAvoir, "=", (Object) null));
                    builds.add(build);
                }
            }
            if (tableAvoir.contains("POURCENT_REMISE")) {
                SQLField fieldRemiseAvoir = tableAvoir.getField("POURCENT_REMISE");
                if (fieldRemiseAvoir.getType().getSize() > 500) {
                    final String fName = fieldRemiseAvoir.getName();
                    alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "0", false);

                    UpdateBuilder build = new UpdateBuilder(tableAvoir);
                    build.set(fieldRemiseAvoir.getName(), "0");
                    build.setWhere(new Where(fieldRemiseAvoir, "=", (Object) null));
                    builds.add(build);
                }
            }
            if (!alter.isEmpty())
                alters.add(alter);
        }

        {
            SQLTable tableFacture = root.getTable("SAISIE_VENTE_FACTURE_ELEMENT");
            final AlterTable alter = new AlterTable(tableFacture);
            if (tableFacture.contains("POURCENT_ACOMPTE")) {
                SQLField fieldAcompteFacture = tableFacture.getField("POURCENT_ACOMPTE");
                if (fieldAcompteFacture.getType().getSize() > 500) {
                    final String fName = fieldAcompteFacture.getName();
                    alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "100", false);

                    UpdateBuilder build = new UpdateBuilder(tableFacture);
                    build.set(fieldAcompteFacture.getName(), "100");
                    build.setWhere(new Where(fieldAcompteFacture, "=", (Object) null));
                    builds.add(build);
                }
            }

            if (tableFacture.contains("POURCENT_REMISE")) {
                SQLField fieldRemiseFacture = tableFacture.getField("POURCENT_REMISE");
                if (fieldRemiseFacture.getType().getSize() > 500) {
                    final String fName = fieldRemiseFacture.getName();
                    alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "0", false);

                    UpdateBuilder build = new UpdateBuilder(tableFacture);
                    build.set(fieldRemiseFacture.getName(), "0");
                    build.setWhere(new Where(fieldRemiseFacture, "=", (Object) null));
                    builds.add(build);
                }
            }
            if (tableFacture.getFieldsName().contains("REPARTITION_POURCENT")) {
                SQLField fieldRepFacture = tableFacture.getField("REPARTITION_POURCENT");
                if (fieldRepFacture.getType().getSize() > 500) {
                    final String fName = fieldRepFacture.getName();
                    alter.alterColumn(fName, EnumSet.allOf(Properties.class), "numeric(6,2)", "0", false);

                    UpdateBuilder build = new UpdateBuilder(tableFacture);
                    build.set(fieldRepFacture.getName(), "0");
                    build.setWhere(new Where(fieldRepFacture, "=", (Object) null));
                    builds.add(build);
                }
            }

            if (!alter.isEmpty())
                alters.add(alter);

        }
        if (alters.size() > 0) {
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            for (UpdateBuilder updateBuilder : builds) {
                ds.execute(updateBuilder.asString());
            }

            for (final String sql : ChangeTable.cat(alters, root.getName())) {
                ds.execute(sql);
            }
            root.refetch();
        }
    }

    private void fixUnboundedVarchar(DBRoot root) throws SQLException {
        final Set<String> namesSet = CollectionUtils.createSet("NOM", "PRENOM", "SURNOM", "LOGIN", "PASSWORD");
        final List<AlterTable> alters = new ArrayList<AlterTable>();
        final List<UpdateBuilder> builds = new ArrayList<UpdateBuilder>();
        for (final SQLTable t : root.getTables()) {
            final AlterTable alter = new AlterTable(t);
            for (final SQLField f : t.getFields()) {
                if (f.getType().getType() == Types.VARCHAR && f.getType().getSize() == Integer.MAX_VALUE) {

                    UpdateBuilder build = new UpdateBuilder(t);
                    build.set(f.getName(), "''");
                    build.setWhere(new Where(f, "=", (Object) null));
                    builds.add(build);

                    final String fName = f.getName();
                    final int size;
                    if (t.getName().contains("USER") && namesSet.contains(fName))
                        size = 128;
                    else if (fName.equals("TEL") || fName.startsWith("TEL_"))
                        size = 32;
                    else if (fName.contains("INFO"))
                        size = 4096;
                    else if (fName.contains("FORMULE"))
                        size = 1024;
                    else if (fName.equals("CONTENU"))
                        size = 2048;
                    else
                        // e.g. IDCC.NOM > 350
                        size = 512;
                    alter.alterColumn(fName, EnumSet.allOf(Properties.class), "varchar(" + size + ")", "''", false);
                }
            }
            if (!alter.isEmpty())
                alters.add(alter);
        }
        if (alters.size() > 0) {
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            for (UpdateBuilder build : builds) {
                ds.execute(build.asString());
            }

            for (final String sql : ChangeTable.cat(alters, root.getName())) {
                ds.execute(sql);
            }
            root.refetch();
        }

    }

    private void updateToV1Dot3(final DBRoot root) throws SQLException {
        final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

        addForeignKeyFactureOnEcheance(root);
        addFieldForPartialInvoice(root);

        // Numérotation client
        {
            SQLTable tableNum = root.getTable("NUMEROTATION_AUTO");
            boolean alterNum = false;
            AlterTable t = new AlterTable(tableNum);
            if (!tableNum.getFieldsName().contains("CLIENT_START")) {
                t.addColumn("CLIENT_START", "integer DEFAULT 0");
                alterNum = true;
            }
            if (!tableNum.getFieldsName().contains("CLIENT_FORMAT")) {
                t.addVarCharColumn("CLIENT_FORMAT", 48);
                alterNum = true;
            }

            if (alterNum) {
                try {
                    ds.execute(t.asString());
                    tableNum.getSchema().updateVersion();
                    tableNum.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table NUMEROTATION_AUTO", ex);
                }
            }
        }

        // Eecheance founisseur
        {
            SQLTable tableEchF = root.getTable("ECHEANCE_FOURNISSEUR");
            AlterTable t = new AlterTable(tableEchF);
            if (!tableEchF.getFieldsName().contains("REG_COMPTA")) {
                t.addColumn("REG_COMPTA", "boolean DEFAULT false");
                try {
                    ds.execute(t.asString());
                    tableEchF.getSchema().updateVersion();
                    tableEchF.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout du champ REG_COMPTA sur la table ECHEANCE_FOUNISSEUR", ex);
                }
            }
        }

        SQLTable tableTaxe = root.getTable("TAXE");
        boolean containsTaxeDefault = tableTaxe.contains("DEFAULT");
        if (!containsTaxeDefault) {
            AlterTable t = new AlterTable(tableTaxe);
            t.addColumn("DEFAULT", "boolean DEFAULT false");

            try {
                ds.execute(t.asString());
                tableTaxe.getSchema().updateVersion();
                tableTaxe.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table TAXE", ex);
            }

        }

        // Ajout de la TVA à 20
        SQLSelect selTVA = new SQLSelect();
        selTVA.addSelect(tableTaxe.getKey(), "COUNT");
        selTVA.setWhere(new Where(tableTaxe.getField("TAUX"), "=", 20));
        Object result = root.getBase().getDataSource().executeScalar(selTVA.asString());
        if (result == null || ((Number) result).longValue() == 0) {
            SQLRowValues rowVals = new SQLRowValues(tableTaxe);
            rowVals.put("NOM", "TVA 20%");
            rowVals.put("TAUX", Float.valueOf(20));
            rowVals.put("DEFAULT", Boolean.TRUE);
            rowVals.commit();
        } else if (!containsTaxeDefault) {
            SQLSelect selTVA20 = new SQLSelect();
            selTVA20.addSelectStar(tableTaxe);
            selTVA20.setWhere(new Where(tableTaxe.getField("TAUX"), "=", 20));
            List<SQLRow> lTVA = SQLRowListRSH.execute(selTVA20);
            if (lTVA != null && lTVA.size() > 0) {
                SQLRowValues rowVals = lTVA.get(0).asRowValues();
                rowVals.put("DEFAULT", Boolean.TRUE);
                rowVals.update();
            }

        }

        // Article
        {
            SQLTable tableProduct = root.getTable("ARTICLE");
            boolean alterTableProduct = false;
            AlterTable t = new AlterTable(tableProduct);
            if (!tableProduct.getFieldsName().contains("ID_COMPTE_PCE")) {
                t.addForeignColumn("ID_COMPTE_PCE", root.getTable("COMPTE_PCE"));
                alterTableProduct = true;
            }
            if (!tableProduct.getFieldsName().contains("ID_COMPTE_PCE_ACHAT")) {
                t.addForeignColumn("ID_COMPTE_PCE_ACHAT", root.getTable("COMPTE_PCE"));
                alterTableProduct = true;
            }
            if (alterTableProduct) {
                try {
                    ds.execute(t.asString());
                    tableProduct.getSchema().updateVersion();
                    tableProduct.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table ARTICLE", ex);
                }
            }
        }

        // Famille Article
        {
            SQLTable tableArticleFamily = root.getTable("FAMILLE_ARTICLE");
            boolean alterArticleFamily = false;
            AlterTable t = new AlterTable(tableArticleFamily);
            if (!tableArticleFamily.getFieldsName().contains("ID_COMPTE_PCE")) {
                t.addForeignColumn("ID_COMPTE_PCE", root.getTable("COMPTE_PCE"));
                alterArticleFamily = true;
            }
            if (!tableArticleFamily.getFieldsName().contains("ID_COMPTE_PCE_ACHAT")) {
                t.addForeignColumn("ID_COMPTE_PCE_ACHAT", root.getTable("COMPTE_PCE"));
                alterArticleFamily = true;
            }
            if (alterArticleFamily) {
                try {
                    ds.execute(t.asString());
                    tableArticleFamily.getSchema().updateVersion();
                    tableArticleFamily.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table FAMILLE_ARTICLE", ex);
                }
            }
        }

        // ECRITURE
        {
            SQLTable tableRecords = root.getTable("ECRITURE");
            boolean alterRecords = false;
            AlterTable t = new AlterTable(tableRecords);
            if (!tableRecords.getFieldsName().contains("DATE_EXPORT")) {
                t.addColumn("DATE_EXPORT", "date");
                alterRecords = true;
            }

            if (!tableRecords.getFieldsName().contains("CODE_CLIENT")) {
                t.addVarCharColumn("CODE_CLIENT", 256);
                alterRecords = true;
            }
            if (alterRecords) {
                try {
                    ds.execute(t.asString());
                    tableRecords.getSchema().updateVersion();
                    tableRecords.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table ECRITURE", ex);
                }
            }
        }
        addInfoField(root, ds, "AVOIR_FOURNISSEUR");
        addInfoField(root, ds, "AVOIR_CLIENT");

        boolean refetchRoot = false;
        if (!root.contains("CODE_FOURNISSEUR")) {

            SQLCreateTable createCode = new SQLCreateTable(root, "CODE_FOURNISSEUR");
            createCode.addVarCharColumn("CODE", 256);
            createCode.addForeignColumn("FOURNISSEUR");
            createCode.addForeignColumn("ARTICLE");
            try {
                ds.execute(createCode.asString());
                insertUndef(createCode);
                root.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table CODE_FOURNISSEUR", ex);
            }

        }

        // Chargement des tables fraichement créées
        if (refetchRoot)
            root.refetch();

        createFactureFournisseur(root);

        addSupplierCode(root, ds, "BON_RECEPTION_ELEMENT");
        addSupplierCode(root, ds, "COMMANDE_ELEMENT");

        // Undefined
        SQLTable.setUndefID(root.getSchema(), "ARTICLE_DESIGNATION", 1);
        SQLTable.setUndefID(root.getSchema(), "ARTICLE_TARIF", 1);
        SQLTable.setUndefID(root.getSchema(), "CODE_STATUT_CAT_CONV", 1);
        SQLTable.setUndefID(root.getSchema(), "CONTACT_ADMINISTRATIF", 1);
        SQLTable.setUndefID(root.getSchema(), "CONTACT_FOURNISSEUR", 1);

        SQLTable.setUndefID(root.getSchema(), "LANGUE", 1);
        SQLTable.setUndefID(root.getSchema(), "MODELE", 1);
        SQLTable.setUndefID(root.getSchema(), "OBJECTIF_COMMERCIAL", 1);
        SQLTable.setUndefID(root.getSchema(), "TARIF", 1);

        SQLTable.setUndefID(root.getSchema(), "UNITE_VENTE", 1);

        // Create transfer tables
        ComptaPropsConfiguration.setSocieteSQLInjector(root);
        SQLInjector.createTransferTables(root);
        // Move transfer info to SAISIE_VENTE_FACTURE
        convertTransfer(root, Arrays.asList("COMMANDE_CLIENT", "DEVIS", "BON_DE_LIVRAISON"), "SAISIE_VENTE_FACTURE");
        // Fix keys
        if (root.getServer().getSQLSystem().equals(SQLSystem.H2)) {
            final ChangeIDToInt c = new ChangeIDToInt(root.getDBSystemRoot());
            c.changeAll(root);
            root.getDBSystemRoot().reload(Collections.singleton(root.getName()));
        }
    }

    private void convertTransfer(DBRoot root, List<String> tablesSrc, String tableDest) throws SQLException {
        final SQLTable tableDestination = root.getTable(tableDest);
        if (tableDestination.contains("SOURCE") && tableDestination.contains("IDSOURCE")) {
            for (String tableSrc : tablesSrc) {
                convertTransfer(root.getTable(tableSrc), tableDestination);
            }
            final AlterTable alter = new AlterTable(tableDestination);
            alter.dropColumn("SOURCE");
            alter.dropColumn("IDSOURCE");
            final String req = alter.asString();
            root.getDBSystemRoot().getDataSource().execute(req);
            root.refetchTable(tableDest);
        }
    }

    private void convertTransfer(final SQLTable tableSource, final SQLTable tableDest) throws SQLException {
        SQLInjector inj = SQLInjector.getInjector(tableSource, tableDest);
        final SQLRowValues vals = new SQLRowValues(tableDest);
        vals.putNulls("SOURCE", "IDSOURCE");
        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(vals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                Where w = new Where(tableDest.getField("SOURCE"), "=", tableSource.getName());
                w = w.and(new Where(tableDest.getField("IDSOURCE"), "!=", tableSource.getUndefinedIDNumber()));
                // remove archive idsource
                w = w.and(new Where(tableDest.getField("IDSOURCE"), "=", tableSource.getKey()));
                input.setWhere(w);
                return input;
            }
        });
        List<SQLRowValues> rows = fetcher.fetch();
        for (SQLRowValues sqlRowValues : rows) {
            inj.addTransfert(sqlRowValues.getInt("IDSOURCE"), sqlRowValues.getID());
        }
    }

    private void addInfoField(final DBRoot root, final SQLDataSource ds, String tableName) {
        SQLTable tableBL = root.getTable(tableName);
        boolean alterBL = false;
        AlterTable t = new AlterTable(tableBL);
        if (!tableBL.getFieldsName().contains("INFOS")) {
            t.addVarCharColumn("INFOS", 1024);
            alterBL = true;
        }
        if (alterBL) {
            try {
                ds.execute(t.asString());
                tableBL.getSchema().updateVersion();
                tableBL.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table AVOIR FOURNISSEUR", ex);
            }
        }
    }

    private void addSupplierCode(final DBRoot root, final SQLDataSource ds, String tableName) {
        SQLTable tableBL = root.getTable(tableName);
        boolean alterBL = false;
        AlterTable t = new AlterTable(tableBL);
        if (!tableBL.contains("ID_CODE_FOURNISSEUR")) {
            t.addForeignColumn("ID_CODE_FOURNISSEUR", root.getTable("CODE_FOURNISSEUR"));
            alterBL = true;
        }
        if (alterBL) {
            try {
                ds.execute(t.asString());
                tableBL.getSchema().updateVersion();
                tableBL.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table " + tableName, ex);
            }
        }
    }

    private void checkDepartementExists(DBRoot root, String dpt, String chefLieu, String regionAdmin, String numero) throws SQLException {
        SQLSelect sel = new SQLSelect();
        final SQLTable tableDpt = root.getTable("DEPARTEMENT");
        sel.addSelect(tableDpt.getKey());
        sel.addSelect(tableDpt.getField("NOM"));
        Where w = new Where(tableDpt.getField("NOM"), "=", dpt);
        sel.setWhere(w);
        int result = SQLRowListRSH.execute(sel).size();
        if (result == 0) {
            SQLRowValues rowVals = new SQLRowValues(tableDpt);
            rowVals.put("NOM", dpt);
            rowVals.put("CHEF_LIEU", chefLieu);
            rowVals.put("REGION_ADMIN", regionAdmin);
            rowVals.put("NUMERO", numero);
            rowVals.commit();
        }
    }

    private void updateToV1Dot4(final DBRoot root) throws SQLException {

        checkPrefsComptable(root);
        checkDepartementExists(root, "Aisne", "Laon", "Picardie", "02");

        if (root.contains("TARIF_AGENCE")) {

            List<String> tablesUiLocked = Arrays.asList("BON_DE_LIVRAISON", "COMMANDE_CLIENT", "SAISIE_VENTE_FACTURE", "AVOIR_CLIENT", "COMMANDE", "BON_RECEPTION", "FACTURE_FOURNISSEUR");
            for (String tableName : tablesUiLocked) {
                final SQLTable table = root.getTable(tableName);
                AlterTable t = new AlterTable(table);
                if (!table.contains("UI_LOCK")) {
                    t.addVarCharColumn("UI_LOCK", 512);
                    t.addForeignColumn("ID_USER_UI_LOCK", root.findTable("USER_COMMON"));
                    table.getBase().getDataSource().execute(t.asString());
                    table.getSchema().updateVersion();
                    table.fetchFields();
                }

            }

            if (!root.contains("RELIQUAT_BL")) {
                final SQLCreateTable createTable = new SQLCreateTable(root, "RELIQUAT_BL");
                createTable.addForeignColumn("ARTICLE");
                createTable.addForeignColumn("ID_BON_DE_LIVRAISON_ORIGINE", root.getTable("BON_DE_LIVRAISON"));
                createTable.addForeignColumn("ID_BON_DE_LIVRAISON", root.getTable("BON_DE_LIVRAISON"));
                createTable.addForeignColumn("ID_BON_DE_LIVRAISON_ELEMENT", root.getTable("BON_DE_LIVRAISON_ELEMENT"));
                createTable.addIntegerColumn("QTE", 1);
                createTable.addDecimalColumn("QTE_UNITAIRE", 16, 6, BigDecimal.valueOf(1), false);
                createTable.addForeignColumn("UNITE_VENTE");

                try {
                    root.getBase().getDataSource().execute(createTable.asString());
                    insertUndef(createTable);
                    root.refetchTable("RELIQUAT_BL");
                    root.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de la création de la table " + "RELIQUAT_BL", ex);
                }
            }

            if (!root.getTable("ARTICLE").contains("TRANSPORT")) {
                final SQLTable tableArticle = root.getTable("ARTICLE");
                AlterTable alterArticle = new AlterTable(tableArticle);
                alterArticle.addBooleanColumn("TRANSPORT", false, false);
                tableArticle.getBase().getDataSource().execute(alterArticle.asString());
                tableArticle.getSchema().updateVersion();
                tableArticle.fetchFields();
            }

            if (!root.contains("RELIQUAT_BR")) {
                final SQLCreateTable createTable = new SQLCreateTable(root, "RELIQUAT_BR");
                createTable.addForeignColumn("ARTICLE");
                createTable.addForeignColumn("ID_BON_RECEPTION_ORIGINE", root.getTable("BON_RECEPTION"));
                createTable.addForeignColumn("ID_BON_RECEPTION", root.getTable("BON_RECEPTION"));
                createTable.addForeignColumn("ID_BON_RECEPTION_ELEMENT", root.getTable("BON_RECEPTION_ELEMENT"));
                createTable.addIntegerColumn("QTE", 1);
                createTable.addDecimalColumn("QTE_UNITAIRE", 16, 6, BigDecimal.valueOf(1), false);
                createTable.addForeignColumn("UNITE_VENTE");

                try {
                    root.getBase().getDataSource().execute(createTable.asString());
                    insertUndef(createTable);
                    root.refetchTable("RELIQUAT_BL");
                    root.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de la création de la table " + "RELIQUAT_BR", ex);
                }
            }

            {
                if (root.getName().endsWith("54")) {
                    final SQLTable tableArticle = root.getTable("ARTICLE");
                    AlterTable tArt = new AlterTable(tableArticle);
                    if (!tableArticle.contains("LABEL_DOUANE")) {
                        tArt.addVarCharColumn("LABEL_DOUANE", 512);
                        tableArticle.getBase().getDataSource().execute(tArt.asString());
                        tableArticle.getSchema().updateVersion();
                        tableArticle.fetchFields();
                    }
                }
            }

            {
                final SQLTable tableFour = root.getTable("FOURNISSEUR");
                AlterTable tFour = new AlterTable(tableFour);
                boolean updateFour = false;

                if (!tableFour.contains("ALG_REGISTRE")) {
                    updateFour = true;
                    tFour.addVarCharColumn("ALG_REGISTRE", 512);
                }
                if (!tableFour.contains("ALG_MATRICULE")) {
                    updateFour = true;
                    tFour.addVarCharColumn("ALG_MATRICULE", 512);
                }
                if (!tableFour.contains("ALG_ARTICLE")) {
                    updateFour = true;
                    tFour.addVarCharColumn("ALG_ARTICLE", 512);
                }

                if (updateFour) {
                    tableFour.getBase().getDataSource().execute(tFour.asString());
                    tableFour.getSchema().updateVersion();
                    tableFour.fetchFields();
                }
            }

        }

        if (root.getTable("COEFF_PRIME") == null) {
            final SQLCreateTable createTable = new SQLCreateTable(root, "COEFF_PRIME");
            // TODO numeric to real
            createTable.addColumn("PRIME_PERSO", "real DEFAULT 0");
            createTable.addColumn("PRIME_RECONSTRUCTION", "real DEFAULT 0");
            createTable.addColumn("PRIME_ANCIENNETE", "real DEFAULT 0");
            createTable.addColumn("PRIME_DEROULEMENT", "real DEFAULT 0");

            try {
                root.getDBSystemRoot().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("COEFF_PRIME");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "COEFF_PRIME", ex);
            }

            SQLTable tableInfosSal = root.getTable("INFOS_SALARIE_PAYE");
            if (!tableInfosSal.contains("ID_COEFF_PRIME")) {
                AlterTable t = new AlterTable(tableInfosSal);
                t.addForeignColumn("ID_COEFF_PRIME", root.getTable("COEFF_PRIME"));

                try {

                    tableInfosSal.getBase().getDataSource().execute(t.asString());
                    tableInfosSal.getSchema().updateVersion();
                    tableInfosSal.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + tableInfosSal.getName(), ex);
                }
            }
        }

        SQLTable tableInfosSal = root.getTable("INFOS_SALARIE_PAYE");
        if (!tableInfosSal.contains("ID_SALARIE")) {
            // Ajout INFOS_SALARIE_PAYE.ID_SALARIE
            AlterTable t = new AlterTable(tableInfosSal);
            t.addForeignColumn("ID_SALARIE", root.getTable("SALARIE"));

            try {

                tableInfosSal.getBase().getDataSource().execute(t.asString());
                tableInfosSal.getSchema().updateVersion();
                tableInfosSal.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + tableInfosSal.getName(), ex);
            }

            // Ajout CONTRAT.DEBUT_CONTRAT
            SQLTable tableContrat = root.getTable("CONTRAT_SALARIE");
            AlterTable tContrat = new AlterTable(tableContrat);
            tContrat.addDateAndTimeColumn("DATE_DEBUT");
            tContrat.addDateAndTimeColumn("DATE_MODIFICATION");
            // tContrat.addForeignColumn("ID_INFOS_SALARIE_PAYE_MODIFIE", tableInfosSal);
            try {
                tableContrat.getBase().getDataSource().execute(tContrat.asString());
                tableContrat.getSchema().updateVersion();
                tableContrat.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + tableContrat.getName(), ex);
            }

            // Ajout FICHE_PAYE.INFOS_SALARIE_PAYE
            SQLTable tableFichePaye = root.getTable("FICHE_PAYE");
            AlterTable tFicheSal = new AlterTable(tableFichePaye);
            tFicheSal.addForeignColumn("ID_INFOS_SALARIE_PAYE", root.getTable("INFOS_SALARIE_PAYE"));
            try {

                tableFichePaye.getBase().getDataSource().execute(tFicheSal.asString());
                tableFichePaye.getSchema().updateVersion();
                tableFichePaye.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + tableFichePaye.getName(), ex);
            }

            {
                // Mise à jour des dates de début de contrat
                UpdateBuilder builderContrat = new UpdateBuilder(tableContrat);
                builderContrat.addBackwardVirtualJoin(tableInfosSal, "ID_CONTRAT_SALARIE");
                builderContrat.setFromVirtualJoinField("DATE_DEBUT", tableInfosSal.getAlias(), "DATE_ARRIVE");
                tableInfosSal.getDBSystemRoot().getDataSource().execute(builderContrat.asString());
            }
            {
                // Mise à jour des INFOS_SALARIE_PAYE dans les fiches
                SQLTable tableSalarie = root.getTable("SALARIE");
                UpdateBuilder builderFiche = new UpdateBuilder(tableFichePaye);
                builderFiche.addForwardVirtualJoin(tableSalarie, "ID_SALARIE");
                builderFiche.setFromVirtualJoinField("ID_INFOS_SALARIE_PAYE", tableSalarie.getAlias(), "ID_INFOS_SALARIE_PAYE");
                tableInfosSal.getDBSystemRoot().getDataSource().execute(builderFiche.asString());
            }
        }

        // Cumuls paye
        SQLTable tableCumulsPaye = root.getTable("CUMULS_PAYE");

        AlterTable tCumuls = new AlterTable(tableCumulsPaye);
        tCumuls.alterColumn("NET_IMP_C", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tCumuls.alterColumn("COT_PAT_C", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tCumuls.alterColumn("COT_SAL_C", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tCumuls.alterColumn("SAL_BRUT_C", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tCumuls.alterColumn("NET_A_PAYER_C", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tCumuls.alterColumn("CSG_C", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        if (tableCumulsPaye.contains("HEURE_TRAV") && !tableCumulsPaye.contains("HEURE_TRAV_C")) {

            AlterTable tColumnNameCumuls = new AlterTable(tableCumulsPaye);

            tColumnNameCumuls.addClause(new DeferredClause() {
                @Override
                public String asString(ChangeTable<?> ct, SQLName tableName) {
                    return (root.getServer().getSQLSystem() == SQLSystem.POSTGRESQL ? "RENAME COLUMN \"HEURE_TRAV\" TO \"HEURE_TRAV_C\"" : "ALTER COLUMN \"HEURE_TRAV\" RENAME TO \"HEURE_TRAV_C\"");
                }

                @Override
                public ClauseType getType() {
                    return ClauseType.OTHER;
                }
            });
            try {

                tableCumulsPaye.getBase().getDataSource().execute(tColumnNameCumuls.asString());
                tableCumulsPaye.getSchema().updateVersion();
                tableCumulsPaye.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la modification du champ de la table " + tableCumulsPaye.getName(), ex);
            }
        } else if (!tableCumulsPaye.contains("HEURE_TRAV_C")) {
            tCumuls.addDecimalColumn("HEURE_TRAV_C", 16, 2, BigDecimal.ZERO, false);
        }

        try {

            tableCumulsPaye.getBase().getDataSource().execute(tCumuls.asString());
            tableCumulsPaye.getSchema().updateVersion();
            tableCumulsPaye.fetchFields();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur lors de la modification des champs de la table " + tableCumulsPaye.getName(), ex);
        }

        // element paye
        SQLTable tableElementPaye = root.getTable("FICHE_PAYE_ELEMENT");

        AlterTable tEltPaye = new AlterTable(tableElementPaye);
        tEltPaye.alterColumn("TAUX_PAT", EnumSet.allOf(Properties.class), "numeric(16,6)", "0", true);
        tEltPaye.alterColumn("MONTANT_SAL_AJ", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tEltPaye.alterColumn("TAUX_SAL", EnumSet.allOf(Properties.class), "numeric(16,6)", "0", true);
        tEltPaye.alterColumn("MONTANT_SAL_DED", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tEltPaye.alterColumn("MONTANT_PAT", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tEltPaye.alterColumn("NB_BASE", EnumSet.allOf(Properties.class), "numeric(16,6)", "0", true);

        try {

            tableElementPaye.getBase().getDataSource().execute(tEltPaye.asString());
            tableElementPaye.getSchema().updateVersion();
            tableElementPaye.fetchFields();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur lors de la modification des champs de la table " + tableElementPaye.getName(), ex);
        }

        // paye
        SQLTable tablePaye = root.getTable("FICHE_PAYE");

        AlterTable tPaye = new AlterTable(tablePaye);
        tPaye.alterColumn("ACOMPTE", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tPaye.alterColumn("NET_IMP", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tPaye.alterColumn("COT_PAT", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tPaye.alterColumn("NET_A_PAYER", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tPaye.alterColumn("SAL_BRUT", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tPaye.alterColumn("CSG", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);
        tPaye.alterColumn("COT_SAL", EnumSet.allOf(Properties.class), "numeric(16,2)", "0", true);

        try {
            tablePaye.getBase().getDataSource().execute(tPaye.asString());
            tablePaye.getSchema().updateVersion();
            tablePaye.fetchFields();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erreur lors de la modification des champs de la table " + tablePaye.getName(), ex);
        }

        if (root.getTable("ARTICLE_ELEMENT") == null) {
            final SQLCreateTable createTable = new SQLCreateTable(root, "ARTICLE_ELEMENT");
            createTable.addForeignColumn("ARTICLE");
            createTable.addForeignColumn("ID_ARTICLE_PARENT", root.getTable("ARTICLE"));
            createTable.addIntegerColumn("QTE", 1);
            createTable.addDecimalColumn("QTE_UNITAIRE", 16, 6, BigDecimal.valueOf(1), false);
            createTable.addForeignColumn("UNITE_VENTE");
            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("ARTICLE_ELEMENT");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "ARTICLE_ELEMENT", ex);
            }
        }

        if (root.getTable("VILLE") == null) {
            final SQLCreateTable createTable = new SQLCreateTable(root, "VILLE");
            createTable.addLongColumn("X_LAMBERT", 0L, true);
            createTable.addLongColumn("Y_LAMBERT", 0L, true);
            createTable.addLongColumn("POPULATION", 0L, true);
            createTable.addVarCharColumn("NOM", 512);
            createTable.addVarCharColumn("CODE_POSTAL", 256);
            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("VILLE");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "VILLE", ex);
            }
        }

        // Gestion des différentes numérotation
        if (!root.getTable("NUMEROTATION_AUTO").contains("NOM")) {
            final SQLTable tableNum = root.getTable("NUMEROTATION_AUTO");
            AlterTable tNum = new AlterTable(tableNum);
            tNum.addColumn("NOM", "varchar (256)", "'Standard'", true);

            root.getBase().getDataSource().execute(tNum.asString());
            tableNum.fetchFields();
            root.getSchema().updateVersion();

        }

        // Gestion du timbre fiscal
        if (!root.getTable("CLIENT").contains("TIMBRE_FISCAL")) {
            AlterTable tClient = new AlterTable(root.getTable("CLIENT"));
            tClient.addBooleanColumn("TIMBRE_FISCAL", Boolean.FALSE, false);
            root.getBase().getDataSource().execute(tClient.asString());
            root.getSchema().updateVersion();
        }
        // Obsolete
        if (!root.getTable("CLIENT").contains("OBSOLETE")) {
            AlterTable tClient = new AlterTable(root.getTable("CLIENT"));
            tClient.addBooleanColumn("OBSOLETE", Boolean.FALSE, false);
            root.getBase().getDataSource().execute(tClient.asString());
            root.getSchema().updateVersion();
        }

        // Relance
        if (!root.getTable("RELANCE").contains("ID_ECHEANCE_CLIENT")) {
            AlterTable tRelance = new AlterTable(root.getTable("RELANCE"));
            tRelance.addForeignColumn("ID_ECHEANCE_CLIENT", root.getTable("ECHEANCE_CLIENT"));
            root.getBase().getDataSource().execute(tRelance.asString());
            root.getSchema().updateVersion();
        }

        if (!root.getTable("AVOIR_CLIENT_ELEMENT").contains("RETOUR_STOCK")) {
            final SQLTable tableAvoirElt = root.getTable("AVOIR_CLIENT_ELEMENT");
            AlterTable tAvoir = new AlterTable(tableAvoirElt);
            tAvoir.addBooleanColumn("RETOUR_STOCK", Boolean.FALSE, false);
            root.getBase().getDataSource().execute(tAvoir.asString());
            root.getSchema().updateVersion();
            tableAvoirElt.fetchFields();
        }

        final SQLTable tableVenteFacture = root.getTable("SAISIE_VENTE_FACTURE");
        AlterTable tFacture = new AlterTable(tableVenteFacture);
        boolean alterFacture = false;
        boolean upNET = false;
        if (!tableVenteFacture.contains("SOUMIS_TIMBRE_FISCAL")) {
            tFacture.addBooleanColumn("SOUMIS_TIMBRE_FISCAL", Boolean.FALSE, true);
            alterFacture = true;
        }
        if (!tableVenteFacture.contains("TAUX_TIMBRE_FISCAL")) {
            tFacture.addDecimalColumn("TAUX_TIMBRE_FISCAL", 16, 4, BigDecimal.ONE, true);
            alterFacture = true;
        }
        if (!tableVenteFacture.contains("TOTAL_TIMBRE_FISCAL")) {
            tFacture.addLongColumn("TOTAL_TIMBRE_FISCAL", 0L, true);
            alterFacture = true;
        }
        if (!tableVenteFacture.contains("NET_A_PAYER")) {
            tFacture.addLongColumn("NET_A_PAYER", 0L, true);
            alterFacture = true;
            upNET = true;
        }
        if (alterFacture) {
            root.getBase().getDataSource().execute(tFacture.asString());
            root.getSchema().updateVersion();
            tableVenteFacture.fetchFields();
            if (upNET) {
                String req = "UPDATE " + tableVenteFacture.getSQLName().quote() + " SET \"NET_A_PAYER\"=(\"T_TTC\"-\"T_AVOIR_TTC\")";
                root.getBase().getDataSource().execute(req);
            }
        }

        SQLTable tableTaxe = root.getTable("TAXE");
        boolean updateTaxe = false;
        AlterTable tTaxe = new AlterTable(tableTaxe);
        if (!tableTaxe.contains("ID_COMPTE_PCE_VENTE")) {
            tTaxe.addForeignColumn("ID_COMPTE_PCE_VENTE", root.getTable("COMPTE_PCE"));
            updateTaxe = true;
        }
        if (!tableTaxe.contains("ID_COMPTE_PCE_VENTE_SERVICE")) {
            tTaxe.addForeignColumn("ID_COMPTE_PCE_VENTE_SERVICE", root.getTable("COMPTE_PCE"));
            updateTaxe = true;
        }

        if (updateTaxe) {
            try {
                tableTaxe.getBase().getDataSource().execute(tTaxe.asString());
                tableTaxe.getSchema().updateVersion();
                tableTaxe.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table TAXE", ex);
            }

        }

        // Tarification par quantite
        if (root.getTable("TARIF_QUANTITE") == null) {
            final SQLCreateTable createTableQtyTarif = new SQLCreateTable(root, "TARIF_QUANTITE");
            createTableQtyTarif.addForeignColumn("ID_ARTICLE", root.getTable("ARTICLE"));
            createTableQtyTarif.addDecimalColumn("QUANTITE", 16, 3, BigDecimal.ONE, false);
            createTableQtyTarif.addDecimalColumn("POURCENT_REMISE", 16, 3, null, true);
            createTableQtyTarif.addDecimalColumn("PRIX_METRIQUE_VT_1", 16, 6, null, true);
            try {
                root.getBase().getDataSource().execute(createTableQtyTarif.asString());
                insertUndef(createTableQtyTarif);
                root.refetchTable("TARIF_QUANTITE");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "TARIF_QUANTITE", ex);
            }
        }
        // Articles fournisseurs
        addArticleFournisseur(root);

        SQLTable tableCmdF = root.getTable("COMMANDE");
        if (!tableCmdF.contains("ID_MODELE")) {
            AlterTable t = new AlterTable(tableCmdF);
            t.addForeignColumn("ID_MODELE", root.getTable("MODELE"));
            tableCmdF.getBase().getDataSource().execute(t.asString());
            tableCmdF.getSchema().updateVersion();
            tableCmdF.fetchFields();
        }

        SQLTable tableArtF = root.getTable("ARTICLE_FOURNISSEUR");
        if (!tableArtF.contains("ID_FAMILLE_ARTICLE")) {
            AlterTable t = new AlterTable(tableArtF);
            t.addForeignColumn("ID_FAMILLE_ARTICLE", root.getTable("FAMILLE_ARTICLE"));
            tableArtF.getBase().getDataSource().execute(t.asString());
            tableArtF.getSchema().updateVersion();
            tableArtF.fetchFields();
        }

        if (!tableCmdF.contains("DATE_RECEPTION_DEMANDEE")) {
            AlterTable t = new AlterTable(tableCmdF);
            t.addColumn("DATE_RECEPTION_DEMANDEE", "date");
            t.addColumn("DATE_RECEPTION_CONFIRMEE", "date");
            tableCmdF.getBase().getDataSource().execute(t.asString());
            tableCmdF.getSchema().updateVersion();
            tableCmdF.fetchFields();
        }

        SQLTable tableEcr = root.getTable("ECRITURE");
        if (!tableEcr.contains("NOM_PIECE")) {
            AlterTable t = new AlterTable(tableEcr);
            t.addVarCharColumn("NOM_PIECE", 1024);
            tableEcr.getBase().getDataSource().execute(t.asString());
            tableEcr.getSchema().updateVersion();
            tableEcr.fetchFields();
        }

        SQLTable tableKm = root.getTable("SAISIE_KM_ELEMENT");
        if (!tableKm.contains("NOM_PIECE")) {
            AlterTable t = new AlterTable(tableKm);
            t.addVarCharColumn("NOM_PIECE", 1024);

            tableKm.getBase().getDataSource().execute(t.asString());
            tableKm.getSchema().updateVersion();
            tableKm.fetchFields();
        }

        {
            SQLSelect sel = new SQLSelect();
            final SQLTable tableStyle = root.getTable("STYLE");
            sel.addSelect(tableStyle.getKey());
            sel.setWhere(new Where(tableStyle.getField("NOM"), "=", "Composant"));
            String req = sel.asString();
            List<Map<String, Object>> l = root.getDBSystemRoot().getDataSource().execute(req);

            if (l.size() == 0) {
                SQLRowValues rowValsStyle = new SQLRowValues(tableStyle);
                rowValsStyle.put("NOM", "Composant");
                rowValsStyle.put("CODE", "COMP");
                rowValsStyle.insert();
            }
        }

        {
            // Fix qté et style
            List<String> tableElt = Arrays.asList("AVOIR_CLIENT_ELEMENT", "BON_DE_LIVRAISON_ELEMENT", "BON_RECEPTION_ELEMENT", "COMMANDE_CLIENT_ELEMENT", "COMMANDE_ELEMENT", "DEVIS_ELEMENT",
                    "SAISIE_VENTE_FACTURE_ELEMENT");
            for (String string : tableElt) {
                final SQLTable table = root.getTable(string);
                Number undefined = table.getUndefinedIDNumber();
                if (undefined != null && undefined.intValue() > 0) {
                    UpdateBuilder build = new UpdateBuilder(table);
                    build.setObject("ID_STYLE", 2);
                    build.setObject("QTE", 1);
                    build.setWhere(new Where(table.getKey(), "=", undefined.intValue()));
                    table.getDBSystemRoot().getDataSource().execute(build.asString());
                }
            }
        }

        // ADresse de livraison et remise
        {
            SQLTable tableDevis = root.getTable("DEVIS");
            addAdresseField(tableDevis, root);
            addRemiseField(tableDevis, root);

            SQLTable tableVF = root.getTable("SAISIE_VENTE_FACTURE");
            addAdresseField(tableVF, root);
            addRemiseField(tableVF, root);

            SQLTable tableCmd = root.getTable("COMMANDE_CLIENT");
            addAdresseField(tableCmd, root);
            addRemiseField(tableCmd, root);

            SQLTable tableBon = root.getTable("BON_DE_LIVRAISON");
            addAdresseField(tableBon, root);
            addRemiseField(tableBon, root);

            SQLTable tableAvoir = root.getTable("AVOIR_CLIENT");
            addAdresseField(tableAvoir, root);
            addRemiseField(tableAvoir, root);
        }
        // Service client
        if (root.getTable("CLIENT_DEPARTEMENT") == null) {
            final SQLCreateTable createTableDpt = new SQLCreateTable(root, "CLIENT_DEPARTEMENT");
            createTableDpt.addForeignColumn("CLIENT");
            if (root.contains("ADRESSE")) {
                createTableDpt.addForeignColumn("ADRESSE");
            } else {
                createTableDpt.addForeignColumn("ID_ADRESSE", root.findTable("ADRESSE"));
            }
            createTableDpt.addVarCharColumn("INFOS", 2048);
            createTableDpt.addVarCharColumn("NOM", 256);
            try {
                root.getBase().getDataSource().execute(createTableDpt.asString());
                insertUndef(createTableDpt);
                root.refetchTable("CLIENT_DEPARTEMENT");
                root.getSchema().updateVersion();
            } catch (SQLException ex2) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CLIENT_DEPARTEMENT", ex2);
            }
        }

        SQLTable tableVFElt = root.getTable("SAISIE_VENTE_FACTURE_ELEMENT");
        patchFieldElt1Dot4(tableVFElt, root);

        SQLTable tableDevisElt = root.getTable("DEVIS_ELEMENT");
        patchFieldElt1Dot4(tableDevisElt, root);

        SQLTable tableCmdElt = root.getTable("COMMANDE_CLIENT_ELEMENT");

        patchFieldElt1Dot4(tableCmdElt, root);

        SQLTable tableBonElt = root.getTable("BON_DE_LIVRAISON_ELEMENT");
        patchFieldElt1Dot4(tableBonElt, root);

        SQLTable tableAvoirElt = root.getTable("AVOIR_CLIENT_ELEMENT");
        patchFieldElt1Dot4(tableAvoirElt, root);

        addNiveauElementField(root.getTable("COMMANDE_ELEMENT"));
        addNiveauElementField(root.getTable("BON_RECEPTION_ELEMENT"));
        addNiveauElementField(root.getTable("FACTURE_FOURNISSEUR_ELEMENT"));

        addContact(root);

        {
            // Gestion des préparations de commandes
            final SQLTable tableCommandeClient = root.getTable("COMMANDE_CLIENT");
            AlterTable tCommandeClient = new AlterTable(tableCommandeClient);
            boolean updateCmdCli = false;

            if (!tableCommandeClient.contains("ETAT_COMMANDE")) {
                updateCmdCli = true;
                tCommandeClient.addIntegerColumn("ETAT_COMMANDE", EtatCommandeClient.A_PREPARER.getId());
            }
            if (!tableCommandeClient.contains("EMBALLAGE")) {
                updateCmdCli = true;
                tCommandeClient.addVarCharColumn("EMBALLAGE", 256);
            }
            if (!tableCommandeClient.contains("NUMERO_EXPEDITION")) {
                updateCmdCli = true;
                tCommandeClient.addVarCharColumn("NUMERO_EXPEDITION", 256);
            }

            if (!tableCommandeClient.contains("ID_TAXE_PORT")) {
                updateCmdCli = true;
                tCommandeClient.addForeignColumn("ID_TAXE_PORT", root.getTable("TAXE"));
            }

            if (!tableCommandeClient.contains("PORT_HT")) {
                updateCmdCli = true;
                tCommandeClient.addLongColumn("PORT_HT", Long.valueOf(0), false);
            }

            if (!tableCommandeClient.contains("REMISE_HT")) {
                updateCmdCli = true;
                tCommandeClient.addLongColumn("REMISE_HT", Long.valueOf(0), false);
            }

            if (updateCmdCli) {
                tableCommandeClient.getBase().getDataSource().execute(tCommandeClient.asString());
                tableCommandeClient.getSchema().updateVersion();
                tableCommandeClient.fetchFields();
            }
        }

        {
            // Gestion des préparations de commandes
            final SQLTable tableBonL = root.getTable("BON_DE_LIVRAISON");
            AlterTable tBonL = new AlterTable(tableBonL);
            boolean updateBonL = false;

            if (!tableBonL.contains("TOTAL_DEVISE")) {
                updateBonL = true;
                tBonL.addLongColumn("TOTAL_DEVISE", Long.valueOf(0), false);
            }

            if (!tableBonL.contains("TOTAL_SERVICE")) {
                updateBonL = true;
                tBonL.addLongColumn("TOTAL_SERVICE", Long.valueOf(0), false);
            }

            if (!tableBonL.contains("ID_TAXE_PORT")) {
                updateBonL = true;
                tBonL.addForeignColumn("ID_TAXE_PORT", root.getTable("TAXE"));
            }

            if (!tableBonL.contains("PORT_HT")) {
                updateBonL = true;
                tBonL.addLongColumn("PORT_HT", Long.valueOf(0), false);
            }

            if (!tableBonL.contains("REMISE_HT")) {
                updateBonL = true;
                tBonL.addLongColumn("REMISE_HT", Long.valueOf(0), false);
            }

            if (updateBonL) {
                tableBonL.getBase().getDataSource().execute(tBonL.asString());
                tableBonL.getSchema().updateVersion();
                tableBonL.fetchFields();
            }
        }

        final List<String> asList = Arrays.asList("SAISIE_VENTE_FACTURE", "BON_RECEPTION", "BON_DE_LIVRAISON");
        for (String tableVirtualStockName : asList) {
            SQLTable tableVirtualStock = root.getTable(tableVirtualStockName);
            if (!tableVirtualStock.contains("CREATE_VIRTUAL_STOCK")) {
                AlterTable t = new AlterTable(tableVirtualStock);
                t.addBooleanColumn("CREATE_VIRTUAL_STOCK", Boolean.TRUE, false);
                tableVirtualStock.getBase().getDataSource().execute(t.asString());
                tableVirtualStock.getSchema().updateVersion();
                tableVirtualStock.fetchFields();
            }
        }

        // Remise dans les devis
        final SQLTable tableDevis = root.getTable("DEVIS");
        AlterTable tDevis = new AlterTable(tableDevis);
        boolean updateDevis = false;

        if (!tableDevis.contains("POURCENT_REMISE")) {
            updateDevis = true;
            tDevis.addColumn("POURCENT_REMISE", "numeric (12,8)");
        }
        if (!tableDevis.contains("MONTANT_REMISE")) {
            updateDevis = true;
            tDevis.addColumn("MONTANT_REMISE", "numeric (16,8)");
        }
        if (!tableDevis.contains("T_HA")) {
            updateDevis = true;
            tDevis.addColumn("T_HA", "bigint", "0", false);
        }

        if (!tableDevis.contains("DUNNING_DATE")) {
            updateDevis = true;
            tDevis.addColumn("DUNNING_DATE", "date");
        }

        if (updateDevis) {
            tableDevis.getBase().getDataSource().execute(tDevis.asString());
            tableDevis.getSchema().updateVersion();
            tableDevis.fetchFields();
        }

        final SQLTable tableKmElt = root.getTable("SAISIE_KM_ELEMENT");
        if (!tableKmElt.contains("ANALYTIQUE")) {
            AlterTable t = new AlterTable(tableKmElt);
            t.addVarCharColumn("ANALYTIQUE", 256);
            tableKmElt.getBase().getDataSource().execute(t.asString());
            tableKmElt.getSchema().updateVersion();
            tableKmElt.fetchFields();

        }

        final SQLTable tableClient = root.getTable("CLIENT");
        final SQLTable tableAdresse = tableClient.getForeignTable("ID_ADRESSE");
        if (tableAdresse != null) {
            AlterTable t = new AlterTable(tableAdresse);
            boolean updateADr = false;
            if (!tableAdresse.contains("PROVINCE")) {
                t.addVarCharColumn("PROVINCE", 256);
                updateADr = true;
            }
            if (!tableAdresse.contains("LIBELLE")) {
                t.addVarCharColumn("LIBELLE", 256);
                updateADr = true;
            }
            if (!tableAdresse.contains("TYPE")) {
                t.addVarCharColumn("TYPE", 256);
                updateADr = true;
            }
            if (!tableAdresse.contains("EMAIL_CONTACT")) {
                t.addVarCharColumn("EMAIL_CONTACT", 256);
                updateADr = true;
            }
            if (updateADr) {
                tableAdresse.getBase().getDataSource().execute(t.asString());
                tableAdresse.getSchema().updateVersion();
                tableAdresse.fetchFields();
            }
        }

        if (tableClient != null) {
            AlterTable t = new AlterTable(tableClient);
            boolean upClient = false;
            if (!tableClient.contains("REMIND_DATE")) {
                t.addDateAndTimeColumn("REMIND_DATE");
                upClient = true;
            }
            if (!tableClient.contains("BLOQUE_LIVRAISON")) {
                t.addBooleanColumn("BLOQUE_LIVRAISON", false, false);
                upClient = true;
            }
            if (!tableClient.contains("BLOQUE")) {
                upClient = true;
                t.addBooleanColumn("BLOQUE", false, false);
            }
            if (!tableClient.contains("ID_COMPTE_PCE_SERVICE")) {
                upClient = true;
                t.addForeignColumn("ID_COMPTE_PCE_SERVICE", tableClient.getTable("COMPTE_PCE"));
            }

            if (!tableClient.contains("ID_COMPTE_PCE_PRODUIT")) {
                upClient = true;
                t.addForeignColumn("ID_COMPTE_PCE_PRODUIT", tableClient.getTable("COMPTE_PCE"));
            }

            if (!tableClient.contains("ENCOURS_MAX")) {
                upClient = true;
                t.addDecimalColumn("ENCOURS_MAX", 16, 2, BigDecimal.valueOf(0), false);
            }
            if (!tableClient.contains("CATEGORIES")) {
                upClient = true;
                t.addVarCharColumn("CATEGORIES", 2048);
            }
            if (!tableClient.contains("NOTE_FINANCIERE")) {
                upClient = true;
                t.addVarCharColumn("NOTE_FINANCIERE", 1024);
            }
            if (!tableClient.contains("CENTRE_GESTION")) {
                upClient = true;
                t.addVarCharColumn("CENTRE_GESTION", 1024);
            }
            if (!tableClient.contains("METHODE_RELANCE")) {
                upClient = true;
                t.addVarCharColumn("METHODE_RELANCE", 1024);
            }
            if (!tableClient.contains("SITE_INTERNET")) {
                upClient = true;
                t.addVarCharColumn("SITE_INTERNET", 2048);
            }
            if (!tableClient.contains("GROUPE")) {
                upClient = true;
                t.addVarCharColumn("GROUPE", 1024);
            }
            if (!tableClient.contains("ID_COMMERCIAL")) {
                upClient = true;
                t.addForeignColumn("ID_COMMERCIAL", tableClient.getTable("COMMERCIAL"));
            }

            if (upClient) {
                tableClient.getBase().getDataSource().execute(t.asString());
                tableClient.getSchema().updateVersion();
                tableClient.fetchFields();
            }
        }
        if (!root.contains("CATEGORIE_CLIENT")) {
            final SQLCreateTable createCategorieClientTable = new SQLCreateTable(root, "CATEGORIE_CLIENT");
            createCategorieClientTable.addVarCharColumn("NOM", 1024);

            try {
                root.getDBSystemRoot().getDataSource().execute(createCategorieClientTable.asString());
                insertUndef(createCategorieClientTable);
                tableDevis.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table CATEGORIE_CLIENT", ex);
            }

        }

        final SQLTable tableAssoc = root.getTable("ASSOCIATION_ANALYTIQUE");
        if (tableAssoc != null && !tableAssoc.contains("GESTION_AUTO")) {
            AlterTable t = new AlterTable(tableAssoc);
            t.addBooleanColumn("GESTION_AUTO", false, false);
            tableAssoc.getBase().getDataSource().execute(t.asString());
            tableAssoc.getSchema().updateVersion();
            tableAssoc.fetchFields();
        }
        if (!root.contains("CALENDAR_ITEM")) {
            final SQLCreateTable createTaskGroupTable = new SQLCreateTable(root, "CALENDAR_ITEM_GROUP");
            createTaskGroupTable.addVarCharColumn("NAME", 1024);
            createTaskGroupTable.addVarCharColumn("DESCRIPTION", 1024 * 8);

            final SQLCreateTable createTaskTable = new SQLCreateTable(root, "CALENDAR_ITEM");
            createTaskTable.addDateAndTimeColumn("START");
            createTaskTable.addDateAndTimeColumn("END");
            createTaskTable.addLongColumn("DURATION_S", 0L, false);
            createTaskTable.addVarCharColumn("SUMMARY", 1024);
            createTaskTable.addVarCharColumn("DESCRIPTION", 1024 * 8);
            createTaskTable.addVarCharColumn("FLAGS", 1024);
            createTaskTable.addVarCharColumn("STATUS", 128);
            createTaskTable.addForeignColumn(createTaskGroupTable);
            createTaskTable.addLongColumn("SOURCE_ID", null, true);
            createTaskTable.addVarCharColumn("SOURCE_TABLE", 256);
            createTaskTable.addVarCharColumn("UID", 1024);
            createTaskTable.addVarCharColumn("LOCATION", 4096);
            try {
                root.getDBSystemRoot().getDataSource().execute(createTaskGroupTable.asString());
                insertUndef(createTaskGroupTable);
                root.getDBSystemRoot().getDataSource().execute(createTaskTable.asString());
                insertUndef(createTaskTable);
                tableDevis.getSchema().updateVersion();
                root.refetchTable("CALENDAR_ITEM_GROUP");
                root.refetchTable("CALENDAR_ITEM");

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table TASK", ex);
            }

        }

        // Remise sur tarif client
        if (root.contains("ARTICLE_TARIF")) {

            AlterTable t = new AlterTable(root.getTable("ARTICLE_TARIF"));
            boolean alterTarif = false;
            if (!root.getTable("ARTICLE_TARIF").contains("POURCENT_REMISE")) {
                t.addDecimalColumn("POURCENT_REMISE", 16, 6, BigDecimal.valueOf(0), false);
                alterTarif = true;
            }

            if (!root.getTable("ARTICLE_TARIF").contains("QTE")) {
                t.addIntegerColumn("QTE", 1);
                alterTarif = true;
            }
            if (alterTarif) {
                root.getTable("ARTICLE_TARIF").getBase().getDataSource().execute(t.asString());
                root.getTable("ARTICLE_TARIF").fetchFields();
                root.getTable("ARTICLE").getSchema().updateVersion();
            }
        }
        // SKU et Type d'expedition
        final SQLTable articleTable = root.getTable("ARTICLE");
        if (!articleTable.contains("SKU")) {
            AlterTable a = new AlterTable(articleTable);
            a.addVarCharColumn("SKU", 256);
            try {
                articleTable.getBase().getDataSource().execute(a.asString());
                articleTable.getSchema().updateVersion();
                articleTable.fetchFields();
            } catch (SQLException ex2) {
                throw new IllegalStateException("Erreur lors de l'ajout du champs SKU sur la table ARTICLE", ex2);
            }
        }

        try {
            final SQLTable cmdClientTable = root.getTable("COMMANDE_CLIENT");
            if (!cmdClientTable.contains("TYPE_EXPEDITION")) {
                final AlterTable a = new AlterTable(cmdClientTable);
                a.addVarCharColumn("TYPE_EXPEDITION", 256);
                cmdClientTable.getBase().getDataSource().execute(a.asString());
                cmdClientTable.getSchema().updateVersion();
                cmdClientTable.fetchFields();
            }
        } catch (SQLException ex2) {
            throw new IllegalStateException("Erreur lors de l'ajout du champs TYPE_EXPEDITION sur la table COMMANDE_CLIENT", ex2);
        }
        try {
            final SQLTable axeTable = root.getTable("AXE_ANALYTIQUE");
            if (!axeTable.contains("TYPE")) {
                final AlterTable a = new AlterTable(axeTable);
                a.addVarCharColumn("TYPE", 256);
                axeTable.getBase().getDataSource().execute(a.asString());
                axeTable.getSchema().updateVersion();
                axeTable.fetchFields();
            }
        } catch (SQLException ex2) {
            throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table AXE_ANALYTIQUE", ex2);
        }
        // Historique des taux
        if (!root.contains("DEVISE_HISTORIQUE")) {
            final SQLCreateTable createTable = new SQLCreateTable(root, "DEVISE_HISTORIQUE");
            createTable.addDateAndTimeColumn("DATE");
            createTable.addVarCharColumn("SRC", 8);
            createTable.addVarCharColumn("DST", 8);
            createTable.addDecimalColumn("TAUX", 12, 6, new BigDecimal(1), false);
            createTable.addDecimalColumn("TAUX_COMMERCIAL", 12, 6, new BigDecimal(1), false);
            try {
                root.getDBSystemRoot().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                tableDevis.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table TASK", ex);
            }
        }

        final SQLTable tableContact = root.getTable("CONTACT");
        if (tableContact != null && !tableContact.contains("NO_MAILING")) {
            AlterTable t = new AlterTable(tableContact);
            t.addBooleanColumn("NO_MAILING", false, false);
            tableContact.getBase().getDataSource().execute(t.asString());
            tableContact.getSchema().updateVersion();
            tableContact.fetchFields();
        }

        SQLTable tableBonR = root.getTable("FACTURE_FOURNISSEUR");
        if (!tableBonR.contains("TAUX_APPLIQUE")) {
            AlterTable t = new AlterTable(tableBonR);
            t.addDecimalColumn("TAUX_APPLIQUE", 12, 6, BigDecimal.ONE, true);
            tableBonR.getBase().getDataSource().execute(t.asString());
            tableBonR.getSchema().updateVersion();
            tableBonR.fetchFields();
        }
        root.getTable("ARTICLE").getSchema().updateVersion();
        // Valeur par défaut des numérotations
        SQLRowValues rNumerotation = root.getTable("NUMEROTATION_AUTO").getRow(2).asRowValues();
        boolean numerotationFixed = false;
        if (rNumerotation.getString("AVOIR_F_FORMAT").trim().isEmpty()) {
            rNumerotation.put("AVOIR_F_FORMAT", "'AVOIR'yyMM-000");
        }
        if (rNumerotation.getString("CLIENT_FORMAT").trim().isEmpty()) {
            rNumerotation.put("CLIENT_FORMAT", "'CLI'00000");
        }
        if (numerotationFixed) {
            rNumerotation.commit();
        }

        //

        final SQLTable tableCalendarItem = root.getTable("CALENDAR_ITEM");
        if (tableCalendarItem != null) {
            final AlterTable tVF = new AlterTable(tableCalendarItem);
            boolean needUpdate = false;
            if (!tableCalendarItem.getFieldsName().contains("UID")) {
                tVF.addVarCharColumn("UID", 1024);
                needUpdate = true;
            }
            if (!tableCalendarItem.getFieldsName().contains("LOCATION")) {
                tVF.addVarCharColumn("LOCATION", 4096);
                needUpdate = true;
            }
            if (needUpdate) {
                try {
                    tableCalendarItem.getBase().getDataSource().execute(tVF.asString());
                    tableCalendarItem.getSchema().updateVersion();
                    tableCalendarItem.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout de UID sur la table CALENDAR_ITEM", ex);
                }
            }
        }

    }

    private void updateStyle(final DBRoot root) throws SQLException {
        SQLTable style = root.getTable("STYLE");
        SQLRowValues rowVals = new SQLRowValues(style);
        rowVals.put("NOM", null);
        SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
        List<SQLRowValues> list = fetcher.fetch();
        boolean containsInvisible = false;
        for (SQLRowValues sqlRowValues : list) {
            if (sqlRowValues.getString("NOM").equals("Invisible")) {
                containsInvisible = true;
            }
        }
        if (!containsInvisible) {
            SQLRowValues rowValsStyle = new SQLRowValues(style);
            rowValsStyle.put("NOM", "Invisible");
            rowValsStyle.put("CODE", "INV");
            rowValsStyle.insert();
        }

    }

    private void updateToV1Dot2(final DBRoot root) throws SQLException {
        // bigint -> int ID_METRIQUE BON_DE_LIVRAISON_ELEMENT
        final SQLTable tableLivraisonElement = root.getTable("BON_DE_LIVRAISON_ELEMENT");
        AlterTable alter = new AlterTable(tableLivraisonElement);
        alter.alterColumn("ID_METRIQUE_2", EnumSet.of(Properties.TYPE), "integer", null, null);
        String req3 = alter.asString();
        root.getDBSystemRoot().getDataSource().execute(req3);

        final SQLTable tableDevis = root.getTable("DEVIS");
        final SQLDataSource ds = root.getDBSystemRoot().getDataSource();
        if (!tableDevis.getFieldsName().contains("DATE_VALIDITE")) {
            AlterTable t = new AlterTable(tableDevis);
            t.addColumn("DATE_VALIDITE", "date");
            try {
                ds.execute(t.asString());
                tableDevis.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champ DATE_VALIDITE à la table DEVIS", ex);
            }
        } else {
            AlterTable t = new AlterTable(tableDevis);
            t.alterColumn("DATE_VALIDITE", EnumSet.allOf(Properties.class), "date", null, true);
            try {
                ds.execute(t.asString());
                tableDevis.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champ DATE_VALIDITE à la table DEVIS", ex);
            }
        }

        final SQLTable tableEtatDevis = root.getTable("ETAT_DEVIS");
        if (tableEtatDevis.getRow(5) == null && tableEtatDevis.getRowCount() <= 4) {
            SQLRowValues rowVals = new SQLRowValues(tableEtatDevis);
            rowVals.put("NOM", "En cours de rédaction");
            rowVals.commit();

        }

        SQLRowValues rowValsOrdre = new SQLRowValues(tableEtatDevis);
        rowValsOrdre.put("ORDRE", new BigDecimal("1.505"));
        rowValsOrdre.update(EtatDevisSQLElement.EN_ATTENTE);

        rowValsOrdre.put("ORDRE", new BigDecimal("2.505"));
        rowValsOrdre.update(EtatDevisSQLElement.ACCEPTE);

        rowValsOrdre.put("ORDRE", new BigDecimal("3.505"));
        rowValsOrdre.update(EtatDevisSQLElement.REFUSE);

        rowValsOrdre.put("ORDRE", new BigDecimal("4.505"));
        rowValsOrdre.update(EtatDevisSQLElement.EN_COURS);

        // Ajout de la TVA à 0
        SQLSelect selTVA = new SQLSelect();
        SQLTable tableTaxe = root.getTable("TAXE");
        selTVA.addSelect(tableTaxe.getKey(), "COUNT");
        selTVA.setWhere(new Where(tableTaxe.getField("TAUX"), "=", 0));
        Object result = root.getBase().getDataSource().executeScalar(selTVA.asString());
        if (result == null || ((Number) result).longValue() == 0) {
            SQLRowValues rowVals = new SQLRowValues(tableTaxe);
            rowVals.put("NOM", "Non applicable");
            rowVals.put("TAUX", Float.valueOf(0));
            rowVals.commit();
        }

        // Bon de livraison
        {
            SQLTable tableBL = root.getTable("BON_DE_LIVRAISON");
            boolean alterBL = false;
            AlterTable t = new AlterTable(tableBL);
            if (!tableBL.getFieldsName().contains("SOURCE")) {
                t.addVarCharColumn("SOURCE", 512);
                alterBL = true;
            }
            if (!tableBL.getFieldsName().contains("IDSOURCE")) {
                t.addColumn("IDSOURCE", "integer DEFAULT 1");
                alterBL = true;
            }

            if (!tableBL.getFieldsName().contains("DATE_LIVRAISON")) {
                t.addColumn("DATE_LIVRAISON", "date");
                alterBL = true;
            }
            if (alterBL) {
                try {
                    ds.execute(t.asString());
                    tableBL.getSchema().updateVersion();
                    tableBL.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table BON_DE_LIVRAISON", ex);
                }
            }
        }

        // Fournisseur
        {
            SQLTable tableBL = root.getTable("FOURNISSEUR");
            boolean alterBL = false;
            AlterTable t = new AlterTable(tableBL);
            if (!tableBL.getFieldsName().contains("ID_COMPTE_PCE_CHARGE")) {
                t.addForeignColumn("ID_COMPTE_PCE_CHARGE", root.getTable("COMPTE_PCE"));
                alterBL = true;
            }
            if (alterBL) {
                try {
                    ds.execute(t.asString());
                    tableBL.getSchema().updateVersion();
                    tableBL.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table FOURNISSEUR", ex);
                }
            }
        }

        // Numérotation
        {
            SQLTable tableNum = root.getTable("NUMEROTATION_AUTO");
            boolean alterNum = false;
            AlterTable t = new AlterTable(tableNum);
            if (!tableNum.getFieldsName().contains("AVOIR_F_START")) {
                t.addColumn("AVOIR_F_START", "integer DEFAULT 0");
                alterNum = true;
            }
            if (!tableNum.getFieldsName().contains("AVOIR_F_FORMAT")) {
                t.addVarCharColumn("AVOIR_F_FORMAT", 48);
                alterNum = true;
            }

            if (alterNum) {
                try {
                    ds.execute(t.asString());
                    tableNum.getSchema().updateVersion();
                    tableNum.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table NUMEROTATION_AUTO", ex);
                }
            }
        }

        SQLTable tableArticle = root.getTable("ARTICLE");

        AlterTable t = new AlterTable(tableArticle);
        boolean alterArticle = false;
        if (!tableArticle.getFieldsName().contains("QTE_ACHAT")) {
            t.addColumn("QTE_ACHAT", "integer DEFAULT 1");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("DESCRIPTIF")) {
            t.addVarCharColumn("DESCRIPTIF", 2048);
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("CODE_BARRE")) {
            t.addVarCharColumn("CODE_BARRE", 256);
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("GESTION_STOCK")) {
            t.addColumn("GESTION_STOCK", "boolean DEFAULT true");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("CODE_DOUANIER")) {
            t.addVarCharColumn("CODE_DOUANIER", 256);
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("QTE_MIN")) {
            t.addColumn("QTE_MIN", "integer DEFAULT 1");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("ID_FOURNISSEUR")) {
            t.addForeignColumn("ID_FOURNISSEUR", root.findTable("FOURNISSEUR", true));
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("PV_U_DEVISE")) {
            t.addColumn("PV_U_DEVISE", "bigint default 0");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("ID_DEVISE_HA")) {
            t.addForeignColumn("ID_DEVISE_HA", root.findTable("DEVISE", true));
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("PA_DEVISE")) {
            t.addColumn("PA_DEVISE", "bigint default 0");
            alterArticle = true;
        }
        if (!tableArticle.getFieldsName().contains("ID_PAYS")) {
            t.addForeignColumn("ID_PAYS", root.findTable("PAYS", true));
            alterArticle = true;
        }
        if (alterArticle) {
            try {
                ds.execute(t.asString());
                tableArticle.getSchema().updateVersion();
                tableArticle.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table ARTICLE", ex);
            }
        }

        // Création de la table Langue
        boolean refetchRoot = false;
        if (!root.contains("OBJECTIF_COMMERCIAL")) {

            SQLCreateTable createObjectif = new SQLCreateTable(root, "OBJECTIF_COMMERCIAL");
            createObjectif.addVarCharColumn("MOIS", 32);
            createObjectif.addColumn("ANNEE", "integer");
            createObjectif.addColumn("MARGE_HT", "bigint DEFAULT 0");
            createObjectif.addColumn("POURCENT_MARGE", "numeric (16,8)");
            createObjectif.addColumn("CHIFFRE_AFFAIRE", "bigint DeFAULT 0");
            createObjectif.addForeignColumn("COMMERCIAL");
            try {
                ds.execute(createObjectif.asString());
                insertUndef(createObjectif);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table OBJECTIF_COMMERCIAL", ex);
            }

        }

        if (!root.contains("LANGUE")) {

            SQLCreateTable createLangue = new SQLCreateTable(root, "LANGUE");
            createLangue.addVarCharColumn("CODE", 256);
            createLangue.addVarCharColumn("NOM", 256);
            createLangue.addVarCharColumn("CHEMIN", 256);
            try {
                ds.execute(createLangue.asString());
                insertUndef(createLangue);
                tableDevis.getSchema().updateVersion();
                root.refetchTable(createLangue.getName());
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table LANGUE", ex);
            }

            final String[] langs = new String[] { "FR", "Français", "EN", "Anglais", "SP", "Espagnol", "DE", "Allemand", "NL", "Néerlandais", "IT", "Italien" };
            // ('FR', 'Français', 1.000), ('EN', 'Anglais', 2.000)
            final List<String> values = new ArrayList<String>();
            final SQLBase base = root.getBase();
            for (int i = 0; i < langs.length; i += 2) {
                final int order = values.size() + 1;
                values.add("(" + base.quoteString(langs[i]) + ", " + base.quoteString(langs[i + 1]) + ", " + order + ")");
            }
            final String valuesStr = CollectionUtils.join(values, ", ");
            final String insertVals = "INSERT INTO " + getTableName(createLangue).quote() + "(" + SQLBase.quoteIdentifier("CODE") + ", " + SQLBase.quoteIdentifier("NOM") + ", "
                    + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") VALUES" + valuesStr;
            ds.execute(insertVals);
        }

        // Création de la table Modéle
        if (!root.contains("MODELE")) {

            SQLCreateTable createModele = new SQLCreateTable(root, "MODELE");
            createModele.addVarCharColumn("NOM", 256);
            createModele.addForeignColumn("ID_TYPE_MODELE", root.findTable("TYPE_MODELE", true));
            try {
                ds.execute(createModele.asString());
                insertUndef(createModele);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table MODELE", ex);
            }
        }

        // Création de la table Modéle
        if (!root.contains("CONTACT_FOURNISSEUR")) {

            SQLCreateTable createModele = new SQLCreateTable(root, "CONTACT_FOURNISSEUR");
            createModele.addVarCharColumn("NOM", 256);
            createModele.addVarCharColumn("PRENOM", 256);
            createModele.addVarCharColumn("TEL_DIRECT", 256);
            createModele.addVarCharColumn("TEL_MOBILE", 256);
            createModele.addVarCharColumn("EMAIL", 256);
            createModele.addVarCharColumn("FAX", 256);
            createModele.addVarCharColumn("FONCTION", 256);
            createModele.addVarCharColumn("TEL_PERSONEL", 256);
            createModele.addVarCharColumn("TEL_STANDARD", 256);
            createModele.addForeignColumn("ID_TITRE_PERSONNEL", root.findTable("TITRE_PERSONNEL"));
            createModele.addForeignColumn("ID_FOURNISSEUR", root.findTable("FOURNISSEUR"));

            try {
                ds.execute(createModele.asString());
                insertUndef(createModele);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table MODELE", ex);
            }
        }

        // Création de la table Tarif
        if (!root.contains("TARIF")) {

            SQLCreateTable createTarif = new SQLCreateTable(root, "TARIF");
            createTarif.addVarCharColumn("NOM", 256);
            createTarif.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            createTarif.addForeignColumn("ID_TAXE", root.findTable("TAXE", true));
            createTarif.asString();
            try {
                ds.execute(createTarif.asString());
                insertUndef(createTarif);
                tableDevis.getSchema().updateVersion();
                root.refetchTable(createTarif.getName());
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table TARIF", ex);
            }
        }

        // Création de la table article Tarif
        if (!root.contains("ARTICLE_TARIF")) {

            SQLCreateTable createTarif = new SQLCreateTable(root, "ARTICLE_TARIF");
            createTarif.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            createTarif.addForeignColumn("ID_TAXE", root.findTable("TAXE", true));
            createTarif.addForeignColumn("ID_TARIF", root.findTable("TARIF", true));
            createTarif.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));
            createTarif.addColumn("PV_HT", "bigint DEFAULT 0");
            createTarif.addColumn("PV_TTC", "bigint DEFAULT 0");
            createTarif.addColumn("PRIX_METRIQUE_VT_1", "bigint DEFAULT 0");
            createTarif.addColumn("PRIX_METRIQUE_VT_2", "bigint DEFAULT 0");
            createTarif.addColumn("PRIX_METRIQUE_VT_3", "bigint DEFAULT 0");
            createTarif.asString();
            try {
                ds.execute(createTarif.asString());
                insertUndef(createTarif);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table ARTICLE_TARIF", ex);
            }
        }

        // Création de la table article Désignation
        if (!root.contains("ARTICLE_DESIGNATION")) {

            SQLCreateTable createTarif = new SQLCreateTable(root, "ARTICLE_DESIGNATION");
            createTarif.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));
            createTarif.addForeignColumn("ID_LANGUE", root.findTable("LANGUE", true));
            createTarif.addVarCharColumn("NOM", 1024);
            createTarif.asString();
            try {
                ds.execute(createTarif.asString());
                insertUndef(createTarif);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table ARTICLE_DESIGNATION", ex);
            }
        }

        if (!root.contains("UNITE_VENTE")) {

            SQLCreateTable createUnite = new SQLCreateTable(root, "UNITE_VENTE");
            createUnite.addVarCharColumn("CODE", 32);
            createUnite.addVarCharColumn("NOM", 256);
            createUnite.addColumn("A_LA_PIECE", "boolean DEFAULT false");
            createUnite.addVarCharColumn("INFOS", 256);
            try {
                ds.execute(createUnite.asString());
                insertUndef(createUnite);
                final String insert = "INSERT into " + getTableName(createUnite).quote()
                        + "(\"CODE\",\"NOM\",\"A_LA_PIECE\",\"ORDRE\") VALUES('pièce','à la pièce',true,1),('m','mètres',false,2),('m²','mètres carré',false,3),('m3','mètres cube',false,4),('l','litres',false,5),('kg','kilos',false,6),('h','heures',false,7),('j','jours',false,8),('mois','mois',false,9)";
                root.getDBSystemRoot().getDataSource().execute(insert);
                tableDevis.getSchema().updateVersion();
                refetchRoot = true;
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table UNITE_VENTE", ex);
            }

        }

        // Chargement des tables fraichement créées
        if (refetchRoot)
            root.refetch();

        if (!tableArticle.getFieldsName().contains("ID_UNITE_VENTE")) {
            AlterTable alterTableArticle = new AlterTable(tableArticle);
            alterTableArticle.addForeignColumn("ID_UNITE_VENTE", root.findTable("UNITE_VENTE", true).getSQLName(), "ID", "2");
            try {
                ds.execute(alterTableArticle.asString());
                tableArticle.getSchema().updateVersion();
                tableArticle.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champ UNITE_VENTE sur la table ARTICLE", ex);
            }
        }

        SQLTable tableVFElt = root.getTable("SAISIE_VENTE_FACTURE_ELEMENT");
        addVenteEltField(tableVFElt, root);

        SQLTable tableDevisElt = root.getTable("DEVIS_ELEMENT");
        addVenteEltField(tableDevisElt, root);

        SQLTable tableCmdElt = root.getTable("COMMANDE_CLIENT_ELEMENT");
        addVenteEltField(tableCmdElt, root);

        SQLTable tableBonElt = root.getTable("BON_DE_LIVRAISON_ELEMENT");
        addVenteEltField(tableBonElt, root);

        SQLTable tableAvoirElt = root.getTable("AVOIR_CLIENT_ELEMENT");
        addVenteEltField(tableAvoirElt, root);

        SQLTable tableCmdFournElt = root.getTable("COMMANDE_ELEMENT");
        addHAElementField(tableCmdFournElt, root);
        if (root.contains("DEMANDE_PRIX_ELEMENT")) {
            SQLTable tableDmdFournElt = root.getTable("DEMANDE_PRIX_ELEMENT");
            addHAElementField(tableDmdFournElt, root);
        }

        SQLTable tableBonRecptElt = root.getTable("BON_RECEPTION_ELEMENT");
        addHAElementField(tableBonRecptElt, root);

        SQLTable tableBonRecpt = root.getTable("BON_RECEPTION");
        addDeviseHAField(tableBonRecpt, root);

        SQLTable tableCommande = root.getTable("COMMANDE");
        addDeviseHAField(tableCommande, root);

        patchFieldElt1Dot3(root.getTable("ARTICLE"), root);
        patchFieldElt1Dot3(root.getTable("ARTICLE_TARIF"), root);

        if (!tableCommande.getFieldsName().contains("ID_ADRESSE")) {
            AlterTable alterCmd = new AlterTable(tableCommande);
            alterCmd.addForeignColumn("ID_ADRESSE", root.findTable("ADRESSE", true));
            try {
                ds.execute(alterCmd.asString());
                tableCommande.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table COMMANDE", ex);
            }

        }
        if (!tableCommande.getFieldsName().contains("ID_CLIENT")) {
            AlterTable alterCmd = new AlterTable(tableCommande);
            alterCmd.addForeignColumn("ID_CLIENT", root.findTable("CLIENT"));
            try {
                ds.execute(alterCmd.asString());
                tableCommande.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table COMMANDE", ex);
            }

        }

        {
            addTotalDeviseField(tableDevis, root);
            addModeleField(tableDevis, root);

            SQLTable tableVF = root.getTable("SAISIE_VENTE_FACTURE");
            addTotalDeviseField(tableVF, root);
            addModeleField(tableVF, root);

            addTotalDeviseField(tableDevis, root);
            addModeleField(tableDevis, root);

            SQLTable tableCmd = root.getTable("COMMANDE_CLIENT");
            addTotalDeviseField(tableCmd, root);
            addModeleField(tableCmd, root);

            SQLTable tableBon = root.getTable("BON_DE_LIVRAISON");
            addTotalDeviseField(tableBon, root);
            addModeleField(tableBon, root);

            SQLTable tableAvoir = root.getTable("AVOIR_CLIENT");
            addTotalDeviseField(tableAvoir, root);
            addModeleField(tableAvoir, root);
        }
        // Change client
        {
            SQLTable tableClient = root.getTable("CLIENT");

            AlterTable tClient = new AlterTable(tableClient);
            boolean alterClient = false;

            if (!tableClient.getFieldsName().contains("ID_TARIF")) {
                tClient.addForeignColumn("ID_TARIF", root.findTable("TARIF", true));
                alterClient = true;
            }
            if (!tableClient.getFieldsName().contains("ID_PAYS")) {
                tClient.addForeignColumn("ID_PAYS", root.findTable("PAYS", true));
                alterClient = true;
            }
            if (!tableClient.getFieldsName().contains("ID_LANGUE")) {
                tClient.addForeignColumn("ID_LANGUE", root.findTable("LANGUE", true));
                alterClient = true;
            }

            if (!tableClient.getFieldsName().contains("ID_DEVISE")) {
                tClient.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
                alterClient = true;
            }
            if (alterClient) {
                try {
                    ds.execute(tClient.asString());
                    tableClient.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table CLIENT", ex);
                }
            }
        }

        // Change Pays
        {
            SQLTable tablePays = root.getTable("PAYS");

            AlterTable tPays = new AlterTable(tablePays);
            boolean alterPays = false;

            if (!tablePays.getFieldsName().contains("ID_TARIF")) {
                tPays.addForeignColumn("ID_TARIF", root.findTable("TARIF", true));
                alterPays = true;
            }
            if (!tablePays.getFieldsName().contains("ID_LANGUE")) {
                tPays.addForeignColumn("ID_LANGUE", root.findTable("LANGUE", true));
                alterPays = true;
            }
            if (alterPays) {
                try {
                    ds.execute(tPays.asString());
                    tablePays.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table PAYS", ex);
                }
            }
        }
        // Change Commande
        {
            SQLTable tableCmd = root.getTable("COMMANDE");

            AlterTable tCmd = new AlterTable(tableCmd);
            boolean alterCmd = false;

            if (!tableCmd.getFieldsName().contains("EN_COURS")) {
                tCmd.addColumn("EN_COURS", "boolean default true");
                alterCmd = true;
            }
            if (alterCmd) {
                try {
                    ds.execute(tCmd.asString());
                    tableCmd.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table COMMANDE", ex);
                }
            }
        }

        // Change VF
        {
            SQLTable tableVenteFacture = root.getTable("SAISIE_VENTE_FACTURE");

            AlterTable tVF = new AlterTable(tableVenteFacture);
            boolean alterVF = false;

            if (!tableVenteFacture.getFieldsName().contains("ID_TAXE_PORT")) {
                tVF.addForeignColumn("ID_TAXE_PORT", root.findTable("TAXE"));
                alterVF = true;
            }
            if (alterVF) {
                try {
                    ds.execute(tVF.asString());
                    tableVenteFacture.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table SAISIE_VENTE_FACTURE", ex);
                }
            }
        }

        // Change Fournisseur
        {
            SQLTable tableFournisseur = root.getTable("FOURNISSEUR");

            AlterTable tFourn = new AlterTable(tableFournisseur);
            boolean alterFourn = false;

            if (!tableFournisseur.getFieldsName().contains("ID_LANGUE")) {
                tFourn.addForeignColumn("ID_LANGUE", root.findTable("LANGUE", true));
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("ID_DEVISE")) {
                tFourn.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("RESPONSABLE")) {
                tFourn.addVarCharColumn("RESPONSABLE", 256);
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("TEL_P")) {
                tFourn.addVarCharColumn("TEL_P", 256);
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("MAIL")) {
                tFourn.addVarCharColumn("MAIL", 256);
                alterFourn = true;
            }
            if (!tableFournisseur.getFieldsName().contains("INFOS")) {
                tFourn.addVarCharColumn("INFOS", 2048);
                alterFourn = true;
            }

            if (alterFourn) {
                try {
                    ds.execute(tFourn.asString());
                    tableFournisseur.getSchema().updateVersion();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs sur la table FOURNISSEUR", ex);
                }
            }
        }

        updateN4DS(root);

        root.refetch();
    }

    /**
     * Mise à jour du schéma pour N4DS
     * 
     * @param root
     * @throws SQLException
     */
    private void updateN4DS(DBRoot root) throws SQLException {

        {
            SQLTable table = root.findTable("INFOS_SALARIE_PAYE");
            boolean alter = false;
            AlterTable t = new AlterTable(table);
            if (!table.getFieldsName().contains("CODE_AT")) {
                t.addVarCharColumn("CODE_AT", 18);
                alter = true;
            }
            if (!table.getFieldsName().contains("CODE_SECTION_AT")) {
                t.addVarCharColumn("CODE_SECTION_AT", 18);
                alter = true;
            }

            if (alter) {
                try {
                    table.getBase().getDataSource().execute(t.asString());
                    table.getSchema().updateVersion();
                    table.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
                }
            }

        }

        if (!root.contains("CODE_STATUT_CAT_CONV")) {

            SQLCreateTable createTarif = new SQLCreateTable(root, "CODE_STATUT_CAT_CONV");

            createTarif.addVarCharColumn("CODE", 6);
            createTarif.addVarCharColumn("NOM", 256);
            createTarif.asString();
            try {
                root.getBase().getDataSource().execute(createTarif.asString());
                insertUndef(createTarif);

                String insert = "INSERT into " + getTableName(createTarif).quote() + "(\"CODE\",\"NOM\") VALUES ";
                insert += " ('01','agriculteur salarié de son exploitation')";
                insert += ", ('02','artisan ou commerçant salarié de son entreprise')";
                insert += ", ('03','cadre dirigeant (votant au collège employeur des élections prud''''hommales)')";
                insert += ", ('04','autres cadres au sens de la convention collective (ou du statut pour les régimes spéciaux)')";
                insert += ", ('05','profession intermédiaire (technicien, contremaître, agent de maîtrise, clergé)')";
                insert += ", ('06','employé administratif d''''entreprise, de commerce, agent de service')";
                insert += ", ('07','ouvriers qualifiés et non qualifiés y compris ouvriers agricoles');";
                createTarif.getRoot().getDBSystemRoot().getDataSource().execute(insert);

                root.getSchema().updateVersion();
                root.refetch();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table CODE_STATUT_CAT_CONV", ex);
            }
        }

        // Création de la table Modéle
        if (!root.contains("CONTACT_ADMINISTRATIF")) {

            SQLCreateTable createModele = new SQLCreateTable(root, "CONTACT_ADMINISTRATIF");
            createModele.addVarCharColumn("NOM", 256);
            createModele.addVarCharColumn("PRENOM", 256);
            createModele.addVarCharColumn("TEL_DIRECT", 256);
            createModele.addVarCharColumn("TEL_MOBILE", 256);
            createModele.addVarCharColumn("EMAIL", 256);
            createModele.addVarCharColumn("FAX", 256);
            createModele.addVarCharColumn("FONCTION", 256);
            createModele.addVarCharColumn("TEL_PERSONEL", 256);
            createModele.addVarCharColumn("TEL_STANDARD", 256);
            createModele.addForeignColumn("ID_TITRE_PERSONNEL", root.findTable("TITRE_PERSONNEL"));
            createModele.addColumn("N4DS", "boolean DEFAULT false");

            try {
                root.getBase().getDataSource().execute(createModele.asString());
                insertUndef(createModele);
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table MODELE", ex);
            }
        }

        {
            SQLTable tableContrat = root.findTable("CONTRAT_SALARIE", true);
            boolean alter2 = false;
            AlterTable t2 = new AlterTable(tableContrat);
            // UGRR
            if (!tableContrat.getFieldsName().contains("CODE_IRC_UGRR")) {
                t2.addVarCharColumn("CODE_IRC_UGRR", 18);
                alter2 = true;
            }
            if (!tableContrat.getFieldsName().contains("NUMERO_RATTACHEMENT_UGRR")) {
                t2.addVarCharColumn("NUMERO_RATTACHEMENT_UGRR", 64);
                alter2 = true;
            }
            // UGRC
            if (!tableContrat.getFieldsName().contains("CODE_IRC_UGRC")) {
                t2.addVarCharColumn("CODE_IRC_UGRC", 18);
                alter2 = true;
            }
            if (!tableContrat.getFieldsName().contains("NUMERO_RATTACHEMENT_UGRC")) {
                t2.addVarCharColumn("NUMERO_RATTACHEMENT_UGRC", 64);
                alter2 = true;
            }

            // Retraite Compl
            if (!tableContrat.getFieldsName().contains("CODE_IRC_RETRAITE")) {
                t2.addVarCharColumn("CODE_IRC_RETRAITE", 18);
                alter2 = true;
            }
            if (!tableContrat.getFieldsName().contains("NUMERO_RATTACHEMENT_RETRAITE")) {
                t2.addVarCharColumn("NUMERO_RATTACHEMENT_RETRAITE", 64);
                alter2 = true;
            }

            if (!tableContrat.getFieldsName().contains("ID_CODE_STATUT_CAT_CONV")) {
                t2.addForeignColumn("ID_CODE_STATUT_CAT_CONV", root.findTable("CODE_STATUT_CAT_CONV", true));
                alter2 = true;
            }

            if (alter2) {
                try {
                    tableContrat.getBase().getDataSource().execute(t2.asString());
                    tableContrat.getSchema().updateVersion();
                    tableContrat.fetchFields();
                } catch (SQLException ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + tableContrat.getName(), ex);
                }
            }

        }
    }

    private void addDeviseHAField(SQLTable table, DBRoot root) throws SQLException {
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            alter = true;
        }

        if (!table.getFieldsName().contains("T_DEVISE")) {
            t.addColumn("T_DEVISE", "bigint default 0");
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }

    }

    private void patchFieldElt1Dot3(SQLTable table, DBRoot root) {

        List<String> cols = Arrays.asList("PV_HT", "PA_DEVISE_T", "T_PV_HT", "T_PA_TTC", "T_PA_HT", "PA_HT", "T_PV_TTC", "PRIX_METRIQUE_HA_2", "PRIX_METRIQUE_HA_1", "PRIX_METRIQUE_HA_3",
                "PRIX_METRIQUE_VT_2", "PRIX_METRIQUE_VT_1", "MONTANT_HT", "MONTANT_INITIAL", "PRIX_METRIQUE_VT_3", "MARGE_HT", "PA_DEVISE", "PV_U_DEVISE", "PV_T_DEVISE", "PV_TTC", "TARIF_Q18_HT",
                "T_PRIX_FINAL_TTC", "PRIX_FINAL_TTC", "PV_UNIT_HT", "PREBILAN", "MARGE_PREBILAN_HT");

        if ((table.contains("PV_HT") && table.getField("PV_HT").getType().getDecimalDigits() == 0)
                || (table.contains("PV_UNIT_HT") && table.getField("PV_UNIT_HT").getType().getDecimalDigits() == 0)) {
            AlterTable t = new AlterTable(table);
            UpdateBuilder builder = new UpdateBuilder(table);
            List<UpdateBuilder> builds = new ArrayList<UpdateBuilder>();
            for (String field : cols) {
                if (table.contains(field)) {
                    UpdateBuilder builderNonNull = new UpdateBuilder(table);
                    builderNonNull.set(field, "0");
                    builderNonNull.setWhere(new Where(table.getField(field), "=", (Object) null));
                    builds.add(builderNonNull);

                    builder.set(field, table.getField(field).getSQLName().getRest().quote() + "/100");
                    if (field.contains("TTC")) {
                        t.alterColumn(field, EnumSet.allOf(Properties.class), "numeric(16,2)", "0", false);
                    } else {
                        t.alterColumn(field, EnumSet.allOf(Properties.class), "numeric(16,6)", "0", false);
                    }
                }
            }

            try {

                for (UpdateBuilder updateBuilder : builds) {
                    table.getBase().getDataSource().execute(updateBuilder.asString());
                }

                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }

            String req2 = builder.asString();
            root.getDBSystemRoot().getDataSource().execute(req2);
        }

    }

    private void patchFieldElt1Dot4(SQLTable table, DBRoot root) throws SQLException {

        if (!table.contains("MONTANT_REMISE")) {
            AlterTable t = new AlterTable(table);
            t.alterColumn("POURCENT_REMISE", EnumSet.allOf(Properties.class), "numeric(12,8)", "0", true);
            t.addColumn("MONTANT_REMISE", "numeric (16,8)");

            try {

                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }

        if (!table.contains("ID_FAMILLE_ARTICLE")) {
            AlterTable t = new AlterTable(table);
            t.addForeignColumn("ID_FAMILLE_ARTICLE", root.getTable("FAMILLE_ARTICLE"));

            try {

                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du champs ID_FAMILLE_ARTICLE à la table " + table.getName(), ex);
            }
        }

        addNiveauElementField(table);
    }

    private void addNiveauElementField(SQLTable table) throws SQLException {

        if (!table.contains("NIVEAU")) {
            AlterTable t = new AlterTable(table);
            t.addIntegerColumn("NIVEAU", 1);
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout du niveau à la table " + table.getName(), ex);
            }
        }

    }

    private void addHAElementField(SQLTable table, DBRoot root) throws SQLException {

        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("QTE_ACHAT")) {
            t.addColumn("QTE_ACHAT", "integer DEFAULT 1");
            alter = true;
        }
        if (!table.getFieldsName().contains("QTE_UNITAIRE")) {
            t.addColumn("QTE_UNITAIRE", "numeric(16,6) DEFAULT 1");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_UNITE_VENTE")) {
            t.addForeignColumn("ID_UNITE_VENTE", root.findTable("UNITE_VENTE", true).getSQLName(), "ID", "2");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_ARTICLE")) {
            t.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));
            alter = true;
        }
        if (!table.getFieldsName().contains("PA_DEVISE")) {
            t.addColumn("PA_DEVISE", "bigint default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            alter = true;
        }

        if (!table.getFieldsName().contains("PA_DEVISE_T")) {
            t.addColumn("PA_DEVISE_T", "bigint default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("DESCRIPTIF")) {
            t.addVarCharColumn("DESCRIPTIF", 2048);
            alter = true;
        }

        // if (!table.getFieldsName().contains("POURCENT_REMISE")) {
        // t.addColumn("POURCENT_REMISE", "numeric(16,2) DEFAULT 0");
        // alter = true;
        // }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }
        patchFieldElt1Dot3(table, root);
    }

    private void addRemiseField(SQLTable table, DBRoot root) throws SQLException {

        AlterTable t = new AlterTable(table);
        boolean alter = false;
        if (!table.contains("POURCENT_REMISE")) {
            t.addColumn("POURCENT_REMISE", "numeric (12,8)");
            alter = true;
        }
        if (!table.contains("MONTANT_REMISE")) {
            t.addColumn("MONTANT_REMISE", "numeric (16,8)");
            alter = true;
        }
        if (alter) {
            try {

                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs remise à la table " + table.getName(), ex);
            }
        }
    }

    private void addAdresseField(SQLTable table, DBRoot root) throws SQLException {
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("ID_ADRESSE")) {
            t.addForeignColumn("ID_ADRESSE", root.findTable("ADRESSE"));
            alter = true;
        }

        if (!table.getFieldsName().contains("ID_ADRESSE_LIVRAISON")) {
            t.addForeignColumn("ID_ADRESSE_LIVRAISON", root.findTable("ADRESSE"));
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }
    }

    private void addModeleField(SQLTable table, DBRoot root) throws SQLException {
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("ID_MODELE")) {
            t.addForeignColumn("ID_MODELE", root.findTable("MODELE"));
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }
    }

    private void addTotalDeviseField(SQLTable table, DBRoot root) throws SQLException {
        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("T_DEVISE")) {
            t.addColumn("T_DEVISE", "bigint default 0");
            alter = true;
        } else {
            table.getBase().getDataSource().execute("UPDATE " + table.getSQLName().quote() + " SET \"T_DEVISE\"=0 WHERE \"T_DEVISE\" IS NULL");
            t.alterColumn("T_DEVISE", EnumSet.allOf(Properties.class), "bigint", "0", false);
        }
        if (!table.getFieldsName().contains("T_POIDS")) {
            t.addColumn("T_POIDS", "real default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_TARIF")) {
            t.addForeignColumn("ID_TARIF", root.findTable("TARIF", true));
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }
    }

    private void addVenteEltField(SQLTable table, DBRoot root) throws SQLException {

        boolean alter = false;
        AlterTable t = new AlterTable(table);
        if (!table.getFieldsName().contains("QTE_ACHAT")) {
            t.addColumn("QTE_ACHAT", "integer DEFAULT 1");
            alter = true;
        }
        if (!table.getFieldsName().contains("QTE_UNITAIRE")) {
            t.addColumn("QTE_UNITAIRE", "numeric(16,6) DEFAULT 1");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_UNITE_VENTE")) {
            t.addForeignColumn("ID_UNITE_VENTE", root.findTable("UNITE_VENTE", true).getSQLName(), "ID", "2");
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_ARTICLE")) {
            t.addForeignColumn("ID_ARTICLE", root.findTable("ARTICLE", true));
            alter = true;
        }
        if (!table.getFieldsName().contains("CODE_DOUANIER")) {
            t.addVarCharColumn("CODE_DOUANIER", 256);
            alter = true;
        }
        if (!table.getFieldsName().contains("DESCRIPTIF")) {
            t.addVarCharColumn("DESCRIPTIF", 2048);
            alter = true;
        }
        if (!table.getFieldsName().contains("ID_PAYS")) {
            t.addForeignColumn("ID_PAYS", root.findTable("PAYS", true));
            alter = true;
        }
        if (!table.getFieldsName().contains("MARGE_HT")) {
            t.addColumn("MARGE_HT", "bigint default 0");
            alter = true;
        }

        if (!table.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.findTable("DEVISE", true));
            alter = true;
        }
        if (!table.getFieldsName().contains("PV_U_DEVISE")) {
            t.addColumn("PV_U_DEVISE", "bigint default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("POURCENT_REMISE")) {
            t.addColumn("POURCENT_REMISE", "numeric(6,2) default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("PV_T_DEVISE")) {
            t.addColumn("PV_T_DEVISE", "bigint default 0");
            alter = true;
        }
        if (!table.getFieldsName().contains("TAUX_DEVISE")) {
            t.addColumn("TAUX_DEVISE", "numeric (16,8) DEFAULT 1");
            alter = true;
        }

        if (!table.getFieldsName().contains("POIDS_COLIS_NET")) {
            t.addColumn("POIDS_COLIS_NET", "numeric (16,8) DEFAULT 1");
            alter = true;
        }

        if (!table.getFieldsName().contains("T_POIDS_COLIS_NET")) {
            t.addColumn("T_POIDS_COLIS_NET", "numeric (16,8) DEFAULT 1");
            alter = true;
        }

        if (!table.getFieldsName().contains("NB_COLIS")) {
            t.addColumn("NB_COLIS", "integer DEFAULT 0");
            alter = true;
        }
        if (alter) {
            try {
                root.getDBSystemRoot().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }
        patchFieldElt1Dot3(table, root);
    }

    private void updateSocieteSchema(final DBRoot root) throws IOException, Exception {
        final DBSystemRoot sysRoot = root.getDBSystemRoot();
        final SQLDataSource ds = sysRoot.getDataSource();
        System.out.println("InstallationPanel.InstallationPanel() UPDATE COMMERCIAL " + root);
        // Fix commercial Ordre

        SQLTable tableCommercial = root.getTable("COMMERCIAL");
        CorrectOrder orderCorrect = new CorrectOrder(sysRoot);
        orderCorrect.change(tableCommercial);

        // new AddFK(sysRoot).changeAll(root);
        sysRoot.reload(Collections.singleton(root.getName()));

        try {
            // Add article
            final SQLTable tableArticle = root.getTable("ARTICLE");
            if (!tableArticle.getFieldsName().contains("INFOS")) {
                AlterTable t = new AlterTable(tableArticle);
                t.addVarCharColumn("INFOS", 2048);
                try {
                    ds.execute(t.asString());
                } catch (Exception ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout du champ INFO à la table ARTICLE", ex);
                }
            }

            if (sysRoot.getServer().getSQLSystem().equals(SQLSystem.POSTGRESQL)) {
                // Fix Caisse serial
                SQLTable tableCaisse = root.getTable("CAISSE");

                FixSerial f = new FixSerial(sysRoot);
                try {
                    f.change(tableCaisse);
                } catch (SQLException e2) {
                    throw new IllegalStateException("Erreur lors la mise à jours des sequences de la table CAISSE", e2);
                }
            }
            System.out.println("InstallationPanel.InstallationPanel() UPDATE TICKET_CAISSE " + root);
            // add Mvt on Ticket
            SQLTable tableTicket = root.getTable("TICKET_CAISSE");
            if (!tableTicket.getFieldsName().contains("ID_MOUVEMENT")) {
                AlterTable t = new AlterTable(tableTicket);
                t.addForeignColumn("ID_MOUVEMENT", root.getTable("MOUVEMENT"));
                try {
                    ds.execute(t.asString());
                } catch (Exception ex) {
                    throw new IllegalStateException("Erreur lors de l'ajout du champ ID_MOUVEMENT à la table TICKET_CAISSE", ex);
                }
            }

            // Check type de reglement

            System.out.println("InstallationPanel.InstallationPanel() UPDATE TYPE_REGLEMENT " + root);
            SQLTable tableReglmt = root.getTable("TYPE_REGLEMENT");
            SQLSelect sel = new SQLSelect(tableReglmt.getBase());
            sel.addSelect(tableReglmt.getKey());
            sel.setWhere(new Where(tableReglmt.getField("NOM"), "=", "Virement"));
            List<Number> l = (List<Number>) ds.executeCol(sel.asString());
            if (l.size() == 0) {
                SQLRowValues rowVals = new SQLRowValues(tableReglmt);
                rowVals.put("NOM", "Virement");
                rowVals.put("COMPTANT", Boolean.FALSE);
                rowVals.put("ECHEANCE", Boolean.FALSE);
                try {
                    rowVals.commit();
                } catch (SQLException e) {
                    throw new IllegalStateException("Erreur lors de l'ajout du type de paiement par virement", e);
                }
            }

            SQLSelect sel2 = new SQLSelect();
            sel2.addSelect(tableReglmt.getKey());
            sel2.setWhere(new Where(tableReglmt.getField("NOM"), "=", "CESU"));
            @SuppressWarnings("unchecked")
            List<Number> l2 = (List<Number>) ds.executeCol(sel2.asString());
            if (l2.size() == 0) {
                SQLRowValues rowVals = new SQLRowValues(tableReglmt);
                rowVals.put("NOM", "CESU");
                rowVals.put("COMPTANT", Boolean.FALSE);
                rowVals.put("ECHEANCE", Boolean.FALSE);
                try {
                    rowVals.commit();
                } catch (SQLException e) {
                    throw new IllegalStateException("Erreur lors de l'ajout du type CESU", e);
                }
            }
            System.out.println("InstallationPanel.InstallationPanel() UPDATE FAMILLE_ARTICLE " + root);
            //
            final SQLTable tableFam = root.getTable("FAMILLE_ARTICLE");
            final int nomSize = 256;
            if (tableFam.getField("NOM").getType().getSize() < nomSize) {
                final AlterTable t = new AlterTable(tableFam);
                t.alterColumn("NOM", EnumSet.allOf(Properties.class), "varchar(" + nomSize + ")", "''", false);
                try {
                    ds.execute(t.asString());
                } catch (Exception ex) {
                    throw new IllegalStateException("Erreur lors de la modification du champs NOM sur la table FAMILLE_ARTICLE", ex);
                }
            }

            // Suppression des champs 1.0
            System.out.println("InstallationPanel.InstallationPanel() UPDATE FROM 1.0 " + root);
            final List<ChangeTable<?>> changes = new ArrayList<ChangeTable<?>>();

            final ModuleManager instance = new ModuleManager();
            instance.setRoot(root);
            final Collection<ModuleReference> refs = instance.getModulesInstalledRemotely();
            final Set<String> allUsedTable = new HashSet<String>();
            for (ModuleReference ref : refs) {
                Set<String> tableNames = instance.getCreatedItems(ref.getID()).get0();
                allUsedTable.addAll(tableNames);
            }
            System.out.println("Tables created by modules:" + allUsedTable);

            final List<String> alterRequests = ChangeTable.cat(changes, root.getName());
            try {
                for (final String req : alterRequests) {
                    ds.execute(req);
                }
            } catch (Exception e1) {
                throw new IllegalStateException("Erreur lors de la mise à jour des tables v1.0", e1);
            }
            System.out.println("InstallationPanel.InstallationPanel() UPDATE CAISSE " + root);
            // Undefined
            try {
                SQLTable.setUndefID(tableTicket.getSchema(), tableTicket.getName(), 1);
                SQLTable.setUndefID(tableTicket.getSchema(), "CAISSE", 1);
            } catch (SQLException e1) {
                throw new IllegalStateException("Erreur lors de la mise à jour des indéfinis de la table CAISSE", e1);
            }
        } catch (Exception e) {
            ExceptionHandler.handle("updateSocieteSchema on root " + root + " failed", e);
        } finally {
            // Mise à jour du schéma
            root.getSchema().updateVersion();
            root.refetch();
        }
    }

    private void checkRights(DBRoot root) throws SQLException {
        SQLTable table = root.findTable("RIGHT");
        SQLSelect sel = new SQLSelect();
        sel.addSelect(table.getKey());
        sel.addSelect(table.getField("CODE"));
        List<SQLRow> rows = SQLRowListRSH.execute(sel);
        Set<String> codes = new HashSet<String>();
        for (SQLRow row : rows) {
            codes.add(row.getString("CODE"));
        }

        if (!codes.contains("UPDATE_ROW")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "UPDATE_ROW");
            rowVals.put("NOM", "Modification d'une ligne");
            String desc = "Autorise un utilisateur à modifier les éléments de la table spécifiée en objet.";
            rowVals.put("DESCRIPTION", desc);
            rowVals.commit();
        }
        if (!codes.contains("DELETE_ROW")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "DELETE_ROW");
            rowVals.put("NOM", "Suppression d'une ligne");
            String desc = "Autorise un utilisateur à supprimer les éléments de la table spécifiée en objet.";
            rowVals.put("DESCRIPTION", desc);
            rowVals.commit();
        }
        if (!codes.contains("INSERT_ROW")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "INSERT_ROW");
            rowVals.put("NOM", "Ajout d'une ligne");
            String desc = "Autorise un utilisateur à ajouter un élément dans la table spécifiée en objet.";
            rowVals.put("DESCRIPTION", desc);
            rowVals.commit();
        }
        if (!codes.contains("SAVE_ROW")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "SAVE_ROW");
            rowVals.put("NOM", "Export des listes");
            String desc = "Autorise un utilisateur à exporter le contenu des listes via le bouton représentant une disquette.";
            rowVals.put("DESCRIPTION", desc);
            SQLRow row = rowVals.commit();
            SQLRowValues rowValsUserRight = new SQLRowValues(table.getTable("USER_RIGHT"));
            rowValsUserRight.put("ID_RIGHT", row.getID());
            rowValsUserRight.put("HAVE_RIGHT", Boolean.TRUE);
            rowValsUserRight.commit();
        }

        if (!codes.contains("MENU_ACCESS")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "MENU_ACCESS");
            rowVals.put("NOM", "Autoriser l'accés à un menu");
            String desc = "Autorise un utilisateur à visualiser le menu spécifié en objet.";
            rowVals.put("DESCRIPTION", desc);
            SQLRow row = rowVals.commit();
            SQLRowValues rowValsUserRight = new SQLRowValues(table.getTable("USER_RIGHT"));
            rowValsUserRight.put("ID_RIGHT", row.getID());
            rowValsUserRight.put("HAVE_RIGHT", Boolean.TRUE);
            rowValsUserRight.commit();
        }
        if (!codes.contains(GroupSQLComponent.ITEM_RIGHT_CODE)) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", GroupSQLComponent.ITEM_RIGHT_CODE);
            rowVals.put("NOM", "Autoriser à visualiser un champ");
            String desc = "Autorise un utilisateur à visualiser le champ spécifié en objet dans une interface.";
            rowVals.put("DESCRIPTION", desc);
            SQLRow row = rowVals.commit();
            SQLRowValues rowValsUserRight = new SQLRowValues(table.getTable("USER_RIGHT"));
            rowValsUserRight.put("ID_RIGHT", row.getID());
            rowValsUserRight.put("HAVE_RIGHT", Boolean.TRUE);
            rowValsUserRight.commit();
        }
        if (!codes.contains("CORPS_EDITER_PRIX_VENTE")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "CORPS_EDITER_PRIX_VENTE");
            rowVals.put("NOM", "Autoriser à éditer les prix de vente dans les pièces commerciales.");
            String desc = "Autorise un utilisateur à éditer les prix de vente dans les pièces commerciales.";
            rowVals.put("DESCRIPTION", desc);
            SQLRow row = rowVals.commit();
            SQLRowValues rowValsUserRight = new SQLRowValues(table.getTable("USER_RIGHT"));
            rowValsUserRight.put("ID_RIGHT", row.getID());
            rowValsUserRight.put("HAVE_RIGHT", Boolean.TRUE);
            rowValsUserRight.commit();
        }
        if (!codes.contains("CORPS_VOIR_PRIX_ACHAT")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "CORPS_VOIR_PRIX_ACHAT");
            rowVals.put("NOM", "Autoriser à voir les prix d'achat dans les pièces commerciales.");
            String desc = "Autorise un utilisateur à voir les prix d'achat dans les pièces commerciales.";
            rowVals.put("DESCRIPTION", desc);
            SQLRow row = rowVals.commit();
            SQLRowValues rowValsUserRight = new SQLRowValues(table.getTable("USER_RIGHT"));
            rowValsUserRight.put("ID_RIGHT", row.getID());
            rowValsUserRight.put("HAVE_RIGHT", Boolean.TRUE);
            rowValsUserRight.commit();
        }
        if (!codes.contains("CORPS_VERROU_PRIX_MIN_VENTE")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "CORPS_VERROU_PRIX_MIN_VENTE");
            rowVals.put("NOM", "Autoriser à mettre un prix de vente inférieur au prix minimum dans les pièces commerciales.");
            String desc = "Autorise un utilisateur à mettre un prix de vente inférieur au prix minimum dans les pièces commerciales.";
            rowVals.put("DESCRIPTION", desc);
            SQLRow row = rowVals.commit();
            SQLRowValues rowValsUserRight = new SQLRowValues(table.getTable("USER_RIGHT"));
            rowValsUserRight.put("ID_RIGHT", row.getID());
            rowValsUserRight.put("HAVE_RIGHT", Boolean.TRUE);
            rowValsUserRight.commit();
        }
        // if (!codes.contains("MODIF_PRODUCT_KIT")) {
        // SQLRowValues rowVals = new SQLRowValues(table);
        // rowVals.put("CODE", "MODIF_PRODUCT_KIT");
        // rowVals.put("NOM", "Autoriser à modifier les kits créer par autres utilisateurs.");
        // String desc =
        // "Autorise un utilisateur à modifier les kits créer par autres utilisateurs.";
        // rowVals.put("DESCRIPTION", desc);
        // SQLRow row = rowVals.commit();
        // SQLRowValues rowValsUserRight = new SQLRowValues(table.getTable("USER_RIGHT"));
        // rowValsUserRight.put("ID_RIGHT", row.getID());
        // rowValsUserRight.put("HAVE_RIGHT", Boolean.TRUE);
        // rowValsUserRight.commit();
        // }

        if (!codes.contains("SYNC_TEMPLATE")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", "SYNC_TEMPLATE");
            rowVals.put("NOM", "Synchroniser les modèles vers le serveur");
            String desc = "Autorise un utilisateur à synchroniser les modèles.";
            rowVals.put("DESCRIPTION", desc);
            SQLRow row = rowVals.commit();
            SQLRowValues rowValsUserRight = new SQLRowValues(table.getTable("USER_RIGHT"));
            rowValsUserRight.put("ID_RIGHT", row.getID());
            rowValsUserRight.put("HAVE_RIGHT", Boolean.TRUE);
            rowValsUserRight.commit();
        }

    }

    private void findBadForeignKey(DBRoot root) {
        Set<SQLTable> tables = root.getTables();
        for (SQLTable table : tables) {
            findBadForeignKey(root, table);
        }

    }

    private void findBadForeignKey(DBRoot root, SQLTable table) {
        System.out.println("====================================== " + table.getName());
        Set<SQLField> ffields = table.getForeignKeys();

        Set<String> keysString = SQLKey.foreignKeys(table);
        for (String string : keysString) {
            ffields.add(table.getField(string));
        }

        if (ffields.size() == 0) {
            System.out.println("No foreign fields");
        }
        System.out.println("Foreign field for table " + table.getName() + ":" + ffields);
        // Map Champs-> Table sur lequel il pointe
        Map<SQLField, SQLTable> map = new HashMap<SQLField, SQLTable>();
        Set<SQLTable> extTables = new HashSet<SQLTable>();
        for (SQLField sqlField : ffields) {
            SQLTable t = null;
            try {
                t = SQLKey.keyToTable(sqlField);
            } catch (Exception e) {
                System.out.println("Ignoring field:" + sqlField.getName());
            }
            if (t == null) {
                System.out.println("Unable to find table for ff " + sqlField.getName());
            } else {
                extTables.add(t);
                map.put(sqlField, t);
            }
        }
        // Verification des datas
        System.out.println("Foreign table for table " + table.getName() + ":" + extTables);
        // Recupere les ids de toutes les tables
        Map<SQLTable, Set<Number>> ids = getIdsForTables(extTables);

        //
        SQLSelect s = new SQLSelect(true);
        if (table.getPrimaryKeys().size() != 1) {
            return;
        }
        s.addSelect(table.getKey());
        for (SQLField sqlField : map.keySet()) {
            s.addSelect(sqlField);
        }
        List<Map> result = root.getDBSystemRoot().getDataSource().execute(s.asString());
        for (Map resultRow : result) {

            // Pour toutes les lignes
            Set<String> fields = resultRow.keySet();
            for (String field : fields) {
                // Pour tous les champs
                SQLField fField = table.getField(field);
                if (table.getPrimaryKeys().contains(fField)) {
                    continue;
                }
                SQLTable fTable = map.get(fField);
                if (fTable == null) {
                    System.out.println("Error: null table for field" + field);
                    continue;
                }
                Set<Number> values = ids.get(fTable);

                final Object id = resultRow.get(field);
                if (id == null) {
                    continue;
                } else if (!values.contains((Number) id)) {
                    System.out.println("Checking row " + resultRow);
                    System.out.println("Error: No id found in table " + fTable.getName() + " for row " + field + "in table " + table.getName() + " " + resultRow + " knowns id:" + values);
                }
            }
        }
        System.out.println("======================================\n");
    }

    private Map<SQLTable, Set<Number>> getIdsForTables(Set<SQLTable> extTables) {
        Map<SQLTable, Set<Number>> result = new HashMap<SQLTable, Set<Number>>();
        for (SQLTable sqlTable : extTables) {
            result.put(sqlTable, getIdsForTable(sqlTable));
        }
        return result;
    }

    private Set<Number> getIdsForTable(SQLTable table) {
        final DBRoot dbRoot = table.getDBRoot();
        SQLSelect s = new SQLSelect(true);
        s.addSelect(table.getKey());
        List<Number> result = dbRoot.getDBSystemRoot().getDataSource().executeCol(s.asString());
        return new HashSet<Number>(result);
    }

    private void updateCommon(final DBRoot root) throws SQLException {
        SQLUtils.executeAtomic(root.getDBSystemRoot().getDataSource(), new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
                fixCompletion(root);
                return null;
            }
        });

        if (ProductInfo.getInstance().getName().equalsIgnoreCase("OpenConcerto")) {

            final SQLTable tableExercice = root.getTable("EXERCICE_COMMON");

            // FIXME UPDATE Base ILM 1.2->1.3 la table EXERCERCIE ne contenait pas la clef
            // ID_SOCIETE_COMMON
            // if (tableExercice.contains("ID_SOCIETE_COMMON")) {
            String reqUp = "UPDATE " + tableExercice.getSQLName().quote() + " SET \"ID_SOCIETE_COMMON\"=1 WHERE ";
            reqUp += new Where(tableExercice.getKey(), 3, 49).getClause();
            root.getDBSystemRoot().getDataSource().execute(reqUp);

            String reqUp2 = "UPDATE " + tableExercice.getSQLName().quote() + " SET \"ID_SOCIETE_COMMON\"=1 WHERE ";
            reqUp2 += new Where(tableExercice.getKey(), 53, 57).getClause();
            root.getDBSystemRoot().getDataSource().execute(reqUp2);
            // }
            // rm ID 43 - 47 de SOCIETE_COMMON
            final SQLTable tableSociete = root.getTable("SOCIETE_COMMON");
            String req3 = "DELETE FROM " + tableSociete.getSQLName().quote() + " WHERE ";
            req3 += new Where(tableSociete.getKey(), 43, 47).getClause();
            root.getDBSystemRoot().getDataSource().execute(req3);

            // rm ID 3 à 49 de EXERCICE_COMMON

            String req1a = "DELETE FROM " + tableExercice.getSQLName().quote() + " WHERE ";
            req1a += new Where(tableExercice.getKey(), 3, 49).getClause();
            root.getDBSystemRoot().getDataSource().execute(req1a);
            // et 53-57
            root.getDBSystemRoot().getDataSource().execute(req1a);
            String req1b = "DELETE FROM " + tableExercice.getSQLName().quote() + " WHERE ";
            req1b += new Where(tableExercice.getKey(), 53, 57).getClause();
            root.getDBSystemRoot().getDataSource().execute(req1b);
            //

            // TACHE_COMMON, ID_USER_COMMON_*=0 -> 1
            for (final String f : Arrays.asList("ID_USER_COMMON_TO", "ID_USER_COMMON_CREATE", "ID_USER_COMMON_ASSIGN_BY")) {
                final SQLTable tableTache = root.getTable("TACHE_COMMON");
                final UpdateBuilder updateBuilder = new UpdateBuilder(tableTache);
                updateBuilder.set(f, "1").setWhere(new Where(tableTache.getField(f), "=", 0));
                String req2 = updateBuilder.asString();
                root.getDBSystemRoot().getDataSource().execute(req2);
            }
        }
        final SQLTable tableObjet = root.getTable("OBJET");
        if (tableObjet != null && root.getTable("DOMAINE") == null) {
            if (tableObjet.contains("ID_DOMAINE")) {
                final AlterTable alter = new AlterTable(tableObjet);
                alter.dropColumn("ID_DOMAINE");
                final String req = alter.asString();
                root.getDBSystemRoot().getDataSource().execute(req);
                root.refetchTable(tableObjet.getName());
            }
        }

        // FK
        new AddFK(root.getDBSystemRoot()).changeAll(root);
    }

    protected void fixCompletion(DBRoot root) throws SQLException {
        final SQLTable completionT = root.getTable(SQLTextCombo.getTableName());
        if (completionT != null && completionT.getPrimaryKeys().size() == 0) {
            final SQLDataSource ds = root.getDBSystemRoot().getDataSource();

            final Number oldCount = (Number) ds.executeScalar(new SQLSelect().addSelectFunctionStar("count").addFrom(completionT).asString());

            // create the correct table with a new name
            final SQLCreateMoveableTable createTable = SQLTextCombo.getCreateTable(SQLSyntax.get(root));
            createTable.setName(completionT.getName() + "TMP");
            ds.execute(createTable.asString(root.getName()));

            // insert into the new table the old rows without duplicates
            final SQLField refF = completionT.getField(SQLTextCombo.getRefFieldName());
            final SQLField valueF = completionT.getField(SQLTextCombo.getValueFieldName());
            final SQLSelect sel = new SQLSelect();
            sel.addSelect(refF).addSelect(valueF);
            sel.addGroupBy(refF).addGroupBy(valueF);
            final int newCount = new Inserter(createTable, root).insertCount(" ( " + sel.asString() + " )");

            // replace the old table by the new one
            ds.execute("DROP TABLE " + completionT.getSQL());
            ds.execute("ALTER TABLE " + new SQLName(root.getName(), createTable.getName()).quote() + " RENAME to " + completionT.getQuotedName());
            root.getSchema().updateVersion();
            root.refetchTable(completionT.getName());

            System.out.println("Added primary key to " + completionT.getSQL() + " (row count : from " + oldCount + " to " + newCount + ")");
        }
    }

    private void checkPrefsComptable(DBRoot root) throws SQLException {
        SQLTable table = root.findTable("PREFS_COMPTE");
        boolean alter = false;
        AlterTable t = new AlterTable(table);

        if (!table.getFieldsName().contains("ID_COMPTE_PCE_PORT_SOUMIS")) {
            t.addForeignColumn("ID_COMPTE_PCE_PORT_SOUMIS", root.getTable("COMPTE_PCE"));
            alter = true;
        }

        if (!table.getFieldsName().contains("ID_COMPTE_PCE_AVANCE_CLIENT")) {
            t.addForeignColumn("ID_COMPTE_PCE_AVANCE_CLIENT", root.getTable("COMPTE_PCE"));
            alter = true;
        }

        if (!table.getFieldsName().contains("ID_COMPTE_PCE_VALEUR_ENCAISSEMENT")) {
            t.addForeignColumn("ID_COMPTE_PCE_VALEUR_ENCAISSEMENT", root.getTable("COMPTE_PCE"));
            alter = true;
        }

        if (!table.getFieldsName().contains("ID_COMPTE_PCE_PORT_NON_SOUMIS")) {
            t.addForeignColumn("ID_COMPTE_PCE_PORT_NON_SOUMIS", root.getTable("COMPTE_PCE"));
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }

    }

    private void checkCompteDefault(DBRoot root) throws SQLException {
        SQLTable table = root.findTable("COMPTE_PCE_DEFAULT");
        SQLSelect sel = new SQLSelect();
        sel.addSelect(table.getField("NOM"));
        List<String> prefs = root.getBase().getDataSource().executeCol(sel.asString());
        if (!prefs.contains("PortVenteSoumisTVA")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("NOM", "PortVenteSoumisTVA");
            rowVals.put("NUMERO_DEFAULT", "708510");
            rowVals.commit();
        }

        if (!prefs.contains("PortVenteNonSoumisTVA")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("NOM", "PortVenteNonSoumisTVA");
            rowVals.put("NUMERO_DEFAULT", "708520");
            rowVals.commit();
        }

        if (!prefs.contains("AvanceClients")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("NOM", "AvanceClients");
            rowVals.put("NUMERO_DEFAULT", "4191");
            rowVals.commit();
        }

        if (!prefs.contains("ValeurEncaissement")) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("NOM", "ValeurEncaissement");
            rowVals.put("NUMERO_DEFAULT", "511");
            rowVals.commit();
        }

    }

    private void updateSocieteTable(DBRoot root) throws SQLException {
        SQLTable table = root.findTable("SOCIETE_COMMON");
        boolean alter = false;
        AlterTable t = new AlterTable(table);

        if (!table.getFieldsName().contains("RCS")) {
            t.addVarCharColumn("RCS", 256);
            alter = true;
        }

        if (!table.getFieldsName().contains("ID_DEVISE")) {
            t.addForeignColumn("ID_DEVISE", root.getTable("DEVISE"));
            alter = true;
        }

        if (!table.getFieldsName().contains("CAPITAL")) {
            t.addColumn("CAPITAL", "bigint DEFAULT 0");
            alter = true;
        }

        if (alter) {
            try {
                table.getBase().getDataSource().execute(t.asString());
                table.getSchema().updateVersion();
                table.fetchFields();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de l'ajout des champs à la table " + table.getName(), ex);
            }
        }

    }

    private void updateVille(SQLTable tableAdresse) throws SQLException {

        if (tableAdresse != null && tableAdresse.getField("CODE_POSTAL").getType().getJavaType() == Integer.class) {
            String reqRemoveField = "ALTER TABLE " + tableAdresse.getSQLName().quote() + " DROP COLUMN \"CODE_POSTAL\"";
            tableAdresse.getBase().getDataSource().execute(reqRemoveField);
            tableAdresse.getSchema().updateVersion();
            tableAdresse.fetchFields();

            String reqAddVarCharField = "ALTER TABLE " + tableAdresse.getSQLName().quote() + " ADD COLUMN \"CODE_POSTAL\" character varying(256) DEFAULT ''::character varying";
            tableAdresse.getBase().getDataSource().execute(reqAddVarCharField);
            tableAdresse.getSchema().updateVersion();
            tableAdresse.fetchFields();

            String req = "UPDATE " + tableAdresse.getSQLName().quote() + " SET \"VILLE\"= trim (both ' ' from substring(\"VILLE\" from 0 for (position('(' in \"VILLE\")))) ";
            req += ", \"CODE_POSTAL\"=substring(\"VILLE\" from (position('(' in \"VILLE\")+1) for length(\"VILLE\")-position('(' in \"VILLE\")-1) WHERE \"VILLE\" LIKE '% (%)'";
            tableAdresse.getDBRoot().getDBSystemRoot().getDataSource().execute(req);
        }
    }

    private void updateVariablePaye(SQLTable table, String var, double value) throws SQLException {
        if (table == null) {
            throw new IllegalArgumentException("null table");
        }
        SQLSelect sel = new SQLSelect();
        sel.addSelectStar(table);
        sel.setWhere(new Where(table.getField("NOM"), "=", var));
        List<SQLRow> l = (List<SQLRow>) table.getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

        for (SQLRow sqlRow : l) {
            SQLRowValues rowVals = sqlRow.asRowValues();
            rowVals.put("VALEUR", value);
            rowVals.update();
        }
    }

}
