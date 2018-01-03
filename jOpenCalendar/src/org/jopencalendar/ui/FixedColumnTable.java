package org.jopencalendar.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class FixedColumnTable extends JComponent implements ChangeListener, PropertyChangeListener {
    private JTable main;
    private JTable fixed;
    private JScrollPane scrollPane;

    /*
     * Specify the number of columns to be fixed and the scroll pane containing the table.
     */
    public FixedColumnTable(int fixedColumns, TableModel model) {
        main = new JTable(model);
        this.scrollPane = new JScrollPane(main);

        main.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        main.setAutoCreateColumnsFromModel(false);
        main.addPropertyChangeListener(this);

        // Use the existing table to create a new table sharing
        // the DataModel and ListSelectionModel
        fixed = new JTable();
        fixed.setAutoCreateColumnsFromModel(false);
        fixed.setModel(main.getModel());
        fixed.setSelectionModel(main.getSelectionModel());
        fixed.setFocusable(false);

        // Remove the fixed columns from the main table
        // and add them to the fixed table
        for (int i = 0; i < fixedColumns; i++) {
            TableColumnModel columnModel = main.getColumnModel();
            TableColumn column = columnModel.getColumn(0);
            columnModel.removeColumn(column);
            fixed.getColumnModel().addColumn(column);
        }

        // Add the fixed table to the scroll pane
        fixed.setPreferredScrollableViewportSize(fixed.getPreferredSize());
        scrollPane.setRowHeaderView(fixed);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, fixed.getTableHeader());

        // Synchronize scrolling of the row header with the main table
        scrollPane.getRowHeader().addChangeListener(this);

        fixed.getTableHeader().addMouseListener(new MouseAdapter() {
            TableColumn column;
            int columnWidth;
            int pressedX;

            public void mousePressed(MouseEvent e) {
                JTableHeader header = (JTableHeader) e.getComponent();
                TableColumnModel tcm = header.getColumnModel();
                int columnIndex = tcm.getColumnIndexAtX(e.getX());
                Cursor cursor = header.getCursor();
                if (columnIndex == tcm.getColumnCount() - 1 && cursor == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)) {
                    column = tcm.getColumn(columnIndex);
                    columnWidth = column.getWidth();
                    pressedX = e.getX();
                    header.addMouseMotionListener(this);
                }
            }

            public void mouseReleased(MouseEvent e) {
                JTableHeader header = (JTableHeader) e.getComponent();
                header.removeMouseMotionListener(this);
            }

            public void mouseDragged(MouseEvent e) {
                int width = columnWidth - pressedX + e.getX();
                column.setPreferredWidth(width);
                JTableHeader header = (JTableHeader) e.getComponent();
                JTable table = header.getTable();
                table.setPreferredScrollableViewportSize(table.getPreferredSize());
                JScrollPane scrollPane = (JScrollPane) table.getParent().getParent();
                scrollPane.revalidate();
            }
        });
        this.setLayout(new GridLayout(1, 1));
        this.add(scrollPane);

        main.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                // Reload column names
                final TableColumnModel columnModel = main.getColumnModel();
                final int size = columnModel.getColumnCount();
                for (int i = 0; i < size; i++) {
                    columnModel.getColumn(i).setHeaderValue(main.getModel().getColumnName(i + 1));
                }
                main.getTableHeader().repaint();
            }
        });

    }

    public void stateChanged(ChangeEvent e) {
        // Sync the scroll pane scrollbar with the row header
        JViewport viewport = (JViewport) e.getSource();
        scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
    }

    public void propertyChange(PropertyChangeEvent e) {
        // Keep the fixed table in sync with the main table
        if ("selectionModel".equals(e.getPropertyName())) {
            fixed.setSelectionModel(main.getSelectionModel());
        }
        if ("model".equals(e.getPropertyName())) {
            fixed.setModel(main.getModel());
        }
    }

    public void setRowHeight(int height) {
        this.fixed.setRowHeight(height);
        this.main.setRowHeight(height);
    }

    public void setShowHorizontalLines(boolean showGrid) {
        this.fixed.setShowHorizontalLines(showGrid);
        this.main.setShowHorizontalLines(showGrid);
    }

    public void setShowGrid(boolean showGrid) {
        this.fixed.setShowGrid(showGrid);
        this.main.setShowGrid(showGrid);
    }

    public void setColumnWidth(int index, int width) {
        getColumn(index).setWidth(width);
        if (index == 0) {
            final Dimension preferredSize = fixed.getPreferredSize();
            preferredSize.setSize(width, preferredSize.getHeight());
            fixed.setPreferredScrollableViewportSize(preferredSize);
            JScrollPane scrollPane = (JScrollPane) fixed.getParent().getParent();
            scrollPane.revalidate();
        }
    }

    public void setColumnMinWidth(int index, int width) {
        getColumn(index).setMinWidth(width);
    }

    public void setColumnMaxWidth(int index, int width) {
        getColumn(index).setMaxWidth(width);
    }

    public TableColumn getColumn(int index) {
        final int columnCount = fixed.getColumnCount();
        if (index < columnCount) {
            return this.fixed.getColumnModel().getColumn(index);
        } else {
            return this.main.getColumnModel().getColumn(index - columnCount);
        }

    }

    @Override
    public synchronized void addMouseListener(MouseListener l) {
        this.fixed.addMouseListener(l);
        this.main.addMouseListener(l);
    }

    @Override
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        this.fixed.addMouseMotionListener(l);
        this.main.addMouseMotionListener(l);
    }

    public int getSelectedRow() {
        return this.main.getSelectedRow();
    }

    public void ensureVisible(int row, int column) {
        main.scrollRectToVisible(main.getCellRect(row, column, true));
    }

    public int rowAtPoint(Point point) {
        return this.main.rowAtPoint(point);
    }

    public int columnAtPoint(Point point) {
        return this.main.columnAtPoint(point);
    }

    public void setToolTipTextOnHeader(String text) {
        this.main.setToolTipText(text);
    }

    public void setReorderingAllowed(boolean b) {
        main.getTableHeader().setReorderingAllowed(b);
    }

}