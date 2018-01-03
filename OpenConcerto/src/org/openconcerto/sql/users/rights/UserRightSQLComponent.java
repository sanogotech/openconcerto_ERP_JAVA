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
 
 package org.openconcerto.sql.users.rights;

import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.ComboSQLRequest.KeepMode;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.CollectionUtils;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class UserRightSQLComponent extends GroupSQLComponent {

    public static String ID = "user.right";
    private JTextField objectField = new JTextField();
    private JPanel customEditorPanel = new JPanel(new GridBagLayout());
    private final ElementComboBox right = new ElementComboBox();
    private final GridBagConstraints cCustomPanel;
    private RightEditor currentRightEditor;
    private JComponent currentRightEditorComponent;

    private int userID = SQLRow.NONEXISTANT_ID;

    public UserRightSQLComponent(SQLElement element) {
        super(element, (Group) GlobalMapper.getInstance().get(ID));
        this.customEditorPanel.setBorder(null);
        this.customEditorPanel.setOpaque(false);
        this.cCustomPanel = new DefaultGridBagConstraints();
        this.cCustomPanel.weightx = 1;
        this.currentRightEditor = RightEditorManager.getInstance().getDefaultRightEditor();
        this.objectField.setVisible(false);
    }

    @Override
    public JComponent createEditor(String id) {
        if (id.equals("ID_USER_COMMON")) {
            final SQLRequestComboBox user = new SQLRequestComboBox();
            return user;
        } else if (id.equals("OBJECT")) {

            return this.objectField;
        } else if (id.equals("ID_RIGHT")) {
            this.right.setListIconVisible(false);
            return this.right;
        } else if (id.equals("user.right.parameters.editor")) {
            updateCodeEditor(null);
            return this.customEditorPanel;
        }
        return super.createEditor(id);
    }

    @Override
    public void select(SQLRowAccessor r) {
        // TODO Auto-generated method stub
        super.select(r);
        if (this.currentRightEditorComponent != null && r != null) {
            this.currentRightEditor.setValue(r.getString("OBJECT"), getTable().getDBRoot(), getElement().getDirectory(), this.currentRightEditorComponent);
        }
    }

    @Override
    public JComponent getLabel(String id) {
        JComponent comp = super.getLabel(id);
        if (id.equals("OBJECT")) {
            comp.setVisible(false);
        }
        return comp;

    }

    @Override
    protected Set<String> createRequiredNames() {
        return CollectionUtils.createSet("ID_RIGHT", "HAVE_RIGHT");
    }

    @Override
    protected void addViews() {
        super.addViews();
        SQLRequestComboBox user = (SQLRequestComboBox) getEditor("ID_USER_COMMON");
        user.getRequest().setUndefLabel("Par défaut");
        this.right.getRequest().addToGraphToFetch(Collections.singleton("CODE"));
        this.right.getRequest().keepRows(KeepMode.ROW);
        this.right.fillCombo();
        this.right.addModelListener("selectedValue", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                UserRightSQLComponent.this.objectField.setText("");

                UserRightSQLComponent.this.customEditorPanel.removeAll();
                updateCodeEditor((IComboSelectionItem) evt.getNewValue());
                UserRightSQLComponent.this.customEditorPanel.revalidate();
                UserRightSQLComponent.this.customEditorPanel.repaint();
            }
        });
    }

    private void updateCodeEditor(final IComboSelectionItem newValue) {
        final String code = newValue == null ? "" : newValue.getRow().getString("CODE");
        this.currentRightEditor = RightEditorManager.getInstance().getRightEditor(code);
        this.currentRightEditorComponent = this.currentRightEditor.getRightEditor(code, getTable().getDBRoot(), getElement().getDirectory(), this.objectField);
        this.customEditorPanel.add(this.currentRightEditorComponent, this.cCustomPanel);
    }

    @Override
    protected SQLRowValues createDefaults() {
        if (this.userID >= SQLRow.MIN_VALID_ID)
            return new SQLRowValues(getTable()).put("ID_USER_COMMON", this.userID);
        else
            return null;
    }

    public final void setUserID(int userID) {
        this.userID = userID;
    }
}
