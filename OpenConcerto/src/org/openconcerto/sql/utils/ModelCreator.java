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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.DefaultListModel;
import org.openconcerto.utils.ClipboardUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.LogUtils;
import org.openconcerto.utils.StringUtils;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.dbutils.ResultSetHandler;
import org.jedit.JEditTextArea;
import org.jedit.JavaTokenMarker;

public class ModelCreator extends JFrame implements ListSelectionListener {
    JTabbedPane pane = new JTabbedPane();
    private JList list;
    private final Preferences pref;
    private JButton buttonConnect;
    final JTextField rootTF = new JTextField();

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LogUtils.rmRootHandlers();
                LogUtils.setUpConsoleHandler();
                Logger.getLogger("org.openconcerto.sql").setLevel(Level.WARNING);
                ModelCreator m = new ModelCreator();
                m.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                m.setSize(1000, 800);
                m.setVisible(true);
            }
        });

    }

    ModelCreator() {
        super("FrameWork SQL Toolbox");
        this.pref = Preferences.userRoot().node("/ilm/sql/" + getClass().getSimpleName());

        JPanel confPanel = new JPanel();
        confPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        confPanel.add(new JLabel("Root name:"), c);
        c.gridx++;
        c.gridwidth = 4;
        c.weightx = 1;

        rootTF.setText(this.pref.get("url", "psql://login:password@192.168.1.10:5432/OpenConcerto/OpenConcerto42"));
        confPanel.add(rootTF, c);

        // Ligne 4
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 4;
        buttonConnect = new JButton("Connexion");
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout());
        toolbar.add(buttonConnect);
        JButton bAnalyse = new JButton("Analyse de toutes les tables");

        toolbar.add(bAnalyse);
        confPanel.add(toolbar, c);
        final DefaultListModel model = new DefaultListModel();

        this.list = new JList(model);
        this.list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final SQLTable t = (SQLTable) value;
                final String v = t.getSQLName().toString();

                return super.getListCellRendererComponent(list, v, index, isSelected, cellHasFocus);
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(new JScrollPane(this.list));
        split.setRightComponent(this.pane);
        split.setDividerLocation(360);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;

        confPanel.add(split, c);

        this.setContentPane(confPanel);

        buttonConnect.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                rootTF.setEnabled(false);
                buttonConnect.setText("Connexion en cours");
                buttonConnect.setEnabled(false);
                final String[] args = rootTF.getText().split(" ");
                final String url = args[0];
                final List<String> rootsToMap = args.length == 1 ? Collections.<String> emptyList() : SQLRow.toList(args[1]);
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connect(model, url, rootsToMap);
                    }
                });
                t.setDaemon(true);
                t.start();
            }

        });
        this.list.addListSelectionListener(this);
        bAnalyse.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                analyseAll();

            }
        });
    }

    protected void analyseAll() {
        PrintStream ptr = System.out;
        final ListModel model = this.list.getModel();
        int stop = model.getSize();
        for (int i = 0; i < stop; i++) {
            SQLTable t = (SQLTable) model.getElementAt(i);
            analyse(ptr, t);
        }
    }

    private void analyse(final PrintStream ptr, SQLTable t) {
        final DecimalFormat df = new DecimalFormat("0.##%");

        ptr.println("Table " + t.getName());
        final int totalCount = t.getRowCount(false, ArchiveMode.UNARCHIVED);
        ptr.println("Nombre de lignes : " + totalCount);
        if (totalCount > 0) {
            final List<String> fieldNames = new ArrayList<String>(t.getFieldsNames(VirtualFields.CONTENT));
            Collections.sort(fieldNames);
            for (String fieldName : fieldNames) {
                final SQLField sqlField = t.getField(fieldName);
                SQLSelect select = new SQLSelect();
                select.addSelect(sqlField);
                select.addSelectFunctionStar("count");
                select.addGroupBy(sqlField);
                select.addRawOrder("count(*) DESC");
                select.setLimit(10);

                ptr.print("- champ " + SQLBase.quoteIdentifier(fieldName));
                final Number uniqCount = (Number) t.getDBSystemRoot().getDataSource()
                        .executeScalar("SELECT COUNT(*) FROM (" + new SQLSelect().addSelect(sqlField).addGroupBy(sqlField).asString() + ") g");
                if (uniqCount.longValue() > 10l) {
                    ptr.print(", top 10 des valeurs (sur " + uniqCount + "):");
                }
                ptr.println();
                t.getDBSystemRoot().getDataSource().execute(select.asString(), new ResultSetHandler() {
                    @Override
                    public Object handle(ResultSet rs) throws SQLException {
                        while (rs.next()) {
                            final int count = rs.getInt(2);
                            String percent = df.format(count / (double) totalCount);
                            percent = StringUtils.rightAlign(percent, 5);
                            ptr.print("  " + percent + " : \"" + rs.getObject(1) + "\"");
                            if (count != totalCount) {
                                ptr.print(" (" + count + "/" + totalCount + ")");
                            }

                            ptr.println();
                        }
                        return null;
                    }
                });

            }
        }
        ptr.println();
    }

    public void valueChanged(ListSelectionEvent e) {
        this.pane.removeAll();

        final SQLTable table = (SQLTable) this.list.getSelectedValue();
        // e.g. COMPLETION has no content fields
        if (table.getContentFields().size() > 0) {
            String c = RowBackedCodeGenerator.getJavaName(table.getName());
            this.pane.add("Code RowBacked", createTA(RowBackedCodeGenerator.getCode(table, c, null)));

            this.pane.add("Code BaseSQLElement", createTA(ClassGenerator.generateAutoLayoutedJComponent(table, c + "SQLElement", null)));
            this.pane.add("Code SQLConfElement", createTA(ClassGenerator.generateSQLConfElement(table, c + "SQLElement", null)));
            this.pane.add("Code Group", createTA(ClassGenerator.generateGroup(table, c + "EditGroup", null)));
            this.pane.add("Field Mapping", createTA(ClassGenerator.generateFieldMapping(table, c, null)));
            this.pane.add("Mapping XML", createTA(ClassGenerator.generateMappingXML(table, c)));

        }
    }

    private final JPanel createTA(final String text) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        final JEditTextArea res = new JEditTextArea();
        res.setEditable(false);
        res.setTokenMarker(new JavaTokenMarker());
        res.setText(text);
        res.setCaretPosition(0);
        p.add(res, c);

        JButton b = new JButton("Copy to clipboard");
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ClipboardUtils.setClipboardContents(res.getText());

            }
        });
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        p.add(b, c);

        return p;
    }

    private void connect(final DefaultListModel model, final String textUrl, final List<String> roots) {
        ModelCreator.this.pref.put("url", textUrl);

        try {
            final SQL_URL url = SQL_URL.create(textUrl);
            final DBSystemRoot sysRoot = SQLServer.create(url, roots, null);
            final List<SQLTable> tables = new ArrayList<SQLTable>(sysRoot.getRoot(url.getRootName()).getDescs(SQLTable.class));
            Collections.sort(tables, new Comparator<SQLTable>() {

                public int compare(SQLTable o1, SQLTable o2) {
                    String v1 = o1.getSQLName().toString();
                    String v2 = o2.getSQLName().toString();

                    return v1.compareTo(v2);
                }
            });
            model.removeAllElements();
            model.addAll(tables);
        } catch (Exception e1) {
            ExceptionHandler.handle(ModelCreator.this, "erreur d'URL", e1);
            JOptionPane.showMessageDialog(ModelCreator.this, e1.getMessage(), "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
        }
        buttonConnect.setEnabled(true);
        buttonConnect.setText("Connexion");
        rootTF.setEnabled(true);
    }
}
