package org.openconcerto.modules.operation;

import java.util.List;

import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItem;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLElement;

public class JCalendarItemDB extends JCalendarItem {
    private final String tableSource;
    private final int id;
    private String status;
    private String type;
    private int idCalendarGroup;
    private String plannerXML;
    private String plannerUID;
    private String siteName;
    private String siteComment;
    private Number siteId;
    private int idSource;

    public JCalendarItemDB(int id, String tableSource, int idSource, int idCalendarGroup) {
        this.tableSource = tableSource;
        this.id = id;
        this.idCalendarGroup = idCalendarGroup;
        this.idSource = idSource;
    }

    public SQLElement getSourceElement() {
        final SQLElement e = PropsConfiguration.getInstance().getDirectory().getElement(tableSource);
        return e;
    }

    public String getTableSource() {
        return this.tableSource;
    }

    public int getSourceId() {
        return idSource;
    }

    public int getIdCalendarGroup() {
        return idCalendarGroup;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JCalendarItemDB) {
            final JCalendarItemDB o = (JCalendarItemDB) obj;
            return o.id == id;
        }
        return super.equals(obj);
    }

    public void setOperationStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setOperationType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getFlagsString() {
        final List<Flag> flags = getFlags();
        final int size = flags.size();
        String str = "";
        for (int i = 0; i < size; i++) {
            str += flags.get(i).getTypeId();
            if (i < size - 1) {
                str += ",";
            }
        }
        return str;
    }

    public String getPlannerXML() {
        return plannerXML;
    }

    public void setPlannerXML(String string) {
        this.plannerXML = string;
    }

    public String getPlannerUID() {
        return plannerUID;
    }

    public void setPlannerUID(String plannerUID) {
        this.plannerUID = plannerUID;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteComment() {
        return siteComment;
    }

    public void setSiteComment(String siteComment) {
        this.siteComment = siteComment;
    }

    public void setSiteId(Number idNumber) {
        this.siteId = idNumber;
    }

    public Number getSiteId() {
        return siteId;
    }

    public int getId() {
        return this.id;
    }
}
