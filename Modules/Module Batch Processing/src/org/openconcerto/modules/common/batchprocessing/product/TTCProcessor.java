package org.openconcerto.modules.common.batchprocessing.product;

import java.math.BigDecimal;

import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.utils.ConvertDevise;
import org.openconcerto.modules.common.batchprocessing.NumberProcessor;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;

public class TTCProcessor extends NumberProcessor {

    public TTCProcessor(SQLField field) {
        super(field);
    }

    @Override
    public void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to) {
        // M.A.J. du HT et du HT métrique
        final SQLRow r = from.asRow();
        final BigDecimal taux = BigDecimal.valueOf(TaxeCache.getCache().getTauxFromId(r.getForeignID("ID_TAXE")));
        final BigDecimal ttc = to.getBigDecimal("PV_TTC");
        final BigDecimal ht = ConvertDevise.getHtFromTtc(ttc, taux, from.getTable().getField("PV_HT").getType().getDecimalDigits());
        to.put("PV_HT", ht);
        to.put("PRIX_METRIQUE_VT_1", ht);
    }
}
