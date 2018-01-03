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

import javax.swing.JComponent;
import javax.swing.JTextField;

public interface RightEditor {

    public JComponent getRightEditor(final String right, final DBRoot root, final SQLElementDirectory directory, final JTextField fieldObject);

    public void setValue(final String object, final DBRoot root, final SQLElementDirectory directory, final JComponent editorComponent);
}
