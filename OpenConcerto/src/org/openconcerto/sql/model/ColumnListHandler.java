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
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.openconcerto.sql.model;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * <code>ResultSetHandler</code> implementation that converts one <code>ResultSet</code> column into
 * a <code>List</code> of <code>Object</code>s. This class is thread safe.
 * 
 * @see ResultSetHandler
 * @since DbUtils 1.1
 */
public class ColumnListHandler extends ColumnListHandlerGeneric<Object> {

    /**
     * Creates a new instance of ColumnListHandler. The first column of each row will be returned
     * from <code>handle()</code>.
     */
    public ColumnListHandler() {
        this(1);
    }

    /**
     * Creates a new instance of ColumnListHandler.
     * 
     * @param columnIndex The index of the column to retrieve from the <code>ResultSet</code>.
     */
    public ColumnListHandler(int columnIndex) {
        this(columnIndex, null);
    }

    /**
     * Creates a new instance of ColumnListHandler.
     * 
     * @param columnName The name of the column to retrieve from the <code>ResultSet</code>.
     */
    public ColumnListHandler(String columnName) {
        this(-1, columnName);
    }

    private ColumnListHandler(int columnIndex, String columnName) {
        super(columnIndex, columnName, Object.class);
    }
}
