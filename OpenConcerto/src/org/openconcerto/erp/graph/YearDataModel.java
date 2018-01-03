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
 
 package org.openconcerto.erp.graph;

import org.openconcerto.utils.RTInterruptedException;

import java.util.Calendar;
import java.util.Date;

import org.jopenchart.DataModel1D;

public abstract class YearDataModel extends DataModel1D {

    private Thread thread;
    private int year;

    public YearDataModel(final int year) {
        this.year = year;
    }

    @Override
    public final int getSize() {
        return 1;
    }

    public synchronized void load() {
        loadYear(this.year);
    }

    public synchronized void loadYear(int year) {
        if (thread != null) {
            thread.interrupt();
        }
        this.year = year;

        thread = new Thread() {
            @Override
            public void run() {
                setState(LOADING);
                // Clear
                YearDataModel.this.clear();
                fireDataModelChanged();
                try {
                    final Calendar c = Calendar.getInstance();
                    c.set(getYear(), Calendar.JANUARY, 1);
                    final Date d1 = new Date(c.getTimeInMillis());
                    c.set(Calendar.MONTH, Calendar.DECEMBER);
                    c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
                    final Date d2 = new Date(c.getTimeInMillis());
                    Thread.yield();

                    final double value = computeValue(d1, d2);
                    if (((int) value) != 0) {
                        YearDataModel.this.setValueAt(0, value);
                        fireDataModelChanged();
                        Thread.sleep(10);
                    }

                    if (!isInterrupted()) {
                        setState(LOADED);
                        fireDataModelChanged();
                    }
                } catch (InterruptedException e) {
                    // Thread stopped because of year changed
                } catch (RTInterruptedException e) {
                    // Thread stopped because of year changed
                }

            }
        };

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

    }

    public final synchronized int getYear() {
        return year;
    }

    /**
     * Compute value between 2 dates
     */
    public abstract double computeValue(Date d1, Date d2);
}
