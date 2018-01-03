package org.openconcerto.modules.common.batchprocessing;

import java.awt.Component;
import java.math.BigDecimal;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.openconcerto.modules.common.batchprocessing.product.HTProcessor;
import org.openconcerto.modules.common.batchprocessing.product.PurchaseProcessor;
import org.openconcerto.modules.common.batchprocessing.product.TTCProcessor;
import org.openconcerto.modules.common.batchprocessing.product.TVAProcessor;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.ui.VFlowLayout;

public class BatchDetailPanel extends JPanel {
    private BatchProcessor processor;

    BatchDetailPanel() {
        this.setLayout(new VFlowLayout());
    }

    public void setField(SQLField field) {
        this.removeAll();

        final SQLType type = field.getType();
        final Class<?> javaType = type.getJavaType();
        final String fName = field.getName();
        if (fName.equals("PV_TTC")) {
            final NumberProcessor p = new TTCProcessor(field);
            this.add(p);
            this.processor = p;
        } else if (fName.equals("PV_HT")) {
            final NumberProcessor p = new HTProcessor(field);
            this.add(p);
            this.processor = p;
        } else if (fName.equals("ID_TAXE")) {
            final ReferenceProcessor p = new TVAProcessor(field);
            this.add(p);
            this.processor = p;
        } else if (fName.equals("PA_HT")) {
            final NumberProcessor p = new PurchaseProcessor(field);
            this.add(p);
            this.processor = p;
        } else if (javaType.equals(Boolean.class)) {
            final BooleanProcessor p = new BooleanProcessor(field);
            this.add(p);
            this.processor = p;
        } else if (field.isKey()) {
            final ReferenceProcessor p = new ReferenceProcessor(field);
            this.add(p);
            this.processor = p;
        } else if (javaType.equals(String.class)) {
            final StringProcessor p = new StringProcessor(field);
            this.add(p);
            this.processor = p;
        } else if (javaType.equals(Date.class)) {
            final DateProcessor p = new DateProcessor(field);
            this.add(p);
            this.processor = p;
        } else if (javaType.equals(BigDecimal.class) || javaType.equals(Float.class) || javaType.equals(Double.class) || javaType.equals(Integer.class) || javaType.equals(Long.class)) {
            final NumberProcessor p = new NumberProcessor(field);
            this.add(p);
            this.processor = p;
        }

        revalidate();
        repaint();
    }

    public BatchProcessor getProcessor() {
        return processor;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (processor != null && processor instanceof JComponent) {
            Component[] l = ((JComponent) processor).getComponents();
            for (int i = 0; i < l.length; i++) {
                l[i].setEnabled(false);
            }
        }
        super.setEnabled(enabled);
    }
}
