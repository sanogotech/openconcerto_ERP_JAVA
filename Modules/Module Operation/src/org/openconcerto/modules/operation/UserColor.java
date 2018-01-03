package org.openconcerto.modules.operation;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;

public class UserColor {
    private static UserColor instance;
    private String[] colors = new String[] { "#303F9F", "#31DE39", "#FFC107", "#C2185B", "#00796B", "#212121", "#00BCD4", "#A6A6A6", "#795548", "#B3E5FC", "#FF0000" };
    private Map<Integer, Color> map = new HashMap<Integer, Color>();

    public UserColor() {
        List<User> users = UserManager.getInstance().getAllUser();
        final int size = users.size();
        for (int i = 0; i < size; i++) {
            User u = users.get(i);
            map.put(u.getId(), Color.decode(colors[i % colors.length]));
        }
    }

    public synchronized Color getColor(int id) {
        return map.get(id);
    }

    public synchronized static final UserColor getInstance() {
        if (instance == null) {
            instance = new UserColor();
        }
        return instance;
    }
}
