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
 
 package org.openconcerto.erp.core.sales.order.element;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class StockCommande {

    Map<Integer, BigDecimal> map = new HashMap<Integer, BigDecimal>();

    public void addQty(Integer id, BigDecimal qty) {
        BigDecimal b = getQty(id);
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        b = b.add(qty);
        map.put(id, b);
    }

    public BigDecimal getQty(Integer id) {
        BigDecimal b = map.get(id);
        if (b == null) {
            b = BigDecimal.ZERO;
        }
        return b;
    }

    public Map<Integer, BigDecimal> getMap() {
        return this.map;
    }

}
