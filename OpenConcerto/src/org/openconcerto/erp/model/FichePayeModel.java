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
 
 package org.openconcerto.erp.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.SQLJavaEditor;
import org.openconcerto.erp.core.humanresources.payroll.element.PeriodeValiditeSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.VariablePayeSQLElement;
import org.openconcerto.erp.preferences.PayeGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

// TODO gestion de la place des rubriques dans l'ordre brut - cot - net et comm everywhere
// FIXME Thread pour le calcul
public class FichePayeModel extends AbstractTableModel {

    // Rubrique
    private Vector<SQLRowValues> vectRubrique;
    private Vector<SQLRowValues> vectRowValsToDelete;

    // table des rubriques
    private final Map<String, SQLTable> mapTableSource = new HashMap<String, SQLTable>();

    // Id de la fiche de paye concernee
    private int idFiche;

    private String[] title;

    private final static SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
    private final static SQLTable tableProfilElt = Configuration.getInstance().getBase().getTable("PROFIL_PAYE_ELEMENT");
    private final static SQLTable tableFichePayeElt = base.getTable("FICHE_PAYE_ELEMENT");
    private final static SQLTable tableFichePaye = base.getTable("FICHE_PAYE");
    private final static SQLTable tableValidite = Configuration.getInstance().getBase().getTable("PERIODE_VALIDITE");

    private SQLJavaEditor javaEdit = new SQLJavaEditor(VariablePayeSQLElement.getMapTree());

    // liste des variable de paye à calculer
    private BigDecimal salBrut, cotPat, cotSal, netImp, netAPayer, csg, csgSansAbattement;

    private Map<Integer, String> mapField;

    private final BigDecimal tauxCSG;

    public FichePayeModel(int idFiche) {

        System.err.println("NEW FICHE PAYE MODEL");

        this.idFiche = idFiche;
        this.vectRubrique = new Vector<SQLRowValues>();
        this.vectRowValsToDelete = new Vector<SQLRowValues>();

        // Titres des colonnes
        this.title = new String[9];
        this.title[0] = "Libellé";
        this.title[1] = "Base";
        this.title[2] = "Taux sal.";
        this.title[3] = "Montant sal. à ajouter";
        this.title[4] = "Montant sal. à déduire";
        this.title[5] = "Taux pat.";
        this.title[6] = "Montant pat.";
        this.title[7] = "Impression";
        this.title[8] = "Dans la Période";

        SQLTable tableNet = Configuration.getInstance().getBase().getTable("RUBRIQUE_NET");
        SQLTable tableBrut = Configuration.getInstance().getBase().getTable("RUBRIQUE_BRUT");
        SQLTable tableCotis = Configuration.getInstance().getBase().getTable("RUBRIQUE_COTISATION");
        SQLTable tableComm = Configuration.getInstance().getBase().getTable("RUBRIQUE_COMM");
        this.mapTableSource.put(tableNet.getName(), tableNet);
        this.mapTableSource.put(tableBrut.getName(), tableBrut);
        this.mapTableSource.put(tableCotis.getName(), tableCotis);
        this.mapTableSource.put(tableComm.getName(), tableComm);

        this.mapField = new HashMap<Integer, String>();
        this.mapField.put(new Integer(0), "NOM");
        this.mapField.put(new Integer(1), "NB_BASE");
        this.mapField.put(new Integer(2), "TAUX_SAL");
        this.mapField.put(new Integer(3), "MONTANT_SAL_AJ");
        this.mapField.put(new Integer(4), "MONTANT_SAL_DED");
        this.mapField.put(new Integer(5), "TAUX_PAT");
        this.mapField.put(new Integer(6), "MONTANT_PAT");
        this.mapField.put(new Integer(7), "IMPRESSION");
        this.mapField.put(new Integer(8), "IN_PERIODE");

        SQLPreferences prefs = new SQLPreferences(tableFichePaye.getTable().getDBRoot());
        this.tauxCSG = new BigDecimal(prefs.getDouble(PayeGlobalPreferencePanel.ASSIETTE_CSG, 0.9825D));

        // loadElement();
        // methodeTmp();
    }

    private void resetValueFiche() {

        /*
         * if (this.threadUpdate != null && this.threadUpdate.isAlive()) { this.threadUpdate.stop();
         * }
         */
        this.salBrut = BigDecimal.ZERO;
        this.cotPat = BigDecimal.ZERO;
        this.cotSal = BigDecimal.ZERO;
        this.netAPayer = BigDecimal.ZERO;
        this.netImp = BigDecimal.ZERO;
        this.csg = BigDecimal.ZERO;
        this.csgSansAbattement = BigDecimal.ZERO;
    }

