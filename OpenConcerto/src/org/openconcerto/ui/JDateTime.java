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

import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.ParseException;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Allow to edit a date with a time.
 * 
 * @author Sylvain CUAZ
 */
public final class JDateTime extends JPanel implements ValueWrapper<Date> {

    private final JDate date;
    private final JTime time;
    private Date value;
    private final PropertyChangeSupport supp;

    /**
     * Create the component with the current hour.
     */
    public JDateTime() {
        this(false, false);
    }

    /**
     * Create the component.
     * 
     * @param fillWithCurrentDate <code>true</code> if this should be filled with the current date
     *        and hour, else empty.
     */
    public JDateTime(final boolean fillWithCurrentDate) {
        this(fillWithCurrentDate, fillWithCurrentDate);
    }

    /**
     * Create the component.
     * 
     * @param fillWithCurrentDate <code>true</code> if this should be filled with the current date,
     *        else empty.
     * @param fillWithCurrentHour <code>true</code> if this should be filled with the current hour,
     *        else empty.
     */
    public JDateTime(final boolean fillWithCurrentDate, final boolean fillWithCurrentHour) {
        super();
        this.setOpaque(false);
        this.date = new JDate(fillWithCurrentDate);
        this.time = new JTime(fillWithCurrentHour);
        this.supp = new PropertyChangeSupport(this);

        final PropertyChangeListener l = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateValue();
            }
        };
        this.date.addValueListener(l);
        this.time.addValueListener(l);

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        this.add(this.date, c);
        c.gridx++;
        c.weightx = 1;
        c.insets = new Insets(0, 4, 0, 0);
        this.add(this.time, c);

        this.resetValue();
        updateValue();
    }

    protected void updateValue() {
        if (this.date.getValue() == null) {
            this.value = null;
        } else if (this.time.getValue() == null) {
            this.value = new Date(this.date.getValue().getTime());
        } else {
            this.value = new Date(this.date.getValue().getTime() + this.time.getTimeInMillis());
        }
        this.supp.firePropertyChange("value", null, this.value);
    }

    @Override
    public final void resetValue() {
        this.date.resetValue();
        this.time.resetValue();
    }

    @Override
    public final void setValue(final Date val) {
        this.time.setValue(val);
        this.date.setValue(val == null ? null : new Date(val.getTime() - this.time.getTimeInMillis()));
    }

    @Override
    public final Date getValue() {
        return this.value;
    }

    @Override
    public final void addValueListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("value", l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("value", l);
    }

    public void commitEdit() throws ParseException {
        this.date.commitEdit();
        this.time.commitEdit();
    }

    @Override
    public JComponent getComp() {
        return this;
    }

    @Override
    public ValidState getValidState() {
        return ValidState.getTrueInstance();
    }

    @Override
    public void addValidListener(ValidListener l) {
        // nothing to do
    }

    @Override
    public void removeValidListener(ValidListener l) {
        // nothing to do
    }

    public static void main(String[] args) {
        final Runnable r = new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                final JFrame f = new JFrame();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setContentPane(new JDateTime());
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
