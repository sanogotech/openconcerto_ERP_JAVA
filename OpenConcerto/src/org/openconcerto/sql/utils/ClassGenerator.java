/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 /*
 * Created on 24 janv. 2005
 */
package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.RowItemDesc;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.io.NewLineWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class ClassGenerator {

    /**
     * Genere dans out.java le squelette d'une classe 'classname' et la traduction a ajouter au
     * mapping xml
     * 
     * @param t le nom de la table a utiliser
     * @param classname le nom de la classe
     */
    public static void generate(SQLTable t, String classname) {
        try {
            final NewLineWriter out = new NewLineWriter(FileUtils.createWriter(new File("out.java")));
            final List<SQLField> f = t.getOrderedFields();
            f.remove(t.getArchiveField());
            f.remove(t.getOrderField());
            f.remove(t.getKey());
            generateAutoLayoutedJComponent(t, f, classname, out, null);

            out.println("");
            generateMappingXML(t, f, out);

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void generateMappingXML(SQLTable t, List<SQLField> f, NewLineWriter out) throws IOException {
        out.println("<TABLE name=\"" + t.getName() + "\">");
        for (final SQLField element : f) {
            final RowItemDesc desc = SQLFieldTranslator.getDefaultDesc(element);
            out.println("   <FIELD name=\"" + element.getName() + "\" label=\"" + desc.getLabel() + "\" titleLabel=\"" + desc.getTitleLabel() + "\" />");

        }
        out.println("</TABLE>");
    }

    public static List<SQLField> generateAutoLayoutedJComponent(SQLTable t, List<SQLField> f, String classname, NewLineWriter out, String packageName) throws IOException {

        if (packageName != null && packageName.length() > 0) {
            out.print("package ");
            out.print(packageName);
            out.println(";");
            out.println();
        }
        out.println("import org.openconcerto.sql.element.ConfSQLElement;");
        out.println("import org.openconcerto.sql.element.SQLComponent;");
        out.println("import org.openconcerto.sql.element.UISQLComponent;");
        out.println("import org.openconcerto.sql.model.SQLRow;");

        out.println();
        out.println("import java.util.ArrayList;");
        out.println("import java.util.HashSet;");
        out.println("import java.util.List;");
        out.println("import java.util.Set;");
        out.println();
        out.println("public class " + classname + " extends ConfSQLElement {");
        out.println();

        // Constructor
        out.println("    public " + classname + "() {");// static final String NAME = \"un ??\";");
        out.println("        super(\"" + t.getName() + "\", \"un " + t.getName().toLowerCase() + " \", \"" + t.getName().toLowerCase() + "s\");");
        out.println("    }");
        out.println();

        // List
        out.println("    protected List<String> getListFields() {");
        out.println("        final List<String> l = new ArrayList<String>();");
        for (final SQLField element : f) {
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                out.println("        l.add(\"" + element.getName() + "\");");
            }
        }
        out.println("        return l;");
        out.println("    }");
        out.println();

        // Combo
        out.println("    protected List<String> getComboFields() {");
        out.println("        final List<String> l = new ArrayList<String>();");
        for (final SQLField element : f) {
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                out.println("        l.add(\"" + element.getName() + "\");");
            }
        }
        out.println("        return l;");
        out.println("    }");
        out.println();

        // UI
        out.println("    public SQLComponent createComponent() {");
        out.println("        return new UISQLComponent(this) {");
        out.println();
        out.println("            @Override");
        out.println("            protected Set<String> createRequiredNames() {");
        out.println("                final Set<String> s = new HashSet<String>();");
        for (final SQLField element : f) {
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                out.println("                // s.add(\"" + element.getName() + "\");");
            }
        }
        out.println("                return s;");
        out.println("            }");
        out.println();
        out.println("            public void addViews() {");
        SQLField first = null;
        for (final SQLField element : f) {
            if (first == null) {
                first = element;
            }
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                out.println("                this.addView(\"" + element.getName() + "\");");
            }
        }
        out.println("            }");
        out.println("        };");
        out.println("    }");
        out.println();
        out.println("    public String getDescription(SQLRow fromRow) {");
        if (first != null) {
            out.println("        return fromRow.getString(\"" + first.getName() + "\");");
        }
        out.println("    }");
        out.println();
        out.println("}");

        return f;
    }

    public static String generateAutoLayoutedJComponent(SQLTable table, String c, String packageName) {
        StringWriter b = new StringWriter();
        final List<SQLField> f = getOrderedContentFields(table);
        try {
            generateAutoLayoutedJComponent(table, f, c, new NewLineWriter(b), packageName);
        } catch (IOException e) {
            // shouldn't happen with StringWriter
            throw new IllegalStateException(e);
        }
        return b.toString();
    }

    public static String generateMappingXML(SQLTable table, String c) {
        StringWriter b = new StringWriter();
        final List<SQLField> f = getOrderedContentFields(table);
        try {
            generateMappingXML(table, f, new NewLineWriter(b));
        } catch (IOException e) {
            // shouldn't happen with StringWriter
            throw new IllegalStateException(e);
        }
        return b.toString();
    }

    private static List<SQLField> getOrderedContentFields(SQLTable table) {
        final List<SQLField> f = table.getOrderedFields();
        f.retainAll(table.getContentFields());
        return f;
    }

    public static String getStandardClassName(String n) {
        int nb = n.length();
        StringBuilder b = new StringBuilder(nb);
        if (n.toUpperCase().equals(n)) {
            n = n.toLowerCase();
        }
        n = StringUtils.firstUp(n);

        for (int i = 0; i < nb; i++) {
            char c = n.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                b.append(c);
            }

        }
        return b.toString();
    }

    public static String generateGroup(SQLTable table, String className, String packageName) {
        StringBuilder b = new StringBuilder();
        b.append("import org.openconcerto.ui.group.Group;\n\n");

        b.append("public class " + className + " extends Group {\n\n");
        final List<SQLField> f = table.getOrderedFields();
        f.remove(table.getArchiveField());
        f.remove(table.getOrderField());
        f.remove(table.getKey());
        f.remove(table.getCreationDateField());
        f.remove(table.getCreationUserField());
        f.remove(table.getModifDateField());
        f.remove(table.getModifUserField());
        String name = table.getName().toLowerCase().replace('_', '.');
        b.append("    public " + className + " () {\n");
        b.append("        super(\"" + name + "\");\n");
        for (SQLField sqlField : f) {
            String n = name + "." + sqlField.getFieldName().toLowerCase().replace("id_", "").replace('_', '.');

            b.append("        addItem(\"" + n + "\");\n");
        }

        b.append("    }\n\n");
        b.append("}\n");
        return b.toString();
    }

    public static String generateSQLConfElement(SQLTable table, String classname, String packageName) {
        final List<SQLField> f = table.getOrderedFields();
        f.remove(table.getArchiveField());
        f.remove(table.getOrderField());
        f.remove(table.getKey());
        f.remove(table.getCreationDateField());
        f.remove(table.getCreationUserField());
        f.remove(table.getModifDateField());
        f.remove(table.getModifUserField());

        StringBuilder b = new StringBuilder();
        if (packageName != null && packageName.length() > 0) {
            b.append("package ");
            b.append(packageName);
            b.append(";\n");
            b.append("\n");
        }

        b.append("import org.openconcerto.sql.element.SQLComponent;\n");
        b.append("import org.openconcerto.sql.model.DBRoot;\n");
        b.append("import model.AbstractModel;\n");

        b.append("\n");
        b.append("import java.util.ArrayList;\n");
        b.append("import java.util.HashSet;\n");
        b.append("import java.util.List;\n");
        b.append("import java.util.Set;\n");
        b.append("\n");
        b.append("public class " + classname + " extends SocieteSQLElement {\n");
        b.append("\n");

        // Constructor
        b.append("    public " + classname + "() {\n");
        b.append("        super(\"" + table.getName() + "\", \"un " + table.getName().toLowerCase() + " \", \"" + table.getName().toLowerCase() + "s\");\n");
        String g = StringUtils.firstUpThenLow(table.getName()) + "EditGroup";
        b.append("        this.setDefaultGroup(new " + g + "());\n");

        b.append("    }\n");
        b.append("\n");

        // List
        b.append("    protected List<String> getListFields() {\n");
        b.append("        final List<String> l = new ArrayList<String>();\n");
        for (final SQLField element : f) {
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                b.append("        l.add(\"" + element.getName() + "\");\n");
            }
        }
        b.append("        return l;\n");
        b.append("    }\n");
        b.append("\n");

        // Combo
        b.append("    protected List<String> getComboFields() {\n");
        b.append("        final List<String> l = new ArrayList<String>();\n");
        for (final SQLField element : f) {
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                b.append("        l.add(\"" + element.getName() + "\");\n");
            }
        }
        b.append("        return l;\n");
        b.append("    }\n");
        b.append("\n");

        // UI
        b.append("    public SQLComponent createComponent() {\n");
        b.append("        return null;\n");
        b.append("    }\n");
        //
        String code = table.getName().toLowerCase().replace('_', '.');
        b.append("    @Override\n");
        b.append("    protected String createCode() {\n");
        b.append("        return \"" + code + "\";\n");
        b.append("    }\n");

        b.append("\n");
        b.append("}");
        return b.toString();
    }

    // field mapping
    public static String generateFieldMapping(SQLTable table, String classname, String packageName) {
        final List<SQLField> f = table.getOrderedFields();
        f.remove(table.getArchiveField());
        f.remove(table.getOrderField());
        f.remove(table.getKey());
        f.remove(table.getCreationDateField());
        f.remove(table.getCreationUserField());
        f.remove(table.getModifDateField());
        f.remove(table.getModifUserField());
        StringBuilder b = new StringBuilder();
        String tableId = table.getName().toLowerCase().replace('_', '.');
        b.append("<table id=\"" + tableId + "\" name=\"" + table.getName() + "\">\n");
        for (final SQLField sqlField : f) {
            String n = tableId + "." + sqlField.getFieldName().toLowerCase().replace("id_", "").replace('_', '.');
            b.append("   <field id=\"" + n + "\" name=\"" + sqlField.getName() + "\" />\n");
        }
        b.append("</table>\n");
        return b.toString();
    }

}
