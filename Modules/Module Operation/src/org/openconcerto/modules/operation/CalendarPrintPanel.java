package org.openconcerto.modules.operation;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.print.CalendarItemPrinter;
import org.openconcerto.sql.users.User;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.PrintPreviewFrame;

public class CalendarPrintPanel extends JPanel {
    final JCheckBox preview = new JCheckBox("Aperçu");
    final JButton bPrint = new JButton("Imprimer");

    public CalendarPrintPanel(final OperationCalendarManager manager, final int week, final int year, final List<User> selectedUsers, final List<String> selectedStates) {
        preview.setSelected(true);
        //
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        final JLabel l = new JLabel("Date de début", SwingConstants.RIGHT);
        this.add(l, c);
        c.gridx++;
        final JDate d1 = new JDate(false, true);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, week);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        d1.setDate(cal.getTime());
        c.weightx = 1;
        this.add(d1, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        final JLabel l2 = new JLabel("Date de fin", SwingConstants.RIGHT);

        this.add(l2, c);
        c.gridx++;
        final JDate d2 = new JDate(false, true);
        cal.add(Calendar.DAY_OF_YEAR, 7);
        d2.setDate(cal.getTime());
        c.weightx = 1;
        this.add(d2, c);
        final JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.RIGHT));
        p.add(preview);
        p.add(bPrint);
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        this.add(p, c);
        //

        bPrint.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (d1.getDate().after(d2.getDate())) {
                    return;
                }

                final List<Pageable> p = new ArrayList<Pageable>();

                for (User user : selectedUsers) {
                    final List<User> l = new ArrayList<User>();
                    l.add(user);
                    final List<JCalendarItem> itemInWeek = manager.getItemIn(d1.getDate(), d2.getDate(), l, selectedStates);
                    if (!itemInWeek.isEmpty()) {
                        Collections.sort(itemInWeek, new Comparator<JCalendarItem>() {
                            @Override
                            public int compare(JCalendarItem o1, JCalendarItem o2) {
                                return o1.getDtStart().compareTo(o2.getDtStart());
                            }
                        });
                        final PageFormat pf = new PageFormat();
                        pf.setPaper(new A4());
                        final CalendarItemPrinter printable = new OperationCalendarItemPrinter(user.getFullName(), itemInWeek, pf);

                        p.add(printable);
                    }
                }
                if (p.isEmpty()) {
                    JOptionPane.showMessageDialog(CalendarPrintPanel.this, "Aucune page à imprimer.\nMerci de vérifier la période.");
                    return;
                }
                if (preview.isSelected()) {

                    final PrintPreviewFrame f = new PrintPreviewFrame(new ListOfPageable(p)) {
                        @Override
                        public void printAllPages() {
                            printPages(p);
                            closeFrame();
                        }

                    };
                    f.pack();
                    f.setLocationRelativeTo(CalendarPrintPanel.this);
                    f.setVisible(true);
                } else {
                    printPages(p);
                    closeFrame();
                }
            }
        });
    }

    protected void closeFrame() {
        SwingUtilities.getWindowAncestor(this).dispose();
    }

    public void printPages(final List<Pageable> pageables) {
        PrinterJob job = PrinterJob.getPrinterJob();
        boolean ok = job.printDialog();
        if (ok) {
            for (Pageable document : pageables) {
                PageFormat p = job.getPageFormat(null);
                System.err.println("CalendarPrintPanel.printPages():Size: " + p.getWidth() + " x " + p.getHeight());
                System.err.println("CalendarPrintPanel.printPages():Imagageable: " + p.getImageableWidth() + " x " + p.getImageableHeight());
                job.getPageFormat(null).getImageableHeight();
                job.setPageable(document);
                try {
                    job.print();
                } catch (PrinterException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