    public void loadAllElements() {

        System.err.println("Start At " + new Date());
        if (this.idFiche <= 1) {
            System.err.println("Aucune fiche associée");
            return;
        }

        // RAZ
        resetValueFiche();

        /*
         * this.threadUpdate = new Thread("Update Fiche Paye") { public void run() {
         */
        this.vectRubrique = new Vector<SQLRowValues>();

        SQLRow rowFiche = tableFichePaye.getRow(this.idFiche);

        this.javaEdit.setSalarieID(rowFiche.getInt("ID_SALARIE"));

        // éléments de la fiche de paye
        SQLSelect selAllIDFicheElt = new SQLSelect();

        selAllIDFicheElt.addSelect(tableFichePayeElt.getField("ID"));
        selAllIDFicheElt.addSelect(tableFichePayeElt.getField("POSITION"));
        selAllIDFicheElt.setWhere(new Where(tableFichePayeElt.getField("ID_FICHE_PAYE"), "=", this.idFiche));

        selAllIDFicheElt.setDistinct(true);

        selAllIDFicheElt.addRawOrder("\"FICHE_PAYE_ELEMENT\".\"POSITION\"");
        String reqAllIDFichelElt = selAllIDFicheElt.asString();

        System.err.println("Request " + reqAllIDFichelElt);

        Object[] objIDFicheElt = ((List) base.getDataSource().execute(reqAllIDFichelElt, new ArrayListHandler())).toArray();

        System.err.println(objIDFicheElt.length + " elements to load");

        for (int i = 0; i < objIDFicheElt.length; i++) {
            SQLRow row = tableFichePayeElt.getRow(Integer.parseInt(((Object[]) objIDFicheElt[i])[0].toString()));

            String source = row.getString("SOURCE");
            int idSource = row.getInt("IDSOURCE");

            if (source.trim().length() != 0) {

                // System.err.println("Source != null");

                if (this.mapTableSource.get(source) != null) {
                    SQLRow rowSource = this.mapTableSource.get(source).getRow(idSource);

                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_BRUT")) {
                        loadElementBrut(rowSource, row);
                    }

                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_COTISATION")) {
                        loadElementCotisation(rowSource, row);
                    }

                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_NET")) {
                        loadElementNet(rowSource, row);
                    }

                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_COMM")) {
                        loadElementComm(rowSource, row);
                    }

                } else {
                    System.err.println("Table " + source + " non référencée");
                }
            }
        }
        System.err.println(this.vectRubrique.size() + " elements ADDed ");

        fireTableDataChanged();
        /*
         * } }; this.threadUpdate.start();
         */

        System.err.println("End At " + new Date());
    }

    public String getColumnName(int column) {

        return this.title[column];
    }

    public int getRowCount() {

        return this.vectRubrique.size();
    }

    public int getColumnCount() {

        return this.title.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {

        SQLRowValues row = this.vectRubrique.get(rowIndex);
        Object o = null;

        if (row != null) {
            o = row.getObject(this.mapField.get(new Integer(columnIndex)).toString());
        }

        return o;
    }

    public Class<?> getColumnClass(int columnIndex) {
        Class<?> cl = tableFichePayeElt.getField(this.mapField.get(new Integer(columnIndex))).getType().getJavaType();
        return cl;
    }

    /*
     * public boolean isCellEditable(int rowIndex, int columnIndex) {
     * 
     * if (columnIndex == 0) { return true; }
     * 
     * SQLRowValues rowVals = (SQLRowValues) this.vectRubrique.get(rowIndex);
     * 
     * Object ob = rowVals.getObject("SOURCE"); String source = (ob == null) ? "" : ob.toString();
     * 
     * if ((source.trim().length() != 0) && (!source.equalsIgnoreCase("RUBRIQUE_COTISATION"))) {
     * 
     * if (!source.equalsIgnoreCase("RUBRIQUE_COMM")) { if (columnIndex > 5) { return false; } else
     * {
     * 
     * if (columnIndex == 1 || columnIndex == 2) { return true; }
     * 
     * if (source.equalsIgnoreCase("RUBRIQUE_COT") && (columnIndex == 5)) {
     * 
     * return true; } } } } return false; }
     * 
     * public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
     * 
     * System.err.println("_______***$$$$ " + this.mapField.get(new
     * Integer(columnIndex)).toString()); SQLRowValues rowVals = (SQLRowValues)
     * this.vectRubrique.get(rowIndex); rowVals.put(this.mapField.get(new
     * Integer(columnIndex)).toString(), aValue); rowVals.put("VALIDE", Boolean.TRUE);
     * 
     * try { rowVals.update(); } catch (SQLException e) { e.printStackTrace(); }
     * 
     * calculValue(); }
     */
    public boolean containValueAt(int rowIndex, int columnIndex) {

        if (columnIndex == 0) {
            return true;
        }

        SQLRowValues rowVals = this.vectRubrique.get(rowIndex);

        Object ob = rowVals.getObject("SOURCE");
        String source = (ob == null) ? "" : ob.toString();

        Object obId = rowVals.getObject("IDSOURCE");
        int idSource = (obId == null) ? 1 : rowVals.getInt("IDSOURCE");

        if ((source.trim().length() != 0) && (!source.equalsIgnoreCase("RUBRIQUE_COTISATION"))) {
            /*
             * if (source.equalsIgnoreCase("RUBRIQUE_COMM")) { return true; } else {
             */

            if (columnIndex > 4) {
                return false;
            } else {
                SQLRow row = this.mapTableSource.get(source).getRow(idSource);
                if (source.equalsIgnoreCase("RUBRIQUE_BRUT")) {
                    if ((row.getInt("ID_TYPE_RUBRIQUE_BRUT") == 2) && (columnIndex == 4)) {
                        return false;
                    }
                    if ((row.getInt("ID_TYPE_RUBRIQUE_BRUT") == 3) && (columnIndex == 3)) {
                        return false;
                    }
                } else {
                    if (source.equalsIgnoreCase("RUBRIQUE_NET")) {
                        if ((row.getInt("ID_TYPE_RUBRIQUE_NET") == 2) && (columnIndex == 4)) {
                            return false;
                        }
                        if ((row.getInt("ID_TYPE_RUBRIQUE_NET") == 3) && (columnIndex == 3)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                // }
            }
        } else {
            if (columnIndex == 3) {
                return false;
            }
        }

        return true;
    }

    public void loadFromProfil(final int idProfil) {

        System.err.println("Load from profil");

        resetValueFiche();

        /*
         * this.threadUpdate = new Thread("Update Fiche Paye") { public void run() {
         */
        // On supprime les anciennes lignes de la fiche
        while (this.vectRubrique.size() > 0) {

            this.vectRowValsToDelete.add(this.vectRubrique.remove(0));
        }

        // this.vectRubrique = new Vector();

        // Listes des rubriques du profil
        SQLSelect selAllIDProfilElt = new SQLSelect();

        selAllIDProfilElt.addSelect(tableProfilElt.getField("ID"));
        selAllIDProfilElt.addSelect(tableProfilElt.getField("POSITION"));
        selAllIDProfilElt.setWhere(new Where(tableProfilElt.getField("ID_PROFIL_PAYE"), "=", idProfil));
        selAllIDProfilElt.addRawOrder("\"PROFIL_PAYE_ELEMENT\".\"POSITION\"");

        String reqAllIDProfilElt = selAllIDProfilElt.asString();

        Object[] objIDProfilElt = ((List) Configuration.getInstance().getBase().getDataSource().execute(reqAllIDProfilElt, new ArrayListHandler())).toArray();

        for (int i = 0; i < objIDProfilElt.length; i++) {
            SQLRow rowTmp = tableProfilElt.getRow(Integer.parseInt(((Object[]) objIDProfilElt[i])[0].toString()));

            String source = rowTmp.getString("SOURCE");
            int idSource = rowTmp.getInt("IDSOURCE");

            if (this.mapTableSource.get(source) != null) {
                SQLRow row = this.mapTableSource.get(source).getRow(idSource);

                if (row.getTable().getName().equalsIgnoreCase("RUBRIQUE_BRUT")) {
                    loadElementBrut(row, null);
                }
                if (row.getTable().getName().equalsIgnoreCase("RUBRIQUE_COTISATION")) {
                    loadElementCotisation(row, null);
                }
                if (row.getTable().getName().equalsIgnoreCase("RUBRIQUE_NET")) {
                    loadElementNet(row, null);
                }
                if (row.getTable().getName().equalsIgnoreCase("RUBRIQUE_COMM")) {
                    loadElementComm(row, null);
                }

            } else {
                System.err.println("FichePayeModel.java --> Table non référencée dans la Map. Table name : " + source);
            }
        }

        // this.vectRubrique = new Vector();

        // updateFields(this.idFiche);
        fireTableDataChanged();

        /*
         * } }; this.threadUpdate.start();
         */
    }

    public String getSourceAt(int rowIndex) {
        return this.vectRubrique.get(rowIndex).getString("SOURCE");
    }

    public int upRow(int rowIndex) {
        // On vérifie qu'il est possible de remonter la ligne
        if ((this.vectRubrique.size() > 1) && (rowIndex > 0)) {
            System.err.println("UP");
            SQLRowValues tmp = this.vectRubrique.get(rowIndex);
            this.vectRubrique.set(rowIndex, this.vectRubrique.get(rowIndex - 1));
            this.vectRubrique.set(rowIndex - 1, tmp);
            this.fireTableDataChanged();
            return rowIndex - 1;
        }
        System.err.println("can't up!!");
        return rowIndex;
    }

    public int downRow(int rowIndex) {
        // On vérifie qu'il est possible de descendre la ligne
        if ((rowIndex >= 0) && (this.vectRubrique.size() > 1) && (rowIndex + 1 < this.vectRubrique.size())) {

            System.err.println("DOWN");
            SQLRowValues tmp = this.vectRubrique.get(rowIndex);
            this.vectRubrique.set(rowIndex, this.vectRubrique.get(rowIndex + 1));
            this.vectRubrique.set(rowIndex + 1, tmp);
            this.fireTableDataChanged();
            return rowIndex + 1;
        }

        System.err.println("can't down!!!");
        return rowIndex;
    }

    public void setLastRowAT(int rowIndex) {

        // On vérifie qu'il est possible de descendre la ligne
        if ((rowIndex > 0) && (rowIndex < this.vectRubrique.size())) {
            this.vectRubrique.add(rowIndex, this.vectRubrique.remove(this.vectRubrique.size() - 1));
        }
        this.fireTableDataChanged();
    }

    public void setFicheID(int id) {
        this.idFiche = id;

        // this.javaEdit.setSalarieID(this.tableFichePaye.getRow(this.idFiche).getInt("ID_SALARIE"));
        this.loadAllElements();
    }

    /***********************************************************************************************
     * Ajouter une ligne
     * 
     * @param row SQLRow RUBRIQUE_BRUT, RUBRIQUE_COTISATION, RUBRIQUE_NET, RUBRIQUE_COMM
     * @param index index ou doit etre insere la row
     */
    public void addRowAt(SQLRow row, int index) {

        int size = this.vectRubrique.size();
        if (row.getTable().getName().equalsIgnoreCase("RUBRIQUE_BRUT")) {
            this.loadElementBrut(row, null);
        } else {
            if (row.getTable().getName().equalsIgnoreCase("RUBRIQUE_COTISATION")) {
                this.loadElementCotisation(row, null);
            } else {
                if (row.getTable().getName().equalsIgnoreCase("RUBRIQUE_NET")) {
                    this.loadElementNet(row, null);
                } else {
                    if (row.getTable().getName().equalsIgnoreCase("RUBRIQUE_COMM")) {
                        this.loadElementComm(row, null);
                    }
                }
            }
        }

        if (size != this.vectRubrique.size()) {
            setLastRowAT(index);
        }
        if (!row.getTable().getName().equalsIgnoreCase("RUBRIQUE_COMM")) {
            calculValue();
        }
        this.fireTableDataChanged();
    }

    public void removeRow(int rowIndex) {
        // System.err.println("_________________________________REMOVE");
        if (rowIndex >= 0) {

            SQLRowValues rowVals = this.vectRubrique.remove(rowIndex);
            this.vectRowValsToDelete.add(rowVals);

            if (!rowVals.getString("SOURCE").equalsIgnoreCase("RUBRIQUE_COMM")) {
                calculValue();
            }
            this.fireTableDataChanged();
        }
    }

    public void updateFields(int idFiche) {

        // System.err.println("UPDATE FIELDS");

        for (int i = 0; i < this.vectRowValsToDelete.size(); i++) {
            SQLRowValues rowVals = this.vectRowValsToDelete.get(i);

            if (rowVals.getID() != SQLRow.NONEXISTANT_ID) {

                rowVals.put("ARCHIVE", 1);
                try {
                    rowVals.update();
                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }
        }

        this.vectRowValsToDelete = new Vector<SQLRowValues>();

        for (int i = 0; i < this.vectRubrique.size(); i++) {
            SQLRowValues rowVals = this.vectRubrique.get(i);
            rowVals.put("ID_FICHE_PAYE", idFiche);
            rowVals.put("POSITION", i);

            try {
                rowVals.commit();
            } catch (SQLException e) {

                e.printStackTrace();
            }
        }
    }

    public void showData() {

        if (this.vectRubrique.size() == 0) {
            System.err.println("Vecteur contains no value.");
        }
        for (int i = 0; i < this.vectRubrique.size(); i++) {
            System.err.println(this.vectRubrique.get(i));
        }
    }

    public Object getVectorObjectAt(int index) {
        return this.vectRubrique.get(index);
    }

    private boolean isEltInPeriod(SQLRow rowSource) {
        SQLRow rowFiche = tableFichePaye.getRow(this.idFiche);
        int mois = rowFiche.getInt("ID_MOIS") - 1;

        Object ob = PeriodeValiditeSQLElement.mapTranslate().get(Integer.valueOf(mois));

        if (ob == null) {
            return false;
        }
        String moisName = ob.toString();

        SQLRow rowPeriodeValid = tableValidite.getRow(rowSource.getInt("ID_PERIODE_VALIDITE"));

        return (rowPeriodeValid.getBoolean(moisName));
    }

    private boolean isEltImprimable(SQLRow rowSource, SQLRowValues row) {

        int impression = rowSource.getInt("ID_IMPRESSION_RUBRIQUE");

        if (impression == 3) {
            return true;
        } else {
            if (impression == 4) {
                return false;
            } else {
                if (impression == 2) {

                    BigDecimal montantSalAjOb = row.getBigDecimal("MONTANT_SAL_AJ");
                    BigDecimal montantSalAj = (montantSalAjOb == null) ? BigDecimal.ZERO : montantSalAjOb;

                    BigDecimal montantSalDedOb = row.getBigDecimal("MONTANT_SAL_DED");
                    BigDecimal montantSalDed = (montantSalDedOb == null) ? BigDecimal.ZERO : montantSalDedOb;

                    BigDecimal montantPatOb = row.getBigDecimal("MONTANT_PAT");
                    BigDecimal montantPat = (montantPatOb == null) ? BigDecimal.ZERO : montantPatOb;

                    if (montantSalAj.signum() == 0 && montantSalDed.signum() == 0 && montantPat.signum() == 0) {
                        return false;
                    }
                    return true;

                }
            }
        }
        return true;
    }

    /**
     * charge un élément de la fiche dans rowVals, dont la rubrique source est rowSource et
     * l'élément row si l'élément existe déja
     * 
     * @param rowVals
     * @param rowSource
     * @param row
     * @return true si on doit calculer les valeurs
     */
    private boolean loadElement(SQLRowValues rowVals, SQLRow rowSource, SQLRow row) {

        if (row != null) {
            rowVals.loadAbsolutelyAll(row);
        }

        // on vérifie que la rubrique s'applique pour le mois concerné
        // if (!isEltInPeriod(rowSource)) {
        // System.err.println("Not In periode");
        rowVals.put("IN_PERIODE", Boolean.valueOf(isEltInPeriod(rowSource)));
        // }

        rowVals.put("SOURCE", rowSource.getTable().getName());
        rowVals.put("IDSOURCE", rowSource.getID());

        Object ob = rowVals.getObject("VALIDE");
        boolean b = (ob == null) ? false : new Boolean(ob.toString()).booleanValue();

        if (rowVals.getObject("ID_FICHE_PAYE") != null && !rowVals.isForeignEmpty("ID_FICHE_PAYE") && rowVals.getForeign("ID_FICHE_PAYE").getBoolean("VALIDE")) {
            b = true;
        }

        return b;
    }

    private void updateValueFiche() {

        if (!tableFichePaye.getRow(idFiche).getBoolean("VALIDE")) {
            SQLRowValues rowValsFiche = new SQLRowValues(tableFichePaye);

            rowValsFiche.put("SAL_BRUT", this.salBrut);
            rowValsFiche.put("NET_IMP", this.netImp.add(this.salBrut));
            rowValsFiche.put("NET_A_PAYER", this.netAPayer.add(this.salBrut));
            rowValsFiche.put("COT_SAL", this.cotSal);
            rowValsFiche.put("COT_PAT", this.cotPat);
            rowValsFiche.put("CSG", this.salBrut.add(this.csg).multiply(this.tauxCSG).add(this.csgSansAbattement));

            try {
                rowValsFiche.update(this.idFiche);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /***********************************************************************************************
     * Charge un élément d'une rubrique de brut
     * 
     * @param rowSource row de la rubrique source
     * @param row row de l'élément de la fiche
     **********************************************************************************************/
    private void loadElementBrut(SQLRow rowSource, SQLRow row) {

        // System.err.println("________________________LOAD ELT BRUT");
        SQLRowValues rowVals = new SQLRowValues(tableFichePayeElt);

        if (!loadElement(rowVals, rowSource, row)) {

            // System.err.println("________________________Recalcul des ELT BRUT ");
            Object baseOb = this.javaEdit.checkFormule(rowSource.getString("BASE"), "BASE");
            Object tauxSalOb = this.javaEdit.checkFormule(rowSource.getString("TAUX"), "TAUX");
            rowVals.put("NOM", rowSource.getString("NOM"));

            rowVals.put("NB_BASE", (baseOb == null) ? null : new BigDecimal(baseOb.toString()));
            rowVals.put("TAUX_SAL", (tauxSalOb == null) ? null : new BigDecimal(tauxSalOb.toString()));
        }

        calculBrut(rowSource, rowVals);

        boolean b = isEltImprimable(rowSource, rowVals);
        // System.err.println("Impression --- > " + b);
        rowVals.put("IMPRESSION", Boolean.valueOf(b));

        this.vectRubrique.add(rowVals);
    }

    /**
     * Calcul le montant d'une rubrique de brut et met à jour les variables du salarié
     * 
     * @param rowSource
     * @param rowVals
     */
    private void calculBrut(SQLRow rowSource, SQLRowValues rowVals) {

        if (rowVals.getBoolean("IN_PERIODE")) {

            BigDecimal baseOb = rowVals.getBigDecimal("NB_BASE");
            BigDecimal tauxSalOb = rowVals.getBigDecimal("TAUX_SAL");

            BigDecimal base = (baseOb == null) ? BigDecimal.ZERO : baseOb;
            BigDecimal tauxSal = (tauxSalOb == null) ? BigDecimal.ZERO : tauxSalOb;

            // Calcul du montant
            String formuleMontant = rowSource.getString("MONTANT");

            BigDecimal montant = BigDecimal.ZERO;
            if (formuleMontant.trim().length() == 0) {
                montant = base.multiply(tauxSal).setScale(2, RoundingMode.HALF_UP);
            } else {
                Object montantNet = this.javaEdit.checkFormule(rowSource.getString("MONTANT"), "MONTANT");
                BigDecimal montantNetS = (montantNet == null) ? BigDecimal.ZERO : new BigDecimal(montantNet.toString());
                montant = montantNetS;
            }

            // Retenue
            if (rowSource.getInt("ID_TYPE_RUBRIQUE_BRUT") == 3) {

                rowVals.put("MONTANT_SAL_DED", montant);
                this.salBrut = this.salBrut.subtract(montant);
            } // Gain
            else {

                rowVals.put("MONTANT_SAL_AJ", montant);
                this.salBrut = this.salBrut.add(montant);
            }

            // Mis a jour du salaire brut
            updateValueFiche();
        }
    }

    private void calculNet(SQLRow rowSource, SQLRowValues rowVals) {

        if (rowVals.getBoolean("IN_PERIODE")) {

            BigDecimal baseOb = rowVals.getBigDecimal("NB_BASE");
            BigDecimal tauxSalOb = rowVals.getBigDecimal("TAUX_SAL");

            BigDecimal base = baseOb == null ? BigDecimal.ZERO : baseOb;
            BigDecimal tauxSal = tauxSalOb == null ? BigDecimal.ZERO : tauxSalOb;

            // Calcul du montant
            String formuleMontant = rowSource.getString("MONTANT");

            BigDecimal montant = BigDecimal.ZERO;
            if (formuleMontant.trim().length() == 0) {
                montant = base.multiply(tauxSal).setScale(2, RoundingMode.HALF_UP);
            } else {
                Object montantNet = this.javaEdit.checkFormule(rowSource.getString("MONTANT"), "MONTANT");
                if (montantNet != null) {
                    montant = new BigDecimal(montantNet.toString());
                }
            }

            // Retenue
            if (rowSource.getInt("ID_TYPE_RUBRIQUE_NET") == 3) {

                rowVals.put("MONTANT_SAL_DED", montant);

                this.netAPayer = this.netAPayer.subtract(montant);
                if (!rowSource.getBoolean("IMPOSABLE")) {
                    this.netImp = this.netImp.subtract(montant);
                }

            } // Gain
            else {

                rowVals.put("MONTANT_SAL_AJ", montant);

                this.netAPayer = this.netAPayer.add(montant);
                if (!rowSource.getBoolean("IMPOSABLE")) {
                    this.netImp = this.netImp.add(montant);
                }
            }

            // Mis a jour du salaire net
            updateValueFiche();
        }
    }

    private void loadElementNet(SQLRow rowSource, SQLRow row) {
        SQLRowValues rowVals = new SQLRowValues(tableFichePayeElt);
        // System.err.println("________________________LOAD ELT NET");

        if (!loadElement(rowVals, rowSource, row)) {

            Object baseOb = this.javaEdit.checkFormule(rowSource.getString("BASE"), "BASE");
            Object tauxSalOb = this.javaEdit.checkFormule(rowSource.getString("TAUX"), "TAUX");
            rowVals.put("NOM", rowSource.getString("NOM"));
            rowVals.put("NB_BASE", (baseOb == null) ? null : new BigDecimal(baseOb.toString()));
            rowVals.put("TAUX_SAL", (tauxSalOb == null) ? null : new BigDecimal(tauxSalOb.toString()));
        }

        calculNet(rowSource, rowVals);

        boolean b = isEltImprimable(rowSource, rowVals);
        // System.err.println("Impression --- > " + b);
        rowVals.put("IMPRESSION", Boolean.valueOf(b));

        this.vectRubrique.add(rowVals);
    }

    private void calculCotisation(SQLRow rowSource, SQLRowValues rowVals) {

        if (((Boolean) rowVals.getObject("IN_PERIODE")).booleanValue()) {

            BigDecimal baseOb = rowVals.getBigDecimal("NB_BASE");
            BigDecimal tauxSalOb = rowVals.getBigDecimal("TAUX_SAL");
            BigDecimal tauxPatOb = rowVals.getBigDecimal("TAUX_PAT");

            BigDecimal base = baseOb == null ? BigDecimal.ZERO : baseOb;
            BigDecimal tauxSal = tauxSalOb == null ? BigDecimal.ZERO : tauxSalOb;
            BigDecimal tauxPat = tauxPatOb == null ? BigDecimal.ZERO : tauxPatOb;

            // Calcul du montant
            BigDecimal montantSal = base.multiply(tauxSal).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
            BigDecimal montantPat = base.multiply(tauxPat).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);

            rowVals.put("MONTANT_SAL_DED", montantSal);
            rowVals.put("MONTANT_PAT", montantPat);

            this.netAPayer = this.netAPayer.subtract(montantSal);
            if (!rowSource.getBoolean("IMPOSABLE")) {
                this.netImp = this.netImp.subtract(montantSal);
            }

            if (rowSource.getBoolean("PART_PAT_IMPOSABLE")) {
                this.netImp = this.netImp.add(montantPat);
            }

            if (rowSource.getBoolean("PART_CSG")) {
                this.csg = this.csg.add(montantPat);
            }
            if (rowSource.getBoolean("PART_CSG_SANS_ABATTEMENT")) {
                this.csgSansAbattement = this.csgSansAbattement.add(montantPat);
            }

            this.cotSal = this.cotSal.add(montantSal);
            this.cotPat = this.cotPat.add(montantPat);

            // Mis a jour des cotisations
            updateValueFiche();
        }
    }

    private void loadElementCotisation(final SQLRow rowSource, SQLRow row) {
        SQLRowValues rowVals = new SQLRowValues(tableFichePayeElt);
        // System.err.println("________________________LOAD ELT COTISATION");

        if (!loadElement(rowVals, rowSource, row)) {

            // On calcule les valeurs
            Object baseOb = this.javaEdit.checkFormule(rowSource.getString("BASE"), "BASE");
            if (!this.javaEdit.isCodeValid()) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null, "La formule BASE pour la rubrique " + rowSource.getString("CODE") + " n'est pas correcte!");
                    }
                });
            }
            Object tauxSalOb = this.javaEdit.checkFormule(rowSource.getString("TX_SAL"), "TX_SAL");
            if (!this.javaEdit.isCodeValid()) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null, "La formule TX_SAL pour la rubrique " + rowSource.getString("CODE") + " n'est pas correcte!");
                    }
                });
            }
            Object tauxPatOb = this.javaEdit.checkFormule(rowSource.getString("TX_PAT"), "TX_PAT");
            if (!this.javaEdit.isCodeValid()) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null, "La formule TX_PAT pour la rubrique " + rowSource.getString("CODE") + " n'est pas correcte!");
                    }
                });
            }
            rowVals.put("NOM", rowSource.getString("NOM"));
            rowVals.put("NB_BASE", (baseOb == null) ? null : new BigDecimal(baseOb.toString()));
            rowVals.put("TAUX_SAL", (tauxSalOb == null) ? null : new BigDecimal(tauxSalOb.toString()));
            rowVals.put("TAUX_PAT", (tauxPatOb == null) ? null : new BigDecimal(tauxPatOb.toString()));
        }

        calculCotisation(rowSource, rowVals);

        boolean b = isEltImprimable(rowSource, rowVals);
        // System.err.println("Impression --- > " + b);
        rowVals.put("IMPRESSION", Boolean.valueOf(b));

        this.vectRubrique.add(rowVals);
    }

    public void loadElementComm(SQLRow rowSource, SQLRow row) {
        SQLRowValues rowVals = new SQLRowValues(tableFichePayeElt);
        // System.err.println("________________________LOAD ELT COMM");

        if (loadElement(rowVals, rowSource, row)) {
            this.vectRubrique.add(rowVals);
            return;
        }

        Object baseOb = this.javaEdit.checkFormule(rowSource.getString("NB_BASE"), "BASE");
        Object tauxSalOb = this.javaEdit.checkFormule(rowSource.getString("TAUX_SAL"), "SAL");
        Object tauxPatOb = this.javaEdit.checkFormule(rowSource.getString("TAUX_PAT"), "PAT");
        Object montantPatOb = this.javaEdit.checkFormule(rowSource.getString("MONTANT_PAT"), "MONTANT");
        Object montantAdOb = this.javaEdit.checkFormule(rowSource.getString("MONTANT_SAL_AJ"), "MONTANT");
        Object montantDedOb = this.javaEdit.checkFormule(rowSource.getString("MONTANT_SAL_DED"), "MONTANT");
        rowVals.put("NOM", rowSource.getBoolean("NOM_VISIBLE") ? rowSource.getString("NOM") : "");
        rowVals.put("NB_BASE", (baseOb == null) ? null : new BigDecimal(baseOb.toString()));
        rowVals.put("TAUX_SAL", (tauxSalOb == null) ? null : new BigDecimal(tauxSalOb.toString()));
        rowVals.put("TAUX_PAT", (tauxPatOb == null) ? null : new BigDecimal(tauxPatOb.toString()));
        rowVals.put("MONTANT_PAT", (montantPatOb == null) ? null : new BigDecimal(montantPatOb.toString()));
        rowVals.put("MONTANT_SAL_AJ", (montantAdOb == null) ? null : new BigDecimal(montantAdOb.toString()));
        rowVals.put("MONTANT_SAL_DED", (montantDedOb == null) ? null : new BigDecimal(montantDedOb.toString()));

        boolean b = isEltImprimable(rowSource, rowVals);
        // System.err.println("Impression --- > " + b);
        rowVals.put("IMPRESSION", Boolean.valueOf(b));

        this.vectRubrique.add(rowVals);
    }

    private void calculValue() {

        System.err.println("Start calculValue At " + new Date());

        resetValueFiche();

        /*
         * this.threadUpdate = new Thread("Update Fiche Paye") { public void run() {
         */
        Vector<SQLRowValues> vectTmp = new Vector<SQLRowValues>(this.vectRubrique);

        this.vectRubrique = new Vector<SQLRowValues>();
        for (int i = 0; i < vectTmp.size(); i++) {

            SQLRowValues rowVals = vectTmp.get(i);
            String source = rowVals.getString("SOURCE");
            int idSource = rowVals.getInt("IDSOURCE");
            SQLRow row = tableFichePayeElt.getRow(rowVals.getID());

            if (source.trim().length() != 0) {

                // System.err.println("Source != null");

                if (this.mapTableSource.get(source) != null) {
                    SQLRow rowSource = this.mapTableSource.get(source).getRow(idSource);

                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_BRUT")) {
                        loadElementBrut(rowSource, row);
                    }
                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_COTISATION")) {
                        loadElementCotisation(rowSource, row);
                    }
                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_NET")) {
                        loadElementNet(rowSource, row);
                    }
                    if (rowSource.getTable().getName().equalsIgnoreCase("RUBRIQUE_COMM")) {
                        loadElementComm(rowSource, row);
                    }
                } else {
                    System.err.println("Table " + source + " non référencée");
                }
            }
        }
        System.err.println(this.vectRubrique.size() + " elements ADDed ");

        fireTableDataChanged();
        /*
         * } }; this.threadUpdate.start();
         */

        System.err.println("End calculValue At " + new Date());
    }

    public void validElt() {

        System.err.println("Validation des éléments de la fiche.");
        for (int i = 0; i < this.vectRubrique.size(); i++) {
            SQLRowValues rowVals = this.vectRubrique.get(i);
            rowVals.put("VALIDE", Boolean.valueOf(true));

            try {
                rowVals.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.err.println("Validation terminée.");
    }
}
