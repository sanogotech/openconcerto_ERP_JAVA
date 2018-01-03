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
 
 package org.openconcerto.ui;

import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.ui.component.InteractionMode.InteractionComponent;
import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.FormatGroup;
import org.openconcerto.utils.TimeUtils;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.i18n.TM.MissingMode;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.KeyStroke;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.JTextComponent;

import org.jopencalendar.ui.DatePicker;

/**
 * Un composant d'édition de date acceptant les formats "dd/MM/yy" et "d MMMM yyyy".
 * 
 * @author Sylvain CUAZ
 */
public final class JDate extends JComponent implements ValueWrapper<Date>, TextComponent, InteractionComponent {

    private static boolean CommitEachValidEditDefault = false;
    public static String DATE_PICKER_FORMATS_KEY = "org.openconcerto.DatePicker.formats";

    static {
        final UIDefaults lafDefaults = UIManager.getLookAndFeelDefaults();
        // don't overwrite if e.g. set in the main()
        if (lafDefaults.get(DATE_PICKER_FORMATS_KEY) == null) {
            final String formats = TM.getInstance().translate(MissingMode.NULL, "jdate.formats");
            // else keep defaults
            if (formats != null && formats.trim().length() > 0) {
                final String[] array = formats.split("\n");
                lafDefaults.put(DATE_PICKER_FORMATS_KEY, array);
            }
        }
    }

    static DateFormat[] getDefaultDateFormats() {
        final UIDefaults lafDefaults = UIManager.getLookAndFeelDefaults();
        final String[] formats = (String[]) lafDefaults.get(DATE_PICKER_FORMATS_KEY);
        if (formats == null || formats.length == 0) {
            return new DateFormat[] { DateFormat.getDateInstance() };
        } else {
            final DateFormat[] l = new DateFormat[formats.length];
            for (int i = 0; i < l.length; i++) {
                l[i] = new SimpleDateFormat(formats[i]);
            }
            return l;
        }
    }

    static Format getDefaultDateFormat() {
        final DateFormat[] formats = getDefaultDateFormats();
        if (formats.length == 1) {
            return formats[0];
        } else {
            return new FormatGroup(formats);
        }
    }

    public static void setCommitEachValidEditDefault(final boolean commitEachValidEditDefault) {
        CommitEachValidEditDefault = commitEachValidEditDefault;
    }

    public static boolean getCommitEachValidEditDefault() {
        return CommitEachValidEditDefault;
    }

    private final boolean fillWithCurrentDate;
    private final boolean commitEachValidEdit;
    private final Calendar cal;

    private final DatePicker picker;

    /**
     * Créé un composant d'édition de date, vide.
     */
    public JDate() {
        this(false);
    }

    /**
     * Créé un composant d'édition de date.
     * 
     * @param fillWithCurrentDate <code>true</code> si on veut préremplir avec la date
     *        d'aujourd'hui, sinon vide.
     * @see #getCommitEachValidEditDefault()
     */
    public JDate(final boolean fillWithCurrentDate) {
        this(fillWithCurrentDate, getCommitEachValidEditDefault());
    }

