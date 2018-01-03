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

import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

public class RightEditorManager {

    private final static RightEditorManager INSTANCE = new RightEditorManager();

    private final Map<String, RightEditor> editors;
    private final RightEditor defaultEditor;

    private RightEditorManager() {
        this.editors = new HashMap<String, RightEditor>();
        this.defaultEditor = new DefaultRightEditor();
    }

    public static RightEditorManager getInstance() {
        return INSTANCE;
    }

    public RightEditor getRightEditor(String right) {
        assert SwingUtilities.isEventDispatchThread();
        RightEditor e = this.editors.get(right);
        if (e == null) {
            return this.defaultEditor;
        } else {
            return e;
        }
    }

    public void register(String right, RightEditor editor) {
        assert SwingUtilities.isEventDispatchThread();
        this.editors.put(right, editor);
    }

    public RightEditor getDefaultRightEditor() {
        return this.defaultEditor;
    }

}
