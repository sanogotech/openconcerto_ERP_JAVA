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

/**
 * A field and a function in an SQL statement, such as 'sum(obs.CONSTAT)'.
 *
 * @author vincent
 */
public class SQLFunctionField implements FieldRef {

    public static class SQLFunction {
        public static final SQLFunction LOWER = new SQLFunction("LOWER");
        public static final SQLFunction UPPER = new SQLFunction("UPPER");
        public static final SQLFunction COUNT = new SQLFunction("COUNT");
        public static final SQLFunction SUM = new SQLFunction("SUM");

        private final String sqlString;

        public SQLFunction(final String sqlString) {
            this.sqlString = sqlString;
        }

        public String getSQL() {
            return this.sqlString;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " " + this.getSQL();
        }
    }

    private final SQLFunction function;
    private final FieldRef field;

    public SQLFunctionField(final SQLFunction function, final FieldRef field) {
        this.function = function;
        this.field = field;
    }

    public SQLFunction getFunction() {
        return this.function;
    }

    @Override
    public SQLField getField() {
        return this.field.getField();
    }

    @Override
    public String getAlias() {
        return this.field.getAlias();
    }

    @Override
    public String getFieldRef() {
        return this.getFunction().getSQL() + "(" + this.field.getFieldRef() + ")";
    }

    @Override
    public TableRef getTableRef() {
        return this.field.getTableRef();
    }
}
