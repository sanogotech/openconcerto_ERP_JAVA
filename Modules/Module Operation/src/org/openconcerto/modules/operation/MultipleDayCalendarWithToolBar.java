package org.openconcerto.modules.operation;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jopencalendar.ui.DatePicker;
import org.jopencalendar.ui.DayView;
import org.jopencalendar.ui.ItemPartView;
import org.jopencalendar.ui.MultipleDayView;
import org.jopencalendar.ui.MultipleDayViewHeader;

public class MultipleDayCalendarWithToolBar extends JPanel {
    private OperationDayView weekView;

    final JScrollPane contentPane = new JScrollPane();
    private Date date;

    public MultipleDayCalendarWithToolBar(OperationDayView mDayView) {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));

        Calendar cal = Calendar.getInstance();

        this.date = cal.getTime();
        final DatePicker jDate = new DatePicker(false);
        jDate.setDate(date);
        JButton bLeft = createButton(new ImageIcon(DayView.class.getResource("left.png")));
        JButton bRight = createButton(new ImageIcon(DayView.class.getResource("right.png")));
        toolbar.add(bLeft);
        toolbar.add(bRight);
        toolbar.add(jDate);
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
        JButton reloadButton = createButton(new ImageIcon(ItemPartView.class.getResource("auto.png")));
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
        weekView = mDayView;
        contentPane.setColumnHeaderView(new MultipleDayViewHeader(weekView));
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
        weekView.loadDay(date);

        jDate.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getNewValue() != null) {
                    final Date d = (Date) evt.getNewValue();
                    date = d;
                    weekView.loadDay(d);
                }
            }
        });
        bLeft.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                c.add(Calendar.DAY_OF_YEAR, -1);
                final Date time = c.getTime();
                jDate.setDate(time);
                date = time;
            }
        });
        bRight.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                c.add(Calendar.DAY_OF_YEAR, 1);
                final Date time = c.getTime();
                jDate.setDate(time);
                date = time;
            }
        });

    }

    public void reload() {
        weekView.reload();
    }

    public MultipleDayView getWeekView() {
        return weekView;
    }

    public void scrollTo(int hour) {
        contentPane.getVerticalScrollBar().setValue(hour * weekView.getRowHeight());
    }

    public JScrollPane getContentPane() {
        return contentPane;
    }

    JButton createButton(ImageIcon ico) {
        JButton b = new JButton(ico);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setMargin(new Insets(4, 10, 4, 10));
        return b;
    }
}
