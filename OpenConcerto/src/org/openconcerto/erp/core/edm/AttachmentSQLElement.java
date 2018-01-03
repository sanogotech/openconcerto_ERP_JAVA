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
 
 package org.openconcerto.erp.core.edm;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

public class AttachmentSQLElement extends ComptaSQLConfElement {

    public final static String DIRECTORY_PREFS = "EDMdirectory";

    public AttachmentSQLElement() {
        super("ATTACHMENT", "un attachement", "attachements");

    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("SOURCE_TABLE");
        l.add("SOURCE_ID");
        l.add("NAME");
        l.add("MIMETYPE");
        l.add("FILENAME");
        l.add("STORAGE_PATH");
        l.add("THUMBNAIL");
        l.add("THUMBNAIL_WIDTH");
        l.add("THUMBNAIL_HEIGHT");
        l.add("TAG");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NAME");
        return l;
    }

    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            public void addViews() {
                this.setLayout(new GridBagLayout());

                final GridBagConstraints c = new DefaultGridBagConstraints();
                c.anchor = GridBagConstraints.NORTHEAST;
                c.gridwidth = 1;

                // // Numero
                // JLabel labelNumero = new JLabel("Numéro ");
                // this.add(labelNumero, c);
                //
                // JTextField textNumero = new JTextField();
                // c.gridx++;
                // c.weightx = 1;
                // this.add(textNumero, c);

                // this.addRequiredSQLObject(textNumero, "NUMERO");
            }
        };
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".attachment";
    }
}
