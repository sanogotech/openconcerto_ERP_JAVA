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
 
 /*
 * Link created on 13 mai 2004
 */
package org.openconcerto.sql.model.graph;

import static org.openconcerto.xml.JDOM2Utils.OUTPUTTER;
import static java.util.Collections.singletonList;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLIdentifier;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.HashingStrategy;
import org.openconcerto.utils.cc.IPredicate;

import java.io.IOException;
import java.io.Writer;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;

import net.jcip.annotations.ThreadSafe;

/**
 * Un lien dans le graphe des tables. Par exemple, si la table ECLAIRAGE a un champ ID_LOCAL, alors
 * il existe un lien avec comme source ECLAIRAGE, comme destination LOCAL et comme label le champ
 * ECLAIRAGE.ID_LOCAL.
 * 
 * @author ILM Informatique 13 mai 2004
 */
@ThreadSafe
public final class Link extends DirectedEdge<SQLTable> {

    public static enum Direction {
        FOREIGN {
            @Override
            public Direction reverse() {
                return REFERENT;
            }
        },
        REFERENT {
            @Override
            public Direction reverse() {
                return FOREIGN;
            }
        },
        ANY {
            @Override
            public Direction reverse() {
                return this;
            }
        };

        public abstract Direction reverse();

        public static Direction fromForeign(Boolean foreign) {
            if (foreign == null)
                return ANY;
            return foreign ? FOREIGN : REFERENT;
        }
    }

    public static class NamePredicate extends IPredicate<Link> {

        private final SQLTable table;
        private final String oppositeRootName, oppositeTableName;
        private final List<String> fieldsNames;

        public NamePredicate(SQLTable table, String oppositeTableName) {
            this(table, null, oppositeTableName);
        }

        public NamePredicate(SQLTable table, String oppositeRootName, String oppositeTableName) {
            this(table, oppositeRootName, oppositeTableName, null);
        }

        public NamePredicate(SQLTable table, String oppositeRootName, String oppositeTableName, List<String> fieldsNames) {
            super();
            if (table == null)
                throw new NullPointerException("Null table");
            this.table = table;
            this.oppositeRootName = oppositeRootName;
            this.oppositeTableName = oppositeTableName;
            this.fieldsNames = fieldsNames;
        }

        @Override
        public boolean evaluateChecked(Link l) {
            // leave at the start to check that this.table is an end of l
            final SQLTable oppositeTable = l.oppositeVertex(this.table);
            return (this.fieldsNames == null || this.fieldsNames.equals(l.getCols())) && (this.oppositeTableName == null || this.oppositeTableName.equals(oppositeTable.getName()))
                    && (this.oppositeRootName == null || this.oppositeRootName.equals(oppositeTable.getDBRoot().getName()));
        }

        @Override
        public String toString() {
            final String links = this.fieldsNames == null ? " for links" : " for links through " + this.fieldsNames;
            final String tables;
            if (this.oppositeRootName == null && this.oppositeTableName == null) {
                tables = " to/from " + this.table;
            } else if (this.oppositeTableName == null) {
                tables = " connecting " + this.table + " and a table in the root " + new SQLName(this.oppositeRootName);
            } else {
                tables = " connecting " + this.table + " and table(s) named " + new SQLName(this.oppositeRootName, this.oppositeTableName);
            }
            return this.getClass().getSimpleName() + links + tables;
        }
    }

    public static enum Rule {

        SET_NULL("SET NULL"), SET_DEFAULT("SET DEFAULT"), CASCADE("CASCADE"), RESTRICT("RESTRICT"), NO_ACTION("NO ACTION");

        private final String sql;

        private Rule(final String sql) {
            this.sql = sql;
        }

        public static Rule fromShort(short s) {
            switch (s) {
            case DatabaseMetaData.importedKeyCascade:
                return CASCADE;
            case DatabaseMetaData.importedKeySetNull:
                return SET_NULL;
            case DatabaseMetaData.importedKeySetDefault:
                return SET_DEFAULT;
            case DatabaseMetaData.importedKeyRestrict:
                return RESTRICT;
            case DatabaseMetaData.importedKeyNoAction:
                return NO_ACTION;
            default:
                throw new IllegalArgumentException("Unknown rule " + s);
            }
        }

