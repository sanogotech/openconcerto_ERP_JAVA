package org.openconcerto.modules.common.batchprocessing.product;

import java.math.BigDecimal;

import org.openconcerto.modules.common.batchprocessing.NumberProcessor;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;

public class PurchaseProcessor extends NumberProcessor {

    public PurchaseProcessor(SQLField field) {
        super(field);
    }

    @Override
    public void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to) {
        final BigDecimal ht = to.getBigDecimal("PA_HT");
        to.put("PRIX_METRIQUE_HA_1", ht);
    }
}
