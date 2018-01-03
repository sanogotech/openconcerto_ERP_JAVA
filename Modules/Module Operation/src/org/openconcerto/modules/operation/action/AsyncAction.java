package org.openconcerto.modules.operation.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingWorker;

import org.openconcerto.utils.ExceptionHandler;

public abstract class AsyncAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                return AsyncAction.this.doInBackground();
            }

            @Override
            protected void done() {
                try {
                    final Object obj = get();
                    AsyncAction.this.done(obj);
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur", e);
                }
            }
        };
        worker.execute();
    }

    public abstract Object doInBackground();

    public abstract void done(Object obj);

}
