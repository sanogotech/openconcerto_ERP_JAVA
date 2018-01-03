package org.jopencalendar.ui;

import java.util.List;

public class ItemPartViewLayouter {

    public void layout(List<ItemPartView> items) {
        final int size = items.size();
        int maxX = 0;
        for (int i = 0; i < size; i++) {
            ItemPartView p = items.get(i);
            for (int j = 0; j < i; j++) {
                ItemPartView layoutedPart = items.get(j);
                if (layoutedPart.conflictWith(p)) {
                    if (layoutedPart.getX() == p.getX()) {
                        p.setX(Math.max(p.getX(), layoutedPart.getX() + 1));
                        if (maxX < p.getX()) {
                            maxX = p.getX();
                        }
                    }
                }
            }
        }
        for (int i = 0; i < size; i++) {
            ItemPartView p = items.get(i);
            p.setMaxColumn(maxX + 1);
        }
    }
}
