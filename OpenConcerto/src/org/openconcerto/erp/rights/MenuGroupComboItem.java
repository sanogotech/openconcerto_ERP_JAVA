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

import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.config.MenuManager;
import org.openconcerto.ui.component.combo.ISearchableCombo;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.model.DefaultIMutableListModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class MenuGroupComboItem {

    private final String id;
    private boolean isGroup;
    private boolean isToLevel;

    public MenuGroupComboItem(String id, boolean isGroup, boolean isTopLevel) {
        this.id = id;
        this.isGroup = isGroup;
        this.isToLevel = isTopLevel;
    }

    public String getId() {
        return id;
    }

    public String getTranslation() {
        final String mngrLabel = MenuManager.getInstance().getLabelForId(id);
        final Action mngrAction = MenuManager.getInstance().getActionForId(id);
        final String mngrActionName = mngrAction == null || mngrAction.getValue(Action.NAME) == null ? null : mngrAction.getValue(Action.NAME).toString();

        return MainFrame.getFirstNonEmpty(Arrays.asList(mngrLabel, mngrActionName, id));
    }

    @Override
    public String toString() {
        return getTranslation();
    }

    public static ISearchableCombo<MenuGroupComboItem> getComboMenu() {
        final ImageIcon icon = new ImageIcon(MenuGroupComboItem.class.getResource("submenu.png"));
        final ImageIcon icon2 = new ImageIcon(MenuGroupComboItem.class.getResource("group.png"));
        final ISearchableCombo<MenuGroupComboItem> box = new ISearchableCombo<MenuGroupComboItem>();
        box.setIconFactory(new ITransformer<MenuGroupComboItem, Icon>() {

            @Override
            public Icon transformChecked(MenuGroupComboItem input) {
                if (!input.isGroup) {
                    return icon;
                }
                if (!input.isToLevel) {
                    return icon2;
                }
                return null;
            }
        });

        DefaultIMutableListModel<MenuGroupComboItem> comboItems = new DefaultIMutableListModel<MenuGroupComboItem>();
        Group g = MenuManager.getInstance().getGroup();

        List<MenuGroupComboItem> result = new ArrayList<MenuGroupComboItem>();
        for (int i = 0; i < g.getSize(); i++) {
            final Group item = (Group) g.getItem(i);
            final MenuGroupComboItem comboItem = new MenuGroupComboItem(item.getId(), item.getSize() > 0, true);
            result.add(comboItem);
            getSubMenu(item, result);
        }

        comboItems.addAll(result);
        box.initCache(comboItems);
        return box;
    }

    private static void getSubMenu(Group g, List<MenuGroupComboItem> result) {
        for (int i = 0; i < g.getSize(); i++) {
            final Item item = g.getItem(i);
            final MenuGroupComboItem comboItem = new MenuGroupComboItem(item.getId(), item instanceof Group, false);
            result.add(comboItem);
            if (item instanceof Group) {
                getSubMenu((Group) item, result);
            }
        }
    }
}
