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

import org.openconcerto.ui.table.TimestampTableCellEditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.jopencalendar.ui.DatePickerPanel;

public class TimestampEditorPanel extends JPanel implements ActionListener {

    private TimeSpinField timeSpinner;
    private TimeTextField timeText;
    private JPanel panelHour;
    private DatePickerPanel pickerPanel;
    private List<ActionListener> listeners = new Vector<ActionListener>();
    private TimestampTableCellEditor aCellEditor;
    private Calendar calendar = Calendar.getInstance();
    private JDate dateEditor;

    public TimestampEditorPanel() {
        this(false);
    }

    public TimestampEditorPanel(boolean useSpinner) {
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 3, 0, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;

        this.panelHour = new JPanel(new GridBagLayout());

        final JLabel labelHour = new JLabel("Heure ", SwingConstants.RIGHT);
        labelHour.setFont(labelHour.getFont().deriveFont(Font.BOLD));
        this.panelHour.add(labelHour, c);
        c.gridx++;
        if (useSpinner) {
            this.timeSpinner = new TimeSpinField();
            this.timeSpinner.setMinimumSize(new Dimension(timeSpinner.getPreferredSize()));
            this.panelHour.add(this.timeSpinner, c);
        } else {
            this.timeText = new TimeTextField();
            this.timeText.grabFocus();
            this.panelHour.add(this.timeText, c);
        }
        c.gridx++;

        final JButton buttonClose = new JButton(new ImageIcon(TimestampEditorPanel.class.getResource("close_popup_gray.png")));
        buttonClose.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (TimestampEditorPanel.this.aCellEditor != null) {
                    TimestampEditorPanel.this.aCellEditor.stopCellEditing();
                }
            }
        });
        buttonClose.setBorderPainted(false);
        buttonClose.setOpaque(false);
        buttonClose.setFocusPainted(false);
        buttonClose.setContentAreaFilled(false);
        buttonClose.setMargin(new Insets(1, 1, 1, 1));
        buttonClose.setFocusable(false);
        c.gridx = 0;
        this.panelHour.setOpaque(false);
        c.gridy++;
        this.panelHour.add(new JLabelBold("Date "), c);
        c.gridx++;
        c.gridwidth = 2;
        dateEditor = new JDate(true, true);
        dateEditor.getTextComp().addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {

                final JTextComponent textComp = dateEditor.getTextComp();
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        // Ne fonctionne que si dans un invokeLater
                        textComp.selectAll();
                    }
                });

            }
        });
        dateEditor.setButtonVisible(false);
        dateEditor.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Calendar c = Calendar.getInstance();
                c.setTimeInMillis(dateEditor.getDate().getTime());
                pickerPanel.setSelectedDate(c);
                calendar.set(Calendar.YEAR, c.get(Calendar.YEAR));
                calendar.set(Calendar.MONTH, c.get(Calendar.MONTH));
                calendar.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH));
                dateOrTimeChanged();

            }
        });
        this.panelHour.add(dateEditor, c);
        c.gridwidth = 1;
        this.add(this.panelHour, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.weightx = 1;
        c.gridx++;
        this.add(buttonClose, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        c.insets = new Insets(4, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);
        setBackground(Color.WHITE);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        this.pickerPanel = new DatePickerPanel();
        this.pickerPanel.setFocusable(false);
        add(this.pickerPanel, c);
        c.gridy++;

        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        this.pickerPanel.addPropertyChangeListener("timeInMillis", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                dateOrTimeChanged();
            }
        });
        if (useSpinner) {
            this.timeSpinner.addPropertyChangeListener("value", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    dateOrTimeChanged();
                }
            });
        } else {
            this.timeText.addPropertyChangeListener("value", new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    dateOrTimeChanged();
                }
            });
            this.timeText.addKeyListener(new KeyListener() {

                @Override
                public void keyTyped(KeyEvent e) {

                }

                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_TAB || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (aCellEditor != null) {
                            aCellEditor.stopCellEditing();
                        }
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {

                }
            });

        }

    }

    public void setTime(Date time) {
        calendar.setTimeInMillis(time.getTime());
        // update UI
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        final int minute = calendar.get(Calendar.MINUTE);
        this.pickerPanel.setSelectedDate(calendar);
        if (timeSpinner != null) {
            this.timeSpinner.setTime(hour, minute);
        } else {
            timeText.setTime(hour, minute);
        }
    }

    public Timestamp getTime() {
        return new Timestamp(calendar.getTimeInMillis());
    }

    public void actionPerformed(ActionEvent e) {
        dateOrTimeChanged();
    }

    public void dateOrTimeChanged() {
        stateChanged();
        fireTimeChangedPerformed();
    }

    public void stateChanged() {
        calendar.setTime(pickerPanel.getSelectedDate());
        dateEditor.setDate(pickerPanel.getSelectedDate());
        if (timeSpinner != null) {
            calendar.set(Calendar.HOUR_OF_DAY, timeSpinner.getHours());
            calendar.set(Calendar.MINUTE, timeSpinner.getMinutes());
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, timeText.getHours());
            calendar.set(Calendar.MINUTE, timeText.getMinutes());
        }
    }

    private void fireTimeChangedPerformed() {
        final int size = this.listeners.size();
        for (int i = 0; i < size; i++) {
            final ActionListener element = (ActionListener) this.listeners.get(i);
            element.actionPerformed(null);
        }

    }

    public void addActionListener(ActionListener listener) {
        this.listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        this.listeners.remove(listener);
    }

    public void setCellEditor(TimestampTableCellEditor editor) {
        this.aCellEditor = editor;
    }

    public void setHourVisible(boolean b) {
        this.panelHour.setVisible(b);
    }

    @Override
    public void requestFocus() {
        timeText.requestFocus();
        timeText.setCaretPosition(0);
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final TimestampEditorPanel t = new TimestampEditorPanel();
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.DAY_OF_YEAR, 8);
        c.set(Calendar.HOUR_OF_DAY, 13);
        c.set(Calendar.MINUTE, 14);

        t.setTime(c.getTime());
        t.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("TimestampEditorPanel got :" + t.getTime());

            }
        });
        f.setContentPane(t);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
