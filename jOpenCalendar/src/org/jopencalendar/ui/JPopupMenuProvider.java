package org.jopencalendar.ui;

import java.util.List;

import javax.swing.JPopupMenu;

import org.jopencalendar.model.JCalendarItemPart;

public interface JPopupMenuProvider {

    public JPopupMenu getPopup(List<JCalendarItemPart> selectedItems, List<JCalendarItemPart> currentColumnParts);

}
