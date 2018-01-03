package org.openconcerto.modules.operation;

import javax.swing.JComponent;

import org.openconcerto.sql.element.GroupSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.ui.component.ITextArea;

public class SiteSQLComponent extends GroupSQLComponent {

    public SiteSQLComponent(SQLElement element) {
        super(element, new SiteGroup());
    }

    @Override
    public JComponent createEditor(String id) {
        if (id.contains("comment")) {
            return new ITextArea(3, 5);
        } else if (id.contains("info")) {
            return new ITextArea(3, 6);
        }
        return super.createEditor(id);
    }
}