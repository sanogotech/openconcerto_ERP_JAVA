package org.jopencalendar.model;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

public class Flag {
    private String typeId;
    private Icon icon;
    private String name;
    private String description;
    private static Map<String, Flag> map = new HashMap<String, Flag>();

    public Flag(String typeId, Icon icon, String name, String description) {
        super();
        this.typeId = typeId;
        this.icon = icon;
        this.name = name;
        this.description = description;
    }

    public String getTypeId() {
        return typeId;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public synchronized static Flag getFlag(String id) {
        return map.get(id);
    }

    public synchronized static void register(Flag f) {
        map.put(f.typeId, f);
    }
}
