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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.erp.core.common.element.StyleSQLElement;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OOXMLCache {

    private Map<SQLRowAccessor, Map<SQLTable, List<SQLRowAccessor>>> cacheReferent = new HashMap<SQLRowAccessor, Map<SQLTable, List<SQLRowAccessor>>>();
    private Map<String, Map<Integer, SQLRowAccessor>> cacheForeign = new HashMap<String, Map<Integer, SQLRowAccessor>>();

    protected SQLRowAccessor getForeignRow(SQLRowAccessor row, SQLField field) {
        Map<Integer, SQLRowAccessor> c = cacheForeign.get(field.getName());

        if (row.getObject(field.getName()) == null) {
            return null;
        }

        int i = row.getForeignID(field.getName());

        if (c != null && c.get(i) != null) {
            return c.get(i);
        } else {

            SQLRowAccessor foreign = row.getForeign(field.getName());

            if (c == null) {
                Map<Integer, SQLRowAccessor> map = new HashMap<Integer, SQLRowAccessor>();
                map.put(i, foreign);
                cacheForeign.put(field.getName(), map);
            } else {
                c.put(i, foreign);
            }

            return foreign;
        }

    }

    protected List<? extends SQLRowAccessor> getReferentRows(List<? extends SQLRowAccessor> row, SQLTable tableForeign) {
        return getReferentRows(row, tableForeign, null, null, false, null, false);
    }

    protected List<? extends SQLRowAccessor> getReferentRows(List<? extends SQLRowAccessor> row, final SQLTable tableForeign, String groupBy, final String orderBy, boolean expandNomenclature,
            String foreignField, boolean excludeQteZero) {
        Map<SQLTable, List<SQLRowAccessor>> c = cacheReferent.get(row.get(0));

        if (c != null && c.get(tableForeign) != null) {
            System.err.println("get referent rows From Cache ");
            return c.get(tableForeign);
        } else {
            List<SQLRowAccessor> list;
            if (row.isEmpty() || (row.size() > 0 && row.get(0).isUndefined())) {
                list = new ArrayList<SQLRowAccessor>();
            } else if (row.size() > 0 && (groupBy == null || groupBy.trim().length() == 0)) {

                list = new ArrayList<SQLRowAccessor>();
                SQLSelect sel = new SQLSelect();
                sel.addSelectStar(tableForeign);

                Where w = null;
                if (foreignField != null && foreignField.trim().length() > 0) {
                    for (SQLRowAccessor rowAccess : row) {
                        if (rowAccess != null && !rowAccess.isUndefined()) {
                            if (w == null) {
                                w = new Where((SQLField) tableForeign.getField(foreignField), "=", rowAccess.getID());
                            } else {
                                w = w.or(new Where((SQLField) tableForeign.getField(foreignField), "=", rowAccess.getID()));
                            }
                        }
                    }
                } else {
                    for (SQLRowAccessor rowAccess : row) {
                        if (rowAccess != null && !rowAccess.isUndefined()) {
                            if (w == null) {
                                w = new Where((SQLField) tableForeign.getForeignKeys(rowAccess.getTable()).toArray()[0], "=", rowAccess.getID());
                            } else {
                                w = w.or(new Where((SQLField) tableForeign.getForeignKeys(rowAccess.getTable()).toArray()[0], "=", rowAccess.getID()));
                            }
                        }
                    }
                }

                if (excludeQteZero) {
                    w = w.and(new Where(tableForeign.getField("QTE"), "!=", 0));
                }
                sel.setWhere(w);
                addSelectOrder(tableForeign, orderBy, sel);
                System.err.println(sel.asString());
                list.addAll(SQLRowListRSH.execute(sel));

            } else {

                final List<String> params = SQLRow.toList(groupBy);
                SQLSelect sel = new SQLSelect();
                sel.addSelect(tableForeign.getKey());
                for (int i = 0; i < params.size(); i++) {
                    sel.addSelect(tableForeign.getField(params.get(i)));
                }

                Where w = null;
                for (SQLRowAccessor rowAccess : row) {
                    if (w == null) {
                        w = new Where((SQLField) tableForeign.getForeignKeys(rowAccess.getTable()).toArray()[0], "=", rowAccess.getID());
                    } else {
                        w = w.or(new Where((SQLField) tableForeign.getForeignKeys(rowAccess.getTable()).toArray()[0], "=", rowAccess.getID()));
                    }
                }
                sel.setWhere(w);
                addSelectOrder(tableForeign, orderBy, sel);
                System.err.println(sel.asString());
                List<SQLRow> result = SQLRowListRSH.execute(sel);

                list = new ArrayList<SQLRowAccessor>();
                Map<Object, SQLRowValues> m = new HashMap<Object, SQLRowValues>();
                for (SQLRow sqlRow : result) {
                    SQLRowValues rowVals;
                    final Integer object = sqlRow.getInt(params.get(0));
                    if (m.get(object) == null || object == 1) {
                        rowVals = sqlRow.asRowValues();
                        // if (object != 1) {
                        // rowVals.put("ID_STYLE", 3);
                        // }
                        m.put(object, rowVals);
                        list.add(rowVals);
                    } else {
                        rowVals = m.get(object);
                        cumulRows(params, sqlRow, rowVals);
                    }
                }
            }

            if (expandNomenclature) {
                list = expandNomenclature(list);
            }

            if (c == null) {
                Map<SQLTable, List<SQLRowAccessor>> map = new HashMap<SQLTable, List<SQLRowAccessor>>();
                map.put(tableForeign, list);
                cacheReferent.put(row.get(0), map);
            } else {
                c.put(tableForeign, list);
            }

            if (orderBy != null && orderBy.trim().length() > 0 && !orderBy.contains(".")) {
                Collections.sort(list, new Comparator<SQLRowAccessor>() {
                    @Override
                    public int compare(SQLRowAccessor o1, SQLRowAccessor o2) {

                        return CompareUtils.compare(o1.getObject(orderBy), o2.getObject(orderBy));
                    }
                });
            }

            return list;
        }
        // return row.getReferentRows(tableForeign);
    }

    private List<SQLRowAccessor> expandNomenclature(List<SQLRowAccessor> list) {
        final List<Integer> idsArt = new ArrayList<Integer>(list.size());
        DBRoot root = null;
        SQLTable table = null;
        for (SQLRowAccessor r : list) {
            root = r.getTable().getDBRoot();
            table = r.getTable();
            if (!r.isForeignEmpty("ID_ARTICLE")) {
                idsArt.add(r.getForeignID("ID_ARTICLE"));
            }
        }
        List<SQLRowAccessor> result = new ArrayList<SQLRowAccessor>();
        if (root != null) {

            Map<String, Integer> style = Configuration.getInstance().getDirectory().getElement(StyleSQLElement.class).getAllStyleByName();

            final SQLTable tableArtElt = root.getTable("ARTICLE_ELEMENT");
            SQLRowValues rowVals = new SQLRowValues(tableArtElt);
            rowVals.putNulls("QTE", "QTE_UNITAIRE", "ID_UNITE_VENTE", "ID_ARTICLE_PARENT");
            rowVals.putRowValues("ID_ARTICLE").setAllToNull();
            SQLRowValuesListFetcher fetch = SQLRowValuesListFetcher.create(rowVals);
            fetch.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    input.setWhere(new Where(input.getAlias(tableArtElt.getField("ID_ARTICLE_PARENT")), idsArt));
                    return input;
                }
            });
            List<SQLRowValues> l = fetch.fetch();
            ListMap<Integer, SQLRowValues> map = new ListMap<Integer, SQLRowValues>();
            for (SQLRowValues sqlRowValues : l) {
                SQLRowValues rowInj = new SQLRowValues(table);
                final SQLRowAccessor foreignArt = sqlRowValues.getForeign("ID_ARTICLE");
                rowInj.put("ID_ARTICLE", foreignArt.asRowValues());
                rowInj.put("QTE", sqlRowValues.getInt("QTE"));
                rowInj.put("NOM", foreignArt.getObject("NOM"));
                rowInj.put("CODE", foreignArt.getObject("CODE"));
                rowInj.put("QTE_UNITAIRE", sqlRowValues.getObject("QTE_UNITAIRE"));
                rowInj.put("ID_UNITE_VENTE", sqlRowValues.getObject("ID_UNITE_VENTE"));
                rowInj.put("PV_HT", BigDecimal.ZERO);
                rowInj.put("T_PV_HT", BigDecimal.ZERO);

                rowInj.put("ID_TAXE", TaxeCache.getCache().getFirstTaxe().getID());
                rowInj.put("NIVEAU", 1);
                rowInj.put("ID_STYLE", style.get("Composant"));
                map.add(sqlRowValues.getForeignID("ID_ARTICLE_PARENT"), rowInj);
            }

            int size = list.size();

            for (int i = 0; i < size; i++) {

                SQLRowAccessor sqlRowAccessor = list.get(i);
                result.add(sqlRowAccessor);

                if (!sqlRowAccessor.isForeignEmpty("ID_ARTICLE") && sqlRowAccessor.getInt("NIVEAU") == 1) {
                    final List<SQLRowValues> c = map.get(sqlRowAccessor.getForeignID("ID_ARTICLE"));
                    if (c != null) {
                        if (i + 1 < size) {
                            SQLRowAccessor rowAccessorNext = list.get(i + 1);
                            if (rowAccessorNext.getInt("NIVEAU") == 1) {
                                for (SQLRowValues sqlRowValues : c) {
                                    sqlRowValues.put("QTE", sqlRowValues.getInt("QTE") * sqlRowAccessor.getInt("QTE"));
                                    result.add(sqlRowValues);
                                }
                            }
                        } else {
                            for (SQLRowValues sqlRowValues : c) {
                                sqlRowValues.put("QTE", sqlRowValues.getInt("QTE") * sqlRowAccessor.getInt("QTE"));
                                result.add(sqlRowValues);
                            }
                        }
                    }
                }

            }

        }
        return result;
    }

    private void addSelectOrder(final SQLTable tableForeign, final String orderBy, SQLSelect sel) {
        if (orderBy != null && orderBy.contains(".")) {
            String fieldRefTable = orderBy.substring(0, orderBy.indexOf('.'));
            String field = orderBy.substring(orderBy.indexOf('.') + 1, orderBy.length());
            sel.addJoin("LEFT", sel.getAlias(tableForeign).getField(fieldRefTable));
            sel.addFieldOrder(sel.getAlias(tableForeign.getForeignTable(fieldRefTable)).getField(field));
        } else {
            sel.addFieldOrder(tableForeign.getOrderField());
        }
    }

    private void cumulRows(final List<String> params, SQLRow sqlRow, SQLRowValues rowVals) {

        for (int i = 1; i < params.size(); i++) {

            if (rowVals.getTable().getField(params.get(i)).getType().getJavaType() == String.class) {
                String string = sqlRow.getString(params.get(i));
                if (params.get(i).equalsIgnoreCase("NOM")) {
                    string = sqlRow.getInt("QTE") + " x " + string;
                }
                rowVals.put(params.get(i), rowVals.getString(params.get(i)) + ", " + string);
            } else if (!rowVals.getTable().getField(params.get(i)).isKey()) {
                Long n = rowVals.getLong(params.get(i));
                rowVals.put(params.get(i), n + sqlRow.getLong(params.get(i)));
            }

        }
    }

    public Map<SQLRowAccessor, Map<SQLTable, List<SQLRowAccessor>>> getCacheReferent() {
        return cacheReferent;
    }

    public void clearCache() {
        cacheReferent.clear();
        cacheForeign.clear();
    }
}
