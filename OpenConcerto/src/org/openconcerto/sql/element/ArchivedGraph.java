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

import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.SetMap;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Allow to find which rows must be unarchived to maintain coherence.
 * 
 * @author Sylvain
 */
final class ArchivedGraph {

    static final VirtualFields ARCHIVE_AND_FOREIGNS = VirtualFields.ARCHIVE.union(VirtualFields.FOREIGN_KEYS);

    private final SQLElementDirectory dir;
    private final SQLRowValues graph;
    // indexed nodes of graph
    private final Map<SQLRow, SQLRowValues> graphRows;
    // rows that haven't been processed
    private final Set<SQLRow> toExpand;

    /**
     * Create a new instance.
     * 
     * @param dir the directory.
     * @param graph the rows (without privates) to unarchive. This object will be modified.
     */
    ArchivedGraph(final SQLElementDirectory dir, final SQLRowValues graph) {
        if (dir == null)
            throw new NullPointerException("Null SQLElementDirectory");
        this.dir = dir;
        this.graph = graph;
        this.graphRows = new HashMap<SQLRow, SQLRowValues>();
        for (final SQLRowValues v : this.graph.getGraph().getItems()) {
            final SQLRowValues prev = this.graphRows.put(v.asRow(), v);
            if (prev != null)
                throw new IllegalStateException("Duplicated row : " + v.asRow());
        }
        assert isIndexCoherent();
        this.toExpand = new HashSet<SQLRow>(this.graphRows.keySet());
    }

    private boolean isIndexCoherent() {
        return this.graphRows.size() == this.graph.getGraphSize();
    }

    private final SQLElement getElement(final SQLTable t) {
        return this.dir.getElement(t);
    }

    private void expandPrivates() {
        final SetMap<SQLTable, Number> idsToExpandPrivate = new SetMap<SQLTable, Number>();
        for (final SQLRow toExpPrivate : this.toExpand) {
            idsToExpandPrivate.add(toExpPrivate.getTable(), toExpPrivate.getIDNumber());
        }
        for (final Entry<SQLTable, Set<Number>> e : idsToExpandPrivate.entrySet()) {
            final SQLElement elem = getElement(e.getKey());
            final Set<Number> ids = e.getValue();
            final SQLRowValues privateGraph = elem.getPrivateGraph(ARCHIVE_AND_FOREIGNS, false, true);
            // if the element has privates or joins to expand
            if (privateGraph.getGraphSize() > 1) {
                // fetch the main row and its privates
                final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(privateGraph, false);
                setWhereAndArchivePolicy(fetcher, ids, ArchiveMode.BOTH);
                final List<SQLRowValues> fetchedRows = fetcher.fetch();
                assert fetchedRows.size() == ids.size();
                for (final SQLRowValues valsFetched : fetchedRows) {
                    // attach to existing graph
                    // need to copy since we modify the graph when loading values
                    for (final SQLRowValues v : new ArrayList<SQLRowValues>(valsFetched.getGraph().getItems())) {
                        final SQLRow row = v.asRow();
                        if (v == valsFetched) {
                            // get the row already in the graph
                            final SQLRowValues toExpandVals = this.graphRows.get(row);
                            // only load private foreign rows, do not overwrite other foreign rows.
                            // I.e. toExpandVals has a foreign row for its parent and an ID for its
                            // private, while valsFetched has the reverse.
                            toExpandVals.load(valsFetched, valsFetched.getForeigns().keySet());
                            assert !valsFetched.hasForeigns();
                            // load referents private rows
                            if (valsFetched.hasReferents()) {
                                for (final Entry<SQLField, ? extends Collection<SQLRowValues>> refEntry : new ListMap<SQLField, SQLRowValues>(valsFetched.getReferentsMap()).entrySet()) {
                                    final SQLField refField = refEntry.getKey();
                                    for (final SQLRowValues ref : refEntry.getValue()) {
                                        ref.put(refField.getName(), toExpandVals);
                                    }
                                }
                            }
                            assert valsFetched.getGraphSize() == 1;
                        } else {
                            this.toExpand.add(row);
                            this.graphRows.put(row, v);
                        }
                    }
                }
            }
        }
    }