    /**
     * Create a date editing component.
     * 
     * @param fillWithCurrentDate <code>true</code> if the initial value should be the current date,
     *        <code>false</code> if the initial value should be <code>null</code>.
     * @param commitEachValidEdit <code>true</code> if each valid edit should change our
     *        {@link #getValue() value}, <code>false</code> to wait for user action (typically when
     *        leaving or hitting enter).
     * @see DefaultFormatter#setCommitsOnValidEdit(boolean)
     */
    public JDate(final boolean fillWithCurrentDate, final boolean commitEachValidEdit) {
        super();

        this.setLayout(new GridLayout(1, 1));
        // no need to fill DatePicker since we call resetValue() at the end of this constructor
        this.picker = new DatePicker(getDefaultDateFormat(), true, false);
        this.add(this.picker);
        this.setMinimumSize(new Dimension(this.picker.getPreferredSize()));

        this.fillWithCurrentDate = fillWithCurrentDate;
        if (commitEachValidEdit) {
            // Committing on valid edit will trigger that 32/01/15 is replaced by 1/02/15
            // and make annoying behaviour...
            Log.get().warning("commitEachValidEdit ignored due to Java bug");
        }
        this.commitEachValidEdit = false;

        final InputMap inputMap = this.getEditor().getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "dayToFuture");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "dayToPast");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "monthToFuture");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "monthToPast");
        final ActionMap actionMap = this.getEditor().getActionMap();
        this.cal = Calendar.getInstance();
        actionMap.put("dayToPast", createChangeDateAction(Calendar.DAY_OF_YEAR, -1));
        actionMap.put("dayToFuture", createChangeDateAction(Calendar.DAY_OF_YEAR, 1));
        actionMap.put("monthToPast", createChangeDateAction(Calendar.MONTH, -1));
        actionMap.put("monthToFuture", createChangeDateAction(Calendar.MONTH, 1));

        this.updateUI();
        this.resetValue();
    }

    private JFormattedTextField getEditor() {
        return this.picker.getEditor();
    }

    public final boolean fillsWithCurrentDate() {
        return this.fillWithCurrentDate;
    }

    protected final Calendar getCal() {
        return this.cal;
    }

    protected AbstractAction createChangeDateAction(final int field, final int amount) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final JTextComponent comp = (JTextComponent) e.getSource();
                if (!comp.isEnabled() || !comp.isEditable())
                    return;

                Date currentVal = getDate();
                if (currentVal == null && fillsWithCurrentDate())
                    currentVal = new Date();
                if (currentVal != null) {
                    getCal().setTime(currentVal);
                    getCal().add(field, amount);
                    setValue(getCal().getTime());
                }
            }
        };
    }

    public void setFormats(final DateFormat[] formats) {
        this.setFormatP(new FormatGroup(formats));
    }

    public void setFormat(final DateFormat format) {
        this.setFormatP(format);
    }

    // private since we don't want to be passed e.g. DecimalFormat
    private void setFormatP(final Format format) {
        final InternationalFormatter formatter = new InternationalFormatter(format) {
            @Override
            public Object stringToValue(final String text) throws ParseException {
                // JXDatePickerFormatter used to handle null date ; InternationalFormatter only use
                // the formats which obviously fail to parse "" and so revert the empty value.
                Object result;
                if (text == null || text.isEmpty()) {
                    result = null;
                } else {
                    result = super.stringToValue(text);

                }
                return result;
            }
        };
        formatter.setCommitsOnValidEdit(this.commitEachValidEdit);
        this.getEditor().setFormatterFactory(new DefaultFormatterFactory(formatter));
    }

    public DateFormat[] getFormats() {
        final AbstractFormatterFactory factory = this.getEditor().getFormatterFactory();
        if (factory != null) {
            final AbstractFormatter formatter = factory.getFormatter(this.getEditor());
            if (formatter instanceof InternationalFormatter) {
                final Format format = ((InternationalFormatter) formatter).getFormat();
                if (format instanceof DateFormat) {
                    return new DateFormat[] { (DateFormat) format };
                } else if (format instanceof FormatGroup) {
                    final List<Format> formats = new ArrayList<Format>(((FormatGroup) format).getFormats());
                    final Iterator<Format> iter = formats.iterator();
                    while (iter.hasNext()) {
                        final Format element = iter.next();
                        if (!(element instanceof DateFormat))
                            iter.remove();
                    }
                    return formats.toArray(new DateFormat[formats.size()]);
                }
            }
        }
        return null;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // replace JXDatePickerFormatter by InternationalFormatter
        this.setFormats(getDefaultDateFormats());
    }

    /**
     * Reset the component as if it has just been created. If {@link #fillsWithCurrentDate()} then
     * the date at the time this method is called will be used (not the date of creation).
     */
    @Override
    public final void resetValue() {
        if (this.fillsWithCurrentDate()) {
            this.setValue(new Date());
        } else {
            this.setValue(null);
        }
    }

    /**
     * Set the value after clearing the time part.
     * 
     * @param date the new value.
     */
    @Override
    public final void setValue(final Date date) {
        final Date timeless;
        if (date == null) {
            timeless = null;
        } else {
            getCal().setTime(date);
            TimeUtils.clearTime(getCal());
            timeless = getCal().getTime();
        }
        this.setDate(timeless);
    }

    @Override
    public final Date getValue() {
        return this.getDate();
    }

    public final boolean isEmpty() {
        return this.getValue() == null;
    }

    @Override
    public final void addValueListener(final PropertyChangeListener l) {
        this.getEditor().addPropertyChangeListener("value", l);
    }

    @Override
    public void rmValueListener(final PropertyChangeListener l) {
        this.getEditor().removePropertyChangeListener("value", l);
    }

    // useful since by default this commits on focus lost, as is a table cell editor. So sometimes
    // the table cell editor is called back before the commit and thus takes the original value.
    public void commitEdit() throws ParseException {
        this.picker.commitEdit();
    }

    @Override
    public JComponent getComp() {
        return this;
    }

    @Override
    public ValidState getValidState() {
        return ValidState.getTrueInstance();
    }

    @Override
    public void addValidListener(final ValidListener l) {
        // nothing to do
    }

    @Override
    public void removeValidListener(final ValidListener l) {
        // nothing to do
    }

    @Override
    public JTextComponent getTextComp() {
        return getEditor();
    }

    /**
     * Set the currently selected date.
     * 
     * @param date date
     */
    public void setDate(Date date) {
        this.picker.setDate(date);
    }

    /**
     * Set the currently selected date.
     * 
     * @param millis milliseconds
     */
    public void setDateInMillis(long millis) {
        this.picker.setDate(new Date(millis));
    }

    /**
     * Returns the currently selected date.
     * 
     * @return Date
     */
    public Date getDate() {
        return this.picker.getDate();
    }

    /**
     * Returns the currently selected date in milliseconds.
     * 
     * @return the date in milliseconds
     */
    public long getDateInMillis() {
        return this.picker.getDate().getTime();
    }

    @Override
    public void setInteractionMode(InteractionMode mode) {
        this.picker.setEditable(mode.isEditable());
        this.setEnabled(mode.isEnabled());
    }

    @Override
    public InteractionMode getInteractionMode() {
        return InteractionMode.from(this.picker.isEnabled(), this.picker.isEditable());
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.picker.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    public void addActionListener(ActionListener actionListener) {
        this.picker.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        this.picker.removeActionListener(actionListener);
    }

    public void setButtonVisible(boolean b) {
        this.picker.setButtonVisible(b);
    }
}
