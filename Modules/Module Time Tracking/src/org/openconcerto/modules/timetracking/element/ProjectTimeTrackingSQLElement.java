package org.openconcerto.modules.timetracking.element;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.ListMap;

public class ProjectTimeTrackingSQLElement extends ComptaSQLConfElement {
    public static final String ELEMENT_CODE = "affaires.temps";

    public ProjectTimeTrackingSQLElement() {
        super("AFFAIRE_TEMPS", "un temps", "temps");
    }

    @Override
    protected String createCode() {
        return ELEMENT_CODE;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_USER_COMMON");
        l.add("ID_AFFAIRE");
        l.add("DESCRIPTIF");
        l.add("DATE");
        l.add("TEMPS");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_USER_COMMON");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        final String groupId = this.getCode() + ".default";
        final Group group = GlobalMapper.getInstance().getGroup(groupId);
        if (group == null) {
            throw new IllegalStateException("No group found for id " + groupId);
        }
        return new ProjectTimeTrackingSQLComponent(this, group);
    }

    @Override
    protected void _initTableSource(SQLTableModelSource res) {
        super._initTableSource(res);

        final BaseSQLTableModelColumn semaine = new BaseSQLTableModelColumn("Semaine", Integer.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {
                return r.getDate("DATE").get(Calendar.WEEK_OF_YEAR);
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(getTable());
                return Collections.singleton(new FieldPath(p, "DATE"));
            }
        };
        res.getColumns().add(semaine);

    }

}
