package org.openconcerto.modules.common.batchprocessing;

import java.math.BigDecimal;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

public class DecimalOrPercentVerifier extends InputVerifier {

    @Override
    public boolean verify(JComponent input) {
        String text = ((JTextField) input).getText().trim();
        if (text.isEmpty()) {
            return false;
        }

        if (text.endsWith("%")) {
            text = text.substring(0, text.length() - 1).trim();

        }

        try {
            BigDecimal value = new BigDecimal(text);

            return value != null;
        } catch (NumberFormatException e) {
            return false;
        }

    }

}
