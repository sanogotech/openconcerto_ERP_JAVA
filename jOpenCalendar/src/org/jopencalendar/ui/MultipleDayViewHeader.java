package org.jopencalendar.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class MultipleDayViewHeader extends Component {

    private MultipleDayView view;

    public MultipleDayViewHeader(MultipleDayView view) {
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
