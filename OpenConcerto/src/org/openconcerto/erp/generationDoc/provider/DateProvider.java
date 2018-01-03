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
 
 package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;

import java.util.Date;

public class DateProvider extends UserInitialsValueProvider {

    private enum TypeDateProvider {
        TODAY, CREATION, MODIFICATION
    };

    private final TypeDateProvider type;

    public DateProvider(TypeDateProvider t) {
        this.type = t;
    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {

        if (this.type == TypeDateProvider.CREATION) {
            return context.getRow().getCreationDate().getTime();
        } else if (this.type == TypeDateProvider.MODIFICATION) {
            return context.getRow().getModificationDate().getTime();
        } else {
            return new Date();
        }
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("date.today", new DateProvider(TypeDateProvider.TODAY));
        SpreadSheetCellValueProviderManager.put("date.creation", new DateProvider(TypeDateProvider.CREATION));
        SpreadSheetCellValueProviderManager.put("date.modification", new DateProvider(TypeDateProvider.MODIFICATION));
    }
}
