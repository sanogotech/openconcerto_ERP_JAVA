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
 
 package org.openconcerto.ui;

import java.awt.FlowLayout;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TimeSpinField extends JPanel {
    private JSpinner spinHour, spinMinute;

    public TimeSpinField() {
        init(0, 0);
    }

    public TimeSpinField(boolean fillWithCurrentTime) {
        if (fillWithCurrentTime) {
            Calendar c = Calendar.getInstance();
            init(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        } else {
            init(0, 0);
        }
    }

    public TimeSpinField(int hour, int minute) {

        init(hour, minute);
    }

    private void init(int hour, int minute) {
        this.setLayout(new FlowLayout());
        this.setOpaque(false);
        spinHour = new JSpinner(new SpinnerNumberModel(hour, 0, 23, 1));
        spinMinute = new JSpinner(new SpinnerNumberModel(minute, 0, 59, 1));
        setTime(hour, minute);
        spinHour.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                firePropertyChange("value", null, null);

            }
        });
        spinMinute.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                firePropertyChange("value", null, null);

            }
        });
        this.add(spinHour);
        this.add(new JLabel(" : "));
        this.add(spinMinute);

    }

    public void setTime(int hour, int minute) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be betwen 0 and 23 but is " + hour);
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Minute must be betwen 0 and 59 but is " + minute);
        }
        this.spinHour.setValue(hour);
        this.spinMinute.setValue(minute);
    }

    public int getHours() {
        return ((Number) this.spinHour.getValue()).intValue();

    }

    public int getMinutes() {
        return ((Number) this.spinMinute.getValue()).intValue();
    }

    public static void main(String[] args) {
        final JFrame f = new JFrame();
        final TimeSpinField time = new TimeSpinField(true);
        time.setTime(10, 59);
        f.setContentPane(time);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
