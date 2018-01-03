package org.openconcerto.modules.common.batchprocessing.product;

import java.math.BigDecimal;

import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.utils.ConvertDevise;
import org.openconcerto.modules.common.batchprocessing.ReferenceProcessor;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;

public class TVAProcessor extends ReferenceProcessor {

    public TVAProcessor(SQLField field) {
        super(field);
    }

    @Override
    public void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to) {
        // M.A.J. du TTC
        final SQLRow r = from.asRow();
        final BigDecimal taux = BigDecimal.valueOf(TaxeCache.getCache().getTauxFromId(to.getForeignID("ID_TAXE")));
        final BigDecimal ht = r.getBigDecimal("PV_HT");
        final BigDecimal ttc = ConvertDevise.getTtcFromHt(ht, taux, r.getTable().getField("PV_TTC").getType().getDecimalDigits());
        to.put("PV_TTC", ttc);
    }
}
