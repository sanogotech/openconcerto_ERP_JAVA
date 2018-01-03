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
 
 package org.openconcerto.sql.users;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.users.rights.UserRights;

import net.jcip.annotations.Immutable;

@Immutable
public final class User {
    private final int id;
    private final String name, firstName, nickName;
    private final UserRights userRights;
    private Boolean active;

    public User(final SQLRowAccessor r) {
        this.id = r.getID();
        this.name = r.getString("NOM").trim();
        this.firstName = r.getString("PRENOM").trim();
        this.nickName = r.getString("SURNOM").trim();
        if (r.getFields().contains("DISABLED")) {
            this.active = !r.getBoolean("DISABLED");
        } else {
            this.active = null;
        }
        this.userRights = new UserRights(this.getId());
    }

    public String getName() {
        return this.name;
    }

    public UserRights getRights() {
        return this.userRights;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return this.getFullName() + " /" + getId();
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getNickName() {
        return this.nickName;
    }

    public String getFullName() {
        return getFirstName() + " " + getName();
    }

    public boolean isActive() {
        return this.active;
    }
}
