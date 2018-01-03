package org.openconcerto.modules.extensionbuilder.translation.field;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

public abstract class SimpleDocumentListener implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);

    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);

    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        try {
            changedUpdate(e, e.getDocument().getText(0, e.getDocument().getLength()));
        } catch (BadLocationException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public abstract void changedUpdate(DocumentEvent e, String text);
}
