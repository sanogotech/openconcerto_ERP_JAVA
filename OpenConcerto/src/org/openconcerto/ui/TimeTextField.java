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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position.Bias;

public class TimeTextField extends JTextField {

    public TimeTextField() {
        super(6);
        init(0, 0);
    }

    public TimeTextField(boolean fillWithCurrentTime) {
        super(6);
        if (fillWithCurrentTime) {
            Calendar c = Calendar.getInstance();
            init(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        } else {
            init(0, 0);
        }
    }

    public TimeTextField(int hour, int minute) {
        super(6);
        init(hour, minute);
    }

    private void init(int hour, int minute) {
        setTime(hour, minute);
        Document d = getDocument();

        this.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        final int caretPosition = getCaretPosition();

                        if (caretPosition < 2) {
                            int i = getHours() + 1;
                            if (i > 23) {
                                i = 0;
                            }
                            final String valueOf = fill2Char(i);
                            ((AbstractDocument) getDocument()).replace(0, 2, valueOf, null);
                            setCaretPosition(caretPosition);
                        } else if (caretPosition > 2) {
                            int i = getMinutes() + 1;
                            if (i > 59) {
                                i = 0;
                            }
                            final String valueOf = fill2Char(i);
                            ((AbstractDocument) getDocument()).replace(3, 2, valueOf, null);
                            setCaretPosition(caretPosition);
                        }

                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        final int caretPosition = getCaretPosition();
                        if (caretPosition < 2) {
                            int i = getHours() - 1;
                            if (i < 0) {
                                i = 23;
                            }
                            final String valueOf = fill2Char(i);
                            ((AbstractDocument) getDocument()).replace(0, 2, valueOf, null);
                            setCaretPosition(caretPosition);
                        } else if (caretPosition > 2) {
                            int i = getMinutes() - 1;
                            if (i < 0) {
                                i = 59;
                            }
                            final String valueOf = fill2Char(i);
                            ((AbstractDocument) getDocument()).replace(3, 2, valueOf, null);
                            setCaretPosition(caretPosition);
                        }
                    }
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }

            }
        });

        final AbstractDocument doc = (AbstractDocument) d;
        doc.setDocumentFilter(new DocumentFilter() {

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (offset == 2) {
                    offset--;
                }
                super.replace(fb, offset + 1, 0, "0", null);
                super.remove(fb, offset, 1);
                setCaretPosition(offset);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (offset > 4) {
                    return;
                }
                final String actualText = doc.getText(0, 5);
                if (length > 0 && text.length() != length) {
                    length = 0;
                }
                final char[] te = actualText.toCharArray();
                final int stop = text.length();
                for (int i = 0; i < stop; i++) {
                    int position = i + offset;
                    te[position] = text.charAt(i);
                }
                for (int i = 0; i < stop; i++) {
                    int position = i + offset;
                    final char c = text.charAt(i);
                    if (!isCharValid(c, position, te)) {
                        return;
                    }
                }

                if (length == 0) {
                    super.remove(fb, offset, text.length());
                }
                super.replace(fb, offset, length, text, attrs);

            }

        });
        // Move cusor to not be on ':'
        this.setNavigationFilter(new NavigationFilter() {
            @Override
            public int getNextVisualPositionFrom(JTextComponent text, int pos, Bias bias, int direction, Bias[] biasRet) throws BadLocationException {
                if (pos == 1 && direction == SwingConstants.EAST) {
                    pos++;
                }
                if (pos == 3 && direction == SwingConstants.WEST) {
                    pos--;
                }
                return super.getNextVisualPositionFrom(text, pos, bias, direction, biasRet);
            }

            @Override
            public void moveDot(FilterBypass fb, int dot, Bias bias) {
                if (dot == 2) {
                    dot = 3;
                }
                super.moveDot(fb, dot, bias);
            }

            @Override
            public void setDot(FilterBypass fb, int dot, Bias bias) {
                if (dot == 2) {
                    dot = 3;
                }
                super.setDot(fb, dot, bias);
            }
        });
        doc.addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (getText().length() == 5) {
                    // Fire only on end of value update
                    firePropertyChange("value", null, getText());
                }
            }
        });
    }

    public void setTime(int hour, int minute) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be betwen 0 and 23 but is " + hour);
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Minute must be betwen 0 and 59 but is " + minute);
        }
        String s = "";
        if (hour < 10) {
            s += "0";
        }
        s += hour + ":";
        if (minute < 10) {
            s += "0";
        }
        s += minute;
        setText(s);
    }

    public int getHours() {
        final String s = getText().substring(0, 2);
        return Integer.parseInt(s);
    }

    public int getMinutes() {
        final String s = getText().substring(3, 5);
        return Integer.parseInt(s);
    }

    protected String fill2Char(int i) {
        if (i < 10) {
            return "0" + i;
        }
        return String.valueOf(i);
    }

    @Override
    public void setCaretPosition(int position) {
        if (position == 2) {
            position++;
        }
        super.setCaretPosition(position);
    }

    public boolean isCharValid(char c, int position, char[] actualText) {
        switch (position) {
        case 0:
            if (c >= '0' && c <= '1')
                return true;
            char c1 = actualText[1];
            if (c == '2' && c1 <= '3')
                return true;
            break;
        case 1:
            if (c >= '0' && c <= '3')
                return true;
            char c0 = actualText[0];
            if (c >= '4' && c <= '9' && (c0 == '0' || c0 == '1'))
                return true;
            break;
        case 2:
            if (c == ':')
                return true;
            break;
        case 3:
            if (c >= '0' && c <= '5')
                return true;
            break;
        case 4:
            if (c >= '0' && c <= '9')
                return true;
            break;
        }
        return false;

    }

    public static void main(String[] args) {
        final JFrame f = new JFrame();
        final TimeTextField time = new TimeTextField(true);
        time.setTime(10, 59);
        f.setContentPane(time);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}
