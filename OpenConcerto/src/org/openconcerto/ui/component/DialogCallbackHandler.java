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
 
 package org.openconcerto.ui.component;

/* Java imports */
import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/* JAAS imports */
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * <p>
 * Uses a Swing dialog window to query the user for answers to authentication questions. This can be
 * used by a JAAS application to instantiate a CallbackHandler
 * 
 * @see javax.security.auth.callback
 */
public class DialogCallbackHandler implements CallbackHandler {

    /* -- Fields -- */

    /* The parent window, or null if using the default parent */
    private final Component parentComponent;
    private static final int JPasswordFieldLen = 8;
    private static final int JTextFieldLen = 8;

    /* -- Methods -- */

    /**
     * Creates a callback dialog with the default parent window.
     */
    public DialogCallbackHandler() {
        this(null);
    }

    /**
     * Creates a callback dialog and specify the parent window.
     *
     * @param parentComponent the parent window -- specify <code>null</code> for the default parent
     */
    public DialogCallbackHandler(final Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    /*
     * An interface for recording actions to carry out if the user clicks OK for the dialog.
     */
    private static interface Action {
        void perform();
    }

    /**
     * Handles the specified set of callbacks.
     *
     * @param callbacks the callbacks to handle
     * @throws UnsupportedCallbackException if the callback is not an instance of NameCallback or
     *         PasswordCallback
     */
    @Override
    public void handle(final Callback[] callbacks) throws UnsupportedCallbackException {
        /* Collect messages to display in the dialog */
        final List<Object> messages = new ArrayList<Object>(3);

        /* Collection actions to perform if the user clicks OK */
        final List<Action> okActions = new ArrayList<Action>(2);

        final ConfirmationInfo confirmation = new ConfirmationInfo();
        final List<JTextComponent> fields = new ArrayList<JTextComponent>();

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof TextOutputCallback) {
                final TextOutputCallback tc = (TextOutputCallback) callbacks[i];

                switch (tc.getMessageType()) {
                case TextOutputCallback.INFORMATION:
                    confirmation.messageType = JOptionPane.INFORMATION_MESSAGE;
                    break;
                case TextOutputCallback.WARNING:
                    confirmation.messageType = JOptionPane.WARNING_MESSAGE;
                    break;
                case TextOutputCallback.ERROR:
                    confirmation.messageType = JOptionPane.ERROR_MESSAGE;
                    break;
                default:
                    throw new UnsupportedCallbackException(callbacks[i], "Unrecognized message type");
                }

                messages.add(tc.getMessage());

            } else if (callbacks[i] instanceof NameCallback) {
                final NameCallback nc = (NameCallback) callbacks[i];

                final JLabel prompt = new JLabel(nc.getPrompt());

                final JTextField name = new JTextField(JTextFieldLen);
                final String defaultName = nc.getDefaultName();
                if (defaultName != null) {
                    name.setText(defaultName);
                }

                /*
                 * Put the prompt and name in a horizontal box, and add that to the set of messages.
                 */
                final Box namePanel = Box.createHorizontalBox();
                namePanel.add(prompt);
                namePanel.add(name);
                messages.add(namePanel);

                /* Store the name back into the callback if OK */
                okActions.add(new Action() {
                    @Override
                    public void perform() {
                        nc.setName(name.getText());
                    }
                });
                fields.add(name);
            } else if (callbacks[i] instanceof PasswordCallback) {
                final PasswordCallback pc = (PasswordCallback) callbacks[i];

                final JLabel prompt = new JLabel(pc.getPrompt());

                final JPasswordField password = new JPasswordField(JPasswordFieldLen);
                if (!pc.isEchoOn()) {
                    password.setEchoChar('*');
                }

                final Box passwordPanel = Box.createHorizontalBox();
                passwordPanel.add(prompt);
                passwordPanel.add(password);
                messages.add(passwordPanel);

                okActions.add(new Action() {
                    @Override
                    public void perform() {
                        pc.setPassword(password.getPassword());
                    }
                });
                fields.add(password);
            } else if (callbacks[i] instanceof ConfirmationCallback) {
                final ConfirmationCallback cc = (ConfirmationCallback) callbacks[i];

                confirmation.setCallback(cc);
                if (cc.getPrompt() != null) {
                    messages.add(cc.getPrompt());
                }

            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }

        // TODO also create dialog in the EDT
        final RunnableFuture<Integer> f = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (!fields.isEmpty()) {
                    fields.get(0).addHierarchyListener(new HierarchyListener() {
                        @Override
                        public void hierarchyChanged(final HierarchyEvent e) {
                            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && e.getComponent().isVisible()) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        e.getComponent().requestFocus();
                                    }
                                });
                            }
                        }
                    });
                }

                /* Display the dialog */
                return JOptionPane.showOptionDialog(DialogCallbackHandler.this.parentComponent, messages.toArray(), "Confirmation", /* title */
                        confirmation.optionType, confirmation.messageType, null, /* icon */
                        confirmation.options, /* options */
                        confirmation.initialValue); /* initialValue */

            }
        });
        SwingUtilities.invokeLater(f);
        final int result;
        try {
            result = f.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        /* Perform the OK actions */
        if (result == JOptionPane.OK_OPTION || result == JOptionPane.YES_OPTION) {
            final Iterator<Action> iterator = okActions.iterator();
            while (iterator.hasNext()) {
                iterator.next().perform();
            }
        }
        confirmation.handleResult(result);
    }

    /*
     * Provides assistance with translating between JAAS and Swing confirmation dialogs.
     */
    private static class ConfirmationInfo {

        private int[] translations;

        int optionType = JOptionPane.OK_CANCEL_OPTION;
        Object[] options = null;
        Object initialValue = null;

        int messageType = JOptionPane.QUESTION_MESSAGE;

        private ConfirmationCallback callback;

        /* Set the confirmation callback handler */
        void setCallback(final ConfirmationCallback callback) throws UnsupportedCallbackException {
            this.callback = callback;

            final int confirmationOptionType = callback.getOptionType();
            switch (confirmationOptionType) {
            case ConfirmationCallback.YES_NO_OPTION:
                this.optionType = JOptionPane.YES_NO_OPTION;
                this.translations = new int[] { JOptionPane.YES_OPTION, ConfirmationCallback.YES, JOptionPane.NO_OPTION, ConfirmationCallback.NO, JOptionPane.CLOSED_OPTION, ConfirmationCallback.NO };
                break;
            case ConfirmationCallback.YES_NO_CANCEL_OPTION:
                this.optionType = JOptionPane.YES_NO_CANCEL_OPTION;
                this.translations = new int[] { JOptionPane.YES_OPTION, ConfirmationCallback.YES, JOptionPane.NO_OPTION, ConfirmationCallback.NO, JOptionPane.CANCEL_OPTION,
                        ConfirmationCallback.CANCEL, JOptionPane.CLOSED_OPTION, ConfirmationCallback.CANCEL };
                break;
            case ConfirmationCallback.OK_CANCEL_OPTION:
                this.optionType = JOptionPane.OK_CANCEL_OPTION;
                this.translations = new int[] { JOptionPane.OK_OPTION, ConfirmationCallback.OK, JOptionPane.CANCEL_OPTION, ConfirmationCallback.CANCEL, JOptionPane.CLOSED_OPTION,
                        ConfirmationCallback.CANCEL };
                break;
            case ConfirmationCallback.UNSPECIFIED_OPTION:
                this.options = callback.getOptions();
                /*
                 * There's no way to know if the default option means to cancel the login, but there
                 * isn't a better way to guess this.
                 */
                this.translations = new int[] { JOptionPane.CLOSED_OPTION, callback.getDefaultOption() };
                break;
            default:
                throw new UnsupportedCallbackException(callback, "Unrecognized option type: " + confirmationOptionType);
            }

            final int confirmationMessageType = callback.getMessageType();
            switch (confirmationMessageType) {
            case ConfirmationCallback.WARNING:
                this.messageType = JOptionPane.WARNING_MESSAGE;
                break;
            case ConfirmationCallback.ERROR:
                this.messageType = JOptionPane.ERROR_MESSAGE;
                break;
            case ConfirmationCallback.INFORMATION:
                this.messageType = JOptionPane.INFORMATION_MESSAGE;
                break;
            default:
                throw new UnsupportedCallbackException(callback, "Unrecognized message type: " + confirmationMessageType);
            }
        }

        /* Process the result returned by the Swing dialog */
        void handleResult(int result) {
            if (this.callback == null) {
                return;
            }

            for (int i = 0; i < this.translations.length; i += 2) {
                if (this.translations[i] == result) {
                    result = this.translations[i + 1];
                    break;
                }
            }
            this.callback.setSelectedIndex(result);
        }
    }
}
