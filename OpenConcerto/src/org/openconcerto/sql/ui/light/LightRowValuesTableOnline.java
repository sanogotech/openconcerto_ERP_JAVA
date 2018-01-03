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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFunctionField;
import org.openconcerto.sql.model.SQLFunctionField.SQLFunction;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.list.ITableModel;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.light.SearchSpec;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.SwingUtilities;

import net.minidev.json.JSONObject;

public class LightRowValuesTableOnline extends LightRowValuesTable {
    private final ITransformer<SQLSelect, SQLSelect> orginTransformer;

    public LightRowValuesTableOnline(final Configuration configuration, final Number userId, final String id, final ITableModel model) {
        super(configuration, userId, id, model);

        this.orginTransformer = ((SQLTableModelSourceOnline) model.getReq()).getReq().getSelectTransf();
    }

    // Clone constructor
    public LightRowValuesTableOnline(final LightRowValuesTableOnline tableElement) {
        super(tableElement);

        this.orginTransformer = tableElement.orginTransformer;
    }

    // Json constructor
    public LightRowValuesTableOnline(final JSONObject json) {
        super(json);
        this.orginTransformer = null;
    }

    @Override
    public void doSearch(final Configuration configuration, final SearchSpec searchSpec, final int offset) {
        this.getTableSpec().setSearch(searchSpec);

        this.setOffset(offset);

        final SQLTableModelSource tableSource = this.getModel().getReq();
        final SearchInfo sInfo = (searchSpec != null) ? new SearchInfo(searchSpec) : null;

        final FutureTask<ListSQLRequest> f = new FutureTask<ListSQLRequest>(new Callable<ListSQLRequest>() {
            @Override
            public ListSQLRequest call() throws Exception {
                final ListSQLRequest req = tableSource.getReq();
                final List<SQLTableModelColumn> columns = tableSource.getColumns();
                req.setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(final SQLSelect sel) {
                        if (LightRowValuesTableOnline.this.orginTransformer != null) {
                            LightRowValuesTableOnline.this.orginTransformer.transformChecked(sel);
                        }
                        setWhere(sel, sInfo, columns);
                        return sel;
                    }
                });
                return req;
            }
        });

        // TODO: clean swing
        SwingUtilities.invokeLater(f);

        try {
            final ListSQLRequest req = f.get();

            // get values
            long t4 = System.currentTimeMillis();
            final List<SQLRowValues> rowValues = req.getValues();
            final int size = rowValues.size();
            long t5 = System.currentTimeMillis();

            System.err.println("DefaultTableContentHandler.handle() getValues() :" + size + " : " + (t5 - t4) + " ms");
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Apply filter on ListSQLRequest
     * 
     * @param sel - The ListSQLRequest select
     * @param sInfo - Search parameters
     */
    private final void setWhere(final SQLSelect sel, final SearchInfo sInfo, final List<SQLTableModelColumn> cols) {
        if (sInfo != null) {
            final Set<SQLField> fields = new HashSet<SQLField>();
            for (final SQLTableModelColumn sqlTableModelColumn : cols) {
                fields.addAll(sqlTableModelColumn.getFields());
            }
            final List<Where> wheres = new ArrayList<Where>();
            final List<Where> wFields = new ArrayList<Where>();

            final List<String> texts = sInfo.getTexts();
            for (String string : texts) {
                wFields.clear();
                for (final SQLField sqlField : fields) {
                    if (sqlField.getType().getJavaType().equals(String.class)) {
                        final Where w = new Where(new SQLFunctionField(SQLFunction.LOWER, sel.getAlias(sqlField)), "LIKE", "%" + string.toLowerCase() + "%");
                        wFields.add(w);
                    }
                }
                wheres.add(Where.or(wFields));
            }

            final Where w;
            if (sel.getWhere() != null) {
                w = Where.and(sel.getWhere(), Where.and(wheres));
            } else {
                w = Where.and(wheres);
            }
            sel.setWhere(w);
            System.err.println(sel.asString());
        }
    }
}
