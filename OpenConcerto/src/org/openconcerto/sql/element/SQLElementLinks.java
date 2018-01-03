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
import org.openconcerto.utils.CollectionMap2.Mode;
import org.openconcerto.utils.CollectionMap2Itf.SetMapItf;
import org.openconcerto.utils.SetMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An indexed set of {@link SQLElementLink}.
 * 
 * @author Sylvain
 */
public final class SQLElementLinks {

    static private final SQLElementLinks EMPTY = new SQLElementLinks(SetMap.<LinkType, SQLElementLink> empty());

    static public final SQLElementLinks empty() {
        return EMPTY;
    }

    private final SetMapItf<LinkType, SQLElementLink> byType;
    private final Map<Path, SQLElementLink> byPath;

    protected SQLElementLinks(SetMapItf<LinkType, SQLElementLink> byType) {
        super();
        final SetMap<LinkType, SQLElementLink> copy = new SetMap<LinkType, SQLElementLink>(Mode.NULL_FORBIDDEN);
        // removeEmptyCollections to allow to use getByType().keySet() or size()
        copy.putAllCollections(byType, true);
        this.byType = SetMap.unmodifiableMap(copy);
        final Map<Path, SQLElementLink> tmp = new HashMap<Path, SQLElementLink>();
        for (final SQLElementLink l : byType.allValues()) {
            tmp.put(l.getPath(), l);
        }
        this.byPath = Collections.unmodifiableMap(tmp);
    }

    public final Map<Path, SQLElementLink> getByPath() {
        return this.byPath;
    }

    public final SQLElementLink getByPath(final Path p) {
        return this.getByPath(p, null);
    }

    public final SQLElementLink getByPath(final Path p, final LinkType type) {
        final SQLElementLink res = this.byPath.get(p);
        if (type == null || res != null && res.getLinkType().equals(type))
            return res;
        else
            return null;
    }

    public final SetMapItf<LinkType, SQLElementLink> getByType() {
        return this.byType;
    }

    public final Set<SQLElementLink> getByType(final LinkType type) {
        return this.byType.getNonNull(type);
    }

    public final SQLElementLink getOneByOwned(final SQLElement owned) {
        return this.getOneByOwned(owned, null);
    }

    public final SQLElementLink getOneByOwned(final SQLElement owned, final LinkType type) {
        final Set<SQLElementLink> res = getByOwned(owned, type);
        if (res.size() == 0)
            return null;
        else if (res.size() == 1)
            return res.iterator().next();
        else
            throw new IllegalStateException("More than one link to " + owned + " : " + res);
    }

    public final Set<SQLElementLink> getByOwned(final SQLElement owned, final LinkType type) {
        final Set<SQLElementLink> res = new HashSet<SQLElementLink>();
        final Collection<SQLElementLink> links = type == null ? this.byPath.values() : this.getByType(type);
        for (final SQLElementLink l : links) {
            if (owned == null || l.getOwned() == owned)
                res.add(l);
        }
        return res;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.byPath.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SQLElementLinks other = (SQLElementLinks) obj;
        return this.byPath.values().equals(other.byPath.values());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.byPath.values();
    }
}
