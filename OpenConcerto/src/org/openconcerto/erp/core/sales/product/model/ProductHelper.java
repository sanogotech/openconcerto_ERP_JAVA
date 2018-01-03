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
 
 package org.openconcerto.erp.core.sales.product.model;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProductHelper {

    private DBRoot root;

    public ProductHelper(DBRoot root) {
        this.root = root;
    }

    public interface PriceField {
    };

    public enum SupplierPriceField implements PriceField {
        PRIX_ACHAT, COEF_TRANSPORT_PORT, COEF_TAXE_D, COEF_TRANSPORT_SIEGE, COEF_FRAIS_MOULE, COEF_FRAIS_INDIRECTS, COEF_PRIX_MINI
    };

    public BigDecimal getEnumPrice(final SQLRowAccessor r, PriceField field) {
        final PriceField[] values = field.getClass().getEnumConstants();
        BigDecimal result = r.getBigDecimal(values[0].toString());
        if (result == null) {
            return null;
        }

        for (int i = 1; i < values.length; i++) {

            BigDecimal m0 = r.getBigDecimal(values[i].toString());
            if (m0 != null && m0.floatValue() > 0) {
                result = result.divide(m0, 2, RoundingMode.HALF_UP);
            }
            if (values[i] == field) {
                break;
            }
        }
        return result;
    }

    public BigDecimal getUnitCostForQuantity(SQLRowAccessor rArticle, int qty) {

        Collection<? extends SQLRowAccessor> l = rArticle.getReferentRows(rArticle.getTable().getTable("ARTICLE_PRIX_REVIENT"));
        BigDecimal result = null;

        for (SQLRowAccessor row : l) {

            if (row.getLong("QTE") > qty) {
                break;
            }
            result = row.getBigDecimal("PRIX");
        }
        if (result == null) {
            // Can occur during editing
            result = BigDecimal.ZERO;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<String> getRequiredProperties(int categoryId) {
        final SQLTable table = root.getTable("FAMILLE_CARACTERISTIQUE");
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(table.getField("NOM"));
        sel.setWhere(table.getField("ID_FAMILLE_ARTICLE"), "=", categoryId);
        final SQLDataSource src = root.getDBSystemRoot().getDataSource();
        return (List<String>) src.executeCol(sel.asString());
    }

    /**
     * Get the minimum quantity used to provide a cost for a product
     * 
     * @return -1 if no quantity are provided
     */
    public int getMinQuantityForCostCalculation(int productId) {
        final SQLTable costTable = root.getTable("ARTICLE_PRIX_REVIENT");
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(costTable.getKey());
        sel.addSelect(costTable.getField("ID_ARTICLE"));
        sel.addSelect(costTable.getField("QTE"));
        sel.setWhere(new Where(costTable.getField("ID_ARTICLE"), "=", productId));
        final SQLDataSource src = root.getDBSystemRoot().getDataSource();
        final List<SQLRow> l = (List<SQLRow>) src.execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));
        if (l.isEmpty()) {
            return -1;
        }
        int min = Integer.MAX_VALUE;
        for (SQLRow sqlRow : l) {
            int n = sqlRow.getInt("QTE");
            if (n < min) {
                min = n;
            }
        }
        return min;
    }

    /**
     * Get the cost for products and quantities
     * 
     * @return for each product ID the unit cost
     */
    public Map<Long, BigDecimal> getUnitCost(Map<Long, Integer> productQties, TypePrice type) {
        final Map<Long, BigDecimal> result = new HashMap<Long, BigDecimal>();

        String fieldPrice = (type == TypePrice.ARTICLE_TARIF_FOURNISSEUR || type == TypePrice.ARTICLE_TARIF_FOURNISSEUR_DDP ? "PRIX_ACHAT_DEVISE_F" : "PRIX");
        String fieldDate = (type == TypePrice.ARTICLE_TARIF_FOURNISSEUR || type == TypePrice.ARTICLE_TARIF_FOURNISSEUR_DDP ? "DATE_PRIX" : "DATE");

        // get all costs
        final SQLTable costTable = root.getTable(type == TypePrice.ARTICLE_TARIF_FOURNISSEUR_DDP ? "ARTICLE_TARIF_FOURNISSEUR" : type.name());
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(costTable.getKey());
        sel.addSelect(costTable.getField("ID_ARTICLE"));
        sel.addSelect(costTable.getField("QTE"));
        sel.addSelect(costTable.getField(fieldPrice));
        if (type == TypePrice.ARTICLE_TARIF_FOURNISSEUR_DDP) {
            for (SupplierPriceField f : SupplierPriceField.values()) {
                sel.addSelect(costTable.getField(f.name()));
            }
        }
        sel.addSelect(costTable.getField(fieldDate));
        sel.setWhere(new Where(costTable.getField("ID_ARTICLE"), true, productQties.keySet()));
        sel.addFieldOrder(costTable.getField("QTE"));
        sel.addFieldOrder(costTable.getField(fieldDate));
        final SQLDataSource src = root.getDBSystemRoot().getDataSource();
        @SuppressWarnings("unchecked")
        final List<SQLRow> l = (List<SQLRow>) src.execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));
        for (SQLRow sqlRow : l) {
            System.out.println(sqlRow.getID() + ":" + sqlRow.getAllValues());
        }
        final int size = l.size();
        if (size == 0 && type == TypePrice.ARTICLE_PRIX_REVIENT) {
            return getUnitCost(productQties, TypePrice.ARTICLE_TARIF_FOURNISSEUR_DDP);
        } else {
            for (Long id : productQties.keySet()) {
                BigDecimal cost = BigDecimal.ZERO;
                final int qty = productQties.get(id);
                for (int i = 0; i < size; i++) {
                    final SQLRow row = l.get(i);
                    if (row.getInt("ID_ARTICLE") == id.intValue()) {
                        // stop when the max qty is found
                        if (row.getLong("QTE") > qty) {
                            if (cost == null) {
                                if (type == TypePrice.ARTICLE_TARIF_FOURNISSEUR_DDP) {
                                    cost = getEnumPrice(row, SupplierPriceField.COEF_TRANSPORT_SIEGE);
                                } else {
                                    cost = row.getBigDecimal(fieldPrice);
                                }
                            }
                            break;
                        }
                        if (type == TypePrice.ARTICLE_TARIF_FOURNISSEUR_DDP) {
                            cost = getEnumPrice(row, SupplierPriceField.COEF_TRANSPORT_SIEGE);
                        } else {
                            cost = row.getBigDecimal(fieldPrice);
                        }

                    }
                }
                if (cost == null) {
                    cost = BigDecimal.ZERO;
                }

                result.put(id, cost);
            }
            return result;
        }
    }

    /**
     * 
     * @param items List de SQLRowAccessor avec ID_ARTICLE, QTE, QTE_UV
     * @return Map article qty
     */
    public List<ProductComponent> getChildWithQtyFrom(final List<ProductComponent> items) {

        return getChildWithQtyFrom(items, new HashSet<Integer>());
    }

    private List<ProductComponent> getChildWithQtyFrom(List<ProductComponent> items, Set<Integer> ancestors) {

        if (root.contains("ARTICLE_ELEMENT")) {

            int originalAncestorsSize = ancestors.size();

            List<ProductComponent> result = new ArrayList<ProductComponent>();

            // liste des ids parents
            final List<Integer> parentsArticleIDs = new ArrayList<Integer>();

            // Quantité par parents
            Map<Integer, ProductComponent> productCompByID = new HashMap<Integer, ProductComponent>();
            final Map<Integer, BigDecimal> qtyParent = new HashMap<Integer, BigDecimal>();
            for (ProductComponent p : items) {
                parentsArticleIDs.add(p.getProduct().getID());
                BigDecimal qty = BigDecimal.ZERO;
                if (qtyParent.get(p.getProduct().getID()) != null) {
                    qty = qtyParent.get(p.getProduct().getID());
                }
                qtyParent.put(p.getProduct().getID(), qty.add(p.getQty()));
            }

            // get all childs
            final SQLTable costTable = root.getTable("ARTICLE_ELEMENT");

            SQLRowValues rowVals = new SQLRowValues(costTable);

            final SQLRowValues stockRowValues = rowVals.putRowValues("ID_ARTICLE").put("ID", null).put("GESTION_STOCK", null).put("CODE", null).put("NOM", null).putRowValues("ID_STOCK");
            stockRowValues.putNulls("QTE_TH", "QTE_RECEPT_ATTENTE", "QTE_REEL", "QTE_LIV_ATTENTE");
            rowVals.putRowValues("ID_ARTICLE_PARENT").put("ID", null);
            rowVals.put("QTE", null);
            rowVals.put("QTE_UNITAIRE", null);

            SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(rowVals);
            fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

                @Override
                public SQLSelect transformChecked(SQLSelect input) {

                    input.setWhere(new Where(costTable.getField("ID_ARTICLE_PARENT"), parentsArticleIDs));
                    return input;
                }
            });

            List<SQLRowValues> childs = fetcher.fetch();

            if (childs.size() > 0) {

                for (SQLRowValues childRowValues : childs) {
                    final SQLRowAccessor foreignArticleParent = childRowValues.getForeign("ID_ARTICLE_PARENT");

                    if (!childRowValues.isForeignEmpty("ID_ARTICLE") && childRowValues.getForeign("ID_ARTICLE") != null) {
                        ProductComponent childComponent = ProductComponent.createFrom(childRowValues);
                        // Test pour éviter les boucles dans les boms
                        if (!ancestors.contains(childComponent.getProduct().getID())) {
                            ancestors.add(foreignArticleParent.getID());
                            // parentsArticleIDs.remove(foreignArticleParent.getID());
                            // Calcul de la quantité qte_unit * qte * qteMergedParent
                            childComponent.setQty(childComponent.getQty().multiply(qtyParent.get(foreignArticleParent.getID()), DecimalUtils.HIGH_PRECISION));

                            // Cumul des valeurs si l'article est présent plusieurs fois dans le bom
                            ProductComponent existProduct = productCompByID.get(childComponent.getProduct().getID());
                            if (existProduct == null) {
                                result.add(childComponent);
                                productCompByID.put(childComponent.getProduct().getID(), childComponent);
                            } else {
                                existProduct.addQty(childComponent.getQty());
                            }
                        }
                    }
                }

                // Recherche si un kit est présent parmis les articles
                final List<ProductComponent> bomFromChilds = getChildWithQtyFrom(new ArrayList(result), ancestors);
                // Merge des valeurs
                for (ProductComponent s : bomFromChilds) {

                    ProductComponent existProduct = productCompByID.get(s.getProduct().getID());
                    if (existProduct == null) {
                        result.add(s);
                        productCompByID.put(s.getProduct().getID(), s);
                    } else {
                        existProduct.addQty(s.getQty());
                    }
                }
            }

            // Ajout des articles présents dans l'ensemble de départ
            if (originalAncestorsSize == 0) {
                for (ProductComponent p : items) {
                    ProductComponent existProduct = productCompByID.get(p.getProduct().getID());
                    if (existProduct == null) {
                        result.add(p);
                        productCompByID.put(p.getProduct().getID(), p);
                    } else {
                        existProduct.addQty(p.getQty());
                    }
                }
            }

            // On supprime les ancestors (kits) du result
            for (Integer anc : ancestors) {
                ProductComponent comp = productCompByID.get(anc);
                if (comp != null) {
                    result.remove(comp);
                }
            }

            return result;
        } else {
            return items;
        }
    }

    public Map<Long, Integer> getBOM(Long productId) {
        final Map<Long, Integer> result = new HashMap<Long, Integer>();
        // get all costs
        final SQLTable costTable = root.getTable("ARTICLE_ELEMENT");
        final SQLSelect sel = new SQLSelect();

        sel.addSelect(costTable.getField("ID_ARTICLE"));
        sel.addSelect(costTable.getField("QTE"));

        sel.setWhere(new Where(costTable.getField("ID_ARTICLE_PARENT"), "=", productId));
        sel.addFieldOrder(costTable.getField("QTE"));
        final SQLDataSource src = root.getDBSystemRoot().getDataSource();
        @SuppressWarnings("unchecked")
        final List<SQLRow> l = (List<SQLRow>) src.execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));
        final int size = l.size();
        for (int i = 0; i < size; i++) {
            final SQLRow row = l.get(i);
            final long id = row.getLong("ID_ARTICLE");
            Integer qte = result.get(id);
            if (qte == null) {
                qte = row.getInt("QTE");
            } else {
                qte = qte + row.getInt("QTE");
            }
            result.put(id, qte);
        }

        return result;
    }

    public enum TypePrice {
        ARTICLE_PRIX_REVIENT, ARTICLE_PRIX_MIN_VENTE, ARTICLE_PRIX_PUBLIC, ARTICLE_TARIF_FOURNISSEUR, ARTICLE_TARIF_FOURNISSEUR_DDP
    };

    public BigDecimal getBomPriceForQuantity(int qty, Collection<? extends SQLRowAccessor> rowValuesProductItems, TypePrice type) {
        final Map<Long, Integer> productQties = new HashMap<Long, Integer>();
        int count = rowValuesProductItems.size();
        for (SQLRowAccessor v : rowValuesProductItems) {
            if (v.getObject("ID_ARTICLE") != null) {
                System.out.println("id:" + v.getObject("ID_ARTICLE"));
                int id = v.getForeignID("ID_ARTICLE");
                int qte = v.getInt("QTE") * qty;
                Integer qteForId = productQties.get(Long.valueOf(id));
                if (qteForId == null) {
                    productQties.put(Long.valueOf(id), qte);
                } else {
                    productQties.put(Long.valueOf(id), qte + qteForId);
                }
            }
        }
        Map<Long, BigDecimal> costs = getUnitCost(productQties, type);
        BigDecimal cost = null;
        for (SQLRowAccessor v : rowValuesProductItems) {
            if (v.getObject("ID_ARTICLE") != null) {
                int id = v.getForeignID("ID_ARTICLE");
                int qte = v.getInt("QTE");
                final BigDecimal unitCost = costs.get(Long.valueOf(id));
                BigDecimal lineCost = unitCost.multiply(BigDecimal.valueOf(qte)).multiply(v.getBigDecimal("QTE_UNITAIRE"));
                if (cost == null) {
                    cost = BigDecimal.ZERO;
                }
                cost = cost.add(lineCost);
            }
        }
        return cost;

    }

    public BigDecimal getUnitCost(int id, int qty, TypePrice type) {
        Map<Long, Integer> productQties = new HashMap<Long, Integer>();
        productQties.put(Long.valueOf(id), Integer.valueOf(qty));
        final Map<Long, BigDecimal> unitCost = getUnitCost(productQties, type);
        System.out.println(">" + unitCost);
        return unitCost.get(Long.valueOf(id));
    }

    public Date getUnitCostDate(int id, int qty, TypePrice type) {
        Map<Long, Integer> productQties = new HashMap<Long, Integer>();
        productQties.put(Long.valueOf(id), Integer.valueOf(qty));
        final Map<Long, Date> unitCost = getUnitCostDate(productQties, type);
        System.out.println(">" + unitCost);
        return unitCost.get(Long.valueOf(id));
    }

    private Map<Long, Date> getUnitCostDate(Map<Long, Integer> productQties, TypePrice type) {
        final Map<Long, Date> result = new HashMap<Long, Date>();

        String fieldPrice = (type == TypePrice.ARTICLE_TARIF_FOURNISSEUR ? "PRIX_ACHAT_DEVISE_F" : "PRIX");
        String fieldDate = (type == TypePrice.ARTICLE_TARIF_FOURNISSEUR ? "DATE_PRIX" : "DATE");

        // get all costs
        final SQLTable costTable = root.getTable(type.name());
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(costTable.getKey());
        sel.addSelect(costTable.getField("ID_ARTICLE"));
        sel.addSelect(costTable.getField("QTE"));
        sel.addSelect(costTable.getField(fieldPrice));
        sel.addSelect(costTable.getField(fieldDate));
        sel.setWhere(new Where(costTable.getField("ID_ARTICLE"), true, productQties.keySet()));
        sel.addFieldOrder(costTable.getField("QTE"));
        sel.addFieldOrder(costTable.getField(fieldDate));
        final SQLDataSource src = root.getDBSystemRoot().getDataSource();
        @SuppressWarnings("unchecked")
        final List<SQLRow> l = (List<SQLRow>) src.execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));
        for (SQLRow sqlRow : l) {
            System.out.println(sqlRow.getID() + ":" + sqlRow.getAllValues());
        }
        final int size = l.size();
        for (Long id : productQties.keySet()) {
            Calendar cost = null;
            final int qty = productQties.get(id);
            for (int i = 0; i < size; i++) {
                final SQLRow row = l.get(i);
                if (row.getInt("ID_ARTICLE") == id.intValue()) {
                    // stop when the max qty is found
                    if (row.getLong("QTE") > qty) {
                        if (cost == null) {
                            cost = row.getDate("DATE");
                        }
                        break;
                    }
                    cost = row.getDate("DATE");

                }
            }
            if (cost != null)
                result.put(id, cost.getTime());
            else
                result.put(id, new Date());
        }
        return result;
    }
}