        public static Rule fromName(String n) {
            return n == null ? null : valueOf(n);
        }

        public String asString() {
            return this.sql;
        }
    }

    static final Set<SQLField> getSingleFields(final Set<Link> links) {
        final Set<SQLField> res = new HashSet<SQLField>(links.size());
        for (final Link l : links) {
            final SQLField singleField = l.getSingleField();
            if (singleField != null)
                res.add(singleField);
            else
                return null;
        }
        return res;
    }

    // ArrayList is thread-safe if not modified
    private final List<SQLField> cols;
    private final List<String> colsNames;
    private final List<SQLField> refCols;
    private final List<String> refColsNames;
    private final String name;
    private final Rule updateRule, deleteRule;

    /**
     * Creates a link between two tables.
     * 
     * @param keys foreign fields of the source table.
     * @param referredCols fields of the destination table.
     * @param foreignKeyName the name of the constraint, can be <code>null</code>.
     * @param updateRule what happens to a foreign key when the primary key is updated.
     * @param deleteRule what happens to the foreign key when primary is deleted.
     */
    Link(List<SQLField> keys, List<SQLField> referredCols, String foreignKeyName, Rule updateRule, Rule deleteRule) {
        super(keys.get(0).getTable(), referredCols.get(0).getTable());
        if (keys.size() != referredCols.size())
            throw new IllegalArgumentException("size mismatch: " + keys + " != " + referredCols);
        this.cols = Collections.unmodifiableList(new ArrayList<SQLField>(keys));
        final ArrayList<String> tmpCols = new ArrayList<String>(this.cols.size());
        for (final SQLField f : this.cols) {
            tmpCols.add(f.getName());
        }
        this.colsNames = Collections.unmodifiableList(tmpCols);
        this.refCols = Collections.unmodifiableList(new ArrayList<SQLField>(referredCols));
        final ArrayList<String> tmpRefCols = new ArrayList<String>(this.refCols.size());
        for (final SQLField f : this.refCols) {
            tmpRefCols.add(f.getName());
        }
        this.refColsNames = Collections.unmodifiableList(tmpRefCols);
        this.name = foreignKeyName;
        this.updateRule = updateRule;
        this.deleteRule = deleteRule;
    }

    public final List<SQLField> getFields() {
        return this.cols;
    }

    /**
     * Get the one and only field of this link.
     * 
     * @return the one and only field of this link, <code>null</code> if there's more than one.
     * @see #getLabel()
     */
    public final SQLField getSingleField() {
        return CollectionUtils.getSole(this.cols);
    }

    public final List<String> getCols() {
        return this.colsNames;
    }

    /**
     * Get the one and only field of this link.
     * 
     * @return the one and only field of this link.
     * @see #getSingleField()
     * @throws IllegalStateException if there's more than one.
     */
    public final SQLField getLabel() throws IllegalStateException {
        if (this.cols.size() == 1)
            return this.cols.get(0);
        else
            throw new IllegalStateException(this + " has more than 1 foreign column: " + this.getFields());
    }

    public List<SQLField> getRefFields() {
        return this.refCols;
    }

    public final List<String> getRefCols() {
        return this.refColsNames;
    }

    public final String getName() {
        return this.name;
    }

    /**
     * The contextual name of the target from the source. Eg if SCH.A points SCH2.B, this would
     * return "SCH2"."B".
     * 
     * @return the name of the target.
     * @see SQLIdentifier#getContextualSQLName(SQLIdentifier)
     */
    public final SQLName getContextualName() {
        return this.getTarget().getContextualSQLName(this.getSource());
    }

    public final Rule getUpdateRule() {
        return this.updateRule;
    }

    public final Rule getDeleteRule() {
        return this.deleteRule;
    }

