package org.jopencalendar.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class WeekViewHeader extends Component {

    private WeekView view;

    public WeekViewHeader(WeekView view) {
        this.view = view;
    }

    @Override
    public void paint(Graphics g) {
        view.paintHeader(g);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(view.getPreferredSize().width + 100, view.getHeaderHeight());
    }
}
