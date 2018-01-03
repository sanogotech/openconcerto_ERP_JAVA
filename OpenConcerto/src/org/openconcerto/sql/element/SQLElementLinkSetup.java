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
import org.openconcerto.sql.element.SQLElementLink.LinkType;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.CompareUtils;

import java.util.List;

/**
 * Set up a single link.
 * 
 * @author Sylvain
 * @see SQLElementLinksSetup#get(Path)
 */
public final class SQLElementLinkSetup {

    private final SQLElementLinksSetup globalSetup;
    private final Path path;
    private final String typeError;
    private LinkType type;
    private ReferenceAction action;
    private String name;

    SQLElementLinkSetup(final SQLElementLinksSetup globalSetup, final Path path, final LinkType type, final String typeError) {
        this.globalSetup = globalSetup;
        this.path = path;
        this.typeError = typeError;
        this.setType(type);
        this.name = null;
    }

    public final SQLElement getElem() {
        return this.globalSetup.getElem();
    }

    public final SQLElement getTargetElement() {
        return getElem().getElementLenient(getPath().getLast());
    }

    public final Path getPath() {
        return this.path;
    }

    public final LinkType getType() {
        return this.type;
    }

    public SQLElementLinkSetup ignore() {
        return this.setType(null);
    }

    public final boolean isIgnored() {
        return this.getType() == null;
    }

    public final SQLElementLinkSetup setType(LinkType type) {
        return this.setType(type, null);
    }

    /**
     * Set the type and action for this link. NOTE : set both parameters at the same time since
     * allowed actions depend on the type.
     * 
     * @param type the type.
     * @param action the action for this link, <code>null</code> action meaning default, i.e. old
     *        action is not taken into consideration.
     * @return this.
     * @throws IllegalArgumentException if the passed type or action isn't allowed.
     */
    public final SQLElementLinkSetup setType(final LinkType type, ReferenceAction action) throws IllegalArgumentException {
        final SQLElement targetElem = getTargetElement();
        if (type == null) {
            if (action != null)
                throw new IllegalArgumentException("No action should be specified for ignored path : " + action);
        } else {
            final List<ReferenceAction> possibleActions = getElem().getPossibleActions(type, targetElem);
            if (action == null)
                action = possibleActions.get(0);
            else if (!possibleActions.contains(action))
                throw new IllegalArgumentException(action + " isn't allowed for " + this);
        }

        if (!CompareUtils.equals(this.type, type)) {
            if (this.type != null && this.typeError != null)
                throw new IllegalArgumentException("Cannot change " + this + " : " + this.typeError);

            if (type == null) {
                if (targetElem != null)
                    throw new IllegalArgumentException("Cannot ignore " + this + " to existing element " + targetElem);
            } else if (type == LinkType.PARENT) {
                // that way the caller don't inadvertently overwrite a parent. And we don't have
                // to guess if the old parent should be ignored or used as a normal link. Or if
                // it can't be modified (e.g. set by getParentFFName())
                if (this.globalSetup.getParent() != null)
                    throw new IllegalArgumentException("Parent already set to " + this.globalSetup.getParent() + ". First set it to another type.");
                if (targetElem.isPrivate())
                    throw new IllegalArgumentException("Cannot use " + this + " for parent, target element is private : " + targetElem);
            } else if (type == LinkType.COMPOSITION) {
                if (!targetElem.isPrivate())
                    throw new IllegalArgumentException("Target element for " + this + " isn't private : " + targetElem);
            } else {
                assert type == LinkType.ASSOCIATION;
                // nothing more to check
            }

            final boolean removingParent = this.type == LinkType.PARENT;
            final boolean settingParent = type == LinkType.PARENT;
            this.type = type;
            if (removingParent)
                this.globalSetup.setParent(null);
            else if (settingParent)
                this.globalSetup.setParent(getPath());
        }

        this.action = action;

        return this;
    }

    public final ReferenceAction getAction() {
        return this.action;
    }

    public final String getName() {
        return this.name;
    }

    public final SQLElementLinkSetup setName(String name) {
        this.name = name;
        return this;
    }

    final SQLElementLink build() {
        return new SQLElementLink(getElem(), getPath(), getTargetElement(), getType(), getName(), getAction());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " for " + this.build();
    }
}