    @Override
    public String toString() {
        return "<" + this.getFields() + " -> " + this.getTarget() + (this.getName() != null ? " '" + this.getName() + "'" : "") + ">";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        final Link o = (Link) other;
        return this.getFields().equals(o.getFields()) && this.getRefFields().equals(o.getRefFields()) && this.getUpdateRule() == o.getUpdateRule() && this.getDeleteRule() == o.getDeleteRule();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.getFields().hashCode();
        result = prime * result + this.getRefFields().hashCode();
        return result;
    }

    // instead of using SQLField, this class only uses String and SQLName
    static private final HashingStrategy<Link> INTERSYSTEM_STRATEGY = new HashingStrategy<Link>() {
        @Override
        public boolean equals(final Link thisLink, final Link otherLink) {
            if (thisLink == otherLink)
                return true;
            if (thisLink == null || otherLink == null)
                return false;
            return thisLink.getSource().getName().equals(otherLink.getSource().getName()) && thisLink.getCols().equals(otherLink.getCols()) && thisLink.getUpdateRule() == otherLink.getUpdateRule()
                    && thisLink.getDeleteRule() == otherLink.getDeleteRule() && thisLink.getRefCols().equals(otherLink.getRefCols())
                    && thisLink.getContextualName().equals(otherLink.getContextualName());
        }

        @Override
        public int computeHashCode(Link l) {
            final int prime = 31;
            int result = 1;
            result = prime * result + l.getCols().hashCode();
            result = prime * result + l.getContextualName().hashCode();
            return result;
        }
    };

    public static final HashingStrategy<Link> getInterSystemHashStrategy() {
        return INTERSYSTEM_STRATEGY;
    }

    void toXML(final Writer pWriter) throws IOException {
        pWriter.write("  <link to=\"" + OUTPUTTER.escapeAttributeEntities(this.getTarget().getSQLName().toString()) + "\" ");
        if (this.getName() != null)
            pWriter.write("name=\"" + OUTPUTTER.escapeAttributeEntities(this.getName()) + "\" ");
        if (this.getUpdateRule() != null)
            pWriter.write("updateRule=\"" + OUTPUTTER.escapeAttributeEntities(this.getUpdateRule().name()) + "\" ");
        if (this.getDeleteRule() != null)
            pWriter.write("deleteRule=\"" + OUTPUTTER.escapeAttributeEntities(this.getDeleteRule().name()) + "\" ");
        if (this.getFields().size() == 1) {
            toXML(pWriter, 0);
            pWriter.write("/>\n");
        } else {
            pWriter.write(">\n");
            for (int i = 0; i < this.getFields().size(); i++) {
                pWriter.write("    <l ");
                toXML(pWriter, i);
                pWriter.write("/>\n");
            }
            pWriter.write("  </link>\n");
        }
    }

    private void toXML(final Writer pWriter, final int i) throws IOException {
        pWriter.write("col=\"");
        pWriter.write(OUTPUTTER.escapeAttributeEntities(this.getFields().get(i).getName()));
        pWriter.write("\" refCol=\"");
        pWriter.write(OUTPUTTER.escapeAttributeEntities(this.getRefFields().get(i).getName()));
        pWriter.write("\"");
    }

    static public Link fromXML(final SQLTable t, final Element linkElem) {
        final SQLName to = SQLName.parse(linkElem.getAttributeValue("to"));
        final SQLTable foreignTable = t.getDBSystemRoot().getDesc(to, SQLTable.class);
        final String linkName = linkElem.getAttributeValue("name");
        final Rule updateRule = Rule.fromName(linkElem.getAttributeValue("updateRule"));
        final Rule deleteRule = Rule.fromName(linkElem.getAttributeValue("deleteRule"));
        final List<Element> lElems = linkElem.getAttribute("col") != null ? singletonList(linkElem) : linkElem.getChildren("l");
        final List<SQLField> cols = new ArrayList<SQLField>();
        final List<SQLField> refcols = new ArrayList<SQLField>();
        for (final Element l : lElems) {
            cols.add(t.getField(l.getAttributeValue("col")));
            refcols.add(foreignTable.getField(l.getAttributeValue("refCol")));
        }
        return new Link(cols, refcols, linkName, updateRule, deleteRule);
    }
}
