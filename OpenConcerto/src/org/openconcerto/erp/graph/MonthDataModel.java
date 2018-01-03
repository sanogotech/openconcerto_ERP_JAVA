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

public abstract class MonthDataModel extends DataModel1D {

    private Thread thread;
    private int year;
    private long total;
    private boolean cumul;

    public MonthDataModel(final int year, boolean cumul) {
        this.year = year;
        this.cumul = cumul;
    }

    public synchronized void load() {
        loadYear(this.year);
    }

    @Override
    public final int getSize() {
        return 12;
    }

    public synchronized void loadYear(int year) {
        if (thread != null) {
            thread.interrupt();
        }
        this.year = year;
        this.thread = new Thread() {

            @Override
            public void run() {
                setState(LOADING);
                // Clear
                MonthDataModel.this.clear();
                fireDataModelChanged();
                setTotal(0);
                try {
                    for (int i = 0; i < 12; i++) {
                        if (isInterrupted()) {
                            break;
                        }
                        final Calendar c = Calendar.getInstance();
                        c.set(getYear(), i, 1);
                        final Date d1 = new Date(c.getTimeInMillis());
                        c.set(Calendar.DATE, c.getActualMaximum(Calendar.DATE));
                        final Date d2 = new Date(c.getTimeInMillis());
                        Thread.yield();
                        final long value = computeValue(d1, d2);
                        addToTotal(value);
                        if (value != 0) {
                            MonthDataModel.this.setValueAt(i, cumul ? getTotal() : value);
                            fireDataModelChanged();
                            Thread.sleep(10);
                        }

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

    public final synchronized long getTotal() {
        return total;
    }

    public final synchronized void setTotal(long t) {
        this.total = t;
    }

    public final synchronized void addToTotal(long t) {
        this.total += t;
    }

    /**
     * Compute value between 2 dates
     */
    public abstract long computeValue(Date d1, Date d2);
}
