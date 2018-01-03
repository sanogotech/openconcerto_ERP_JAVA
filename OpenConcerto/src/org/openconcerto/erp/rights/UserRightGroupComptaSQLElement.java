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

import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.users.rights.UserRightSQLComponent;
import org.openconcerto.sql.users.rights.UserRightSQLElement;

import javax.swing.JComponent;

public class UserRightGroupComptaSQLElement extends UserRightSQLElement {

    public UserRightGroupComptaSQLElement() {
        super();
        // ((Group)GlobalMapper.getInstance().get(UserRightSQLComponent.ID)).
    }

    @Override
    public SQLComponent createComponent() {
        return new UserRightGroupComp(this);
    }

    public final class UserRightGroupComp extends UserRightSQLComponent {

        public UserRightGroupComp(SQLElement element) {
            super(element);
        }

        // @Override
        // protected Set<String> createRequiredNames() {
        // final Set<String> s = new HashSet<String>();
        // s.add("NOM");
        // s.add("ID_ADRESSE");
        // s.add("ID_MODE_REGLEMENT");
        // return s;
        // }

        @Override
        public JComponent createEditor(String id) {
            // if (id.equals("INFOS")) {
            // return new ITextArea(4, 40);
            // } else if (id.equals("COMMENTAIRES")) {
            // return new ITextArea(10, 40);
            // }
            return super.createEditor(id);
        }

        // @Override
        // protected JComponent createLabel(String id) {
        // // TODO Auto-generated method stub
        // if (id.equals("OBJECT")) {
        // return GroupComboItem.getComboMenu();
        // }
        // return super.createLabel(id);
        // }

        @Override
        public JComponent getLabel(String id) {
            // if (id.equals("ID_MODE_REGLEMENT") || id.equals("INFOS") ||
            // id.startsWith("ID_ADRESSE")) {
            // JLabel l = (JLabel) super.getLabel(id);
            // l.setFont(l.getFont().deriveFont(Font.BOLD));
            // return l;
            // }
            // if (id.equals("customerrelationship.customer.contact")) {
            // return new JLabelBold("Contacts");
            // } else if (id.equals("customerrelationship.customer.payment")) {
            // return new JLabelBold("Mode de règlement");
            // } else
            if (id.equals("OBJECT")) {
                return MenuGroupComboItem.getComboMenu();
            }
            return super.getLabel(id);
        }

        // private int userID = SQLRow.NONEXISTANT_ID;
        //
        // private UserRightComp(SQLElement element) {
        // super(element, 2, 1);
        // }
        //
        // @Override
        // protected Set<String> createRequiredNames() {
        // return CollectionUtils.createSet("ID_RIGHT", "HAVE_RIGHT");
        // }
        //
        // public void addViews() {
        // final SQLRequestComboBox user = new SQLRequestComboBox();
        // this.addView(user, "ID_USER_COMMON", "0");
        // user.getRequest().setUndefLabel("Par défaut");
        // final ElementComboBox right = new ElementComboBox();
        // right.setListIconVisible(false);
        // this.addView(right, "ID_RIGHT");
        // this.addView("HAVE_RIGHT");
        // this.addView("OBJECT", "0");
        // }
        //
        // @Override
        // protected SQLRowValues createDefaults() {
        // if (this.userID >= SQLRow.MIN_VALID_ID)
        // return new SQLRowValues(getTable()).put("ID_USER_COMMON", this.userID);
        // else
        // return null;
        // }
        //
        // public final void setUserID(int userID) {
        // this.userID = userID;
        // }
    }
}
