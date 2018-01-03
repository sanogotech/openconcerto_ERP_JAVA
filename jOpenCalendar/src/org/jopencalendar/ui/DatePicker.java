package org.jopencalendar.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class DatePicker extends JPanel {

    private JFormattedTextField text;
    private JDialog dialog;
    private long dialogLostFocusTime;
    private DatePickerPanel pickerPanel;
    private Date date;
    private List<ActionListener> actionListeners = new ArrayList<ActionListener>();
    private static final long RELEASE_TIME_MS = 500;
    private JButton button;

    public DatePicker() {
        this(true);
    }

    public DatePicker(boolean useEditor) {
        this(useEditor, false);
    }

    public DatePicker(boolean useEditor, boolean fillWithCurrentDate) {
        this(DateFormat.getDateInstance(), useEditor, fillWithCurrentDate);
    }

    public DatePicker(Format dateFormat, boolean useEditor, boolean fillWithCurrentDate) {
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.setOpaque(false);
        if (useEditor) {
            text = new JFormattedTextField(dateFormat);
            text.setColumns(12);
            text.addPropertyChangeListener("value", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    setDate((Date) evt.getNewValue());
                }
            });
            this.add(text);
            JPanel spacer = new JPanel();
            spacer.setOpaque(false);
            spacer.setMinimumSize(new Dimension(3, 5));
            spacer.setPreferredSize(new Dimension(3, 5));
            this.add(spacer);
        }

        final ImageIcon icon = new ImageIcon(this.getClass().getResource("calendar.png"));
        button = new JButton(new AbstractAction(null, icon) {

            @Override
            public void actionPerformed(ActionEvent e) {
                final JComponent source = (JComponent) e.getSource();

                if (text != null) {
                    // if the button isn't focusable, no FOCUS_LOST will occur and the text won't
                    // get
                    // committed (or reverted). So act as if the user requested a commit, so that
                    // invalidEdit() can be called (usually causing a beep).
                    if (!source.isFocusable()) {
                        for (final Action a : text.getActions()) {
                            final String name = (String) a.getValue(Action.NAME);
                            if (JTextField.notifyAction.equals(name))
                                a.actionPerformed(new ActionEvent(text, e.getID(), null));
                        }
                    }

                    // if after trying to commit, the value is invalid then don't show the popup
                    if (!text.isEditValid())
                        return;
                }
                if (dialog == null) {
                    final JDialog d = new JDialog(SwingUtilities.getWindowAncestor(DatePicker.this));
                    pickerPanel = new DatePickerPanel();

                    d.setContentPane(pickerPanel);
                    d.setUndecorated(true);
                    pickerPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                    d.setSize(220, 180);

                    pickerPanel.addPropertyChangeListener(DatePickerPanel.TIME_IN_MILLIS, new PropertyChangeListener() {

                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (!DatePicker.this.isEditable())
                                return;
                            final Long millis = (Long) evt.getNewValue();
                            final Calendar c = Calendar.getInstance();
                            c.setTimeInMillis(millis);
                            setDate(c.getTime());
                            if (dialog != null) {
                                dialog.dispose();
                                dialog = null;
                            }
                            fireActionPerformed();
                        }

                    });
                    d.addWindowFocusListener(new WindowFocusListener() {

                        public void windowGainedFocus(WindowEvent e) {
                            // do nothing
                        }

                        public void windowLostFocus(WindowEvent e) {
                            if (dialog != null) {
                                final Window oppositeWindow = e.getOppositeWindow();
                                if (oppositeWindow != null && SwingUtilities.isDescendingFrom(oppositeWindow, dialog)) {
                                    return;
                                }
                                dialog.dispose();
                                dialog = null;
                            }
                            dialogLostFocusTime = System.currentTimeMillis();
                        }

                    });
                    dialog = d;
                }
                // Set picker panel date
                final Calendar calendar = Calendar.getInstance();
                if (date != null) {
                    calendar.setTime(date);
                    pickerPanel.setSelectedDate(calendar);
                } else {
                    pickerPanel.setSelectedDate(null);
                }
                // Show dialog
                final int x = source.getLocation().x;
                final int y = source.getLocation().y + source.getHeight();
                Point p = new Point(x, y);
                SwingUtilities.convertPointToScreen(p, DatePicker.this);
                p = adjustPopupLocationToFitScreen(p.x, p.y, dialog.getSize(), source.getSize());
                dialog.setLocation(p.x, p.y);
                final long time = System.currentTimeMillis() - dialogLostFocusTime;
                if (time > RELEASE_TIME_MS) {
                    dialog.setVisible(true);
                } else {
                    dialogLostFocusTime = System.currentTimeMillis() - RELEASE_TIME_MS;
                }
            }

        });
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusable(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        this.add(button);

        // init & synchronize text field
        setDate(fillWithCurrentDate ? new Date() : null, true);
    }

    public void setDate(Date date) {
        this.setDate(date, false);
    }

    public void setButtonVisible(boolean b) {
        this.button.setVisible(b);
    }

    private void setDate(Date date, boolean force) {
        Date oldDate = this.date;
        if (force || !equals(oldDate, date)) {
            this.date = date;
            if (text != null) {
                text.setValue(date);
            }
            firePropertyChange("value", oldDate, date);
        }
    }

    public Date getDate() {
        return date;
    }

    public JFormattedTextField getEditor() {
        return text;
    }

    public void setEditable(boolean b) {
        if (this.text != null) {
            this.text.setEditable(b);
        }
    }

    public boolean isEditable() {
        if (this.text != null) {
            return this.text.isEditable();
        }
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (final Component c : this.getComponents())
            c.setEnabled(enabled);
    }

    public void addActionListener(ActionListener listener) {
        this.actionListeners.add(listener);

    }

    public void removeActionListener(ActionListener listener) {
        this.actionListeners.remove(listener);

    }

    public void fireActionPerformed() {
        for (ActionListener l : actionListeners) {
            l.actionPerformed(new ActionEvent(this, 0, "dateChanged"));
        }
    }

    /**
     * Tries to find GraphicsConfiguration that contains the mouse cursor position. Can return null.
     */
    private GraphicsConfiguration getCurrentGraphicsConfiguration(Point popupLocation) {
        GraphicsConfiguration gc = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        for (int i = 0; i < gd.length; i++) {
            if (gd[i].getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                GraphicsConfiguration dgc = gd[i].getDefaultConfiguration();
                if (dgc.getBounds().contains(popupLocation)) {
                    gc = dgc;
                    break;
                }
            }
        }

        return gc;
    }

    /**
     * Returns an point which has been adjusted to take into account of the desktop bounds, taskbar
     * and multi-monitor configuration.
     * <p>
     * This adustment may be cancelled by invoking the application with
     * -Djavax.swing.adjustPopupLocationToFit=false
     * 
     * @param popupSize
     */
    Point adjustPopupLocationToFitScreen(int xPosition, int yPosition, Dimension popupSize, Dimension buttonSize) {
        Point popupLocation = new Point(xPosition, yPosition);

        if (GraphicsEnvironment.isHeadless()) {
            return popupLocation;
        }

        // Get screen bounds
        Rectangle scrBounds;
        GraphicsConfiguration gc = getCurrentGraphicsConfiguration(popupLocation);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (gc != null) {
            // If we have GraphicsConfiguration use it to get screen bounds
            scrBounds = gc.getBounds();
        } else {
            // If we don't have GraphicsConfiguration use primary screen
            scrBounds = new Rectangle(toolkit.getScreenSize());
        }

        // Calculate the screen size that popup should fit

        long popupRightX = (long) popupLocation.x + (long) popupSize.width;
        long popupBottomY = (long) popupLocation.y + (long) popupSize.height;
        int scrWidth = scrBounds.width;
        int scrHeight = scrBounds.height;

        // Insets include the task bar. Take them into account.
        Insets scrInsets = toolkit.getScreenInsets(gc);
        scrBounds.x += scrInsets.left;
        scrBounds.y += scrInsets.top;
        scrWidth -= scrInsets.left + scrInsets.right;
        scrHeight -= scrInsets.top + scrInsets.bottom;

        int scrRightX = scrBounds.x + scrWidth;
        int scrBottomY = scrBounds.y + scrHeight;

        // Ensure that popup menu fits the screen
        if (popupRightX > (long) scrRightX) {
            popupLocation.x = scrRightX - popupSize.width;
            popupLocation.y = popupLocation.y - buttonSize.height - popupSize.height - 2;
        }

        if (popupBottomY > (long) scrBottomY) {
            popupLocation.y = scrBottomY - popupSize.height;
            if (popupRightX + buttonSize.width < (long) scrRightX) {
                popupLocation.x += buttonSize.width;
            }
        }

        if (popupLocation.x < scrBounds.x) {
            popupLocation.x = scrBounds.x;
        }

        if (popupLocation.y < scrBounds.y) {
            popupLocation.y = scrBounds.y;
        }

        return popupLocation;
    }

    public void commitEdit() throws ParseException {
        if (text != null) {
            this.text.commitEdit();
        }
    }

    static public final boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 == null || o2 == null)
            return false;
        return o1.equals(o2);
    }

}
