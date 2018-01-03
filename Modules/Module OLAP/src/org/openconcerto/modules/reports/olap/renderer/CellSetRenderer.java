package org.openconcerto.modules.reports.olap.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.olap4j.Cell;
import org.olap4j.CellSet;
import org.olap4j.CellSetAxis;
import org.olap4j.Position;
import org.olap4j.impl.CoordinateIterator;
import org.olap4j.impl.Olap4jUtil;

import org.olap4j.metadata.Member;

public class CellSetRenderer extends JComponent implements Scrollable {
    private static final Color BORDER_COLOR = Color.LIGHT_GRAY;
    private CellSet cellSet;
    private CellSetAxis columnsAxis;
    private AxisInfo columnsAxisInfo;
    private CellSetAxis rowsAxis;
    private AxisInfo rowsAxisInfo;
    private int w;
    private int h;

    public CellSetRenderer() {

    }

    public CellSetRenderer(CellSet set) {
        this.cellSet = set;
        computeCells();
    }

    public void setCellSet(CellSet set) {

        this.cellSet = set;
        computeCells();
        repaint();
        this.setSize(new Dimension(w, h));
    }

    private void computeCells() {
        if (cellSet == null) {
            return;
        }

        // Compute how many rows are required to display the columns axis.
        // In the example, this is 4 (1997, Q1, space, Unit Sales)

        if (cellSet.getAxes().size() > 0) {
            columnsAxis = cellSet.getAxes().get(0);
        } else {
            columnsAxis = null;
        }
        columnsAxisInfo = computeAxisInfo(columnsAxis);

        // Compute how many columns are required to display the rows axis.
        // In the example, this is 3 (the width of USA, CA, Los Angeles)

        if (cellSet.getAxes().size() > 1) {
            rowsAxis = cellSet.getAxes().get(1);
        } else {
            rowsAxis = null;
        }
        rowsAxisInfo = computeAxisInfo(rowsAxis);

    }

    boolean isOuside(int x, int y) {
        return x < rowsAxisInfo.getWidth() && y < columnsAxisInfo.getWidth();
    }

    boolean isValue(int x, int y) {
        return x >= rowsAxisInfo.getWidth() && y >= columnsAxisInfo.getWidth();
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Rectangle clipBounds = g.getClipBounds();

        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        g.setColor(Color.WHITE);
        g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);
        if (cellSet == null) {
            return;
        }

