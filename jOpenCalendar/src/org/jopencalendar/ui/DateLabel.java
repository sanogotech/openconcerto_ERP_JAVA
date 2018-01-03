package org.jopencalendar.ui;

import java.util.Calendar;

import javax.swing.JLabel;

public class DateLabel extends JLabel {
    private static final long serialVersionUID = -5010955735639970080L;
    private long time;

    public DateLabel(String label, int align) {
        super(label, align);
    }

    public void setDate(Calendar cal) {
        this.time = cal.getTimeInMillis();
    }

    public long getTimeInMillis() {
        return time;
    }
}
