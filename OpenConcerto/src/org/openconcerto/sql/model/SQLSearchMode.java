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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class SQLSearchMode {

    static public final SQLSearchMode EQUALS = new SQLSearchMode() {
        @Override
        public String generateSQL(final SQLSyntax s, final String term) {
            return " = " + s.quoteString(term);
        }

        @Override
        public List<SQLSearchMode> getHigherModes() {
            return Collections.emptyList();
        }
    };
    static public final SQLSearchMode STARTS_WITH = new SQLSearchMode() {
        @Override
        public String generateSQL(final SQLSyntax s, final String term) {
            return " like " + s.quoteString(s.getLitteralLikePattern(term) + "%");
        }

        @Override
        public List<SQLSearchMode> getHigherModes() {
            return Collections.singletonList(EQUALS);
        }
    };
    static public final SQLSearchMode ENDS_WITH = new SQLSearchMode() {
        @Override
        public String generateSQL(final SQLSyntax s, final String term) {
            return " like " + s.quoteString("%" + s.getLitteralLikePattern(term));
        }

        @Override
        public List<SQLSearchMode> getHigherModes() {
            return Collections.singletonList(EQUALS);
        }
    };

    private static final List<SQLSearchMode> CONTAINS_HIGHER_MODES = Arrays.asList(EQUALS, STARTS_WITH);
    static public final SQLSearchMode CONTAINS = new SQLSearchMode() {

        @Override
        public String generateSQL(final SQLSyntax s, final String term) {
            return " like " + s.quoteString("%" + s.getLitteralLikePattern(term) + "%");
        }

        @Override
        public List<SQLSearchMode> getHigherModes() {
            return CONTAINS_HIGHER_MODES;
        }
    };

    public final String generateSQL(final DBRoot r, final String term) {
        return this.generateSQL(SQLSyntax.get(r), term);
    }

    public abstract String generateSQL(final SQLSyntax s, final String term);

    // from highest to lowest
    public abstract List<SQLSearchMode> getHigherModes();
}
