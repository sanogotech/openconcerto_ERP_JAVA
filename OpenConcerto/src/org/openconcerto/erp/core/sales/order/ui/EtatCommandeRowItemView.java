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
 
 package org.openconcerto.erp.core.sales.order.ui;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.itemview.VWRowItemView;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.ui.valuewrapper.ValueWrapper;

public class EtatCommandeRowItemView extends VWRowItemView<EtatCommandeClient> {

    public EtatCommandeRowItemView(ValueWrapper<EtatCommandeClient> wrapper) {
        super(wrapper);
    }

    @Override
    public void setEditable(InteractionMode b) {
        if (this.getComp() != null) {
            this.getComp().setEnabled(b.isEnabled());
        }
    }

    @Override
    public void show(SQLRowAccessor r) {

        if (r.getObject(getField().getName()) != null) {
            this.getWrapper().setValue(EtatCommandeClient.fromID(r.getInt(getField().getName())));
        } else {
            this.getWrapper().setValue(null);
        }
    }

    @Override
    public void update(SQLRowValues vals) {
        vals.put(getField().getName(), this.isEmpty() ? SQLRowValues.SQL_DEFAULT : this.getWrapper().getValue().getId());
    }

}
