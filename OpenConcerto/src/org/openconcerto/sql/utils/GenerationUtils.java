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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.element.RowBacked;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;

import java.util.Map;

import ognl.NoSuchPropertyException;
import ognl.ObjectPropertyAccessor;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

public abstract class GenerationUtils {

    static public void setPropertyAccessors() {
        // comment naviguer dans eg site.id_etablissement.numero
        OgnlRuntime.setPropertyAccessor(SQLRowAccessor.class, new PropertyAccessor() {
            public Object getProperty(Map context, Object target, Object name) throws OgnlException {
                final SQLRowAccessor r = (SQLRowAccessor) target;
                final String nom = (String) name;
                final SQLTable table = r.getTable();
                final SQLField realField = table.getFieldRaw(nom);
                // getField() used to be insensitive, but all fields are upper case
                final SQLField field = realField != null ? realField : table.getFieldRaw(nom.toUpperCase());
                final SQLTable refTable;
                if (field != null) {
                    if (table.getForeignKeys().contains(field))
                        // si c'est une clef on trouve la ligne sur laquelle elle pointe
                        // MAYBE renvoyer null si pointe sur l'indéfini
                        return r.getForeign(field.getName());
                    else
                        // sinon on renvoie sa valeur
                        return r.getObject(field.getName());
                } else if ((refTable = table.getDBSystemRoot().getGraph().findReferentTable(table, nom)) != null) {
                    // ce nest pas un champ de la ligne courante
                    // on essaye dans l'autre sens
                    return r.getReferentRows(refTable);
                } else {
                    throw new OgnlException("'" + name + "' n'est ni un champ, ni une table référente de " + r);
                }
            }

            public void setProperty(Map context, Object target, Object name, Object value) throws OgnlException {
                // impossible
                throw new OgnlException("", new UnsupportedOperationException("setProperty not supported on SQL rows"));
            }
        });

        OgnlRuntime.setPropertyAccessor(org.jdom.Element.class, new PropertyAccessor() {
            @Override
            public Object getProperty(Map context, Object target, Object name) {
                org.jdom.Element elem = (org.jdom.Element) target;
                String n = (String) name;
                // that way for XML and SQLRow, fields are accessed the same way
                final String attributeValue = elem.getAttributeValue(n);
                if (attributeValue != null)
                    return attributeValue;
                else
                    // retourne le premier, TODO collections
                    return elem.getChild(n);
            }

            @Override
            public void setProperty(Map context, Object target, Object name, Object value) throws OgnlException {
                // impossible
                throw new OgnlException("", new UnsupportedOperationException("setProperty not supported on XML elements"));
            }
        });
        OgnlRuntime.setPropertyAccessor(org.jdom2.Element.class, new PropertyAccessor() {
            @Override
            public Object getProperty(Map context, Object target, Object name) {
                org.jdom2.Element elem = (org.jdom2.Element) target;
                String n = (String) name;
                // that way for XML and SQLRow, fields are accessed the same way
                final String attributeValue = elem.getAttributeValue(n);
                if (attributeValue != null)
                    return attributeValue;
                else
                    // retourne le premier, TODO collections
                    return elem.getChild(n);
            }

            @Override
            public void setProperty(Map context, Object target, Object name, Object value) throws OgnlException {
                // impossible
                throw new OgnlException("", new UnsupportedOperationException("setProperty not supported on XML elements"));
            }
        });

        OgnlRuntime.setPropertyAccessor(RowBacked.class, new ObjectPropertyAccessor() {
            public Object getProperty(Map context, Object target, Object name) throws OgnlException {
                // try the normal way (ie thru getters), if that fails try the get() method
                try {
                    return super.getProperty(context, target, name);
                } catch (NoSuchPropertyException e) {
                    final RowBacked elem = (RowBacked) target;
                    final String n = (String) name;
                    return elem.get(n.toUpperCase());
                }
            }
        });
    }
}
