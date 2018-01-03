package org.openconcerto.modules.reports.olap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.olap4j.OlapException;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Dimension.Type;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.NamedList;
import org.openconcerto.ui.table.AbstractTreeTableModel;

public class DimensionTreeModel extends AbstractTreeTableModel {
    private List<Dimension> dimensions = new ArrayList<Dimension>();
    private boolean isDimension;

    public DimensionTreeModel(String name, Cube cube, boolean b) {
        super(new DefaultMutableTreeNode(name));
        this.isDimension = b;
        final NamedList<Dimension> dims = cube.getDimensions();
        for (int i = 0; i < dims.size(); i++) {
            final Dimension dim = dims.get(i);
            try {
                if (dim.getDimensionType().equals(Type.MEASURE)) {
                    if (!b) {
                        dimensions.add(dim);
                    }
                } else {
                    if (b) {
                        dimensions.add(dim);
                    }
                }
            } catch (OlapException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        Collections.sort(dimensions, new Comparator<Dimension>() {
            @Override
            public int compare(Dimension o1, Dimension o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return "Col" + column;
    }

    @Override
    public Object getValueAt(Object node, int column) {
        return node + ":" + column;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent.equals(this.getRoot())) {
            return dimensions.get(index);
        }
        if (isDimension) {
            if (parent instanceof Dimension) {
                final Dimension dimension = (Dimension) parent;
                if (dimension.getHierarchies().size() == 1) {
                    return dimension.getHierarchies().get(0).getLevels().get(index);
                } else {
                    return dimension.getHierarchies().get(index);
                }
            }
            if (parent instanceof Hierarchy) {
                Hierarchy h = (Hierarchy) parent;
                return h.getLevels().get(index);
            }
        } else {
            if (parent instanceof Dimension) {
                final Dimension dimension = (Dimension) parent;
                try {
                    if (dimension.getHierarchies().size() == 1) {
                        return dimension.getHierarchies().get(0).getRootMembers().get(index);
                    } else {
                        return dimension.getHierarchies().get(index);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return parent + "-" + index;
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent.equals(this.getRoot())) {
            return dimensions.size();
        }
        if (isDimension) {
            if (parent instanceof Dimension) {
                final Dimension dimension = (Dimension) parent;
                if (dimension.getHierarchies().size() == 1) {
                    return dimension.getHierarchies().get(0).getLevels().size();
                } else {
                    return dimension.getHierarchies().size();
                }

            }
            if (parent instanceof Hierarchy) {
                final Hierarchy h = (Hierarchy) parent;
                return h.getLevels().size();
            }
        } else {

            // Members.......
            if (parent instanceof Dimension) {
                final Dimension dimension = (Dimension) parent;
                try {
                    if (dimension.getHierarchies().size() == 1) {
                        final Hierarchy hierarchy = dimension.getHierarchies().get(0);
                        return hierarchy.getRootMembers().size();
                    } else {
                        return dimension.getHierarchies().size();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return 0;
    }

}
