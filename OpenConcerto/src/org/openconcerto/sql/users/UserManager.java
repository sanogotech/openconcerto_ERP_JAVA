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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableModifiedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class UserManager {
    private static UserManager instance;

    public synchronized static final UserManager getInstance() {
        if (instance == null && Configuration.getInstance() != null) {
            final SQLTable table = Configuration.getInstance().getRoot().findTable("USER_COMMON");
            if (table != null) {
                instance = new UserManager(table);
            }
        }
        return instance;
    }

    public static final User getUser() {
        final UserManager mngr = getInstance();
        return mngr == null ? null : mngr.getCurrentUser();
    }

    public static final int getUserID() {
        final User user = getUser();
        return user == null ? SQLRow.NONEXISTANT_ID : user.getId();
    }

    private final SQLTable t;
    @GuardedBy("this")
    private Map<Integer, User> byID;
    @GuardedBy("this")
    private boolean dirty;
    @GuardedBy("this")
    private User currentUser;

    private UserManager(final SQLTable t) {
        this.byID = Collections.emptyMap();
        this.currentUser = null;
        this.t = t;
        this.t.addTableModifiedListener(new SQLTableModifiedListener() {
            @Override
            public void tableModified(SQLTableEvent evt) {
                clearCache();
            }
        });
        fillUsers();
    }

    public synchronized void clearCache() {
        this.dirty = true;
    }

    private synchronized void fillUsers() {
        // to keep the ORDER for #getAllUser()
        final Map<Integer, User> mutable = new LinkedHashMap<Integer, User>();
        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(new SQLRowValues(this.t).setAllToNull());
        fetcher.setOrdered(true);
        for (final SQLRowValues v : fetcher.fetch()) {
            final User u = new User(v);
            mutable.put(v.getID(), u);
        }
        this.byID = Collections.unmodifiableMap(mutable);
        this.dirty = false;
    }

    public final SQLTable getTable() {
        return this.t;
    }

    private synchronized final Map<Integer, User> getUsers() {
        if (this.dirty) {
            fillUsers();
        }
        return this.byID;
    }

    public synchronized final User getCurrentUser() {
        return this.currentUser;
    }

    public synchronized void setCurrentUser(final int id) {
        this.currentUser = getUser(Integer.valueOf(id));
    }

    public List<User> getAllUser() {
        return new ArrayList<User>(this.getUsers().values());
    }

    public List<User> getAllActiveUsers() {
        final List<User> result = new ArrayList<User>();
        for (User user : this.getUsers().values()) {
            if (user.isActive()) {
                result.add(user);
            }
        }
        return result;
    }

    public User getUser(final Integer v) {
        final Map<Integer, User> users = this.getUsers();
        if (users.containsKey(v))
            return users.get(v);
        else
            throw new IllegalStateException("Bad user! " + v);
    }

}
