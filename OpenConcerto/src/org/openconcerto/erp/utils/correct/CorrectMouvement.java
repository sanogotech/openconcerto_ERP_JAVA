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
 
 package org.openconcerto.erp.utils.correct;

import org.openconcerto.erp.config.Log;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Unarchive ECRITURE to balance MOUVEMENT, then link SAISIE_KM_ELEMENT and a unique matching
 * ECRITURE.
 * 
 * @author Sylvain CUAZ
 */
public class CorrectMouvement extends Changer<DBRoot> {

    static public final SQLSelect createUnbalancedSelect(final DBRoot societeRoot) {
        return createUnbalancedSelect(societeRoot.getTable("ECRITURE").getField("ID_MOUVEMENT"));
    }

    static private final SQLSelect createUnbalancedSelect(final SQLField ecritureMvtFF) {
        final SQLTable ecritureT = ecritureMvtFF.getTable();

        final SQLSelect selUnbalanced = new SQLSelect();
        selUnbalanced.addSelect(ecritureMvtFF);
        selUnbalanced.addGroupBy(ecritureMvtFF);
        selUnbalanced.setHaving(Where.quote(ecritureT.getBase().quote("SUM(%n) != SUM(%n)", ecritureT.getField("DEBIT"), ecritureT.getField("CREDIT"))));
        return selUnbalanced;
    }

    public CorrectMouvement(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected void changeImpl(DBRoot societeRoot) throws SQLException {
        final SQLTable ecritureT = societeRoot.getTable("ECRITURE");

        // some ECRITURE were ARCHIVED creating unbalanced MOUVEMENT
        // find MOUVEMENT that would be balanced if we unarchived all of its ECRITURE
        {
            final SQLField ecritureMvtFF = ecritureT.getField("ID_MOUVEMENT");
            final SQLSelect selUnbalanced = createUnbalancedSelect(ecritureMvtFF);

            final SQLSelect selUnfixable = new SQLSelect(selUnbalanced);
            selUnfixable.setArchivedPolicy(ArchiveMode.BOTH);

            final String selFixableUnbalanced = "( " + selUnbalanced.asString() + "\nEXCEPT\n" + selUnfixable.asString() + " )";

            final UpdateBuilder updateUnbalanced = new UpdateBuilder(ecritureT);
            updateUnbalanced.addVirtualJoin(selFixableUnbalanced, "semiArchivedMvt", false, ecritureMvtFF.getName(), ecritureMvtFF.getName());
            updateUnbalanced.set(ecritureT.getArchiveField().getName(), "0");

            getDS().execute(updateUnbalanced.asString());
        }

        // match SAISIE_KM_ELEMENT with their lost ECRITURE
        if (getSyntax().getSystem() == SQLSystem.H2) {
            Log.get().warning("Matching SAISIE_KM_ELEMENT with their lost ECRITURE unsupported");
        } else {
            final SQLTable saisieKmElemT = societeRoot.getGraph().findReferentTable(ecritureT, "SAISIE_KM_ELEMENT");
            final SQLTable saisieKmT = saisieKmElemT.getForeignTable("ID_SAISIE_KM");
            // select ECRITURE which can be identified in a MOUVEMENT by its CREDIT/DEBIT and isn't
            // already linked to a SAISIE_KM_ELEMENT
            final SQLSelect selIdentifiableNonUsed = new SQLSelect();
            final List<String> uniqueFields = Arrays.asList("ID_MOUVEMENT", "DEBIT", "CREDIT");
            selIdentifiableNonUsed.addAllSelect(ecritureT, uniqueFields);
            final String quotedID = ecritureT.getKey().getSQLName(ecritureT).quote();
            final String uniqueID;
            if (getSyntax().getSystem() == SQLSystem.POSTGRESQL)
                uniqueID = "(array_agg(" + quotedID + "))[1]";
            else
                uniqueID = "cast(GROUP_CONCAT(" + quotedID + ") as integer)";
            final String uniqueIDAlias = "ID";
            selIdentifiableNonUsed.addRawSelect(uniqueID, uniqueIDAlias);
            selIdentifiableNonUsed.addBackwardJoin("LEFT", null, saisieKmElemT.getField("ID_ECRITURE"), null);
            // unused
            selIdentifiableNonUsed.setWhere(Where.isNull(saisieKmElemT.getKey()));
            // identifiable
            for (final String uniqField : uniqueFields)
                selIdentifiableNonUsed.addGroupBy(ecritureT.getField(uniqField));
            selIdentifiableNonUsed.setHaving(Where.createRaw("count(*) = 1"));

            final UpdateBuilder update = new UpdateBuilder(saisieKmElemT);
            update.addForwardVirtualJoin(saisieKmT, "ID_SAISIE_KM");
            update.addRawTable("( " + selIdentifiableNonUsed.asString() + " )", "e");

            Where joinEcritureW = null;
            for (final String uniqField : uniqueFields) {
                final SQLTable t = uniqField.equals("ID_MOUVEMENT") ? saisieKmT : saisieKmElemT;
                joinEcritureW = Where.quote("e." + SQLBase.quoteIdentifier(uniqField) + "= %f", t.getField(uniqField)).and(joinEcritureW);
            }
            final Where dontOverwrite = new Where(saisieKmElemT.getField("ID_ECRITURE"), Where.NULL_IS_DATA_EQ, ecritureT.getUndefinedIDNumber());
            final Where dontUpdateUndef = new Where(saisieKmElemT.getKey(), Where.NULL_IS_DATA_NEQ, saisieKmElemT.getUndefinedIDNumber());
            final Where unarchived = new Where(saisieKmElemT.getArchiveField(), "=", 0);
            update.setWhere(joinEcritureW.and(dontOverwrite).and(dontUpdateUndef).and(unarchived));

            update.set("ID_ECRITURE", "e." + SQLBase.quoteIdentifier(uniqueIDAlias));

            getDS().execute(update.asString());
        }
    }
}
