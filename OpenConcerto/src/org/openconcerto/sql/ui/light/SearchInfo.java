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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.view.search.SearchList;
import org.openconcerto.sql.view.search.TextSearchSpec;
import org.openconcerto.sql.view.search.TextSearchSpec.Mode;
import org.openconcerto.ui.light.SearchContent;
import org.openconcerto.ui.light.SearchSpec;

import java.util.ArrayList;
import java.util.List;

public class SearchInfo {
    // TODO: add notion of operator
    private final SearchList list = new SearchList();
    private final List<String> texts = new ArrayList<String>();

    public SearchInfo(final SearchSpec params) {
        int stop = params.getContent().size();
        for (int i = 0; i < stop; i++) {
            final SearchContent param = params.getContent().get(i);
            final String col = param.getColumn();
            final String type = param.getType();
            final String[] tTexts = param.getText().split(" ");

            Mode mode = Mode.CONTAINS;
            if (type.equals("contains")) {
                mode = Mode.CONTAINS;
            } else if (type.equals("equals")) {
                mode = Mode.EQUALS;
            } else if (type.equals("lth")) {
                mode = Mode.LESS_THAN;
            } else if (type.equals("gth")) {
                mode = Mode.GREATER_THAN;
            } else {
                throw new IllegalArgumentException("mode " + type + " not supported");
            }

            for (final String text : tTexts) {
                this.list.addSearchItem(new TextSearchSpec(text, mode));
                this.texts.add(text);
                Log.get().info("searching column:" + col + "type:" + type + " text:" + text);
            }
        }
    }

    public List<String> getTexts() {
        return this.texts;
    }

    private final List<Object> t = new ArrayList<Object>();

    public boolean match(Object value) {
        this.t.clear();
        this.t.add(value);
        return this.list.match(this.t);
    }
}
