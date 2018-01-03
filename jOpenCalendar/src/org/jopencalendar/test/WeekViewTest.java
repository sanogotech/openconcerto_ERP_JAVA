package org.jopencalendar.test;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.ui.JCalendarItemProvider;
import org.jopencalendar.ui.WeekView;
import org.jopencalendar.ui.WeekViewHeader;

public class WeekViewTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Runnable r = new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                JFrame f = new JFrame();
                f.setSize(1024, 768);
                final JScrollPane contentPane = new JScrollPane();

                final JCalendarItemProvider manager = new JCalendarItemProvider("WeekViewTest") {
                    @Override
                    public List<JCalendarItem> getItemInWeek(int week, int year) {
                        List<JCalendarItem> l = new ArrayList<JCalendarItem>();
                        Calendar c = Calendar.getInstance();
                        c.clear();
                        //
                        c.set(2013, 11, 1, 0, 0, 0);
                        JCalendarItem i1 = new JCalendarItem();
                        i1.setDtStart(c);
                        c.set(2014, 0, 3, 0, 0, 0);
                        i1.setDtEnd(c);
                        i1.setDayOnly(true);
                        i1.setSummary("1/11/13-3/1/14");
                        i1.setDescription("Ceci est un test");
                        l.add(i1);
                        //
                        c.set(2014, 0, 2, 0, 0, 0);
                        JCalendarItem i2 = new JCalendarItem();
                        i2.setDtStart(c);
                        c.set(2014, 0, 4, 0, 0, 0);
                        i2.setDtEnd(c);
                        i2.setDayOnly(true);
                        i2.setSummary("2/1/14-4/1/14");
                        l.add(i2);
                        //
                        c.set(2014, 0, 5, 0, 0, 0);
                        JCalendarItem i3 = new JCalendarItem();
                        i3.setDtStart(c);
                        c.set(2014, 0, 5, 0, 0, 0);
                        i3.setDtEnd(c);
                        i3.setDayOnly(true);
                        i3.setSummary("5/1/14-5/1/14");
                        i3.setDescription("Hello world");
                        l.add(i3);

                        //
                        c.set(2014, 0, 5, 12, 0, 0);
                        JCalendarItem i4 = new JCalendarItem();
                        i4.setDtStart(c);
                        c.set(2014, 0, 5, 13, 0, 0);
                        i4.setDtEnd(c);

                        i4.setSummary("5/12/14 12h-13h");
                        i4.setDescription("Hello world");
                        l.add(i4);

                        //
                        c.set(2014, 0, 1, 11, 0, 0);
                        JCalendarItem i5 = new JCalendarItem();
                        i5.setDtStart(c);
                        c.set(2014, 0, 3, 13, 0, 0);
                        i5.setDtEnd(c);

                        i5.setSummary("1/12/14 11h - 3/12/14 13h");
                        i5.setDescription("Hello world");
                        l.add(i5);
                        //
                        c.set(2014, 0, 5, 10, 0, 0);
                        JCalendarItem i6 = new JCalendarItem();
                        i6.setDtStart(c);
                        c.set(2014, 0, 5, 16, 0, 0);
                        i6.setDtEnd(c);

                        i6.setSummary("5/12/14 10h-16h");
                        i6.setDescription("Hello world");
                        l.add(i6);
                        return l;
                    }
                };
                final WeekView view = new WeekView(manager);
                contentPane.setColumnHeaderView(new WeekViewHeader(view));
                contentPane.setViewportView(view);
                final MouseWheelListener[] l = contentPane.getMouseWheelListeners();
                for (int i = 0; i < l.length; i++) {
                    MouseWheelListener string = l[i];
                    contentPane.removeMouseWheelListener(string);
                }
                contentPane.addMouseWheelListener(new MouseWheelListener() {

                    @Override
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        view.mouseWheelMoved(e, l);
                    }
                });

                f.setContentPane(contentPane);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                f.setVisible(true);

                view.loadWeek(1, 2014, false);

                final int value = 300;
                contentPane.getVerticalScrollBar().setValue(value);
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
