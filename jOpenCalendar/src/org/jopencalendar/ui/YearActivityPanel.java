package org.jopencalendar.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.text.DateFormat;
import java.util.Calendar;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jopencalendar.model.JCalendarItemGroup;

public class YearActivityPanel extends JPanel {
    private final FixedColumnTable fixedColumnTable;
    private JPanel toolbar = new JPanel();
    private int year, week2, week1;
    private YearActivityTableModel tableModel;
    private String title;
    private JButton bPrint;

    public YearActivityPanel(final JCalendarItemProvider manager, final int year) {
        this.setLayout(new GridBagLayout());
        this.year = year;
        this.week1 = 1;
        this.week2 = 53;
        tableModel = new YearActivityTableModel(manager, year);
        final int squareSize = 12;
        this.fixedColumnTable = new FixedColumnTable(1, tableModel);
        fixedColumnTable.setShowHorizontalLines(false);
        fixedColumnTable.setRowHeight(50);

        Calendar mycal = Calendar.getInstance();
        mycal.set(Calendar.YEAR, year);

        // Get the number of days in that month
        for (int i = 1; i < 13; i++) {
            mycal.set(Calendar.MONTH, i - 1);
            final int daysInMonth = mycal.getActualMaximum(Calendar.DAY_OF_MONTH);
            final int w = squareSize * daysInMonth;
            fixedColumnTable.setColumnWidth(i, w);
            fixedColumnTable.setColumnMinWidth(i, w);
            fixedColumnTable.setColumnMaxWidth(i, w);
            fixedColumnTable.getColumn(i).setCellRenderer(new MonthActivityStatesCellRenderer(squareSize));
        }
        fixedColumnTable.setReorderingAllowed(false);
        // Scroll to first non null value on double click
        fixedColumnTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fixedColumnTable.getSelectedRow();
                    for (int i = 1; i < 13; i++) {
                        final Object value = tableModel.getValueAt(row, i);
                        final MonthActivityStates m = (MonthActivityStates) value;
                        if (m.getList() != null) {
                            fixedColumnTable.ensureVisible(row, i - 1);
                            break;
                        }
                    }
                }

            }
        });

        // Tooltip to show the date
        fixedColumnTable.addMouseMotionListener(new MouseAdapter() {
            final Calendar c = Calendar.getInstance();
            int lastDay = -1;
            int lastYear = -1;

            @Override
            public void mouseMoved(MouseEvent evt) {
                int d = 1 + evt.getX() / squareSize;
                int y = getYear();
                if (lastDay != d || lastYear != y) {
                    c.clear();
                    c.set(Calendar.YEAR, y);
                    c.set(Calendar.DAY_OF_YEAR, d);
                    final String text = DateFormat.getDateInstance(DateFormat.FULL).format(c.getTime());
                    fixedColumnTable.setToolTipTextOnHeader(text);
                    lastDay = d;
                    lastYear = y;
                }
            }

        });

        this.toolbar = createToolbar();

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.toolbar, c);
        c.gridy++;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        this.add(fixedColumnTable, c);
    }

    public JCalendarItemGroup getJCalendarItemGroupAt(int index) {
        return this.tableModel.getJCalendarItemGroupAt(index);
    }

    public FixedColumnTable getTable() {
        return fixedColumnTable;
    }

    public int getYear() {
        return this.year;
    }

    public int getWeek1() {
        return week1;
    }

    public int getWeek2() {
        return week2;
    }

    public void setWeek1(int week1) {
        this.week1 = week1;
        tableModel.loadContent(this.year, this.week1, this.week2);
    }

    public void setWeek2(int week2) {
        this.week2 = week2;
        tableModel.loadContent(this.year, this.week1, this.week2);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void reload() {
        tableModel.loadContent(this.year, this.week1, this.week2);
    }

    public void setPrintButtonVisible(boolean b) {
        bPrint.setVisible(b);
    }

    public JPanel createToolbar() {
        final JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel("Année"));
        final JSpinner spinYear = new JSpinner(new SpinnerNumberModel(year, 1000, 5000, 1));
        spinYear.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int y = ((Number) spinYear.getValue()).intValue();
                setYear(y);

            }
        });
        p.add(spinYear);

        p.add(new JLabel("Semaine"));
        final JSpinner spinWeek1 = new JSpinner(new SpinnerNumberModel(1, 1, 53, 1));
        spinWeek1.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int y = ((Number) spinWeek1.getValue()).intValue();
                setWeek1(y);

            }
        });
        p.add(spinWeek1);

        p.add(new JLabel("à"));
        final JSpinner spinWeek2 = new JSpinner(new SpinnerNumberModel(53, 1, 53, 1));
        spinWeek2.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int y = ((Number) spinWeek2.getValue()).intValue();
                setWeek2(y);

            }
        });
        p.add(spinWeek2);

        bPrint = new JButton("Imprimer");
        bPrint.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();
                printAttributes.add(OrientationRequested.LANDSCAPE);

                final PrintJComponentAction a = new PrintJComponentAction(YearActivityPanel.this.fixedColumnTable, PageFormat.LANDSCAPE, YearActivityPanel.this.title);
                a.actionPerformed(arg0);
            }
        });
        p.add(bPrint);
        p.setOpaque(false);
        return p;
    }

    protected void setYear(int y) {
        this.year = y;
        tableModel.loadContent(this.year, this.week1, this.week2);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                JFrame f = new JFrame();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                final YearActivityPanel contentPane = new YearActivityPanel(new JCalendarItemProvider("Test 2015"), 2015);
                contentPane.getTable().setColumnMinWidth(0, 200);
                contentPane.getTable().setColumnWidth(0, 500);
                contentPane.setTitle("Planning Gant");
                f.setContentPane(contentPane);
                f.setSize(1024, 768);
                f.setVisible(true);
            }
        });

    }
}
