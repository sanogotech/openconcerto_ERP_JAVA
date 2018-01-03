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

import org.openconcerto.sql.element.SQLElement.ReferenceAction;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Direction;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;

import java.util.List;

/**
 * A logical link between two elements. It can be a direct foreign {@link Link} or two links through
 * a {@link JoinSQLElement join}. The {@link #getOwner()} needs the {@link #getOwned()}, i.e. if the
 * owner is unarchived the owned will also be unarchived. The owner is responsible for
 * {@link SQLElement#setupLinks(org.openconcerto.sql.element.SQLElement.LinksSetup) setting up} the properties of
 * the link.
 * 
 * @author Sylvain
 */
public final class SQLElementLink {

    public static enum LinkType {
        /** One element is the parent of the other */
        PARENT,
        /** One element is part of the other */
        COMPOSITION,
        /** One element references the other */
        ASSOCIATION
    }

    private final SQLElement owner, owned;
    private final Path path;
    private final LinkType type;
    private final String name;
    private ReferenceAction action;

    protected SQLElementLink(SQLElement owner, Path path, SQLElement owned, final LinkType type, final String name, final ReferenceAction action) {
        super();
        final int length = path.length();
        if (length == 0)
            throw new IllegalArgumentException("Empty path");
        if (owner != null && owner.getTable() != path.getFirst() || owned.getTable() != path.getLast())
            throw new IllegalArgumentException("Wrong path : " + path + " not from owner : " + owner + " to owned : " + owned);
        if (!path.isSingleLink())
            throw new IllegalArgumentException("Isn't single link : " + path);
        // either foreign key or join
        if (length > 2)
            throw new IllegalArgumentException("Path too long : " + path);
        final Step lastStep = path.getStep(-1);
        final boolean endsWithForeign = lastStep.getDirection().equals(Direction.FOREIGN);
        if (length == 1) {
            if (!endsWithForeign)
                throw new IllegalArgumentException("Single step path isn't foreign : " + path);
        } else {
            assert length == 2;
            if (!endsWithForeign || !path.getStep(0).getDirection().equals(Direction.REFERENT))
                throw new IllegalArgumentException("Two steps path isn't a join : " + path);
        }
        if (lastStep.getSingleField() == null)
            throw new IllegalArgumentException("Multi-field not yet supported : " + lastStep);
        this.path = path;
        this.owner = owner;
        this.owned = owned;
        if (type == null || action == null)
            throw new NullPointerException();
        this.type = type;
        if (name != null) {
            this.name = name;
        } else {
            final SQLField singleField = lastStep.getSingleField();
            this.name = singleField != null ? singleField.getName() : lastStep.getSingleLink().getName();
        }
        assert this.getName() != null;
        this.action = action;
    }

    /**
     * The path from the {@link #getOwner() owner} to the {@link #getOwned() owned}. NOTE : the last
     * step is always {@link Direction#FOREIGN}.
     * 
     * @return the path of this link, its {@link Path#length() length} is 1 or 2 for a join.
     */
    public final Path getPath() {
        return this.path;
    }

    public final boolean isJoin() {
        return this.path.length() == 2;
    }

    /**
     * Return the single link.
     * 
     * @return the single foreign link, <code>null</code> if and only if this is a {@link #isJoin()
     *         join} as multi-link paths are invalid.
     */
    public final Link getSingleLink() {
        if (this.isJoin())
            return null;
        final Link res = this.path.getStep(0).getSingleLink();
        // checked in the constructor
        assert res != null;
        return res;
    }

    /**
     * Return the single field of this link.
     * 
     * @return the foreign field of this link, <code>null</code> if and only if this is a
     *         {@link #isJoin() join} as multi-field link are not yet supported.
     */
    public final SQLField getSingleField() {
        final Link l = this.getSingleLink();
        if (l == null)
            return null;
        final SQLField res = l.getSingleField();
        // checked in the constructor
        assert res != null;
        return res;
    }

    public final SQLElement getOwner() {
        return this.owner;
    }

    public final SQLElement getOwned() {
        return this.owned;
    }

    public final JoinSQLElement getJoinElement() {
        if (!this.isJoin())
            return null;
        return (JoinSQLElement) this.getOwner().getElement(this.getPath().getTable(1));
    }

    public final boolean isOwnerTheParent() {
        final boolean owner;
        if (this.getLinkType().equals(LinkType.COMPOSITION))
            owner = true;
        else if (this.getLinkType().equals(LinkType.PARENT))
            owner = false;
        else
            throw new IllegalStateException("Invalid type : " + this.getLinkType());
        return owner;
    }

    public final SQLElement getParent() {
        return this.getParentOrChild(true);
    }

    private final SQLElement getParentOrChild(final boolean parent) {
        return parent == isOwnerTheParent() ? this.getOwner() : this.getOwned();
    }

    public final SQLElement getChild() {
        return this.getParentOrChild(false);
    }

    public final Path getPathToParent() {
        return this.getPathToParentOrChild(true);
    }

    public final Step getStepToParent() {
        return this.getPathToParent().getStep(-1);
    }

    private final Path getPathToParentOrChild(final boolean toParent) {
        return toParent == isOwnerTheParent() ? this.getPath().reverse() : this.getPath();
    }

    public final Path getPathToChild() {
        return this.getPathToParentOrChild(false);
    }

    public final Step getStepToChild() {
        return this.getPathToChild().getStep(-1);
    }

    public final LinkType getLinkType() {
        return this.type;
    }

    public final String getName() {
        return this.name;
    }

    public final ReferenceAction getAction() {
        return this.action;
    }

    public final void setAction(ReferenceAction action) {
        final List<ReferenceAction> possibleActions = getOwner().getPossibleActions(this.getLinkType(), this.getOwned());
        if (!possibleActions.contains(action))
            throw new IllegalArgumentException("Not allowed : " + action);
        this.action = action;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.action.hashCode();
        result = prime * result + this.path.hashCode();
        result = prime * result + this.type.hashCode();
        return result;
    }

    // don't use SQLElement to avoid walking the graph
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SQLElementLink other = (SQLElementLink) obj;
        return this.action.equals(other.action) && this.path.equals(other.path) && this.type.equals(other.type);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " '" + this.getName() + "' " + this.getLinkType() + " " + this.getPath();
    }
}
