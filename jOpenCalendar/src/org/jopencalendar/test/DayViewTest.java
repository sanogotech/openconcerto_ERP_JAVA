package org.jopencalendar.test;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jopencalendar.ui.JCalendarItemProvider;
import org.jopencalendar.ui.DayView;
import org.jopencalendar.ui.MultipleDayViewHeader;

public class DayViewTest {

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

                List<JCalendarItemProvider> managers = new ArrayList<JCalendarItemProvider>();
                for (int i = 0; i < 5; i++) {
                    final JCalendarItemProvider manager = new JCalendarItemProvider("DayViewTest" + i);
                    managers.add(manager);
                }

                final DayView view = new DayView(managers);
                contentPane.setColumnHeaderView(new MultipleDayViewHeader(view));
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

                view.loadDay(1, 2015);

                final int value = 300;
                contentPane.getVerticalScrollBar().setValue(value);
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
