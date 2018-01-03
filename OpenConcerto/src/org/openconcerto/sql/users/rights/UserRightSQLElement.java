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

import static java.util.Arrays.asList;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.utils.i18n.I18nUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UserRightSQLElement extends ConfSQLElement {
    public static final String TABLE_NAME = "USER_RIGHT";

    static public List<SQLCreateTable> getCreateTables(final SQLTable userT) {
        final DBRoot root = userT.getDBRoot();
        final SQLTable t = root.findTable(TABLE_NAME, false);
        if (t != null) {
            return Collections.emptyList();
        }

        final List<SQLCreateTable> res = new ArrayList<SQLCreateTable>();
        final SQLCreateTable create = new SQLCreateTable(root, TABLE_NAME);
        create.addForeignColumn(userT.getName());
        if (root.contains(RightSQLElement.TABLE_NAME)) {
            create.addForeignColumn(RightSQLElement.TABLE_NAME);
        } else {
            final SQLCreateTable createRight = RightSQLElement.getCreateTable(root);
            res.add(createRight);
            create.addForeignColumn(createRight);
        }
        // NULL meaning any
        create.addColumn("OBJECT", "varchar(150) NULL DEFAULT NULL");
        create.addColumn("HAVE_RIGHT", "boolean NOT NULL");
        res.add(create);

        return res;
    }

    public UserRightSQLElement() {
        super(TABLE_NAME);
        this.setL18nPackageName(I18nUtils.getPackageName(TM.class));
        final UserRightGroup group = new UserRightGroup();
        GlobalMapper.getInstance().map(UserRightSQLComponent.ID, group);
        setDefaultGroup(group);
    }

    protected List<String> getListFields() {
        // don't display USER to avoid undefined
        return asList("ID_RIGHT", "OBJECT", "HAVE_RIGHT");
    }

    protected List<String> getComboFields() {
        return getListFields();
    }

    @Override
    protected String getParentFFName() {
        return "ID_USER_COMMON";
    }

    @Override
    // r/o since we set it programmatically to prevent the edition of superuser rights
    public Set<String> getReadOnlyFields() {
        return Collections.singleton("ID_USER_COMMON");
    }

    public SQLComponent createComponent() {
        return new UserRightSQLComponent(this);
    }
}
