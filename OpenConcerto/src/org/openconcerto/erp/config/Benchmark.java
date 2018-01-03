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
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;

public class Benchmark {
    public Benchmark() {

    }

    public static void main(String[] args) throws IOException {
        Benchmark b = new Benchmark();
        b.testWriteHD();
        System.err.println(b.testCPU());
    }

    public String testDB() {
        IResultSetHandler h = new IResultSetHandler(new ResultSetHandler() {

            @Override
            public Object handle(ResultSet arg0) throws SQLException {
                return null;
            }
        }, false, false);
        long t1 = System.currentTimeMillis();
        int c = 0;
        SQLSelect s = new SQLSelect();
        SQLTable t = ComptaPropsConfiguration.getInstanceCompta().getRoot().getDBSystemRoot().findTable("FWK_SCHEMA_METADATA");
        s.addSelect(t.getField("NAME"));
        final SQLDataSource dataSource = ComptaPropsConfiguration.getInstanceCompta().getRoot().getDBSystemRoot().getDataSource();
        for (int i = 0; i < 1000000; i++) {
            final String sql = s.asString();
            dataSource.execute(sql, h);
            long t2 = System.currentTimeMillis();
            if (t2 > t1 + 2000) {
                c = i;
                break;
            }
        }
        return (c / 2) + " req/s";
    }

    public String testCPU() {
        long t0 = System.currentTimeMillis();
        double b[] = new double[1024 * 1024 * 50];
        for (int i = 0; i < b.length; i++) {
            b[i] = i * 456;
        }
        for (int k = 0; k < 10; k++) {
            for (int i = k; i < b.length - 2; i++) {
                b[i] = b[i] + b[i + 1] + 1 / b[i + 2];
            }
        }
        long t1 = System.currentTimeMillis();
        final long time = t1 - t0;
        double speed = b.length / time;

        return Math.round(speed / 100) + " R/s";
    }

    public String testWriteHD() throws IOException {
        long t0 = System.currentTimeMillis();
        long nbTest = 2;
        final int size = 100 * 1024 * 1024;
        for (int i = 0; i < nbTest; i++) {
            File f = File.createTempFile("benchmark", ".raw");
            FileOutputStream fOp = new FileOutputStream(f);

            byte[] b = new byte[size];
            fOp.write(b);
            fOp.flush();
            fOp.close();
            f.delete();
        }
        long t1 = System.currentTimeMillis();
        final long time = t1 - t0;
        long s = nbTest * size;
        double speed = s / time;
        String result = Math.round(speed / 1000) + " MB/s";
        return result;
    }
}
