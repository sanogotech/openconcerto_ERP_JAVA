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
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.utils.CollectionMap2Itf.SetMapItf;
import org.openconcerto.utils.SetMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Hold {@link SQLElementLinkSetup} and maintain at most one parent.
 * 
 * @author Sylvain
 */
public final class SQLElementLinksSetup {

    private final SQLElement elem;
    private final Map<Path, SQLElementLinkSetup> result;
    private Path parent;

    SQLElementLinksSetup(final SQLElement elem) {
        this.elem = elem;
        this.result = new HashMap<Path, SQLElementLinkSetup>();
        for (final Entry<LinkType, Set<Path>> e : this.elem.getDefaultLinkTypes().entrySet()) {
            final LinkType lt = e.getKey();
            final Set<Path> paths = e.getValue();
            final String typeError = lt == LinkType.PARENT ? "it was set by getParentFFName()" : null;
            for (final Path p : paths) {
                final SQLElementLinkSetup previous = this.result.put(p, new SQLElementLinkSetup(this, p, lt, typeError));
                assert previous == null : "Duplicate for " + p + " : " + previous + " and " + lt;
            }
        }
    }

    public final SQLElement getElem() {
        return this.elem;
    }

    public final SQLElementLinkSetup get(final String fk) {
        return this.get(new PathBuilder(this.elem.getTable()).addForeignField(fk).build());
    }

    public final SQLElementLinkSetup get(final Path p) {
        final SQLElementLinkSetup res = this.result.get(p);
        if (res == null)
            throw new IllegalArgumentException("Unknown path " + p + " : " + this.result.keySet());
        return res;
    }

    public final Path getParent() {
        return this.parent;
    }

    final void setParent(Path parent) {
        if (parent != null && this.parent != null)
            throw new IllegalArgumentException("Overwriting " + this.parent + " with " + parent);
        this.parent = parent;
    }

    public final SQLElementLinksSetup ignore(final Path p) {
        this.get(p).ignore();
        return this;
    }

    final SetMapItf<LinkType, SQLElementLink> getResult() {
        final SetMap<LinkType, SQLElementLink> res = new SetMap<LinkType, SQLElementLink>();
        for (final Entry<Path, SQLElementLinkSetup> e : this.result.entrySet()) {
            final SQLElementLinkSetup setup = e.getValue();
            if (!setup.isIgnored()) {
                final boolean added = res.add(setup.getType(), setup.build());
                assert added : "Duplicates";
            }
        }
        return res;
    }
}
