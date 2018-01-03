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
 
 package org.openconcerto.sql.view.list.search;

import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.sql.view.list.SQLTableModelColumns;
import org.openconcerto.utils.Tuple2;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

public final class SearchAll extends SearchRunnable {

    public SearchAll(final SearchQueue q) {
        super(q);
    }

    @Override
    public void run() {
        final Tuple2<List<ListSQLLine>, SQLTableModelColumns> fullListCopy = getAccess().getUpdateQ().copyFullList();
        final List<ListSQLLine> newList = this.filter(fullListCopy.get0());
        if (newList != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getAccess().setList(newList, fullListCopy.get1());
                }
            });
        }
    }

    /**
     * Filter a list.
     * 
     * @param tmp the list to filter.
     * @return the filtered list, or <code>null</code> if interrupted.
     */
    private List<ListSQLLine> filter(final List<ListSQLLine> tmp) {
        if (tmp == null)
            throw new NullPointerException();
        final List<ListSQLLine> res;
        if (!isFiltered()) {
            res = tmp;
        } else {
            final int fullSize = tmp.size();
            final ArrayList<ListSQLLine> l = new ArrayList<ListSQLLine>(fullSize);
            for (final ListSQLLine line : tmp) {
                // clear the interrupt flag
                if (Thread.interrupted()) {
                    return null;
                }
                if (this.matchFilterUnsafe(line))
                    l.add(line);
            }
            // trim if this frees at least 60 kB
            if (fullSize - l.size() > 15000)
                l.trimToSize();
            res = l;
        }
        return res;
    }

}
