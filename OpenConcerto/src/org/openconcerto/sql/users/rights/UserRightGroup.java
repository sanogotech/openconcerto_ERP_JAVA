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

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class UserRightGroup extends Group {

    public UserRightGroup() {
        super("user.right.default");
        final Group g = new Group("user.right");

        g.addItem("ID_USER_COMMON");
        g.addItem("ID_RIGHT");
        g.addItem("OBJECT", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        g.addItem("user.right.parameters.editor", LayoutHints.DEFAULT_LARGE_FIELD_HINTS);
        g.addItem("HAVE_RIGHT");

        this.add(g);
    }

}
