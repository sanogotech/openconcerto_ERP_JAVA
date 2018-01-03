package org.openconcerto.modules.humanresources.travel.expense;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;

import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.sqlobject.itemview.VWRowItemView;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.component.text.TextComponentUtils;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.text.DocumentFilterList;
import org.openconcerto.utils.text.DocumentFilterList.FilterType;
import org.openconcerto.utils.text.LimitedSizeDocumentFilter;

public class ExpenseSQLComponent extends GroupSQLComponent {
    private DecimalFormat decimalFormat = new DecimalFormat("0.##");

    public ExpenseSQLComponent(SQLElement element, Group group) {
        super(element, group);

    }

    @Override
    protected void initDone() {
        super.initDone();
        final PropertyChangeListener listener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateAmount();
            }
        };

        final AbstractDocument comp1 = (AbstractDocument) TextComponentUtils.getDocument(getView("TRAVEL_DISTANCE").getComp());
        DocumentFilterList.add(comp1, new LimitedSizeDocumentFilter(5), FilterType.SIMPLE_FILTER);
        getView("TRAVEL_DISTANCE").addValueListener(listener);

        final AbstractDocument comp2 = (AbstractDocument) TextComponentUtils.getDocument(getView("TRAVEL_RATE").getComp());
        DocumentFilterList.add(comp2, new LimitedSizeDocumentFilter(5), FilterType.SIMPLE_FILTER);

        getView("TRAVEL_RATE").addValueListener(listener);
    }

    @Override
    protected Set<String> createRequiredNames() {
        final Set<String> s = new HashSet<String>(1);
        s.add("DATE");
        s.add("DESCRIPTION");
        s.add("ID_USER_COMMON");
        s.add("ID_EXPENSE_STATE");
        return s;
    }

    @Override
    public JComponent getEditor(String id) {
        if (id.equals("DESCRIPTION")) {
            return new ITextArea();
        } else if (id.equals("DATE")) {
            return new JDate(true);
        } else if (id.endsWith("AMOUNT")) {
            return new DeviseField();
        }
        return super.getEditor(id);
    }

    private void updateAmount() {
        float v1 = getFloat("TRAVEL_DISTANCE");
        float v2 = getFloat("TRAVEL_RATE");
        float total = v1 * v2;
        if (total > 1000000 || total < 0) {
            total = 0;
        }
        final String valueOf = decimalFormat.format(total);
        ((JTextComponent) getView("TRAVEL_AMOUNT").getComp()).setText(valueOf);
    }

    private float getFloat(String id) {
        final Number n = (Number) ((VWRowItemView<?>) getView(id)).getWrapper().getValue();
        if (n == null) {
            return 0;
        }
        return n.floatValue();
    }

}
