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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.element.SQLElement.ReferenceAction;
import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLRowValuesCluster;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.LockStrength;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.utils.CollectionMap2.Mode;
import org.openconcerto.utils.CollectionMap2Itf.ListMapItf;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.CustomEquals;
import org.openconcerto.utils.cc.CustomEquals.ProxyItf;
import org.openconcerto.utils.cc.HashingStrategy;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Cache several trees of rows (a row and its descendants).
 * 
 * @author Sylvain
 */
public final class TreesOfSQLRows {

    public static final TreesOfSQLRows createFromIDs(final SQLElement elem, final Collection<? extends Number> ids) {
        final List<SQLRow> rows = new ArrayList<SQLRow>(ids.size());
        for (final Number id : ids) {
            // don't access the DB here, expand() will do it once for all rows
            rows.add(new SQLRow(elem.getTable(), id.intValue()));
        }
        return new TreesOfSQLRows(elem, rows);
    }

    private static String createRestrictDesc(SQLElement refElem, SQLRowAccessor refVals, SQLElementLink elemLink) {
        final String rowDesc = refElem != null ? refElem.getDescription(refVals.asRow()) : refVals.asRow().toString();
        final String fieldS = getLabel(elemLink, null);
        // la tâche du 26/05 ne peut perdre son champ UTILISATEUR
        return TM.getTM().trM("sqlElement.linkCantBeCut", CollectionUtils.createMap("row", refVals.asRow(), "rowDesc", rowDesc, "fieldLabel", fieldS));
    }

    private static String getLabel(SQLElementLink elemLink, Path p) {
        final SQLFieldTranslator translator = Configuration.getInstance().getTranslator();
        final SQLTable table;
        final String itemName;
        if (elemLink != null) {
            assert p == null || elemLink.getPath().equals(p);
            table = elemLink.getPath().getFirst();
            itemName = elemLink.getName();
        } else {
            assert p.length() == 1 : "Joins should have an Element : " + p;
            assert p.getDirection() == Direction.FOREIGN;
            final SQLField singleField = p.getStep(0).getSingleField();
            table = singleField.getTable();
            itemName = singleField.getName();
        }
        final String fieldLabel = translator.getDescFor(table, itemName).getLabel();
        return fieldLabel != null ? fieldLabel : itemName;
    }

    private final SQLElement elem;
    private final Set<SQLRow> originalRoots;
    private Map<SQLRow, SQLRowValues> trees;
    private Set<SQLRow> mainRows;
    private Map<SQLRow, SQLRowValues> allRows;
    private LinksToCut externReferences;

    public TreesOfSQLRows(final SQLElement elem, SQLRow row) {
        this(elem, Collections.singleton(row));
    }

    public TreesOfSQLRows(final SQLElement elem, final Collection<? extends SQLRowAccessor> rows) {
        super();
        this.elem = elem;
        this.originalRoots = new HashSet<SQLRow>();
        this.trees = null;
        // check each row and remove duplicates (i.e. this.originalRoots might be smaller than rows)
        for (final SQLRowAccessor r : rows) {
            this.elem.check(r);
            this.originalRoots.add(r.asRow());
        }
        this.externReferences = null;
    }

    public final SQLElement getElem() {
        return this.elem;
    }

    /**
     * The unique rows that were passed to the constructor. NOTE : the rows that do not exist or are
     * archived won't be in {@link #getTrees()}.
     * 
     * @return a set of rows.
     */
    public final Set<SQLRow> getRows() {
        return this.originalRoots;
    }

    public final Set<SQLRow> getMainRows() {
        return this.mainRows;
    }

    public final Set<SQLRow> getAllRows() {
        return this.allRows.keySet();
    }

