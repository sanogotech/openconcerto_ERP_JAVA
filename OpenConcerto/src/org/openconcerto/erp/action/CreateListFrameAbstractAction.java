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
 
 package org.openconcerto.erp.action;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.ui.light.LightUIFrameProvider;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.light.ActivationOnSelectionControler;
import org.openconcerto.ui.light.ColumnSpec;
import org.openconcerto.ui.light.ColumnsSpec;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUIFrame;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.ui.light.LightUITable;
import org.openconcerto.ui.light.ListToolbarLine;
import org.openconcerto.ui.light.RowSelectionSpec;
import org.openconcerto.ui.light.TableSpec;
import org.openconcerto.utils.i18n.TranslationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.input.DOMBuilder;

public abstract class CreateListFrameAbstractAction extends CreateFrameAbstractAction implements LightUIFrameProvider {
    abstract public String getTableName();

    @Override
    public LightUIFrame getUIFrame(PropsConfiguration configuration) {
        // Get SQLElement
        SQLElement element = configuration.getDirectory().getElement(getTableName());
        final String elementCode = element.getCode();

        // Title of frame should be the element code with .title
        final String frameTitle = TranslationManager.getInstance().getTranslationForItem(elementCode + ".title");

        // Create frame
        final LightUIFrame frame = new LightUIFrame(elementCode);
        frame.createTitlePanel(frameTitle);

        // Create table
        final String tableId = element.getCode() + ".table";
        final LightUIElement table = getTableCustomEditorProvider(element).createUIElement(tableId);
        table.setFillWidth(true);

        // Get actions associate to the SQLElement and create buttons for them
        final Collection<IListeAction> actions = element.getRowActions();

        final LightUIPanel panel = frame.getFirstChild(LightUIPanel.class);
        final LightUILine l0 = new LightUILine();

        l0.setGridAlignment(LightUILine.ALIGN_LEFT);

        for (final Iterator<IListeAction> iterator = actions.iterator(); iterator.hasNext();) {
            RowAction iListeAction = (RowAction) iterator.next();
            if (iListeAction.inHeader()) {
                LightUIElement element2 = new LightUIElement(iListeAction.getID());
                element2.setType(LightUIElement.TYPE_BUTTON_WITH_CONTEXT);
                element2.setValue(iListeAction.getID());

                String label = TranslationManager.getInstance().getTranslationForAction(iListeAction.getID());

                element2.setLabel(label);

                l0.addChild(element2);

                panel.addControler(new ActivationOnSelectionControler(tableId, element2.getId()));

            }
        }
        panel.addChild(l0);

        final LightUILine l1 = new LightUILine();
        l1.setFillHeight(true);
        l1.setWeightY(1);
        l1.addChild(table);
        panel.addChild(l1);

        panel.addChild(new ListToolbarLine());

        frame.dump(System.out, 0);
        return frame;
    }

    public static CustomEditorProvider getTableCustomEditorProvider(final SQLElement element) {
        // generic list of elements
        return new CustomEditorProvider() {

            @Override
            public LightUIElement createUIElement(final String id) {
                final List<String> possibleColumnIds = new ArrayList<String>();
                final List<String> sortedIds = new ArrayList<String>();

                final SQLTableModelSourceOnline source = element.getTableSource();
                final List<SQLTableModelColumn> columns = source.getColumns();
                final List<ColumnSpec> columnsSpec = new ArrayList<ColumnSpec>(columns.size());

                // Get user preferences for this table
                final long userId = UserManager.getUserID();
                Document columnsPrefs = null;
                try {
                    final DOMBuilder in = new DOMBuilder();
                    final org.w3c.dom.Document w3cDoc = Configuration.getInstance().getXMLConf(userId, id);
                    if (w3cDoc != null) {
                        columnsPrefs = in.build(w3cDoc);
                    }
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Failed to get ColumnPrefs for table " + id + " and for user " + userId + "\n" + ex.getMessage());
                }

                // Create ColumnSpec from the SQLTableModelColumn
                final int sqlColumnsCount = columns.size();
                for (int i = 0; i < sqlColumnsCount; i++) {
                    final SQLTableModelColumn sqlColumn = columns.get(i);
                    // TODO : creer la notion d'ID un peu plus dans le l'esprit sales.invoice.amount
                    final String columnId = sqlColumn.getIdentifier();
                    possibleColumnIds.add(columnId);
                    columnsSpec.add(new ColumnSpec(columnId, sqlColumn.getValueClass(), sqlColumn.getName(), null, false, null));
                }

                // FIXME : recuperer l'info sauvegardée sur le serveur par user (à coder)
                sortedIds.add(columnsSpec.get(0).getId());

                // Create TableSpec
                final ColumnsSpec cSpec = new ColumnsSpec(element.getCode(), columnsSpec, possibleColumnIds, sortedIds);
                cSpec.setAllowMove(true);
                cSpec.setAllowResize(true);
                cSpec.setUserPrefs(columnsPrefs);

                final RowSelectionSpec selectionSpec = new RowSelectionSpec(id);
                final TableSpec tSpec = new TableSpec(id, selectionSpec, cSpec);

                // Create table
                final LightUITable e = new LightUITable(id);
                e.setTableSpec(tSpec);

                return e;
            }
        };
    }
}
