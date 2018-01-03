package org.jopencalendar.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DatePickerPanel extends JPanel implements ActionListener, MouseListener {

    private static final long serialVersionUID = -2882634897084487051L;
    public static final String TIME_IN_MILLIS = "timeInMillis";
    private int rowCount = 7;
    private int colCount = 7;
    private int currentYear;
    private int currentMonth;
    private List<DateLabel> labels = new ArrayList<DateLabel>();
    private JLabel title;
    private JButton bRight;
    private JButton bLeft;
    private Date selectedDate;

    public DatePickerPanel() {
        //
        Calendar cal = Calendar.getInstance();
        this.selectedDate = cal.getTime();
        this.currentYear = cal.get(Calendar.YEAR);
        this.currentMonth = cal.get(Calendar.MONTH);
        //

        this.setLayout(new BorderLayout(2, 2));
        JPanel navigator = new JPanel();
        navigator.setLayout(new BorderLayout());
        bLeft = new JButton(new ImageIcon(this.getClass().getResource("left.png")));
        configureButton(bLeft);

        navigator.add(bLeft, BorderLayout.WEST);
        title = new JLabel("...", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        navigator.add(title, BorderLayout.CENTER);
        bRight = new JButton(new ImageIcon(this.getClass().getResource("right.png")));
        configureButton(bRight);
        navigator.add(bRight, BorderLayout.EAST);
        this.add(navigator, BorderLayout.NORTH);
        final JPanel dayPanel = new JPanel() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.GRAY);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        dayPanel.setBackground(Color.WHITE);
        dayPanel.setLayout(new GridLayout(rowCount, colCount, 4, 4));
        dayPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        //

        String[] dateFormatSymbols = new DateFormatSymbols().getShortWeekdays();
        int f = cal.getFirstDayOfWeek();

        for (int i = 0; i < colCount; i++) {
            int j = i + f;
            if (j >= 8) {
                j = 1;
            }
            String d = dateFormatSymbols[j];
            dayPanel.add(new JLabel(d, SwingConstants.RIGHT));
        }

        //
        int c = 0;
        final MouseAdapter mListener = new MouseAdapter() {
            public long select(MouseEvent e) {
                if (e.getSource() instanceof DateLabel) {
                    final DateLabel dateLabel = (DateLabel) e.getSource();
                    final Calendar c = Calendar.getInstance();
                    final long timeInMillis = dateLabel.getTimeInMillis();
                    c.setTimeInMillis(timeInMillis);
                    setSelectedDate(c);
                    return timeInMillis;
                }
                return -1;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                e.setSource(dayPanel.getComponentAt(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), dayPanel)));
                select(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Nothing
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                e.setSource(dayPanel.getComponentAt(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), dayPanel)));
                long timeInMillis = select(e);
                if (timeInMillis >= 0) {
                    firePropertyChange(TIME_IN_MILLIS, Long.valueOf(0), Long.valueOf(timeInMillis));
                }
            }
        };

        for (int i = 0; i < rowCount - 1; i++) {
            for (int j = 0; j < colCount; j++) {
                final DateLabel label = new DateLabel(String.valueOf(c), SwingConstants.RIGHT);
                label.addMouseListener(mListener);
                label.addMouseMotionListener(mListener);
                label.setOpaque(true);
                this.labels.add(label);
                dayPanel.add(label);
                c++;
            }

        }

        this.add(dayPanel, BorderLayout.CENTER);
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
        // Today
        final JLabel date = new JLabel(df.format(new Date()), SwingConstants.CENTER);
        date.setBorder(BorderFactory.createEmptyBorder(4, 3, 5, 3));
        this.add(date, BorderLayout.SOUTH);
        //
        updateLabels();
        bRight.addActionListener(this);
        bLeft.addActionListener(this);
        date.addMouseListener(this);

    }

    public Date getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(Calendar cal) {
        if (cal == null) {
            this.selectedDate = null;
            setDate(Calendar.getInstance());
        } else if (this.selectedDate == null || !this.selectedDate.equals(cal.getTime())) {
            this.selectedDate = cal.getTime();
            setDate(cal);
        }
    }

    public void setDate(Calendar cal) {
        this.currentYear = cal.get(Calendar.YEAR);
        this.currentMonth = cal.get(Calendar.MONTH);
        updateLabels();
    }

    private void configureButton(final JButton button) {
        int buttonSize = 28;
        button.setMinimumSize(new Dimension(buttonSize, buttonSize));
        button.setPreferredSize(new Dimension(buttonSize, buttonSize));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
    }

    private void updateLabels() {
        int selectedYear = -1;
        int selectedDay = -1;
        if (selectedDate != null) {
            Calendar calSelected = Calendar.getInstance();
            calSelected.setTime(this.selectedDate);
            selectedYear = calSelected.get(Calendar.YEAR);
            selectedDay = calSelected.get(Calendar.DAY_OF_YEAR);
        }

        Calendar cal = Calendar.getInstance();
        int todayYear = cal.get(Calendar.YEAR);
        int todayDay = cal.get(Calendar.DAY_OF_YEAR);
        cal.clear();
        cal.set(Calendar.YEAR, this.currentYear);
        cal.set(Calendar.MONTH, this.currentMonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
        title.setText(sdf.format(cal.getTime()));

        int f = cal.getFirstDayOfWeek();
        int offset = cal.get(Calendar.DAY_OF_WEEK) - f + 1; // offset du 1er jour du mois par
                                                            // rapport a

        if (offset < 2) {
            offset += 7;
        }

        // grille
        cal.add(Calendar.DAY_OF_YEAR, -offset);

        int c = 0;
        for (int i = 0; i < rowCount - 1; i++) {
            for (int j = 0; j < colCount; j++) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
                final DateLabel label = this.labels.get(c);
                label.setDate(cal);
                label.setText(cal.get(Calendar.DAY_OF_MONTH) + "  ");
                if (cal.get(Calendar.MONTH) != this.currentMonth) {
                    if (!label.getForeground().equals(Color.GRAY))
                        label.setForeground(Color.GRAY);
                } else {
                    if (!label.getForeground().equals(Color.BLACK))
                        label.setForeground(Color.BLACK);
                }
                if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                    label.setBackground(Color.LIGHT_GRAY);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(Color.WHITE);
                }
                if (cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.DAY_OF_YEAR) == selectedDay) {
                    label.setBackground(new Color(232, 242, 250));
                    label.setForeground(Color.BLACK);
                }
                c++;
            }

        }

    }

    @Override
    public void setFocusable(boolean focusable) {
        super.setFocusable(focusable);
        bRight.setFocusable(focusable);
        bLeft.setFocusable(focusable);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                final JFrame f = new JFrame();
                final DatePickerPanel picker = new DatePickerPanel();
                picker.addPropertyChangeListener(TIME_IN_MILLIS, new PropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        final Long millis = (Long) evt.getNewValue();
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(millis);
                        f.dispose();

                    }
                });
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setContentPane(picker);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);

                final JFrame f1 = new JFrame();
                final DatePicker p = new DatePicker();
                Calendar c = Calendar.getInstance();
                c.set(Calendar.YEAR, 2010);
                c.set(Calendar.MONTH, 2);
                c.set(Calendar.DAY_OF_MONTH, 1);

                p.setDate(c.getTime());
                p.setDate(null);
                final JPanel pRed = new JPanel();
                pRed.setBackground(Color.RED);
                pRed.add(p);
                pRed.add(new JTextField("Dummy TextField"));

                f1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f1.setContentPane(pRed);
                f1.pack();
                f1.setLocation(f.getLocation().x + f.getWidth() + 10, f.getLocation().y);
                f1.setVisible(true);

            }
        });

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Buttons + / -
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, this.currentYear);
        cal.set(Calendar.MONTH, this.currentMonth);
        if (e.getSource().equals(this.bLeft)) {
            cal.add(Calendar.MONTH, -1);
        } else if (e.getSource().equals(this.bRight)) {
            cal.add(Calendar.MONTH, 1);
        }
        setDate(cal);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (e.getClickCount() > 1) {
            setSelectedDate(cal);
            firePropertyChange(TIME_IN_MILLIS, Long.valueOf(0), Long.valueOf(cal.getTimeInMillis()));
        } else {
            setDate(cal);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

}
