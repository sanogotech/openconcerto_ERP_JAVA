package org.openconcerto.modules.common.batchprocessing;

import org.openconcerto.sql.model.SQLField;

public interface FieldFilter {
    boolean isFiltered(SQLField f);
}
