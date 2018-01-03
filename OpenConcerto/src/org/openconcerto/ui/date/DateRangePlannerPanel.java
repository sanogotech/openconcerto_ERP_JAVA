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
 
 package org.openconcerto.ui.date;

import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.JValidTime;
import org.openconcerto.ui.TM;
import org.openconcerto.ui.component.JRadioButtons;
import org.openconcerto.ui.date.EventProviders.Daily;
import org.openconcerto.ui.date.EventProviders.Monthly;
import org.openconcerto.ui.date.EventProviders.MonthlyDayOfWeek;
import org.openconcerto.ui.date.EventProviders.Weekly;
import org.openconcerto.ui.date.EventProviders.Yearly;
import org.openconcerto.ui.date.EventProviders.YearlyDayOfWeekEventProvider;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StringInputStream;
import org.openconcerto.utils.TimeUtils;
import org.openconcerto.utils.i18n.TM.MissingMode;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.icu.text.RuleBasedNumberFormat;

@SuppressWarnings("unqualified-field-access")
public class DateRangePlannerPanel extends JPanel {

    private static final Object[] WEEK_INDEXES = new Object[] { 1, 2, 3, 4, -2, -1 };

    private static final long serialVersionUID = 1006612828847678846L;

    private JValidTime timeStart;
    private JValidTime timeEnd;
    private Component currentPanel;
    private JRadioButtons<Period> radioPeriod;
    private Map<Period, Component> panels;

    // Date range
    private JRadioButton dayRadio1;
    private JRadioButton dayRadio2;

    private JDate dateStart;
    private JDate dateEnd;
    private JSpinner duration;
    private JSpinner spinDateRangeCount;
    private JSpinner dayEveryDay;

    // weekly
    private JSpinner weekIncrementSpinner;
    private Set<DayOfWeek> weekDays;
    List<JCheckBox> weekCheckboxes = new ArrayList<JCheckBox>();

    // monthly
    private JRadioButton monthRadio1;
    private JRadioButton monthRadio2;
    private int monthIncrement = -1;
    private JSpinner spinDayOfMonth;
    private JComboBox comboWeekOfMonth;
    private JComboBox comboWeekDayOfMonth;
    private JSpinner spinMonth2;
    private JSpinner spinMonth3;
    // yearly
    private int yearlyMonth = -1;
    private JSpinner yearlyDayOfMonth;
    private JComboBox yearlyComboWeekOfMonth;
    private JComboBox yearlyComboWeekDayOfMonth;

    private JComboBox yearMonthCombo;
    private JComboBox yearMonthCombo2;
    private JRadioButton yearRadio1;
    private JRadioButton yearRadio2;

    // Period
    private JRadioButton radioPeriodEndAt;
    private JRadioButton radioPeriodRepeat;
    private JRadioButton radioPeriodNeverEnd;

    protected boolean listenersEnabled = true;

    private DayOfWeek[] week;

