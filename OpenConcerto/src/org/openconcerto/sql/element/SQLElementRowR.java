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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.element.SQLElement.EqualOption;
import org.openconcerto.sql.element.SQLElement.EqualOptionBuilder;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValuesCluster.DiffResult;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class SQLElementRowR extends BaseSQLElementRow {

    static private final Tuple2<List<Integer>, List<SQLRow>> findInterTreeLinks(final SQLRow r, final Map<SQLRow, SQLRow> copies, final SQLElementLink ff, final boolean returnCopies) {
        assert copies.containsKey(r) : "Wrong map";
        // foreigns at theses indexes points to a row inside the tree
        final List<Integer> foreignIndexes = new ArrayList<Integer>();
        // either the above foreigns or their copies
        final List<SQLRow> foreignRows = new ArrayList<SQLRow>();
        int i = 0;
        for (final SQLRow foreignRow : r.getDistantRows(ff.getPath())) {
            final SQLRow mapped = copies.get(foreignRow);
            if (mapped != null) {
                foreignIndexes.add(i);
                foreignRows.add(returnCopies ? mapped : foreignRow);
            }
            i++;
        }
        return Tuple2.create(foreignIndexes, foreignRows);
    }

    public SQLElementRowR(SQLRow row) {
        super(row);
    }

    public SQLElementRowR(SQLElement element, SQLRow row) {
        super(element, row);
    }

    @Override
    public boolean equals(Object obj) {
        return this.equals(obj, EqualOption.ALL);
    }

    // EqualOption.isParentTested() is only used for the root row, since this tests a tree of rows,
    // other rows' parents are implicitly tested.
    public boolean equals(Object obj, final EqualOption option) {
        if (obj instanceof SQLElementRowR) {
            final SQLElementRowR o = (SQLElementRowR) obj;

            // no need to allocate memory, make requests and check links
            if (this.getRow().equals(o.getRow()))
                return true;

            final EqualOption firstOption, noLinkOption;
            if (option.isParentTested() || option.isNonSharedTested()) {
                // links are tested by this, with the copies map, but the parent of the root row
                // should be tested
                final EqualOptionBuilder builder = option.createBuilder().setNonSharedTested(false);
                firstOption = builder.build();
                noLinkOption = builder.setParentTested(false).build();
            } else {
                firstOption = noLinkOption = option;
            }

            // test that each row is equal and has the same children. In doing so map our rows to
            // the other rows. I.e. copies contains all of our descendants.
            final Map<SQLRow, SQLRow> copies = new HashMap<SQLRow, SQLRow>();
            final Map<SQLRow, SQLRow> fromOtherCopies = new HashMap<SQLRow, SQLRow>();
            if (!equalsRec(o, copies, fromOtherCopies, firstOption, noLinkOption))
                return false;

            // For each descendant (including privates and joins) test if a normal relation points
            // to a known row.
            for (final SQLRow thisRow : copies.keySet()) {
                for (final SQLElementLink ff : getElement(thisRow).getOwnedLinks().getByType(SQLElementLink.LinkType.ASSOCIATION)) {
                    // thisRow and its copy should point to the same rows at the same indexes
                    // (excluding rows outside the trees)
                    if (option.isNonSharedTested() || ff.getOwned().isShared()) {
                        final Tuple2<List<Integer>, List<SQLRow>> interTreeLinks = findInterTreeLinks(thisRow, copies, ff, true);
                        // foreigns at theses indexes points to a row inside the tree
                        final List<Integer> foreignIndexes = interTreeLinks.get0();
                        // the copies of the above foreigns
                        final List<SQLRow> foreignRowsCopies = interTreeLinks.get1();

                        final Tuple2<List<Integer>, List<SQLRow>> otherInterTreeLinks = findInterTreeLinks(copies.get(thisRow), fromOtherCopies, ff, false);
                        // foreigns at theses indexes points to a row inside the tree
                        final List<Integer> otherForeignIndexes = otherInterTreeLinks.get0();
                        // the above foreigns
                        final List<SQLRow> otherForeignRows = otherInterTreeLinks.get1();

                        if (!foreignIndexes.equals(otherForeignIndexes) || !foreignRowsCopies.equals(otherForeignRows))
                            return false;
                    }
                }
            }

            return true;
        } else
            return false;
    }

    private boolean equalsRec(SQLElementRowR o, Map<SQLRow, SQLRow> fromThis, Map<SQLRow, SQLRow> fromOther, final EqualOption option, final EqualOption nextOption) {
        final Tuple2<Boolean, DiffResult> diff = this.getElem().diff(this.getRow(), o.getRow(), option);
        if (!diff.get0())
            return false;
        final Map<SQLTable, List<SQLRow>> children1 = this.getElem().getChildrenRows(this.getRow());
        final Map<SQLTable, List<SQLRow>> children2 = this.getElem().getChildrenRows(o.getRow());
        if (!children1.keySet().equals(children2.keySet()))
            return false;
        for (final SQLTable childT : children1.keySet()) {
            final List<SQLRow> l1 = children1.get(childT);
            final List<SQLRow> l2 = children2.get(childT);
            if (l1.size() != l2.size())
                return false;

            final Iterator<SQLRow> lIter1 = l1.iterator();
            final Iterator<SQLRow> lIter2 = l2.iterator();
            while (lIter1.hasNext()) {
                final SQLRow r1 = lIter1.next();
                final SQLRow r2 = lIter2.next();
                final SQLElementRowR o1 = new SQLElementRowR(r1);
                final SQLElementRowR o2 = new SQLElementRowR(r2);
                if (!o1.equalsRec(o2, fromThis, fromOther, nextOption, nextOption))
                    return false;
            }

        }

        final DiffResult diffRes = diff.get1();
        diffRes.fillRowMap(fromThis, true);
        diffRes.fillRowMap(fromOther, false);
        return true;
    }
}