        if (cellSet.getAxes().size() > 2) {
            int[] dimensions = new int[cellSet.getAxes().size() - 2];
            for (int i = 2; i < cellSet.getAxes().size(); i++) {
                CellSetAxis cellSetAxis = cellSet.getAxes().get(i);
                dimensions[i - 2] = cellSetAxis.getPositions().size();
            }
            for (int[] pageCoords : CoordinateIterator.iterate(dimensions)) {
                formatPage(g, pageCoords, columnsAxis, columnsAxisInfo, rowsAxis, rowsAxisInfo);
            }
        } else {
            formatPage(g, new int[] {}, columnsAxis, columnsAxisInfo, rowsAxis, rowsAxisInfo);
        }
    }

    private void formatPage(Graphics g, int[] pageCoords, CellSetAxis columnsAxis, AxisInfo columnsAxisInfo, CellSetAxis rowsAxis, AxisInfo rowsAxisInfo) {
        if (pageCoords.length > 0) {

            for (int i = pageCoords.length - 1; i >= 0; --i) {
                int pageCoord = pageCoords[i];
                final CellSetAxis axis = cellSet.getAxes().get(2 + i);
                System.out.print(axis.getAxisOrdinal() + ": ");
                final Position position = axis.getPositions().get(pageCoord);
                int k = -1;
                for (Member member : position.getMembers()) {
                    if (++k > 0) {
                        System.out.print(", ");
                    }
                    System.out.print(member.getUniqueName());
                }
                System.out.println();
            }
        }
        // Figure out the dimensions of the blank rectangle in the top left
        // corner.
        final int yOffset = columnsAxisInfo.getWidth();
        final int xOffsset = rowsAxisInfo.getWidth();

        // Populate a string matrix
        Matrix matrix = new Matrix(xOffsset + (columnsAxis == null ? 1 : columnsAxis.getPositions().size()), yOffset + (rowsAxis == null ? 1 : rowsAxis.getPositions().size()));

        // Populate corner
        for (int x = 0; x < xOffsset; x++) {
            for (int y = 0; y < yOffset; y++) {
                matrix.set(x, y, "", false, x > 0);
            }
        }

        // Populate matrix with cells representing axes
        // noinspection SuspiciousNameCombination
        populateAxis(matrix, columnsAxis, columnsAxisInfo, true, xOffsset);
        populateAxis(matrix, rowsAxis, rowsAxisInfo, false, yOffset);

        // Populate cell values
        for (Cell cell : cellIter(pageCoords, cellSet)) {
            final List<Integer> coordList = cell.getCoordinateList();
            int x = xOffsset;
            if (coordList.size() > 0) {
                x += coordList.get(0);
            }
            int y = yOffset;
            if (coordList.size() > 1) {
                y += coordList.get(1);
            }
            matrix.set(x, y, cell.getFormattedValue(), true, false);
        }

        final int matrixWidth = matrix.getWidth();
        final int matrixHeight = matrix.getHeight();
        int[] columnWidths = new int[matrixWidth];
        int widestWidth = 0;
        int offsetX = 0;
        int spaceW = (int) g.getFontMetrics().getStringBounds("A", g).getWidth();
        int spaceH = (int) g.getFontMetrics().getStringBounds("A", g).getHeight();
        final int lineHeight = 2 * spaceH;
        Font boldFont = g.getFont().deriveFont(Font.BOLD);
        Font normalFont = g.getFont();

        final Rectangle clipBounds = g.getClipBounds();

        for (int x = 0; x < matrixWidth; x++) {
            int columnWidth = 0;
            for (int y = 0; y < matrixHeight; y++) {
                MatrixCell cell = matrix.get(x, y);

                if (cell != null) {
                    if (isValue(x, y)) {
                        g.setFont(normalFont);
                    } else {
                        g.setFont(boldFont);
                    }

                    int w = (int) g.getFontMetrics().getStringBounds(cell.value, g).getWidth() + 2 * spaceW;
                    columnWidth = Math.max(columnWidth, w);
                }
            }
            columnWidths[x] = columnWidth;

            for (int y = 0; y < matrixHeight; y++) {
                if ((y + 1) * lineHeight < clipBounds.y) {
                    continue;
                }
                if (y * lineHeight > clipBounds.y + clipBounds.height) {
                    break;
                }

                MatrixCell cell = matrix.get(x, y);
                if (cell != null) {
                    if (isValue(x, y)) {
                        g.setColor(Color.WHITE);
                        g.fillRect(offsetX, y * lineHeight, columnWidth, lineHeight);
                    } else if (isOuside(x, y)) {
                        g.setColor(Color.WHITE);
                        g.fillRect(offsetX, y * lineHeight, columnWidth, lineHeight);
                    } else {
                        g.setColor(new Color(240, 240, 240));
                        g.fillRect(offsetX, y * lineHeight, columnWidth, lineHeight);
                    }

                    String value = cell.value;
                    // value += "(" + x + "," + y + ")" + isValue(x, y) + ":" + isOuside(x, y);
                    if (value.length() > 0) {
                        g.setColor(Color.BLACK);

                        int xString = offsetX + spaceW;
                        if (isValue(x, y)) {
                            g.setFont(normalFont);
                            int w = (int) g.getFontMetrics().getStringBounds(value, g).getWidth();
                            xString = offsetX + columnWidth - w - spaceW;

                        } else {
                            g.setFont(boldFont);
                        }

                        g.drawString(value, xString, y * lineHeight + (int) (spaceH * 1.4f));
                    }

                    // Top
                    if (!isOuside(x, y)
                            && (value.length() > 0 || y == columnsAxisInfo.getWidth() || isValue(x, y) || (!isValue(x, y) && (leftNotEmpty(matrix, x, y - 1)) || leftNotEmpty(matrix, x, y)))) {
                        g.setColor(BORDER_COLOR);
                        g.drawLine(offsetX, y * lineHeight, offsetX + columnWidth, y * lineHeight);

                    }
                    if ((y < columnsAxisInfo.getWidth() && x >= rowsAxisInfo.getWidth()) || isValue(x, y) || (!isOuside(x, y) && (!leftNotEmpty(matrix, x, y) || !isValue(x, y)))) {
                        g.setColor(BORDER_COLOR);
                        // Left
                        g.drawLine(offsetX, y * lineHeight, offsetX, (y + 1) * lineHeight);
                    }

                }
            }
            offsetX += columnWidth;
            widestWidth = Math.max(columnWidth, widestWidth);
        }
        // Right
        g.setColor(BORDER_COLOR);
        final int bottomRightX = offsetX;
        final int bottomRightY = matrixHeight * lineHeight;
        g.drawLine(offsetX, 0, offsetX, bottomRightY);
        // Bottom
        g.drawLine(0, bottomRightY, offsetX, bottomRightY);

        this.w = bottomRightX + 1;
        this.h = bottomRightY + 1;

    }

    private boolean leftNotEmpty(Matrix matrix, int x, int y) {
        if (y < 0) {
            return true;
        }
        for (int i = 0; i < x; i++) {
            final MatrixCell matrixCell = matrix.get(i, y);
            if (matrixCell != null && !matrixCell.value.isEmpty()) {
                return true;
            }

        }
        return false;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(w, h);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(w, h);
    }

    /**
     * Computes a description of an axis.
     * 
     * @param axis Axis
     * @return Description of axis
     */
    private AxisInfo computeAxisInfo(CellSetAxis axis) {
        if (axis == null) {
            return new AxisInfo(0);
        }
        final AxisInfo axisInfo = new AxisInfo(axis.getAxisMetaData().getHierarchies().size());
        int p = -1;
        for (Position position : axis.getPositions()) {
            ++p;
            int k = -1;
            for (Member member : position.getMembers()) {
                ++k;
                final AxisOrdinalInfo axisOrdinalInfo = axisInfo.ordinalInfos.get(k);
                final int topDepth = member.isAll() ? member.getDepth() : member.getHierarchy().hasAll() ? 1 : 0;
                if (axisOrdinalInfo.minDepth > topDepth || p == 0) {
                    axisOrdinalInfo.minDepth = topDepth;
                }
                axisOrdinalInfo.maxDepth = Math.max(axisOrdinalInfo.maxDepth, member.getDepth());
            }
        }
        axisInfo.computeWidth();
        return axisInfo;
    }

    /**
     * Populates cells in the matrix corresponding to a particular axis.
     * 
     * @param matrix Matrix to populate
     * @param axis Axis
     * @param axisInfo Description of axis
     * @param isColumns True if columns, false if rows
     * @param offset Ordinal of first cell to populate in matrix
     */
    private void populateAxis(Matrix matrix, CellSetAxis axis, AxisInfo axisInfo, boolean isColumns, int offset) {
        if (axis == null) {
            return;
        }
        Member[] prevMembers = new Member[axisInfo.getWidth()];
        Member[] members = new Member[axisInfo.getWidth()];
        for (int i = 0; i < axis.getPositions().size(); i++) {
            final int x = offset + i;
            Position position = axis.getPositions().get(i);
            int yOffset = 0;
            final List<Member> memberList = position.getMembers();
            for (int j = 0; j < memberList.size(); j++) {
                Member member = memberList.get(j);
                final AxisOrdinalInfo ordinalInfo = axisInfo.ordinalInfos.get(j);
                while (member != null) {
                    if (member.getDepth() < ordinalInfo.minDepth) {
                        break;
                    }
                    final int y = yOffset + member.getDepth() - ordinalInfo.minDepth;
                    members[y] = member;
                    member = member.getParentMember();
                }
                yOffset += ordinalInfo.getWidth();
            }
            boolean same = true;
            for (int y = 0; y < members.length; y++) {
                Member member = members[y];
                same = same && i > 0 && Olap4jUtil.equal(prevMembers[y], member);
                String value = member == null ? "" : member.getCaption();
                if (isColumns) {
                    matrix.set(x, y, value, false, same);
                } else {
                    if (same) {
                        value = "";
                    }
                    // noinspection SuspiciousNameCombination
                    matrix.set(y, x, value, false, false);
                }
                prevMembers[y] = member;
                members[y] = null;
            }
        }
    }

    /**
     * Returns an iterator over cells in a result.
     */
    private static Iterable<Cell> cellIter(final int[] pageCoords, final CellSet cellSet) {
        return new Iterable<Cell>() {
            public Iterator<Cell> iterator() {
                int[] axisDimensions = new int[cellSet.getAxes().size() - pageCoords.length];
                assert pageCoords.length <= axisDimensions.length;
                for (int i = 0; i < axisDimensions.length; i++) {
                    CellSetAxis axis = cellSet.getAxes().get(i);
                    axisDimensions[i] = axis.getPositions().size();
                }
                final CoordinateIterator coordIter = new CoordinateIterator(axisDimensions, true);
                return new Iterator<Cell>() {
                    public boolean hasNext() {
                        return coordIter.hasNext();
                    }

                    public Cell next() {
                        final int[] ints = coordIter.next();
                        final AbstractList<Integer> intList = new AbstractList<Integer>() {
                            public Integer get(int index) {
                                return index < ints.length ? ints[index] : pageCoords[index - ints.length];
                            }

                            public int size() {
                                return pageCoords.length + ints.length;
                            }
                        };
                        return cellSet.getCell(intList);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return this.getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 32;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (getParent() instanceof JViewport) {
            return (((JViewport) getParent()).getWidth() > getPreferredSize().width);
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            return (((JViewport) getParent()).getHeight() > getPreferredSize().height);
        }
        return false;
    }

}
