package org.jopenchart;

import java.util.ArrayList;
import java.util.List;

public class DataModel {
    private List<DataModelListener> listeners = new ArrayList<DataModelListener>();
    private Chart chart;
    public static final int LOADING = 0;
    public static final int LOADED = 1;
    private int state = LOADED;

    public void addDataModelListener(DataModelListener l) {
        synchronized (this.listeners) {
            listeners.add(l);
        }
    }

    public void fireDataModelChanged() {
        final List<DataModelListener> copy = new ArrayList<DataModelListener>();
        synchronized (this.listeners) {
            copy.addAll(this.listeners);
        }
        for (final DataModelListener listener : copy) {
            listener.dataChanged();
        }
    }

    public synchronized void setChart(Chart chart) {
        this.chart = chart;
    }

    public synchronized Chart getChart() {
        return chart;
    }

    public synchronized int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
    }
}
