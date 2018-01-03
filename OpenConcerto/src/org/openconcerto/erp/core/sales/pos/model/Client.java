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
 
 package org.openconcerto.erp.core.sales.pos.model;

import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.sales.pos.POSConfiguration;
import org.openconcerto.erp.core.sales.pos.io.DefaultTicketPrinter;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.ui.TicketCellRenderer;
import org.openconcerto.erp.generationEcritures.GenerationEcritures;
import org.openconcerto.erp.generationEcritures.GenerationReglementVenteNG;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Client {

    public static final Client NONE = new Client(-1, "Client inconnu", BigDecimal.ZERO);
    private int id;
    private String fullName;
    private BigDecimal solde;
    private String addr;

    public Client(int id, String fullName, BigDecimal solde) {
        this.id = id;
        this.fullName = fullName;
        if (solde == null) {
            solde = BigDecimal.ZERO;
        }
        this.solde = solde;
    }

    public int getId() {
        return this.id;
    }

    public String getFullName() {
        return fullName;
    }

    public BigDecimal getSolde() {
        return solde;
    }

    public String getAddr() {
        return addr;
    }

    public void setAdresse(String string) {
        this.addr = string.trim();
    }

    public void credit(final BigDecimal amount, final int paymentType) throws Exception {
        if (amount == null || amount.longValue() <= 0) {
            return;
        }

        // inserer la transaction
        final SQLDataSource ds = Configuration.getInstance().getSystemRoot().getDataSource();
        SQLUtils.executeAtomic(ds, new SQLUtils.SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {

                final SQLTable table = Configuration.getInstance().getRoot().findTable("COMPTE_CLIENT_TRANSACTION");
                SQLRowValues rowVals = new SQLRowValues(table);
                rowVals.put("ID_CLIENT", getId());
                final Date today = new Date();
                rowVals.put("DATE", today);
                rowVals.put("MONTANT", amount);
                SQLRowValues rowValsEltMode = rowVals.putRowValues("ID_MODE_REGLEMENT");
                if (paymentType == Paiement.CB) {
                    rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.CB);
                } else if (paymentType == Paiement.CHEQUE) {
                    rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.CHEQUE);
                } else if (paymentType == Paiement.ESPECES) {
                    rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.ESPECE);
                }
                SQLRow rowTransact = rowVals.commit();
                GenerationEcritures ecr = new GenerationEcritures();
                int idMvt = ecr.getNewMouvement(table.getName(), rowTransact.getID(), 1, "Transact. " + getFullName());
                rowTransact = rowTransact.createEmptyUpdateRow().put("ID_MOUVEMENT", idMvt).commit();

                // mise à jour du solde
                SQLTable tableClient = table.getForeignTable("ID_CLIENT");
                SQLRow row = tableClient.getRow(getId());
                BigDecimal solde = row.getBigDecimal("SOLDE_COMPTE");
                final BigDecimal nouveauSolde = solde.add(amount);
                row.createEmptyUpdateRow().put("SOLDE_COMPTE", nouveauSolde).commit();

                final long centAmountValue = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                // Créeation des réglements et écritures
                try {
                    new GenerationReglementVenteNG("Crédit du solde " + Client.this.fullName, row, new PrixTTC(centAmountValue), today, rowTransact.getForeign("ID_MODE_REGLEMENT"), rowTransact,
                            rowTransact.getForeign("ID_MOUVEMENT"), true, true);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new SQLException(e);
                }

                // TODO VALIDER les écritures ici et en fermeture de caisse

                Client.this.solde = nouveauSolde;
                return null;
            }
        });
    }

    public void printCredit(TicketPrinter prt, int ticketWidth, BigDecimal amount, int paymentType, BigDecimal nouveauSolde) {
        prt.clearBuffer();
        List<TicketLine> headers = POSConfiguration.getInstance().getHeaderLines();
        for (TicketLine line : headers) {
            prt.addToBuffer(line);
        }

        // Date
        prt.addToBuffer("");
        SimpleDateFormat df = new SimpleDateFormat("EEEE d MMMM yyyy à HH:mm", Locale.FRENCH);
        prt.addToBuffer(DefaultTicketPrinter.formatRight(ticketWidth, "Le " + df.format(new Date())));
        prt.addToBuffer("");
        prt.addToBuffer("RECU", TicketPrinter.BOLD);
        prt.addToBuffer("");

        String amountStr = TicketCellRenderer.centsToString(amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue());
        prt.addToBuffer("Paiement de " + amountStr + " euros");
        prt.addToBuffer("En" + new Paiement(paymentType).getTypeAsString());
        prt.addToBuffer("");
        int ntotal = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
        prt.addToBuffer("Nouveau solde : " + TicketCellRenderer.centsToString(ntotal) + " euros");
        prt.addToBuffer("");
        try {
            prt.printBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Transaction> getTransactions() {
        final SQLTable table = Configuration.getInstance().getRoot().findTable("COMPTE_CLIENT_TRANSACTION");
        final SQLRowValues graph = new SQLRowValues(table);
        graph.putNulls("DATE", "MONTANT");
        graph.putRowValues("ID_MODE_REGLEMENT").putRowValues("ID_TYPE_REGLEMENT");
        graph.putRowValues("ID_CLIENT");
        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(graph);
        fetcher.appendSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                return input.setWhere(table.getField("ID_CLIENT"), "=", getId());
            }
        });
        final List<SQLRowValues> values = fetcher.fetch();
        final List<Transaction> l = new ArrayList<Transaction>();
        for (SQLRowValues sqlRowValues : values) {
            final Date date = sqlRowValues.getDate("DATE").getTime();
            final BigDecimal amount = sqlRowValues.getBigDecimal("MONTANT");
            final int idReglement = sqlRowValues.getForeign("ID_MODE_REGLEMENT").getForeignID("ID_TYPE_REGLEMENT");
            int payment = -1;
            if (idReglement == TypeReglementSQLElement.CB) {
                payment = Paiement.CB;
            } else if (idReglement == TypeReglementSQLElement.ESPECE) {
                payment = Paiement.ESPECES;
            } else if (idReglement == TypeReglementSQLElement.CHEQUE) {
                payment = Paiement.CHEQUE;
            } else if (amount.signum() == -1) {
                payment = Paiement.SOLDE;
            } else {

                ExceptionHandler.handle("unknown ID_MODE_REGLEMENT " + idReglement);
            }

            final Transaction t = new Transaction(date, amount, payment);
            l.add(t);
        }
        Collections.sort(l, new Comparator<Transaction>() {

            @Override
            public int compare(Transaction o1, Transaction o2) {
                // Du plus récent au plus ancien
                return o2.getDate().compareTo(o1.getDate());
            }
        });
        return l;
    }
}
