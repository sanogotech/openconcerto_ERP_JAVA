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
 
 package org.openconcerto.erp.rights;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.utils.i18n.TranslationManager;

import java.util.ArrayList;
import java.util.List;

public class GroupItemUIComboItem {

    private final String id;
    private final SQLElement elt;

    public GroupItemUIComboItem(String id, SQLElement elt) {
        this.id = id;
        this.elt = elt;
    }

    public String getId() {
        return id;
    }

    public String getTranslation() {
        String translationForItem = TranslationManager.getInstance().getTranslationForItem(id);
        if (translationForItem != null && translationForItem.trim().length() > 0) {
            return translationForItem;
        }

        if (this.elt.getTable().contains(id)) {
            String fieldLabel = Configuration.getInstance().getTranslator().getLabelFor(this.elt.getTable().getField(id));
            if (fieldLabel != null && fieldLabel.trim().length() > 0) {
                return fieldLabel;
            }
        }

        return id;
    }

    @Override
    public String toString() {
        return getTranslation();
    }

    public static List<GroupItemUIComboItem> getComboMenu(Group g, SQLElement elt) {
        final List<GroupItemUIComboItem> result = new ArrayList<GroupItemUIComboItem>();
        getSubMenu(g, result, elt);

        return result;
    }

    private static void getSubMenu(Group g, List<GroupItemUIComboItem> result, SQLElement elt) {
        final int size = g.getSize();
        for (int i = 0; i < size; i++) {
            final Item item = g.getItem(i);
            result.add(new GroupItemUIComboItem(item.getId(), elt));
            if (item instanceof Group) {
                getSubMenu((Group) item, result, elt);
            }
        }
    }
}
