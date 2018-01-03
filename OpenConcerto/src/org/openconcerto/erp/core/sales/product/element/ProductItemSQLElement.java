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
 
 package org.openconcerto.erp.core.sales.product.element;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.sales.product.component.ProductItemGroup;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.utils.ListMap;

import java.util.ArrayList;
import java.util.List;

public class ProductItemSQLElement extends ComptaSQLConfElement {
    public static final String ELEMENT_CODE = "sales.product.bom";
    public static final String TABLE_PRODUCT_ITEM = "ARTICLE_ELEMENT";

    public ProductItemSQLElement() {
        super(TABLE_PRODUCT_ITEM, "un élément de nomenclature", " éléments de nomenclature");
    }

    @Override
    protected String createCode() {
        return ELEMENT_CODE;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ARTICLE");
        l.add("QTE_UNITAIRE");
        l.add("ID_UNITE_VENTE");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ARTICLE");
        l.add("QTE_UNITAIRE");
        l.add("ID_UNITE_VENTE");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        return new GroupSQLComponent(this, new ProductItemGroup());
    }

    @Override
    protected String getParentFFName() {

        return "ID_ARTICLE_PARENT";
    }
}