    final SQLRowValues expand() {
        // expand once the privates (in the rest of this method they are fetched alongside the
        // main row)
        expandPrivates();

        while (!this.toExpand.isEmpty()) {
            assert isIndexCoherent();

            // find required archived rows
            final SetMap<SQLTable, Number> toFetch = new SetMap<SQLTable, Number>();
            // rows pointing to private are apart since their main rows need to be fetched
            final SetMap<SQLTable, Number> privateToFetch = new SetMap<SQLTable, Number>();
            // for each ASSOCIATION to a private, its last step and all the rows that point to the
            // private
            final ListMap<Step, SQLRowValues> nonEmptyFieldsPointingToPrivates = new ListMap<Step, SQLRowValues>();

            for (final SQLRow rowToExpand : this.toExpand) {
                final SQLTable t = rowToExpand.getTable();
                for (final SQLElementLink ff : getElement(t).getOwnedLinks().getByPath().values()) {
                    // privates are always fetched alongside main rows
                    // nothing to do if we point to a row that cannot be archived
                    if (ff.getLinkType().equals(LinkType.COMPOSITION) || !ff.getOwned().getTable().isArchivable())
                        continue;
                    final SQLElement elem = ff.getOwned();
                    final Step lastStep = ff.getPath().getStep(-1);
                    assert lastStep.isForeign();
                    final String fieldName = lastStep.getSingleField().getName();
                    // OK since we included joins in getPrivateGraph()
                    final Collection<SQLRowValues> rowsWithForeign = this.graphRows.get(rowToExpand).followPath(ff.getPath().minusLast(), CreateMode.CREATE_NONE, false);
                    for (final SQLRowValues rowWithForeign : rowsWithForeign) {
                        if (!rowWithForeign.isForeignEmpty(fieldName)) {
                            final SQLRow foreignRow = rowWithForeign.getForeign(fieldName).asRow();
                            final SQLRowValues existingRow = this.graphRows.get(foreignRow);
                            if (existingRow != null) {
                                rowWithForeign.put(fieldName, existingRow);
                            } else {
                                final SetMap<SQLTable, Number> map;
                                if (elem.isPrivate()) {
                                    // if foreignRow is part of private graph, fetch it later from
                                    // the main row
                                    nonEmptyFieldsPointingToPrivates.add(lastStep, rowWithForeign);
                                    map = privateToFetch;
                                } else {
                                    map = toFetch;
                                }
                                map.add(foreignRow.getTable(), foreignRow.getIDNumber());
                            }
                        }
                    }
                }
            }

            assert isIndexCoherent();

            // ** find ASSOCIATION to non-privates
            final Map<SQLRow, SQLRowValues> archivedForeignRows = fetch(toFetch);

            // attach to existing graph
            final Map<SQLRow, SQLRowValues> added = new HashMap<SQLRow, SQLRowValues>();
            for (final SQLRow rowToExpand : this.toExpand) {
                final SQLTable t = rowToExpand.getTable();
                for (final SQLElementLink ff : getElement(t).getOwnedLinks().getByPath().values()) {
                    final Step lastStep = ff.getPath().getStep(-1);
                    assert lastStep.isForeign();
                    final String fieldName = lastStep.getSingleField().getName();
                    final Collection<SQLRowValues> rowsWithForeign = this.graphRows.get(rowToExpand).followPath(ff.getPath().minusLast(), CreateMode.CREATE_NONE, false);
                    for (final SQLRowValues rowWithForeign : rowsWithForeign) {
                        if (!rowWithForeign.isForeignEmpty(fieldName)) {
                            final SQLRow foreignRow = rowWithForeign.getForeign(fieldName).asRow();
                            final SQLRowValues existingOwnedRow = this.graphRows.get(foreignRow);
                            if (existingOwnedRow != null) {
                                // fetched by previous foreign key
                                rowWithForeign.put(fieldName, existingOwnedRow);
                            } else {
                                final SQLRowValues valsFetched = archivedForeignRows.get(foreignRow);
                                // null meaning excluded because it wasn't archived or points to a
                                // private
                                if (valsFetched != null) {
                                    assert valsFetched.isArchived() : "Not archived : " + valsFetched;
                                    attach(rowWithForeign, lastStep, valsFetched, added, privateToFetch);
                                }
                            }
                        }
                    }
                }
            }

            assert isIndexCoherent();

            // ** find ASSOCIATION to privates

            // only referenced archived rows
            final Map<SQLRow, SQLRowValues> privateFetched = fetch(privateToFetch);
            toFetch.clear();
            for (final SQLRow r : privateFetched.keySet()) {
                final SQLRowAccessor privateRoot = getElement(r.getTable()).fetchPrivateRoot(r, ArchiveMode.BOTH);
                toFetch.add(privateRoot.getTable(), privateRoot.getIDNumber());
            }
            // then fetch private graph (even if the private row referenced is archived its main
            // row might not be)
            final Map<SQLRow, SQLRowValues> mainRowFetched = fetch(toFetch, ArchiveMode.BOTH);
            // attach to existing graph
            for (final Entry<Step, List<SQLRowValues>> e : nonEmptyFieldsPointingToPrivates.entrySet()) {
                final Step step = e.getKey();
                final String fieldName = step.getSingleField().getName();
                for (final SQLRowValues rowWithForeign : e.getValue()) {
                    assert !rowWithForeign.isForeignEmpty(fieldName);
                    final SQLRow foreignRow = rowWithForeign.getForeign(fieldName).asRow();
                    final SQLRowValues existingOwnedRow = this.graphRows.get(foreignRow);
                    if (existingOwnedRow != null) {
                        // fetched by previous foreign key
                        rowWithForeign.put(fieldName, existingOwnedRow);
                    } else {
                        final SQLRowValues valsFetched = mainRowFetched.get(foreignRow);
                        if (valsFetched != null) {
                            // since we kept only archived ones in privateFetched
                            assert valsFetched.isArchived() : "Not archived : " + valsFetched;
                            attach(rowWithForeign, step, valsFetched, added, null);
                        }
                    }
                }
            }

            this.toExpand.clear();
            this.toExpand.addAll(added.keySet());
        }
        return this.graph;
    }

