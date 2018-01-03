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
 
 package org.openconcerto.sql.model.graph;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.Immutable;

/**
 * Toutes les méthodes utilisent des heuristiques. Utiliser par DatabaseGraph pour faire la carte
 * d'une base MySQL.
 */
@Immutable
public class SQLKey {

    static public final String PREFIX = "ID_";

    /**
     * Si le champ passé est une clé.
     * 
     * @param fieldName le champ à tester.
     * @return <code>true</code> si le champ passé est une clé.
     */
    static private boolean isKey(String fieldName) {
        return fieldName.toUpperCase().startsWith(PREFIX);
    }

    /**
     * Retourne les clé étrangères de la table passée.
     * 
     * @param table la table.
     * @return l'ensemble des noms des clés étrangères.
     */
    public static Set<String> foreignKeys(SQLTable table) {
        // we used to name the primary key ID_TABLE, so we must not interpret it as a self reference
        // getSole() so we can have join tables (e.g. ID_CONTENU and ID_SITE are the primary key)
        final String pkeyName = CollectionUtils.getSole(table.getPKsNames());

        final Set<String> result = new HashSet<String>();
        final Iterator i = table.getFields().iterator();
        while (i.hasNext()) {
            final String fieldName = ((SQLField) i.next()).getName();
            // inclure les clés sauf les clés primaires
            if (isKey(fieldName) && !fieldName.equals(pkeyName))
                result.add(fieldName);
        }
        return result;
    }

    /**
     * Pour une clé retourne la table correspondante. The target table will be searched in the same
     * root then in the path.
     * <p>
     * Attention : cette méthode utilise un heuristique et n'est pas infaillible.
     * </p>
     * 
     * @param key la clé, par exemple "OBSERVATION.ID_ARTICLE_2".
     * @return la table, par exemple "ARTICLE".
     * @throws IllegalArgumentException si le champ passé n'est pas une clé.
     * @throws IllegalStateException si la table ne peut être déterminée.
     * @see #isKey(String)
     */
    public static SQLTable keyToTable(SQLField key) {
        SQLTable table = key.getTable();
        String keyName = key.getName();
        if (isKey(keyName)) {
            // remove the keyPrefix
            String rest = keyName.substring(PREFIX.length());
            // remove one by one the last parts
            SQLTable res = null;
            while (res == null && rest.length() > 0) {
                // privilege our own root, then check the rest of the roots
                res = table.getDBRoot().findTable(rest);
                if (res == null) {
                    int last_ = rest.lastIndexOf('_');
                    if (last_ > -1)
                        rest = rest.substring(0, last_);
                    else
                        rest = "";
                }
            }
            if (res == null)
                throw new IllegalStateException("unable to find the table that " + key.getSQLName() + " points to.");
            if (res.getPrimaryKeys().size() != 1)
                throw new IllegalStateException(key + " points to " + res + " which doesn't have 1 primary key.");
            return res;
        } else {
            throw new IllegalArgumentException("passed string is not a key");
        }
    }

    static public enum Type {
        PRIMARY_KEY, FOREIGN_KEY
    }

    static public SQLKey createPrimaryKey(SQLTable t) {
        final List<String> pk = t.getPKsNames();
        if (pk.isEmpty())
            return null;
        return new SQLKey(pk, null);
    }

    static public SQLKey createForeignKey(final Link link) {
        return new SQLKey(link.getCols(), link);
    }

    private final List<String> fields;
    private final Link link;

    private SQLKey(final List<String> fields, final Link link) {
        if (fields.isEmpty())
            throw new IllegalArgumentException("Empty fields");
        this.fields = Collections.unmodifiableList(new ArrayList<String>(fields));
        this.link = link;
    }

    public final List<String> getFields() {
        return this.fields;
    }

    public final Type getType() {
        return this.link == null ? Type.PRIMARY_KEY : Type.FOREIGN_KEY;
    }

    /**
     * The foreign link of this key.
     * 
     * @return the foreign link, <code>null</code> for {@link Type#PRIMARY_KEY}.
     */
    public final Link getForeignLink() {
        return this.link;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.fields.hashCode();
        result = prime * result + ((this.link == null) ? 0 : this.link.hashCode());
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
        final SQLKey other = (SQLKey) obj;
        return this.fields.equals(other.fields) && CompareUtils.equals(this.link, other.link);
    }

    @Override
    public String toString() {
        return this.getType() + " " + this.getFields();
    }
}
