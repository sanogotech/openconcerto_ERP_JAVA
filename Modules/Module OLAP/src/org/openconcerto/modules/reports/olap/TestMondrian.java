package org.openconcerto.modules.reports.olap;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.JFrame;

import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.layout.CellSetFormatter;
import org.olap4j.layout.RectangularCellSetFormatter;
import org.olap4j.layout.TraditionalCellSetFormatter;
import org.olap4j.mdx.IdentifierNode;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.olap4j.query.Query;
import org.olap4j.query.QueryDimension;
import org.olap4j.query.Selection;
import org.openconcerto.modules.reports.olap.renderer.CellSetRenderer;

public class TestMondrian {
    public TestMondrian() {

    }

    public void test() throws Exception {
        Class.forName("mondrian.olap4j.MondrianOlap4jDriver");
        final File file = new File("FoodMart.xml");
        System.out.println(file.exists());
        final String url = "jdbc:mondrian:" + "JdbcDrivers=org.postgresql.Driver;" + "Jdbc=jdbc:postgresql://192.168.1.10/foodmart?user=maillard&password=guigui;" + "Catalog=file:"
                + file.getCanonicalPath().replace('\\', '/') + ";";
        System.out.println(url);
        Connection rConnection = DriverManager.getConnection(url);
        OlapConnection oConnection = rConnection.unwrap(OlapConnection.class);

        NamedList<Cube> cubes = oConnection.getOlapSchema().getCubes();
        Cube salesCube = cubes.get("Sales");

        Query myQuery = new Query("SomeArbitraryName", salesCube);
        dumpQuery(myQuery);
        QueryDimension productDim = myQuery.getDimension("Product");
        QueryDimension storeDim = myQuery.getDimension("Store");
        QueryDimension timeDim = myQuery.getDimension("Time");
        myQuery.getAxis(Axis.ROWS).addDimension(productDim);
        myQuery.getAxis(Axis.COLUMNS).addDimension(storeDim);
        myQuery.getAxis(Axis.FILTER).addDimension(timeDim);
        //
        Member year1997 = salesCube.lookupMember(IdentifierNode.ofNames("Time", "1997").getSegmentList());
        timeDim.include(year1997);
        productDim.include(Selection.Operator.CHILDREN, IdentifierNode.ofNames("Product", "Drink", "Beverages").getSegmentList());

        productDim.exclude(IdentifierNode.ofNames("Product", "Drink", "Beverages", "Carbonated Beverages").getSegmentList());

        myQuery.validate();
        System.out.println(myQuery.getSelect().toString());
        CellSet results = oConnection.createStatement().executeOlapQuery(
                "SELECT NON EMPTY {Hierarchize({{[Department].[Department Description].Members}})} ON COLUMNS, NON EMPTY {Hierarchize({{[Store].[Store Country].Members}, {[Store].[Store State].Members}})} ON ROWS FROM [HR]");

        CellSetFormatter formatter = new RectangularCellSetFormatter(false);
        formatter.format(results, new PrintWriter(System.out, true));
        new TraditionalCellSetFormatter().format(results, new PrintWriter(System.out, true));
        JFrame fr = new JFrame();
        fr.setContentPane(new CellSetRenderer(results));
        fr.setSize(800, 600);
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setVisible(true);

    }

    private void dumpQuery(Query myQuery) {
        try {
            NamedList<Dimension> dims = myQuery.getCube().getDimensions();
            for (Dimension dimension : dims) {

                System.out.println("Dimension: " + dimension.getName() + " type:" + dimension.getDimensionType());
                NamedList<Hierarchy> hierarchies = dimension.getHierarchies();
                for (Hierarchy h : hierarchies) {
                    if (h != null) {
                        NamedList<Member> members = h.getRootMembers();
                        if (members != null) {
                            System.out.println("Members of hierarchy:" + h.getName());
                            for (Member member : members) {
                                System.out.println("  Member:" + member.getName());
                            }
                        }
                        NamedList<Level> levels = h.getLevels();
                        if (levels != null) {
                            System.out.println("Levels of hierarchy:" + h.getName());
                            for (Level l : levels) {
                                System.out.println("  Level:" + l.getName());
                            }
                        }
                    }

                }

                System.out.println();
            }
        } catch (OlapException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        new TestMondrian().test();
    }
}
