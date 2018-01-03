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
 
 package org.openconcerto.sql.model;

import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.utils.cc.HashingStrategy;
import org.openconcerto.xml.JDOM2Utils;
import org.openconcerto.xml.XMLCodecUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;

@Immutable
public final class Constraint {

    @SuppressWarnings("unchecked")
    public static Constraint fromXML(final SQLTable t, Element elem) {
        return new Constraint(t, elem.getAttributeValue("name"), (Map<String, Object>) XMLCodecUtils.decode1(elem.getChildren().get(0)));
    }

    private final SQLTable t;
    private final String name;
    // private copy
    private final Map<String, Object> m;
    @GuardedBy("this")
    private String xml = null;

    private Constraint(final SQLTable t, final String name, final Map<String, Object> row) {
        this.t = t;
        this.name = name;
        this.m = row;
    }

    Constraint(final SQLTable t, final Map<String, Object> row) {
        this.t = t;
        this.m = new HashMap<String, Object>(row);
        this.name = (String) this.m.remove("CONSTRAINT_NAME");
        this.m.remove("TABLE_SCHEMA");
        this.m.remove("TABLE_NAME");
    }

    Constraint(final SQLTable t, final Constraint c) {
        this.t = t;
        this.m = c.m;
        this.name = c.name;
        // don't bother synchronising for xml since when we copy a Constraint it generally has never
        // been used.
    }

    public final SQLTable getTable() {
        return this.t;
    }

    public final String getName() {
        return this.name;
    }

    public final ConstraintType getType() {
        return ConstraintType.find((String) this.m.get("CONSTRAINT_TYPE"));
    }

    /**
     * The fields' names used by this constraint.
     * 
     * @return the fields' names.
     */
    @SuppressWarnings("unchecked")
    public final List<String> getCols() {
        return (List<String>) this.m.get("COLUMN_NAMES");
    }

    public synchronized String toXML() {
        // this is immutable so only compute once the XML
        if (this.xml == null)
            this.xml = "<constraint name=\"" + JDOM2Utils.OUTPUTTER.escapeAttributeEntities(getName()) + "\" >" + XMLCodecUtils.encodeSimple(this.m) + "</constraint>";
        return this.xml;
    }

    // ATTN don't use name since it can be auto-generated (eg by a UNIQUE field)
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Constraint) {
            final Constraint o = (Constraint) obj;
            return this.m.equals(o.m);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.m.hashCode();
    }

    static private final HashingStrategy<Constraint> INTERSYSTEM_STRATEGY = new HashingStrategy<Constraint>() {
        @Override
        public boolean equals(Constraint c1, Constraint c2) {
            return c1.getType().equals(c2.getType()) && c1.getCols().equals(c2.getCols());
        }

        @Override
        public int computeHashCode(Constraint c) {
            final int prime = 31;
            int result = 1;
            result = prime * result + c.getType().hashCode();
            result = prime * result + c.getCols().hashCode();
            return result;
        }
    };

    /**
     * Only use {@link #getType()} and {@link #getCols()} when comparing. {@link #equals(Object)
     * Equals} uses all properties but some properties may not be supported on all systems, or have
     * different syntax (e.g. DEFINITION).
     * 
     * @return a simpler strategy.
     */
    static public final HashingStrategy<Constraint> getInterSystemHashStrategy() {
        return INTERSYSTEM_STRATEGY;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + this.getName() + " " + this.m;
    }
}