    /**
     * Whether the passed trees are contained in this. NOTE : only mains rows are considered, not
     * private ones.
     * 
     * @param o other trees.
     * @return <code>true</code> if all {@link #getMainRows() main rows} of <code>o</code> are in
     *         this and all {@link #getExternReferences() links to cut} as well.
     */
    public final boolean containsAll(final TreesOfSQLRows o) {
        if (this == o)
            return true;
        return this.getMainRows().containsAll(o.getMainRows()) && this.getExternReferences().containsAll(o.getExternReferences());
    }

    public final boolean isFetched() {
        return this.externReferences != null;
    }

    private final void checkFetched() {
        if (!this.isFetched())
            throw new IllegalStateException("Not yet fetched");
    }

    /**
     * Fetch the rows.
     * 
     * @param ls how to lock rows.
     * @return the rows to archive indexed by their roots (a subset of {@link #getRows()}).
     * @throws SQLException if an error occurs.
     */
    public final Map<SQLRow, SQLRowValues> fetch(final LockStrength ls) throws SQLException {
        if (this.isFetched())
            throw new IllegalStateException("Already fetched");

        final Tuple3<Map<SQLRow, SQLRowValues>, Rows, LinksToCut> expand = this.expand(ls);
        this.trees = Collections.unmodifiableMap(expand.get0());
        this.mainRows = Collections.unmodifiableSet(expand.get1().mainRows);
        this.allRows = Collections.unmodifiableMap(expand.get1().vals);
        this.externReferences = expand.get2();

        if (hasFetchedLess())
            Log.get().fine("Some rows are missing : " + this.trees.keySet() + "\n" + this.getRows());
        return this.getTrees();
    }

    public final boolean hasFetchedLess() {
        final Set<SQLRow> rowsFetched = this.trees.keySet();
        assert this.getRows().containsAll(rowsFetched);
        // archived or deleted (or never existed)
        return !rowsFetched.equals(this.getRows());
    }

    /**
     * The trees of rows to archive.
     * 
     * @return the rows to archive indexed by their roots (a subset of {@link #getRows()}).
     */
    public final Map<SQLRow, SQLRowValues> getTrees() {
        checkFetched();
        return this.trees;
    }

    public final Set<SQLRowValuesCluster> getClusters() {
        final Set<SQLRowValuesCluster> res = Collections.newSetFromMap(new IdentityHashMap<SQLRowValuesCluster, Boolean>());
        for (final SQLRowValues r : this.getTrees().values()) {
            // trees can be linked together
            res.add(r.getGraph());
        }
        return res;
    }

