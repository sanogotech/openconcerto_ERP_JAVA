package org.openconcerto.modules.common.batchprocessing;

import java.sql.SQLException;
import java.util.List;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;

public interface BatchProcessor {

    boolean checkParameters();

    void process(List<SQLRowValues> r) throws SQLException;

    void processBeforeUpdate(SQLRowAccessor from, SQLRowValues to);

}
