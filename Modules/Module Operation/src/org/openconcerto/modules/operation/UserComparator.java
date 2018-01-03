package org.openconcerto.modules.operation;

import java.util.Comparator;

import org.openconcerto.sql.users.User;

public class UserComparator implements Comparator<User> {

    @Override
    public int compare(User o1, User o2) {
        return o1.getFullName().compareToIgnoreCase(o2.getFullName());
    }

}
