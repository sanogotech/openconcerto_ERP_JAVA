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
 
 package org.openconcerto.erp.core.sales.pos.model;

import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.utils.DecimalUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class Article {
    private Categorie s;
    private String name;
    private BigDecimal priceTTC;
    private int idTaxe;
    private BigDecimal priceHT;
    private String barCode = "empty barcode";
    private String code = "";
    private final int id;
    private static Map<String, Article> codes = new HashMap<String, Article>();

    public Article(Categorie s1, String string, int id) {
        this.s = s1;
        this.id = id;
        this.name = string;
        s1.addArticle(this);
    }

    public Article(Article a) {
        this.s = a.s;
        this.name = a.name;
        this.priceTTC = a.priceTTC;
        this.idTaxe = a.idTaxe;
        this.priceHT = a.priceHT;
        this.barCode = a.barCode;
        this.id = a.id;
        this.s.addArticle(this);
    }

    public int getId() {
        return this.id;
    }

    public BigDecimal getPriceWithoutTax() {
        return this.priceHT;
    }

    public void setPriceWithoutTax(BigDecimal priceHTInCents) {
        this.priceHT = priceHTInCents;
    }

    public int getIdTaxe() {
        return this.idTaxe;
    }

    public void setIdTaxe(int idTaxe) {
        this.idTaxe = idTaxe;
    }

    public void setBarCode(String bar) {
        this.barCode = bar;
        codes.put(bar, this);
    }

    public void setPriceWithTax(BigDecimal priceInCents) {
        this.priceTTC = priceInCents;
    }

    public BigDecimal getPriceWithTax() {
        return this.priceTTC;
    }

    public String getName() {
        return this.name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    };

    public String getBarCode() {
        return this.barCode;
    }

    public Categorie getCategorie() {
        return this.s;
    }

    @Override
    public String toString() {
        return "Article:" + this.name + " " + this.priceTTC + " cents" + "(HT:" + priceHT + ")";
    }

    public static Article getArticleFromBarcode(String code) {
        return codes.get(code);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        return this.id == ((Article) obj).id;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public void updatePriceWithoutTax(BigDecimal ht) {
        this.priceHT = ht;
        this.priceTTC = computePriceWithTax(ht, this.getIdTaxe());
    }

    public static BigDecimal computePriceWithTax(BigDecimal ht, int idTaxe) {
        final BigDecimal tax = new BigDecimal(TaxeCache.getCache().getTauxFromId(idTaxe)).movePointLeft(2).add(BigDecimal.ONE);
        return ht.multiply(tax).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal computePriceWithoutTax(BigDecimal ttc, int idTaxe) {
        final BigDecimal tax = new BigDecimal(TaxeCache.getCache().getTauxFromId(idTaxe)).movePointLeft(2).add(BigDecimal.ONE);
        return ttc.divide(tax, DecimalUtils.HIGH_PRECISION).setScale(6, RoundingMode.HALF_UP);
    }
}
