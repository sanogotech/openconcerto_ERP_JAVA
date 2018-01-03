package org.openconcerto.modules.customerrelationship.lead;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBackgroundTableCache;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.sqlobject.SQLSearchableTextCombo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.group.Group;

public class LeadSQLComponent extends GroupSQLComponent {
    public LeadSQLComponent(SQLElement element, Group group) {
        super(element, group);
    }

    @Override
    protected Set<String> createRequiredNames() {
        final Set<String> s = new HashSet<String>(1);
        s.add("ID_ADRESSE");
        s.add("NOM");
        return s;
    }

    @Override
    public JComponent getLabel(String id) {
        if (id.equals("customerrelationship.lead.person")) {
            return new JLabelBold("Contact");
        } else if (id.equals("customerrelationship.lead.contact")) {
            return new JLabel();
        } else if (id.equals("customerrelationship.lead.address")) {
            return new JLabelBold("Adresse");
        } else {
            return super.getLabel(id);
        }
    }

    @Override
    public JComponent createEditor(String id) {

        if (id.equals("INFORMATION") || id.equals("INFOS")) {
            final ITextArea jTextArea = new ITextArea();
            jTextArea.setFont(new JLabel().getFont());
            return jTextArea;
        } else if (id.equals("INDUSTRY") || id.equals("STATUS") || id.equals("RATING") || id.equals("SOURCE") || id.equals("DISPO")) {
            return new SQLSearchableTextCombo(ComboLockedMode.UNLOCKED, 1, 20, false);
        } else if (id.equals("DATE")) {
            return new JDate(true);
        }
        return super.createEditor(id);
    }

    @Override
    protected SQLRowValues createDefaults() {
        SQLRowValues rowVals = new SQLRowValues(getTable());
        rowVals.put("STATUS", "Nouveau");

        final int idUser = UserManager.getInstance().getCurrentUser().getId();
        final SQLTable foreignTableComm = getTable().getForeignTable("ID_COMMERCIAL");
        SQLRow rowsComm = SQLBackgroundTableCache.getInstance().getCacheForTable(foreignTableComm).getFirstRowContains(idUser, foreignTableComm.getField("ID_USER_COMMON"));

        if (rowsComm != null) {
            rowVals.put("ID_COMMERCIAL", rowsComm.getID());
        }
        return rowVals;
    }
}
