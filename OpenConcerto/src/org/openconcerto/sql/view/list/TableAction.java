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
 
 package org.openconcerto.sql.view.list;

import java.util.ArrayList;
import java.util.List;

public class TableAction {
    private String id = null;
    private boolean showList = false;
    private List<RowAction> actions = new ArrayList<RowAction>();
    
    public TableAction(final RowAction action) {
        this.id = action.getID();
        this.actions.add(action);
    }
    public TableAction(final String id, final List<RowAction> actions) {
        this.id = id;
        this.actions.addAll(actions);
    }
    
    public String getId() {
        return this.id;
    }
    
    public RowAction getAction(final int index) {
        return this.actions.get(index);
    }
    
    public RowAction getAction(final String id) {
        final int size = this.actions.size();
        for(int i = 0; i < size; i++) {
            final RowAction action = this.actions.get(i);
            if(action.getID().equals(id)) {
                return action;
            }
        }
        return null;
    }
    
    public void setShowList(final boolean showList) {
        this.showList = showList;
    }
    
    public boolean isShowList() {
        return this.showList;
    }
    
    
    public int getActionsCount() {
        return this.actions.size(); 
    }
}
