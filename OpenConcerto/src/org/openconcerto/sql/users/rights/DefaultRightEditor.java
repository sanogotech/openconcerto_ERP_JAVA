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

import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.utils.text.SimpleDocumentListener;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

public class DefaultRightEditor implements RightEditor {

    @Override
    public JComponent getRightEditor(final String right, final DBRoot root, final SQLElementDirectory directory, final JTextField fieldObject) {
        final JTextField rightEditor = new JTextField(50);
        rightEditor.getDocument().addDocumentListener(new SimpleDocumentListener() {

            @Override
            public void update(DocumentEvent e) {
                fieldObject.setText(rightEditor.getText());

            }
        });
        return rightEditor;
    }

    @Override
    public void setValue(String object, DBRoot root, SQLElementDirectory directory, JComponent editorComponent) {
        ((JTextField) editorComponent).setText(object);
    }
}