    // add a link through step and index the new rows.
    private void attach(final SQLRowValues existingRow, final Step step, final SQLRowValues graphToAdd, final Map<SQLRow, SQLRowValues> added, final SetMap<SQLTable, Number> privateToFetch) {
        assert existingRow.getGraph() != graphToAdd.getGraph() : "Already attached";
        assert this.graphRows.get(existingRow.asRow()) == existingRow;
        assert !this.graphRows.containsKey(graphToAdd.asRow());
        for (final SQLRowValues v : graphToAdd.getGraph().getItems()) {
            final SQLRow row = v.asRow();
            added.put(row, v);
            final SQLRowValues prev = this.graphRows.put(row, v);
            assert prev == null : "Duplicate " + row + " in " + this.graph.printGraph();
            // rows were fetched from a different link
            if (privateToFetch != null)
                privateToFetch.remove(v.getTable(), v.getIDNumber());
        }
        existingRow.put(step, graphToAdd);
        assert isIndexCoherent();
    }

    private void setWhereAndArchivePolicy(final SQLRowValuesListFetcher fetcher, final Set<Number> ids, final ArchiveMode archiveMode) {
        for (final SQLRowValuesListFetcher f : fetcher.getFetchers(true).allValues()) {
            f.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    if (f == fetcher && ids != null) {
                        input.andWhere(new Where(fetcher.getGraph().getTable().getKey(), ids));
                    }
                    input.setArchivedPolicy(archiveMode);
                    return input;
                }
            });
        }
    }

    private Map<SQLRow, SQLRowValues> fetch(final SetMap<SQLTable, Number> toFetch) {
        return this.fetch(toFetch, ArchiveMode.ARCHIVED);
    }

    // fetch the passed rows (and their privates if the table is a main one)
    private Map<SQLRow, SQLRowValues> fetch(final SetMap<SQLTable, Number> toFetch, final ArchiveMode archiveMode) {
        final Map<SQLRow, SQLRowValues> res = new HashMap<SQLRow, SQLRowValues>();
        for (final Entry<SQLTable, Set<Number>> e : toFetch.entrySet()) {
            final Set<Number> ids = e.getValue();
            final SQLTable table = e.getKey();
            final SQLElement elem = getElement(table);
            final SQLRowValuesListFetcher fetcher;
            // don't fetch partial data
            if (!elem.isPrivate())
                fetcher = SQLRowValuesListFetcher.create(elem.getPrivateGraph(ARCHIVE_AND_FOREIGNS, false, true));
            else
                // only needs IDs
                fetcher = new SQLRowValuesListFetcher(new SQLRowValues(table).putNulls(table.getFieldsNames(VirtualFields.ARCHIVE)));
            setWhereAndArchivePolicy(fetcher, ids, archiveMode);
            for (final SQLRowValues fetchedVals : fetcher.fetch()) {
                for (final SQLRowValues v : fetchedVals.getGraph().getItems()) {
                    final SQLRow r = v.asRow();
                    res.put(r, v);
                    assert !this.graphRows.containsKey(r) : "already in graph : " + r;
                }
            }
        }
        return res;
    }
}
