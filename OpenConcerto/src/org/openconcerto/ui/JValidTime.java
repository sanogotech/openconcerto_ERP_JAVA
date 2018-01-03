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

import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.checks.ValidChangeSupport;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

/**
 * Allow to edit an always valid time. The day of the date value is always the epoch.
 */
public final class JValidTime extends JPanel implements ValueWrapper<Date>, TextComponent {

    private final boolean fillWithCurrentTime;
    private final TimeTextField text;
    private final ValidChangeSupport validSupp;

    /**
     * Create the component, empty.
     */
    public JValidTime() {
        this(false);
    }

    /**
     * Create the component.
     * 
     * @param fillWithCurrentTime <code>true</code> if this should be filled with the current hour,
     *        else empty.
     */
    public JValidTime(final boolean fillWithCurrentTime) {
        super(new BorderLayout());
        this.fillWithCurrentTime = fillWithCurrentTime;

        this.text = new TimeTextField();
        this.add(this.text, BorderLayout.CENTER);

        this.text.addPropertyChangeListener("editValid", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setValidated((Boolean) evt.getNewValue());
            }
        });
        // initial value
        this.validSupp = new ValidChangeSupport(this, ValidState.getNoReasonInstance(true));

        this.resetValue();
    }

    private JTextComponent getEditor() {
        return this.text;
    }

    @Override
    public final void resetValue() {
        if (this.fillWithCurrentTime) {
            Calendar c = Calendar.getInstance();
            setTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        } else {
            this.setValue(null);
        }
    }

    @Override
    public final void setValue(final Date val) {
        if (val == null) {
            this.text.setTime(0, 0);
        } else {
            long minutes = val.getTime() / (60 * 1000);
            System.out.println(minutes);
            this.text.setTime((int) (minutes / 60), (int) (minutes % 60));
        }

    }

    @Override
    public final Date getValue() {
        return new Date(getTimeInMillis());
    }

    public void setTime(int hours, int minutes) {
        this.text.setTime(hours, minutes);
    }

    public int getHours() {
        return this.text.getHours();
    }

    public int getMinutes() {
        return this.text.getMinutes();
    }

    @Override
    public final void addValueListener(PropertyChangeListener l) {
        this.getEditor().addPropertyChangeListener("value", l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.getEditor().removePropertyChangeListener("value", l);
    }

    @Override
    public JComponent getComp() {
        return this;
    }

    protected final void setValidated(boolean newValue) {
        this.validSupp.fireValidChange(ValidState.getNoReasonInstance(newValue));
    }

    @Override
    public ValidState getValidState() {
        return this.validSupp.getValidState();
    }

    @Override
    public void addValidListener(ValidListener l) {
        this.validSupp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.validSupp.removeValidListener(l);
    }

    @Override
    public JTextComponent getTextComp() {
        return this.getEditor();
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final JValidTime t = new JValidTime(true);
        // 2h10
        t.setTimeInMillis(2 * 60 * 60 * 1000 + 10 * 60 * 1000);
        System.out.println(t.getTimeInMillis());
        f.setContentPane(t);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    public long getTimeInMillis() {
        return (getHours() * 60 + getMinutes()) * 60 * 1000;
    }

    public void setTimeInMillis(long ms) {
        setValue(new Date(ms));
    }
}
