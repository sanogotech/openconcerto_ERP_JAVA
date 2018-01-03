package org.jopencalendar.test;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jopencalendar.model.JCalendarItemPart;
import org.jopencalendar.ui.JCalendarItemProvider;
import org.jopencalendar.ui.CalendarWithToolBar;
import org.jopencalendar.ui.JPopupMenuProvider;
import org.jopencalendar.ui.PrintJComponentAction;
import org.jopencalendar.ui.WeekView;

public class CalendarWithToolbarTest {

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
                final JFrame f = new JFrame();
                f.setSize(1024, 768);
                final CalendarWithToolBar contentPane = new CalendarWithToolBar(new JCalendarItemProvider("Test"));
                final WeekView view = contentPane.getWeekView();
                view.setHourRange(6, 20);
                view.setPopupMenuProvider(new JPopupMenuProvider() {

                    @Override
                    public JPopupMenu getPopup(List<JCalendarItemPart> selectedItems, List<JCalendarItemPart> currentColumnParts) {
                        System.err.println("CalendarWithToolbarTest.selected: " + selectedItems);
                        System.err.println("CalendarWithToolbarTest.inColumn: " + currentColumnParts);
                        JPopupMenu popup = new JPopupMenu();
                        for (JCalendarItemPart item : selectedItems) {
                            popup.add(new JMenuItem(item.getItem().getSummary()));
                        }
                        popup.add(new PrintJComponentAction(contentPane.getContentPane()));
                        return popup;
                    }
                });
                f.setContentPane(contentPane);
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setVisible(true);
            }
        };
        SwingUtilities.invokeLater(r);
    }
}
