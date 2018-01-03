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

public abstract class DayOfMonthDataModel extends DataModel1D {

    private Thread thread;
    private int size = 31;
    private int year;
    private int month;

    public DayOfMonthDataModel(final int year, final int month) {
        this.year = year;
        this.month = month;
    }

    public synchronized void load() {
        loadYear(year, month);
    }

    public synchronized void loadYear(final int year, final int month) {
        if (thread != null) {
            thread.interrupt();
        }

        this.year = year;
        this.month = month;
        thread = new Thread() {
            @Override
            public void run() {
                setState(LOADING);
                // Clear
                DayOfMonthDataModel.this.clear();
                fireDataModelChanged();
                final Calendar cal = Calendar.getInstance();
                cal.set(getYear(), month, 1);
                size = cal.getMaximum(Calendar.DAY_OF_MONTH);

                try {

                    for (int i = 0; i < size; i++) {
                        if (isInterrupted()) {
                            break;
                        }
                        Calendar c = cal;
                        c.set(year, month, i);
                        final Date d1 = new Date(c.getTimeInMillis());

                        final Date d2 = new Date(c.getTimeInMillis());
                        Thread.yield();

                        final long value = computeValue(d1, d2);
                        if (value != 0) {
                            DayOfMonthDataModel.this.setValueAt(i, value);
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

    public final synchronized int getMonth() {
        return month;
    }

    @Override
    public final synchronized int getSize() {
        return size;
    }

    /**
     * Compute value between 2 dates
     */
    public abstract long computeValue(Date d1, Date d2);
}
