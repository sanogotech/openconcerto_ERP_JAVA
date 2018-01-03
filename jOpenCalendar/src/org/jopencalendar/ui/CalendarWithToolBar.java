package org.jopencalendar.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.print.PageFormat;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class CalendarWithToolBar extends JPanel {
    private WeekView weekView;
    private JSpinner spinWeek;
    private JSpinner spinYear;
    private String title;
    final JScrollPane contentPane = new JScrollPane();

    public CalendarWithToolBar(JCalendarItemProvider manager) {
        this(manager, false);
    }

    public CalendarWithToolBar(JCalendarItemProvider manager, boolean showPrintButton) {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));

        Calendar cal = Calendar.getInstance();
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        int year = cal.get(Calendar.YEAR);
        JButton bPrevious = new JButton(new ImageIcon(this.getClass().getResource("left.png")));
        JButton bNext = new JButton(new ImageIcon(this.getClass().getResource("right.png")));
        configureButton(bPrevious);
        configureButton(bNext);
        toolbar.add(bPrevious);
        toolbar.add(bNext);
        bNext.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addWeek(1);

            }
        });
        bPrevious.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addWeek(-1);

            }
        });

        toolbar.add(new JLabel("Semaine"));
        spinWeek = new JSpinner(new SpinnerNumberModel(week, 1, 53, 1));
        toolbar.add(spinWeek);
        toolbar.add(new JLabel(" de "));
        spinYear = new JSpinner(new SpinnerNumberModel(year, 1000, year + 20, 1));
        toolbar.add(spinYear);
        //
        final DatePicker picker = new DatePicker(false);
        toolbar.add(picker);
        //

        final JSlider zoomSlider = new JSlider(1, 9, 1);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.setMajorTickSpacing(1);
        zoomSlider.setPaintTicks(true);
        zoomSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                weekView.setZoom(zoomSlider.getValue());
            }
        });
        toolbar.add(new JLabel(" Zoom"));
        toolbar.add(zoomSlider);
        if (showPrintButton) {
            final JButton jButton = new JButton("Imprimer");
            jButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    final PrintJComponentAction a = new PrintJComponentAction(CalendarWithToolBar.this.getContentPane(), PageFormat.LANDSCAPE, CalendarWithToolBar.this.title);
                    a.actionPerformed(arg0);
                }
            });
            toolbar.add(jButton);
        }
        JButton reloadButton = new JButton(new ImageIcon(this.getClass().getResource("auto.png")));
        configureButton(reloadButton);
        toolbar.add(reloadButton);
        reloadButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        });

        this.setLayout(new GridBagLayout());

        //
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(toolbar, c);
        c.gridy++;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        contentPane.setBorder(BorderFactory.createEmptyBorder());
        contentPane.setOpaque(false);
        weekView = new WeekView(manager);
        contentPane.setColumnHeaderView(new WeekViewHeader(weekView));
        contentPane.setViewportView(weekView);
        contentPane.getViewport().setBackground(Color.WHITE);
        final MouseWheelListener[] l = contentPane.getMouseWheelListeners();
        for (int i = 0; i < l.length; i++) {
            MouseWheelListener string = l[i];
            contentPane.removeMouseWheelListener(string);
        }
        contentPane.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                weekView.mouseWheelMoved(e, l);
            }
        });

        this.add(contentPane, c);
        final int value = 300;
        contentPane.getVerticalScrollBar().setValue(value);
        weekView.loadWeek(week, year, false);
        final ChangeListener listener = new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Calendar c = getCurrentDate();
                picker.setDate(c.getTime());
                weekView.loadWeek(getWeek(), getYear(), false);
            }
        };
        spinWeek.addChangeListener(listener);
        spinYear.addChangeListener(listener);
        picker.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null) {
                    final Date d = (Date) evt.getNewValue();
                    final Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    final int y = c.get(Calendar.YEAR);
                    final int w = c.get(Calendar.WEEK_OF_YEAR);
                    spinWeek.removeChangeListener(listener);
                    spinYear.removeChangeListener(listener);
                    spinYear.setValue(y);
                    spinWeek.setValue(w);
                    spinWeek.addChangeListener(listener);
                    spinYear.addChangeListener(listener);
                    weekView.loadWeek(getWeek(), getYear(), false);
                }

            }
        });

    }

    protected void addWeek(int i) {
        Calendar c = getCurrentDate();
        c.add(Calendar.DAY_OF_YEAR, i * 7);
        final int year = c.get(Calendar.YEAR);
        final int week = c.get(Calendar.WEEK_OF_YEAR);
        spinYear.setValue(year);
        spinWeek.setValue(week);

    }

    public Calendar getCurrentDate() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, getYear());
        c.set(Calendar.WEEK_OF_YEAR, getWeek());
        return c;
    }

    private void configureButton(JButton b) {
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setMargin(new Insets(1, 5, 1, 5));
    }

    public void reload() {
        weekView.reload();
    }

    public int getWeek() {
        return ((Number) spinWeek.getValue()).intValue();
    }

    public int getYear() {
        return ((Number) spinYear.getValue()).intValue();
    }

    public WeekView getWeekView() {
        return weekView;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void scrollTo(int hour) {
        contentPane.getVerticalScrollBar().setValue(hour * weekView.getRowHeight());
    }

    public JScrollPane getContentPane() {
        return contentPane;
    }
}