    // the root rows linked to their privates and descendant, all visited main rows, the rows
    // pointing to visited rows
    private final Tuple3<Map<SQLRow, SQLRowValues>, Rows, LinksToCut> expand(final LockStrength ls) throws SQLException {
        // root rows (linked to their privates if any) indexed by ID
        final Map<Integer, SQLRowValues> valsMap = new HashMap<Integer, SQLRowValues>();
        final Rows hasBeen = new Rows();
        final LinksToCutMutable toCut = new LinksToCutMutable();
        final Map<SQLRow, SQLRowValues> res = new HashMap<SQLRow, SQLRowValues>();

        // fetch privates of root rows
        final SQLRowValues privateGraph = this.getElem().getPrivateGraph(ArchivedGraph.ARCHIVE_AND_FOREIGNS, false, true);
        final NextRows privates = new NextRows(hasBeen, toCut);
        // always fetch to have up to date values, and behave the same way whether the element has
        // privates or not
        final Set<Number> ids = new HashSet<Number>();
        for (final SQLRow r : this.getRows()) {
            ids.add(r.getIDNumber());
        }
        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(privateGraph);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                input.setLockStrength(ls);
                input.addLockedTable(privateGraph.getTable().getName());
                return input.andWhere(new Where(privateGraph.getTable().getKey(), ids));
            }
        });
        for (final SQLRowValues newVals : fetcher.fetch()) {
            final SQLRow r = newVals.asRow();
            valsMap.put(newVals.getID(), newVals);
            privates.collect(newVals);
            res.put(r, newVals);
        }

        privates.expand(ls);
        return Tuple3.create(res, hasBeen, new LinksToCut(toCut.getMap()));
    }

    // NOTE using a collection of vals changed the time it took to archive a site (736 01) from 225s
    // to 5s.
    /**
     * Expand the passed values by going through referent keys.
     * 
     * @param t the table, eg /LOCAL/.
     * @param valsMap the values to expand, eg {3=>LOCAL(3)->BAT(4), 12=>LOCAL(12)->BAT(4)}.
     * @param hasBeen the rows already expanded, eg {BAT[4], LOCAL[3], LOCAL[12]}.
     * @param toCut the links to cut, eg {|BAT.ID_PRECEDENT|=> [BAT[2]]}.
     * @param ignorePrivateParentRF <code>true</code> if {@link LinkType#COMPOSITION private links}
     *        to <code>t</code> should be ignored.
     * @param ls how to lock rows.
     * @throws SQLException if a link is {@link ReferenceAction#RESTRICT}.
     */
    private final void expand(final SQLTable t, final Map<Integer, SQLRowValues> valsMap, final Rows hasBeen, final LinksToCutMutable toCut, final boolean ignorePrivateParentRF, final LockStrength ls)
            throws SQLException {
        if (valsMap.size() == 0)
            return;

        final SQLElement elem = getElem().getElement(t);

        final Set<Link> ownedLinks = new HashSet<Link>();
        for (final SQLElementLink elemLink : elem.getOwnedLinks().getByPath().values()) {
            if (elemLink.isJoin())
                ownedLinks.add(elemLink.getPath().getStep(0).getSingleLink());
        }
        final Map<Link, SQLElementLink> links = new HashMap<Link, SQLElementLink>();
        for (final SQLElementLink elemLink : elem.getLinksOwnedByOthers().getByPath().values()) {
            links.put(elemLink.getPath().getStep(-1).getSingleLink(), elemLink);
        }

        final NextRows privates = new NextRows(hasBeen, toCut);
        for (final Link link : t.getDBSystemRoot().getGraph().getReferentLinks(t)) {
            // all owned links are fetched alongside the main row
            if (ownedLinks.contains(link))
                continue;
            final SQLElementLink elemLink = links.get(link);
            if (elemLink == null)
                throw new IllegalStateException("Referent link " + link + " missing from " + links);
            final Path elemPath = elemLink.getPath();
            assert elemLink.getOwned() == elem;
            assert elemPath.getLast() == t;
            final Link foreignLink = elemPath.getStep(-1).getSingleLink();

            if (ignorePrivateParentRF && elemLink.getLinkType().equals(LinkType.COMPOSITION)) {
                // if we did fetch the referents rows, they would be contained in hasBeen
                continue;
            }
            // eg "ID_LOCAL"
            final String ffName = foreignLink.getSingleField().getName();
            final SQLElement refElem = elemLink.getOwner();
            final Path pathToTableWithFK = elemPath.minusLast();
            final ReferenceAction action = elemLink.getAction();
            if (action == null) {
                throw new IllegalStateException("Null action for " + refElem + " " + elemPath);
            }
            final SQLRowValues graphToFetch;
            if (action == ReferenceAction.CASCADE) {
                // otherwise we would need to find and expand the parent rows of referents
                if (refElem.isPrivate())
                    throw new UnsupportedOperationException("Cannot cascade to private element " + refElem + " from " + elemPath);
                graphToFetch = refElem.getPrivateGraph(ArchivedGraph.ARCHIVE_AND_FOREIGNS, false, true);
            } else {
                graphToFetch = new SQLRowValues(pathToTableWithFK.getFirst());
            }
            // add the foreign fields pointing to the rows to expand
            graphToFetch.assurePath(pathToTableWithFK).putNulls(foreignLink.getCols());
            final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(graphToFetch);
            fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    input.setLockStrength(ls);
                    input.addLockedTable(graphToFetch.getTable().getName());
                    return input;
                }
            });
            final ListMap<Path, SQLRowValuesListFetcher> fetchers = fetcher.getFetchers(pathToTableWithFK);
            if (fetchers.allValues().size() != 1)
                throw new IllegalStateException("Fetcher which references " + t + " not found : " + fetchers);
            fetchers.allValues().iterator().next().appendSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    final FieldRef refField = input.getAlias(pathToTableWithFK.getLast()).getField(ffName);
                    // eg where RECEPTEUR.ID_LOCAL in (3,12)
                    return input.andWhere(new Where(refField, valsMap.keySet()));
                }
            });
            for (final SQLRowValues newVals : fetcher.fetch()) {
                final SQLRow r = newVals.asRow();
                final boolean already = hasBeen.contains(r);
                // a row might reference the same linked row multiple times
                final Collection<SQLRowValues> rowsWithFK = newVals.followPath(pathToTableWithFK, CreateMode.CREATE_NONE, false);
                switch (action) {
                case RESTRICT:
                    throw new SQLException(createRestrictDesc(refElem, newVals, elemLink));
                case CASCADE:
                    if (!already) {
                        // walk before linking to existing graph
                        privates.collect(newVals);
                        // link with existing graph, eg RECEPTEUR(235)->LOCAL(3)
                        for (final SQLRowValues rowWithFK : rowsWithFK) {
                            rowWithFK.putForeign(foreignLink, hasBeen.getValues(rowWithFK.getForeign(foreignLink).asRow()));
                        }
                    }
                    break;
                case SET_EMPTY:
                    // if the row should be archived no need to cut any of its links
                    if (!already)
                        toCut.add(elemLink, rowsWithFK);
                    break;
                }
                // if already expanded just link and do not add to next
                if (already) {
                    // now link the new join row or the existing row
                    for (final SQLRowValues joinRow : rowsWithFK) {
                        final boolean linked = hasBeen.tryToLink(joinRow, foreignLink);
                        if (!linked)
                            throw new IllegalStateException("Join row not found : " + joinRow);
                    }
                }
            }
        }
        privates.expand(ls);
    }

    private final class NextRows {
        private final Rows hasBeen;
        private final LinksToCutMutable toCut;
        private final Map<SQLTable, Map<Integer, SQLRowValues>> mainRows;
        // only contains private rows (no main or join rows)
        private final Map<SQLTable, Map<Integer, SQLRowValues>> privateRows;

        public NextRows(final Rows hasBeen, final LinksToCutMutable toCut) {
            this.hasBeen = hasBeen;
            this.toCut = toCut;
            this.mainRows = new HashMap<SQLTable, Map<Integer, SQLRowValues>>();
            this.privateRows = new HashMap<SQLTable, Map<Integer, SQLRowValues>>();
        }

        /**
         * Record all private rows of the passed graph.
         * 
         * @param mainRow main row linked to its private graph. NOTE : the main row can itself be a
         *        private.
         */
        private void collect(final SQLRowValues mainRow) {
            for (final SQLRowValues privateVals : mainRow.getGraph().getItems()) {
                // since newVals isn't in, its privates can't
                assert !this.hasBeen.contains(privateVals.asRow());
                final SQLElement rowElem = getElem().getElement(privateVals.getTable());
                final Map<SQLTable, Map<Integer, SQLRowValues>> m;
                final boolean isMainRow = privateVals == mainRow;
                if (isMainRow) {
                    assert !(rowElem instanceof JoinSQLElement);
                    m = this.mainRows;
                } else {
                    if (rowElem.isPrivate()) {
                        m = this.privateRows;
                    } else {
                        assert rowElem instanceof JoinSQLElement;
                        m = null;
                    }
                }
                this.hasBeen.put(privateVals.asRow(), privateVals, isMainRow);
                if (m != null) {
                    Map<Integer, SQLRowValues> map = m.get(privateVals.getTable());
                    if (map == null) {
                        map = new HashMap<Integer, SQLRowValues>();
                        m.put(privateVals.getTable(), map);
                    }
                    map.put(privateVals.getID(), privateVals);
                }
            }
        }

        private void expand(final LockStrength ls) throws SQLException {
            this.expand(this.mainRows, false, ls);
            this.expand(this.privateRows, true, ls);
            // if the row has been added to the graph (by another link) no need to cut any of its
            // links
            this.toCut.restoreLinks(this.hasBeen);
        }

        private void expand(final Map<SQLTable, Map<Integer, SQLRowValues>> m, final boolean privateRows, final LockStrength ls) throws SQLException {
            for (final Entry<SQLTable, Map<Integer, SQLRowValues>> e : m.entrySet()) {
                TreesOfSQLRows.this.expand(e.getKey(), e.getValue(), this.hasBeen, this.toCut, privateRows, ls);
            }
        }
    }

    // unique SQLRowValues indexed by SQLRow
    static private final class Rows {

        private final Map<SQLRow, SQLRowValues> vals;
        private final Set<SQLRow> mainRows;

        private Rows() {
            this.vals = new HashMap<SQLRow, SQLRowValues>();
            this.mainRows = new HashSet<SQLRow>();
        }

        private boolean contains(final SQLRow r) {
            return this.vals.containsKey(r);
        }

        private SQLRowValues getValues(final SQLRow r) {
            return this.vals.get(r);
        }

        private void put(final SQLRow r, final SQLRowValues newVals, final boolean isMainRow) {
            assert newVals.asRow().equals(r);
            if (this.vals.put(r, newVals) != null)
                throw new IllegalStateException("Row already in : " + newVals);
            if (isMainRow)
                this.mainRows.add(r);
        }

        // link rowWithFK if it is already contained in this
        private boolean tryToLink(final SQLRowValues rowWithFK, final Link l) {
            final SQLRowValues inGraphRow = this.getValues(rowWithFK.asRow());
            final boolean linked = inGraphRow != null;
            if (linked) {
                // add link
                final SQLRowValues dest = this.getValues(rowWithFK.getForeign(l).asRow());
                if (dest == null)
                    throw new IllegalStateException("destination of " + l + " not found for " + rowWithFK);
                inGraphRow.putForeign(l, dest);
            }
            return linked;
        }

    }

    static private final class LinksToCutMutable {

        // use List to avoid comparing SQLRowValues instances
        private final ListMap<SQLElementLink, SQLRowValues> toCut;

        private LinksToCutMutable() {
            this.toCut = new ListMap<SQLElementLink, SQLRowValues>(32, Mode.NULL_FORBIDDEN, false);
        }

        private void add(SQLElementLink link, Collection<SQLRowValues> rowsWithFK) {
            assert !this.toCut.containsKey(link) || !CollectionUtils.containsAny(this.toCut.get(link), rowsWithFK) : "some rows (and their optional joins) already added : " + link + " " + rowsWithFK;
            // a row might reference the same row multiple times
            this.toCut.addAll(link, rowsWithFK);
        }

        private final ListMap<SQLElementLink, SQLRowValues> getMap() {
            return this.toCut;
        }

        private final void restoreLinks(final Rows hasBeen) {
            final Iterator<Entry<SQLElementLink, List<SQLRowValues>>> iter = this.getMap().entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<SQLElementLink, List<SQLRowValues>> e = iter.next();
                final SQLElementLink elemLink = e.getKey();
                final Link linkToCut = elemLink.getPath().getStep(-1).getSingleLink();

                final Iterator<SQLRowValues> iter2 = e.getValue().iterator();
                while (iter2.hasNext()) {
                    final SQLRowValues rowWithFK = iter2.next();
                    if (hasBeen.tryToLink(rowWithFK, linkToCut)) {
                        // remove from toCut
                        iter2.remove();
                    }
                }
                if (e.getValue().isEmpty())
                    iter.remove();
            }
        }
    }

    // ***

    /**
     * Put all the main (i.e. non private) rows of the trees (except the roots) in a map by table.
     * 
     * @return the descendants by table.
     */
    public final Map<SQLTable, List<SQLRowAccessor>> getDescendantsByTable() {
        final ListMap<SQLTable, SQLRowAccessor> res = new ListMap<SQLTable, SQLRowAccessor>();
        final Set<SQLRow> roots = this.getRows();
        for (final SQLRowValuesCluster c : this.getClusters()) {
            for (final SQLRowValues v : c.getItems()) {
                final SQLRow r = v.asRow();
                if (!roots.contains(r) && this.getMainRows().contains(r))
                    res.add(v.getTable(), v);
            }
        }
        return res;
    }

    // * extern

    static final class LinkToCut implements Comparable<LinkToCut> {
        private final SQLElementLink link;
        private final String label;

        protected LinkToCut(final SQLElementLink link) {
            super();
            if (link == null)
                throw new NullPointerException("Null link");
            this.link = link;
            this.label = TreesOfSQLRows.getLabel(this.link, null);
        }

        public final Path getPath() {
            return this.link.getPath();
        }

        public final SQLTable getTable() {
            return this.getPath().getFirst();
        }

        public final String getItem() {
            return this.link.getName();
        }

        public final String getLabel() {
            return this.label;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            return prime + this.link.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final LinkToCut other = (LinkToCut) obj;
            return this.link.equals(other.link);
        }

        @Override
        public int compareTo(LinkToCut o) {
            final int compareTable = CompareUtils.compareList(this.getTable().getSQLName().asList(), o.getTable().getSQLName().asList());
            if (compareTable != 0)
                return compareTable;
            else
                return this.getItem().compareTo(o.getItem());
        }
    }

    static public final class LinksToCut {

        private final ListMapItf<SQLElementLink, SQLRowValues> toCut;

        private LinksToCut(final ListMapItf<SQLElementLink, SQLRowValues> map) {
            map.removeAllEmptyCollections();
            this.toCut = ListMap.unmodifiableMap(map);
        }

        public final ListMapItf<SQLElementLink, SQLRowValues> getMap() {
            return this.toCut;
        }

        public final SortedMap<LinkToCut, Integer> countByLink() {
            final SortedMap<LinkToCut, Integer> res = new TreeMap<LinkToCut, Integer>();
            for (final Entry<SQLElementLink, List<SQLRowValues>> e : this.getMap().entrySet()) {
                final SQLElementLink elemLink = e.getKey();
                res.put(new LinkToCut(elemLink), e.getValue().size());
            }
            return res;
        }

        boolean containsAll(final LinksToCut o) {
            if (this == o)
                return true;
            if (!this.getMap().keySet().containsAll(o.getMap().keySet()))
                return false;
            final HashingStrategy<SQLRowAccessor> strategy = SQLRowAccessor.getRowStrategy();
            for (final Entry<SQLElementLink, ? extends Collection<SQLRowValues>> e : this.getMap().entrySet()) {
                final List<SQLRowValues> otherRows = o.getMap().get(e.getKey());
                // cannot be empty, see constructor
                if (otherRows != null) {
                    /*
                     * each row can be a graph : it's always the row with the foreign key, so in
                     * case of a join there's also the main row attached. Don't bother comparing the
                     * all values, just use the IDs.
                     */
                    final Set<ProxyItf<SQLRowValues>> thisVals = CustomEquals.createSet(strategy, e.getValue());
                    final Set<ProxyItf<SQLRowValues>> otherVals = CustomEquals.createSet(strategy, otherRows);
                    assert thisVals.size() == e.getValue().size() && otherVals.size() == o.getMap().get(e.getKey()).size() : "There were duplicates";
                    if (!thisVals.containsAll(otherVals))
                        return false;
                }
            }
            return true;
        }
    }

    /**
     * Return the rows that point to these trees.
     * 
     * @return the rows by referent field.
     */
    public final LinksToCut getExternReferences() {
        checkFetched();
        return this.externReferences;
    }
}