    public DateRangePlannerPanel() {
        this.weekDays = new HashSet<DayOfWeek>();
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        final JLabelBold timeLabel = new JLabelBold("Horaires");
        this.add(timeLabel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.gridy++;
        // Time

        this.add(createTimePanel(), c);
        c.gridy++;
        // Period
        final JLabelBold periodLabel = new JLabelBold("Périodicité");
        this.add(periodLabel, c);
        c.gridy++;
        this.add(createPerdiodPanel(), c);
        c.gridy++;
        // Range
        final JLabel rangeLabel = new JLabel("Plage de périodicité");
        this.add(rangeLabel, c);
        c.gridy++;
        c.weighty = 1;
        this.add(createRangePanel(), c);

    }

    private Component createTimePanel() {
        final JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("Heure de début"));
        timeStart = new JValidTime(true);

        p.add(timeStart);
        p.add(new JLabel("  Fin"));
        timeEnd = new JValidTime(true);
        timeEnd.setTimeInMillis(Math.min(86400000 - 1, timeStart.getTimeInMillis() + 3600 * 1000));
        p.add(timeEnd);
        p.add(new JLabel("  Durée"));
        duration = new JSpinner(new SpinnerNumberModel(1, 1, 1439, 1));
        p.add(duration);
        p.add(new JLabel("minutes"));

        //
        long delta = timeEnd.getTimeInMillis() - timeStart.getTimeInMillis();
        if (delta < 60 * 1000) {
            delta = 60 * 1000;
            timeStart.setTimeInMillis(timeEnd.getTimeInMillis() - delta);
        }
        duration.setValue(Integer.valueOf((int) (delta / (60 * 1000))));

        timeStart.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (listenersEnabled) {

                    long ms = timeStart.getTimeInMillis() + getSpinnerValue(duration) * 60 * 1000;
                    ms = ms % (24 * 3600 * 1000);
                    setTimeEnd(ms);
                }

            }
        });

        timeEnd.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (listenersEnabled) {
                    long delta = timeEnd.getTimeInMillis() - timeStart.getTimeInMillis();
                    if (delta < 0) {
                        delta = 60 * 1000;
                        long ms = timeEnd.getTimeInMillis() - getSpinnerValue(duration) * 60 * 1000;
                        if (ms < 0) {
                            ms += 24 * 3600 * 1000;
                        }
                        setTimeStart(ms);
                    } else {
                        final Integer min = Integer.valueOf((int) (delta / (60 * 1000)));
                        setDuration(min);
                    }
                }
            }

        });
        duration.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (listenersEnabled) {
                    try {
                        int min = getSpinnerValue(duration);
                        if (min > 0) {
                            long ms = timeStart.getTimeInMillis() + min * 60 * 1000;
                            ms = ms % (24 * 3600 * 1000);
                            setTimeEnd(ms);
                        }
                    } catch (Exception ex) {
                        // Nothing
                    }
                }
            }
        });

        return p;
    }

    private Component createPerdiodPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        final Map<Period, String> choices = new LinkedHashMap<Period, String>();
        choices.put(Period.DAY, "Quotidienne");
        choices.put(Period.WEEK, "Hebdomadaire");
        choices.put(Period.MONTH, "Mensuelle");
        choices.put(Period.YEAR, "Annuelle");

        radioPeriod = new JRadioButtons<Period>(false, choices);
        radioPeriod.setValue(Period.DAY);
        p.add(radioPeriod, c);
        c.gridx++;
        c.fill = GridBagConstraints.VERTICAL;
        p.add(new JSeparator(JSeparator.VERTICAL), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        this.panels = new HashMap<Period, Component>();
        this.panels.put(Period.DAY, createDayPanel());
        this.panels.put(Period.WEEK, createWeekPanel());
        this.panels.put(Period.MONTH, createMonthPanel());
        this.panels.put(Period.YEAR, createYearPanel());
        this.currentPanel = this.panels.get(Period.DAY);
        p.add(currentPanel, c);
        this.currentPanel.setPreferredSize(new Dimension(currentPanel.getPreferredSize().width + 80, currentPanel.getPreferredSize().height));

        radioPeriod.addValueListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null) {
                    final Period id = (Period) evt.getNewValue();
                    p.remove(currentPanel);
                    currentPanel = panels.get(id);
                    p.add(currentPanel, c);
                    p.revalidate();
                    p.repaint();
                }
            }
        });

        return p;
    }

    private Component createRangePanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        p.add(new JLabel("Début"), c);
        c.fill = GridBagConstraints.NONE;
        c.gridx++;
        dateStart = new JDate(true, true);
        p.add(dateStart, c);
        c.gridx++;
        radioPeriodEndAt = new JRadioButton("Fin le");
        p.add(radioPeriodEndAt, c);
        c.gridx++;
        dateEnd = new JDate(false, true);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        dateEnd.setValue(cal.getTime());
        c.gridwidth = 2;
        p.add(dateEnd, c);
        //
        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 2;
        radioPeriodRepeat = new JRadioButton("Fin après");
        p.add(radioPeriodRepeat, c);
        c.gridx++;
        spinDateRangeCount = new JSpinner(new SpinnerNumberModel(1, 1, 365 * 100, 1));
        spinDateRangeCount.setEnabled(false);
        p.add(spinDateRangeCount, c);
        c.gridx++;
        c.weightx = 1;
        p.add(new JLabel("occurences"), c);
        //
        c.gridy++;
        c.gridwidth = 1;
        c.gridx = 2;
        radioPeriodNeverEnd = new JRadioButton("Pas de fin");
        p.add(radioPeriodNeverEnd, c);
        c.gridx++;

        ButtonGroup group = new ButtonGroup();
        group.add(radioPeriodEndAt);
        group.add(radioPeriodRepeat);
        group.add(radioPeriodNeverEnd);

        radioPeriodEndAt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                spinDateRangeCount.setEnabled(radioPeriodRepeat.isSelected());
                dateEnd.setEnabled(!radioPeriodRepeat.isSelected());
            }
        });
        radioPeriodRepeat.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                spinDateRangeCount.setEnabled(radioPeriodRepeat.isSelected());
                dateEnd.setEnabled(!radioPeriodRepeat.isSelected());
            }
        });
        radioPeriodNeverEnd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                spinDateRangeCount.setEnabled(false);
                dateEnd.setEnabled(false);
            }
        });
        // par défaut : pas de fin
        dateEnd.setEnabled(false);
        radioPeriodNeverEnd.setSelected(true);
        return p;
    }

    // DAY
    private Component createDayPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 2);
        //
        final JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dayRadio1 = new JRadioButton("Tous les ");
        dayRadio1.setSelected(true);
        dayEveryDay = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        final JLabel labelEvery = new JLabel("jour");
        dayEveryDay.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (getSpinnerValue(dayEveryDay) == 1) {
                    labelEvery.setText("jour");
                } else {
                    labelEvery.setText("jours");
                }

            }
        });
        p1.add(dayRadio1);
        p1.add(dayEveryDay);
        p1.add(labelEvery);
        //
        final JPanel p2 = new JPanel();
        p2.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        dayRadio2 = new JRadioButton("Tous les jours ouvrables");
        p2.add(dayRadio2);
        //
        dayRadio1.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dayEveryDay.setEnabled(dayRadio1.isSelected());
            }
        });
        dayRadio2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dayEveryDay.setEnabled(dayRadio1.isSelected());
            }
        });

        //
        final ButtonGroup g = new ButtonGroup();
        g.add(dayRadio1);
        g.add(dayRadio2);
        p1.setOpaque(false);
        p2.setOpaque(false);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        p.add(p1, c);
        c.gridy++;
        c.weighty = 1;

        p.add(p2, c);
        return p;

    }

    // WEEK

    private Component createWeekPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 4, 0, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        //
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p1.add(new JLabel("Toutes les"));
        weekIncrementSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        final JLabel labelEvery = new JLabel("semaine, le :");
        weekIncrementSpinner.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (getSpinnerValue(weekIncrementSpinner) == 1) {
                    labelEvery.setText("semaine, le :");
                } else {
                    labelEvery.setText("semaines, le :");
                }

            }
        });
        p1.add(weekIncrementSpinner);
        p1.add(labelEvery);
        c.gridwidth = 4;
        p.add(p1, c);
        //
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;

        final String[] namesOfDays = DateFormatSymbols.getInstance().getWeekdays();
        week = DayOfWeek.getWeek(Calendar.getInstance());
        final int weekLength = week.length;
        final int midWeek = weekLength / 2;
        for (int i = 0; i < weekLength; i++) {
            final DayOfWeek d = week[i];
            if (i == midWeek) {
                c.weightx = 1;
            } else {
                c.weightx = 0;
            }
            final JCheckBox cb = new JCheckBox(namesOfDays[d.getCalendarField()]);
            cb.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    // Do not use ItemListener in order to avoid issue on loading from xml
                    if (cb.isSelected()) {
                        weekDays.add(d);
                    } else {
                        weekDays.remove(d);
                        if (weekDays.isEmpty()) {
                            cb.setSelected(true);
                            weekDays.add(d);
                        }
                    }
                }
            });

            p.add(cb, c);
            weekCheckboxes.add(cb);

            if (i == midWeek) {
                c.gridx = 0;
                c.gridy++;
                c.weighty = 1;
            } else {
                c.gridx++;
            }
        }
        // Select first
        weekCheckboxes.get(0).setSelected(true);
        weekDays.add(week[0]);
        //
        return p;
    }

    protected final void setMonthIncrement(Object src) {
        this.monthIncrement = getSpinnerValue((JSpinner) src);
    }

    // MONTH
    private Component createMonthPanel() {
        final Calendar cal = Calendar.getInstance();

        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 4, 0, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        //
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        monthRadio1 = new JRadioButton("Le");
        monthRadio1.setSelected(true);
        p1.add(monthRadio1);
        this.spinDayOfMonth = createDayOfMonthSpinner(cal);
        p1.add(this.spinDayOfMonth);
        p1.add(new JLabel("tous les"));
        spinMonth2 = new JSpinner(new SpinnerNumberModel(1, 1, 96, 1));
        final ChangeListener setMonthIncrementCL = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setMonthIncrement(e.getSource());
            }
        };
        spinMonth2.addChangeListener(setMonthIncrementCL);
        p1.add(spinMonth2);
        p1.add(new JLabel("mois"));
        p.add(p1, c);
        //
        JPanel p2 = new JPanel();
        p2.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        monthRadio2 = new JRadioButton("Le");
        p2.add(monthRadio2);
        this.comboWeekOfMonth = createWeekOfMonthCombo();
        p2.add(comboWeekOfMonth);

        this.comboWeekDayOfMonth = createWeekDayOfMonthCombo(cal);
        p2.add(comboWeekDayOfMonth);
        p2.add(new JLabel("tous les"));
        spinMonth3 = new JSpinner(new SpinnerNumberModel(1, 1, 96, 1));
        p2.add(spinMonth3);
        p2.add(new JLabel("mois"));
        spinMonth3.addChangeListener(setMonthIncrementCL);

        c.gridy++;
        c.weighty = 1;
        p.add(p2, c);
        ButtonGroup g = new ButtonGroup();
        g.add(monthRadio1);
        g.add(monthRadio2);

        final ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean selected = monthRadio1.isSelected();
                spinDayOfMonth.setEnabled(selected);
                spinMonth2.setEnabled(selected);

                comboWeekOfMonth.setEnabled(!selected);
                comboWeekDayOfMonth.setEnabled(!selected);
                spinMonth3.setEnabled(!selected);

                setMonthIncrement(selected ? spinMonth2 : spinMonth3);
            }
        };
        monthRadio1.addActionListener(listener);
        monthRadio2.addActionListener(listener);
        // initialize
        listener.actionPerformed(null);
        return p;
    }

    protected JSpinner createDayOfMonthSpinner(final Calendar cal) {
        final int minDayOfMonth = cal.getMinimum(Calendar.DAY_OF_MONTH);
        final int maxDayOfMonth = cal.getMaximum(Calendar.DAY_OF_MONTH);
        return new JSpinner(new SpinnerNumberModel(minDayOfMonth, minDayOfMonth, maxDayOfMonth, 1));
    }

    protected JComboBox createWeekOfMonthCombo() {
        final JComboBox res = new JComboBox(WEEK_INDEXES);
        final RuleBasedNumberFormat f = new RuleBasedNumberFormat(RuleBasedNumberFormat.SPELLOUT);
        final String rule = TM.getInstance().translate(MissingMode.NULL, "day.spelloutRule");
        if (rule != null)
            f.setDefaultRuleSet(rule);
        res.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final String v = getWeekOfMonthTranslation(f, ((Number) value).longValue());
                return super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus);
            }

        });
        return res;
    }

    private static String getWeekOfMonthTranslation(long weekIndex) {
        final RuleBasedNumberFormat f = new RuleBasedNumberFormat(RuleBasedNumberFormat.SPELLOUT);
        final String rule = TM.getInstance().translate(MissingMode.NULL, "day.spelloutRule");
        if (rule != null) {
            f.setDefaultRuleSet(rule);
        }
        return getWeekOfMonthTranslation(f, weekIndex);
    }

    private static String getWeekOfMonthTranslation(final RuleBasedNumberFormat f, long weekIndex) {
        final String v;
        if (weekIndex == -2)
            v = TM.getInstance().translate("day.spellout.beforeLast");
        else if (weekIndex == -1)
            v = TM.getInstance().translate("day.spellout.last");
        else
            v = f.format(weekIndex);
        return v;
    }

    protected JComboBox createWeekDayOfMonthCombo(final Calendar cal) {
        final String[] namesOfDays = DateFormatSymbols.getInstance().getWeekdays();
        final DayOfWeek[] week = DayOfWeek.getWeek(cal);
        final JComboBox res = new JComboBox(week);
        res.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, namesOfDays[((DayOfWeek) value).getCalendarField()], index, isSelected, cellHasFocus);
            }
        });
        return res;
    }

    protected JComboBox createMonthCombo() {
        final String[] namesOfMonths = DateFormatSymbols.getInstance().getMonths();
        final Object[] monthsIndex = new Object[namesOfMonths.length];
        for (int i = 0; i < namesOfMonths.length; i++) {
            // from Calendar.MONTH : starts at 0
            monthsIndex[i] = i;
        }
        final JComboBox res = new JComboBox(monthsIndex);
        res.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, namesOfMonths[((Number) value).intValue()], index, isSelected, cellHasFocus);
            }
        });
        return res;
    }

    // YEAR
    private Component createYearPanel() {
        final Calendar cal = Calendar.getInstance();

        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 4, 0, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        //
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        yearRadio1 = new JRadioButton("Chaque");
        yearRadio1.setSelected(true);
        p1.add(yearRadio1);
        this.yearlyDayOfMonth = this.createDayOfMonthSpinner(cal);
        p1.add(this.yearlyDayOfMonth);

        yearMonthCombo = createMonthCombo();
        p1.add(yearMonthCombo);
        p.add(p1, c);
        //
        JPanel p2 = new JPanel();
        p2.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        yearRadio2 = new JRadioButton("Le");
        p2.add(yearRadio2);
        this.yearlyComboWeekOfMonth = this.createWeekOfMonthCombo();
        p2.add(this.yearlyComboWeekOfMonth);

        yearlyComboWeekDayOfMonth = this.createWeekDayOfMonthCombo(cal);
        p2.add(yearlyComboWeekDayOfMonth);
        p2.add(new JLabel("de"));
        yearMonthCombo2 = createMonthCombo();
        p2.add(yearMonthCombo2);

        c.gridy++;
        c.weighty = 1;
        p.add(p2, c);
        ButtonGroup g = new ButtonGroup();
        g.add(yearRadio1);
        g.add(yearRadio2);

        final ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean selected = yearRadio1.isSelected();
                yearlyDayOfMonth.setEnabled(selected);
                yearMonthCombo.setEnabled(selected);

                yearlyComboWeekOfMonth.setEnabled(!selected);
                yearlyComboWeekDayOfMonth.setEnabled(!selected);
                yearMonthCombo2.setEnabled(!selected);

                setYearlyMonth(selected ? yearMonthCombo : yearMonthCombo2);
            }
        };
        yearRadio1.addActionListener(listener);
        yearRadio2.addActionListener(listener);
        final ItemListener monthL = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setYearlyMonth(e.getSource());
                }
            }
        };
        yearMonthCombo.addItemListener(monthL);
        yearMonthCombo2.addItemListener(monthL);
        // initialize
        listener.actionPerformed(null);
        return p;
    }

    protected final void setYearlyMonth(final Object comp) {
        this.yearlyMonth = (Integer) ((JComboBox) comp).getSelectedItem();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (final Exception e) {
                    e.printStackTrace();
                }

                final JFrame f = new JFrame();
                JPanel p = new JPanel();

                final DateRangePlannerPanel planner = new DateRangePlannerPanel();
                p.setLayout(new BorderLayout());
                p.add(planner, BorderLayout.CENTER);

                JPanel tools = new JPanel();
                p.add(tools, BorderLayout.SOUTH);
                final JButton b = new JButton("Print ranges");
                tools.add(b);
                b.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        List<DateRange> ranges = planner.getRanges();
                        System.out.println("Printing " + ranges.size() + " ranges :");
                        for (DateRange dateRange : ranges) {
                            System.out.println(dateRange);
                        }
                        final String configXML = planner.getConfigXML();
                        System.out.println(getDescriptionFromXML(configXML));

                    }
                });
                final JButton bLoad = new JButton("Load");
                tools.add(bLoad);
                bLoad.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Loading...");
                        try {
                            String configXML = FileUtils.read(new File("test.xml"));
                            System.out.println(configXML);
                            planner.configureFromXML(configXML);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }

                    }
                });

                final JButton bSave = new JButton("Save");
                tools.add(bSave);

                bSave.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Saving...");
                        final String configXML = planner.getConfigXML();
                        System.out.println(configXML);
                        try {
                            FileUtils.write(configXML, new File("test.xml"));
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });

                f.setContentPane(p);
                f.pack();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setVisible(true);

            }
        });
    }

    public List<DateRange> getRanges() {
        final Period type = radioPeriod.getValue();
        final Date startDate = this.dateStart.getValue();
        final int timeStartInMS = (int) this.timeStart.getTimeInMillis();
        final int timeEndInMS = (int) this.timeEnd.getTimeInMillis();

        // final boolean endAfterDate = this.radioEndAt.isSelected();
        final Date endDate;
        final int eventCount;
        if (this.radioPeriodEndAt.isSelected()) {
            endDate = this.dateEnd.getValue();
            if (endDate.compareTo(startDate) < 0)
                throw new IllegalArgumentException("End before start");
            eventCount = -1;
        } else if (this.radioPeriodNeverEnd.isSelected()) {
            // plan for the next 20 year
            final Calendar c = Calendar.getInstance();
            c.add(Calendar.YEAR, 20);
            endDate = c.getTime();
            if (endDate.compareTo(startDate) < 0)
                throw new IllegalArgumentException("End before start");
            eventCount = -1;
        } else {
            endDate = null;
            eventCount = getSpinnerValue(this.spinDateRangeCount);
            if (eventCount <= 0)
                throw new IllegalArgumentException("Negative event count : " + eventCount);
        }

        final Calendar c = Calendar.getInstance();
        c.setTime(startDate);

        final EventProvider prov;
        if (type == Period.DAY) {
            if (dayRadio1.isSelected()) {
                final int incr = getSpinnerValue(dayEveryDay);
                prov = new Daily(incr);
            } else {
                prov = new Weekly(1, DayOfWeek.WORKING_DAYS);
            }
        } else if (type == Period.WEEK) {
            if (this.weekDays.isEmpty()) {
                prov = null;
            } else {
                final int incr = getSpinnerValue(this.weekIncrementSpinner);
                prov = new Weekly(incr, this.weekDays);
            }
        } else if (type == Period.MONTH) {
            if (this.spinDayOfMonth.isEnabled()) {
                prov = new Monthly(getSpinnerValue(this.spinDayOfMonth), this.monthIncrement);
            } else {
                prov = new MonthlyDayOfWeek((Integer) this.comboWeekOfMonth.getSelectedItem(), (DayOfWeek) this.comboWeekDayOfMonth.getSelectedItem(), this.monthIncrement);
            }
        } else if (type == Period.YEAR) {
            if (this.yearlyDayOfMonth.isEnabled()) {
                prov = new Yearly(getSpinnerValue(this.yearlyDayOfMonth), this.yearlyMonth, 1);
            } else {
                prov = new YearlyDayOfWeekEventProvider((Integer) this.yearlyComboWeekOfMonth.getSelectedItem(), (DayOfWeek) this.yearlyComboWeekDayOfMonth.getSelectedItem(), this.yearlyMonth, 1);
            }
        } else {
            throw new IllegalStateException("invalid type: " + type);
        }
        if (prov == null) {
            // Do not return Collections.emptyList() to allow reuse;
            return new ArrayList<DateRange>(0);
        }

        prov.next(c, true);
        final List<DateRange> result = new ArrayList<DateRange>();
        // use a different calendar since setStartAndStop() might change the day
        final Calendar startStopCal = (Calendar) c.clone();
        while (before(c, endDate) && lessThan(result.size(), eventCount)) {
            final DateRange r = new DateRange();
            final Date currentTime = c.getTime();
            startStopCal.setTime(currentTime);
            setStartAndStop(r, startStopCal, timeStartInMS, timeEndInMS);
            result.add(r);
            prov.next(c, false);
            // prevent infinite loop
            if (currentTime.compareTo(c.getTime()) >= 0)
                throw new IllegalStateException("Provider hasn't moved time forward");
        }
        return result;
    }

    private boolean before(Calendar c, Date endDate) {
        if (endDate == null)
            return true;
        return c.getTime().compareTo(endDate) <= 0;
    }

    private boolean lessThan(int currentEventCount, int eventCount) {
        if (eventCount < 0)
            return true;
        return currentEventCount < eventCount;
    }

    public void setStartDate(Date dateStart) {
        this.dateStart.setDate(dateStart);
    }

    protected void setStartAndStop(DateRange r, final Calendar c, final int timeStartInMS, final int timeEndInMS) {
        final int day = c.get(Calendar.DAY_OF_YEAR);
        TimeUtils.clearTime(c);
        c.add(Calendar.MILLISECOND, timeStartInMS);
        if (c.get(Calendar.DAY_OF_YEAR) != day)
            throw new IllegalArgumentException("More than a day : " + timeStartInMS);
        r.setStart(c.getTimeInMillis());

        if (timeEndInMS < timeStartInMS) {
            // pass midnight
            TimeUtils.clearTime(c);
            c.add(Calendar.DAY_OF_YEAR, 1);
            c.add(Calendar.MILLISECOND, timeEndInMS);
            // timeEndInMS < timeStartInMS && timeStartInMS < dayLength thus timeEndInMS < dayLength
            assert c.get(Calendar.DAY_OF_YEAR) == day + 1;
        } else {
            c.add(Calendar.MILLISECOND, timeEndInMS - timeStartInMS);
            if (c.get(Calendar.DAY_OF_YEAR) != day)
                throw new IllegalArgumentException("More than a day : " + timeEndInMS);
        }
        r.setStop(c.getTimeInMillis());
    }

    public String getConfigXML() {
        final StringBuilder b = new StringBuilder();
        b.append("<planner>");
        // Horaires
        b.append("<schedule start=\"");
        b.append(timeStart.getTimeInMillis());
        b.append("\" end=\"");
        b.append(timeEnd.getTimeInMillis());
        b.append("\" />");
        // Periodicité
        Period p = radioPeriod.getValue();
        int f = p.getCalendarField();
        b.append("<period type=\"");
        b.append(f);
        b.append("\">");
        if (f == Period.DAY.getCalendarField()) {
            b.append("<day every=\"");
            if (this.dayRadio1.isSelected()) {
                b.append(getSpinnerValue(dayEveryDay));
            } else {
                b.append("wd");
            }
            b.append("\" />");
        } else if (f == Period.WEEK.getCalendarField()) {
            b.append("<week every=\"");
            b.append(getSpinnerValue(weekIncrementSpinner));
            b.append("\">");
            for (JCheckBox cb : this.weekCheckboxes) {
                if (cb.isSelected()) {
                    b.append("<true/>");
                } else {
                    b.append("<false/>");
                }
            }
            b.append("</week>");
        } else if (f == Period.MONTH.getCalendarField()) {
            if (this.monthRadio1.isSelected()) {
                b.append("<month day=\"");
                b.append(getSpinnerValue(spinDayOfMonth));
                b.append("\" every=\"");
                b.append(getSpinnerValue(spinMonth2));
                b.append("\" />");
            } else {
                b.append("<month weekOfMonth=\"");
                b.append(comboWeekOfMonth.getSelectedIndex());
                b.append("\" dayOfMonth=\"");
                b.append(comboWeekDayOfMonth.getSelectedIndex());
                b.append("\" every=\"");
                b.append(getSpinnerValue(spinMonth3));
                b.append("\" />");
            }
        } else if (f == Period.YEAR.getCalendarField()) {
            if (this.yearRadio1.isSelected()) {
                b.append("<year day=\"");
                b.append(getSpinnerValue(this.yearlyDayOfMonth));
                b.append("\" month=\"");
                b.append(this.yearMonthCombo.getSelectedIndex());
                b.append("\" />");
            } else {
                b.append("<year weekOfMonth=\"");
                b.append(this.yearlyComboWeekOfMonth.getSelectedIndex());
                b.append("\" weekDayOfMonth=\"");
                b.append(this.yearlyComboWeekDayOfMonth.getSelectedIndex());
                b.append("\" month=\"");
                b.append(this.yearMonthCombo2.getSelectedIndex());
                b.append("\" />");
            }
        } else {
            throw new IllegalStateException("Unknown period:" + f);
        }

        b.append("</period>");
        // Plage
        b.append("<range start=\"");
        b.append(this.dateStart.getValue().getTime());
        b.append("\" ");
        if (this.radioPeriodEndAt.isSelected()) {
            b.append("end=\"");
            b.append(this.dateEnd.getValue().getTime());
            b.append("\"");
        } else if (this.radioPeriodRepeat.isSelected()) {
            b.append("repeat=\"");
            b.append(getSpinnerValue(spinDateRangeCount));
            b.append("\"");
        }
        b.append("/>");
        b.append("</planner>");
        return b.toString();
    }

    public void configureFromXML(String xml) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document dom = db.parse(new StringInputStream(xml, "UTF8"));
        // Schedule
        final NodeList l1 = dom.getElementsByTagName("schedule");
        final Node nSchedule = l1.item(0);
        final long tStart = getAttributeAsLong(nSchedule, "start");
        final long tStop = getAttributeAsLong(nSchedule, "end");
        listenersEnabled = false;
        this.timeStart.setTimeInMillis(tStart);
        this.timeEnd.setTimeInMillis(tStop);
        this.duration.setValue((tStop - tStart) / (60 * 1000));
        listenersEnabled = true;
        // Period
        final NodeList l2 = dom.getElementsByTagName("period");
        final Node nPeriod = l2.item(0);
        final int perioType = Integer.parseInt(nPeriod.getAttributes().getNamedItem("type").getTextContent());

        if (perioType == Period.DAY.getCalendarField()) {
            this.radioPeriod.setValue(Period.DAY);
            final Node nDay = nPeriod.getFirstChild();
            Integer l = getAttributeAsInteger(nDay, "every");
            if (l != null) {
                this.dayRadio1.setSelected(true);
                this.dayEveryDay.setValue(l);
                this.dayEveryDay.setEnabled(true);
            } else {
                this.dayRadio2.setSelected(true);
                this.dayEveryDay.setEnabled(false);
            }
        } else if (perioType == Period.WEEK.getCalendarField()) {
            this.radioPeriod.setValue(Period.WEEK);
            final Node nWeek = nPeriod.getFirstChild();
            Integer e = getAttributeAsInteger(nWeek, "every");
            this.weekIncrementSpinner.setValue(e);
            NodeList l = nWeek.getChildNodes();
            weekDays.clear();
            for (int i = 0; i < l.getLength(); i++) {
                Node n = l.item(i);
                final boolean selected = n.getNodeName().equals("true");
                this.weekCheckboxes.get(i).setSelected(selected);
                if (selected) {
                    weekDays.add(week[i]);
                }
            }
        } else if (perioType == Period.MONTH.getCalendarField()) {
            this.radioPeriod.setValue(Period.MONTH);
            final Node nMonth = nPeriod.getFirstChild();
            Integer d = getAttributeAsInteger(nMonth, "day");
            if (d != null) {
                this.monthRadio1.setSelected(true);
                Integer every = getAttributeAsInteger(nMonth, "every");
                spinDayOfMonth.setEnabled(true);
                spinMonth2.setEnabled(true);
                comboWeekOfMonth.setEnabled(false);
                comboWeekDayOfMonth.setEnabled(false);
                spinMonth3.setEnabled(false);
                //
                spinDayOfMonth.setValue(d);
                spinMonth2.setValue(every);
            } else {
                this.monthRadio2.setSelected(true);
                //
                spinDayOfMonth.setEnabled(false);
                spinMonth2.setEnabled(false);
                comboWeekOfMonth.setEnabled(true);
                comboWeekDayOfMonth.setEnabled(true);
                spinMonth3.setEnabled(true);
                //
                Integer weekOfMonth = getAttributeAsInteger(nMonth, "weekOfMonth");
                Integer dayOfMonth = getAttributeAsInteger(nMonth, "dayOfMonth");
                Integer every = getAttributeAsInteger(nMonth, "every");
                comboWeekOfMonth.setSelectedIndex(weekOfMonth.intValue());
                comboWeekDayOfMonth.setSelectedIndex(dayOfMonth.intValue());
                spinMonth3.setValue(every);
            }
        } else if (perioType == Period.YEAR.getCalendarField()) {
            this.radioPeriod.setValue(Period.YEAR);
            final Node nYear = nPeriod.getFirstChild();
            Integer d = getAttributeAsInteger(nYear, "day");
            if (d != null) {
                this.yearRadio1.setSelected(true);
                Integer m = getAttributeAsInteger(nYear, "month");
                this.yearlyDayOfMonth.setEnabled(true);
                this.yearMonthCombo.setEnabled(true);
                this.yearlyComboWeekOfMonth.setEnabled(false);
                this.yearlyComboWeekDayOfMonth.setEnabled(false);
                this.yearMonthCombo2.setEnabled(false);
                //
                this.yearlyDayOfMonth.setValue(d);
                this.yearMonthCombo.setSelectedIndex(m.intValue());
            } else {
                this.yearRadio2.setSelected(true);
                this.yearlyDayOfMonth.setEnabled(false);
                this.yearMonthCombo.setEnabled(false);
                this.yearlyComboWeekOfMonth.setEnabled(true);
                this.yearlyComboWeekDayOfMonth.setEnabled(true);
                this.yearMonthCombo2.setEnabled(true);

                Integer weekOfMonth = getAttributeAsInteger(nYear, "weekOfMonth");
                this.yearlyComboWeekOfMonth.setSelectedIndex(weekOfMonth.intValue());
                Integer weekDayOfMonth = getAttributeAsInteger(nYear, "weekDayOfMonth");
                this.yearlyComboWeekDayOfMonth.setSelectedIndex(weekDayOfMonth.intValue());
                Integer m = getAttributeAsInteger(nYear, "month");
                this.yearMonthCombo2.setSelectedIndex(m.intValue());
            }
        } else {
            throw new IllegalStateException("Unknown period:" + perioType);
        }

        // Year
        final NodeList l3 = dom.getElementsByTagName("range");
        final Node nRange = l3.item(0);
        final Long start = getAttributeAsLong(nRange, "start");
        final Long end = getAttributeAsLong(nRange, "end");
        final Integer repeat = getAttributeAsInteger(nRange, "repeat");
        this.dateStart.setValue(new Date(start));
        if (end != null) {
            this.radioPeriodEndAt.setSelected(true);
            this.dateEnd.setEnabled(true);
            this.spinDateRangeCount.setEnabled(false);
            this.dateEnd.setValue(new Date(end));
        } else if (repeat != null) {
            this.radioPeriodRepeat.setSelected(true);
            this.dateEnd.setEnabled(false);
            this.spinDateRangeCount.setEnabled(true);
            this.spinDateRangeCount.setValue(repeat);
        } else {
            this.dateEnd.setEnabled(false);
            this.spinDateRangeCount.setEnabled(false);
            this.radioPeriodNeverEnd.setSelected(true);
        }

    }

    public static String getDescriptionFromXML(String xml) {
        String result = "";
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            final Document dom = db.parse(new StringInputStream(xml, "UTF8"));
            // Schedule
            final NodeList l1 = dom.getElementsByTagName("schedule");
            final Node nSchedule = l1.item(0);
            final long tStart = getAttributeAsLong(nSchedule, "start");
            final long tStop = getAttributeAsLong(nSchedule, "end");
            result += "De " + formatTime(tStart) + " à " + formatTime(tStop) + " ";

            // Period
            final NodeList l2 = dom.getElementsByTagName("period");
            final Node nPeriod = l2.item(0);
            final int perioType = Integer.parseInt(nPeriod.getAttributes().getNamedItem("type").getTextContent());

            if (perioType == Period.DAY.getCalendarField()) {

                final Node nDay = nPeriod.getFirstChild();
                Integer l = getAttributeAsInteger(nDay, "every");
                if (l != null) {
                    if (l.intValue() == 1) {
                        result += "tous les jours";
                    } else {
                        result += "tous les " + l + " jours";
                    }
                } else {
                    result += "tous les jours ouvrables";
                }
            } else if (perioType == Period.WEEK.getCalendarField()) {

                final Node nWeek = nPeriod.getFirstChild();
                Integer e = getAttributeAsInteger(nWeek, "every");
                if (e == 1) {
                    result += "toutes les semaines, le ";
                } else {
                    result += "toutes les " + e + " semaines, le ";
                }
                NodeList l = nWeek.getChildNodes();
                final String[] namesOfDays = DateFormatSymbols.getInstance().getWeekdays();
                final DayOfWeek[] week = DayOfWeek.getWeek(Calendar.getInstance());
                for (int i = 0; i < l.getLength(); i++) {
                    Node n = l.item(i);
                    if (n.getNodeName().equals("true")) {
                        result += namesOfDays[week[i].getCalendarField()] + " ";
                    }
                }
            } else if (perioType == Period.MONTH.getCalendarField()) {

                final Node nMonth = nPeriod.getFirstChild();
                Integer d = getAttributeAsInteger(nMonth, "day");
                if (d != null) {
                    result += "le " + d;
                    int every = getAttributeAsInteger(nMonth, "every");
                    if (every == 1) {
                        result += " tous les mois";
                    } else {
                        result += " tous les " + every + " mois";
                    }

                } else {
                    Integer weekOfMonth = getAttributeAsInteger(nMonth, "weekOfMonth");
                    Integer dayOfMonth = getAttributeAsInteger(nMonth, "dayOfMonth");
                    int every = getAttributeAsInteger(nMonth, "every");
                    Integer index = (Integer) WEEK_INDEXES[weekOfMonth.intValue()];

                    result += "le " + getWeekOfMonthTranslation(index);
                    final String[] namesOfDays = DateFormatSymbols.getInstance().getWeekdays();
                    final DayOfWeek[] week = DayOfWeek.getWeek(Calendar.getInstance());
                    result += " " + namesOfDays[week[dayOfMonth].getCalendarField()];
                    if (every == 1) {
                        result += " tous les mois";
                    } else {
                        result += " tous les " + every + " mois";
                    }
                }
            } else if (perioType == Period.YEAR.getCalendarField()) {
                final Node nYear = nPeriod.getFirstChild();
                Integer d = getAttributeAsInteger(nYear, "day");
                if (d != null) {
                    result += "chaque " + d;
                    final String[] namesOfMonths = DateFormatSymbols.getInstance().getMonths();
                    Integer m = getAttributeAsInteger(nYear, "month");
                    result += " " + namesOfMonths[m.intValue()];
                } else {
                    Integer weekOfMonth = getAttributeAsInteger(nYear, "weekOfMonth");
                    result += "le ";
                    Integer index = (Integer) WEEK_INDEXES[weekOfMonth.intValue()];
                    result += getWeekOfMonthTranslation(index);
                    final String[] namesOfDays = DateFormatSymbols.getInstance().getWeekdays();
                    final DayOfWeek[] week = DayOfWeek.getWeek(Calendar.getInstance());
                    int i = getAttributeAsInteger(nYear, "weekDayOfMonth").intValue();
                    result += " " + namesOfDays[week[i].getCalendarField()];
                    final String[] namesOfMonths = DateFormatSymbols.getInstance().getMonths();
                    Integer m = getAttributeAsInteger(nYear, "month");
                    result += " de " + namesOfMonths[m.intValue()];
                }
            } else {
                throw new IllegalStateException("Unknown period:" + perioType);
            }
            result += ", ";
            // Year
            final NodeList l3 = dom.getElementsByTagName("range");
            final Node nRange = l3.item(0);
            final Long start = getAttributeAsLong(nRange, "start");
            final Long end = getAttributeAsLong(nRange, "end");
            final Integer repeat = getAttributeAsInteger(nRange, "repeat");
            DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
            result += "à partir du " + df.format(new Date(start));

            if (end != null) {
                result += " jusqu'au " + df.format(new Date(end)) + ".";
            } else if (repeat != null) {

                result += " " + repeat + " fois.";
            } else {
                result += ".";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String formatTime(long time) {
        long minutes = time / (60 * 1000);

        String h = String.valueOf((int) (minutes / 60));
        if (h.length() < 2) {
            h = "0" + h;
        }
        String m = String.valueOf((int) (minutes % 60));
        if (m.length() < 2) {
            m = "0" + m;
        }
        return h + ":" + m;
    }

    private static Long getAttributeAsLong(Node item, String attrName) {
        final Node n = item.getAttributes().getNamedItem(attrName);
        if (n == null)
            return null;
        try {
            final long l = Long.parseLong(n.getTextContent());
            return l;
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer getAttributeAsInteger(Node item, String attrName) {
        final Node n = item.getAttributes().getNamedItem(attrName);
        if (n == null)
            return null;
        try {
            final int l = Integer.parseInt(n.getTextContent());
            return l;
        } catch (Exception e) {
            return null;
        }
    }

    public JDate getJDateStart() {
        return dateStart;
    }

    public JDate getJDateEnd() {
        return dateEnd;
    }

    public void setTimeEnd(final long ms) {
        this.listenersEnabled = false;
        if (timeEnd.getTimeInMillis() != ms) {
            timeEnd.setTimeInMillis(ms);
        }
        this.listenersEnabled = true;
    }

    public void setTimeStart(final long ms) {
        this.listenersEnabled = false;
        if (timeStart.getTimeInMillis() != ms) {
            timeStart.setTimeInMillis(ms);
        }
        this.listenersEnabled = true;
    }

    public void setDuration(final Integer min) {
        this.listenersEnabled = false;
        if (getSpinnerValue(duration) != min) {
            duration.setValue(min);
        }
        this.listenersEnabled = true;
    }

    public int getSpinnerValue(JSpinner s) {
        final Object o = s.getValue();
        if (o == null)
            return 0;
        return ((Number) o).intValue();

    }

}
