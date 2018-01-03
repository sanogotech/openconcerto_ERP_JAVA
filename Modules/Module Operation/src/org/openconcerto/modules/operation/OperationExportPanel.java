package org.openconcerto.modules.operation;

import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.jopencalendar.model.JCalendarItem;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.TemplateManager;
import org.openconcerto.openoffice.OOUtils;
import org.openconcerto.openoffice.spreadsheet.Sheet;
import org.openconcerto.openoffice.spreadsheet.SpreadSheet;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.date.DateRangePlannerPanel;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.StreamUtils;
import org.openconcerto.utils.cc.ITransformer;

public class OperationExportPanel extends JPanel {
    final JCheckBox lockedCheckBox = new JCheckBox("verrouillées uniquement");
    final JButton bPrint = new JButton("Exporter");

    public OperationExportPanel(final OperationCalendarManager manager, final List<SQLRowValues> rowsSite) {
        lockedCheckBox.setSelected(true);
        //
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        final JLabel l = new JLabel("Date de début", SwingConstants.RIGHT);
        this.add(l, c);
        c.gridx++;
        final JDate d1 = new JDate(false, true);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        d1.setDate(cal.getTime());
        c.weightx = 1;
        this.add(d1, c);
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        final JLabel l2 = new JLabel("Date de fin", SwingConstants.RIGHT);

        this.add(l2, c);
        c.gridx++;
        final JDate d2 = new JDate(false, true);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.SECOND, -1);
        d2.setDate(cal.getTime());
        c.weightx = 1;
        this.add(d2, c);

        //
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        final JRadioButton radio1 = new JRadioButton("interventions");
        radio1.setSelected(true);
        this.add(radio1, c);
        final JRadioButton radio2 = new JRadioButton("plannifications");
        c.gridy++;
        this.add(radio2, c);
        final ButtonGroup g = new ButtonGroup();
        g.add(radio1);
        g.add(radio2);

        //
        final JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.RIGHT));
        p.add(lockedCheckBox);
        p.add(bPrint);
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        this.add(p, c);
        //

        bPrint.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (d1.getDate().after(d2.getDate())) {
                    return;
                }
                final List<JCalendarItem> items = manager.getItemIn(d1.getDate(), d2.getDate(), null, null);
                final List<JCalendarItemDB> itemsToExport = new ArrayList<JCalendarItemDB>(items.size());
                if (lockedCheckBox.isSelected()) {
                    for (JCalendarItem jCalendarItem : items) {
                        JCalendarItemDB i = (JCalendarItemDB) jCalendarItem;
                        if (i.getFlagsString().contains("locked")) {
                            itemsToExport.add(i);
                        }
                    }
                } else {
                    for (JCalendarItem jCalendarItem : items) {
                        JCalendarItemDB i = (JCalendarItemDB) jCalendarItem;
                        itemsToExport.add(i);
                    }
                }
                if (rowsSite != null && !rowsSite.isEmpty()) {
                    final Set<String> allowedSites = new HashSet<String>();
                    for (SQLRowValues r : rowsSite) {
                        String siteName = r.getString("NAME");
                        allowedSites.add(siteName);
                    }
                    final List<JCalendarItemDB> filtered = new ArrayList<JCalendarItemDB>(itemsToExport.size());
                    for (JCalendarItemDB i : itemsToExport) {
                        if (allowedSites.contains(i.getSiteName())) {
                            filtered.add(i);
                        }
                    }
                    itemsToExport.clear();
                    itemsToExport.addAll(filtered);
                }

                if (itemsToExport.isEmpty()) {
                    JOptionPane.showMessageDialog(OperationExportPanel.this, "Aucune intervention trouvée.\nMerci de vérifier la période et le verrouillage des interventions.");
                    return;
                }

                Collections.sort(itemsToExport, new Comparator<JCalendarItem>() {

                    @Override
                    public int compare(JCalendarItem o1, JCalendarItem o2) {
                        if (o1.getUserId().equals(o2.getUserId())) {
                            return o1.getDtStart().getTime().compareTo(o2.getDtStart().getTime());
                        }
                        return (int) (((Number) o1.getUserId()).longValue() - ((Number) o2.getUserId()).longValue());
                    }
                });
                if (radio1.isSelected()) {
                    export(itemsToExport);
                } else {
                    exportPlan(itemsToExport);
                }
                closeFrame();

            }
        });
    }

    protected void exportPlan(List<JCalendarItemDB> itemsToExport) {

        final List<Long> ids = ModuleOperation.getOperationIdsFrom(new HashSet<JCalendarItemDB>(itemsToExport));

        final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        final SQLTable table = root.getTable(ModuleOperation.TABLE_OPERATION);
        final SQLRowValues valOperation = new SQLRowValues(table);

        final SQLRowValues valSite = new SQLRowValues(root.getTable(ModuleOperation.TABLE_SITE));
        valSite.putNulls("NAME", "COMMENT");

        final SQLRowValues userVals = valOperation.putRowValues("ID_USER_COMMON").putNulls("NOM");

        // valOperation.put("ID_CALENDAR_ITEM_GROUP", valsCalendarItemsGroup);
        valOperation.put("ID_USER_COMMON", userVals);
        valOperation.put("ID_SITE", valSite);
        valOperation.putNulls("STATUS", "DESCRIPTION", "TYPE", "PLANNER_XML", "PLANNER_UID");

        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(valOperation);
        fetcher.setFullOnly(true);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                input.andWhere(new Where(table.getKey(), ids));
                return input;
            }
        });

        final List<SQLRowValues> itemsFetched = fetcher.fetch();
        final List<SQLRowValues> items = new ArrayList<SQLRowValues>();
        final Set<String> plannnerUId = new HashSet<String>();
        for (SQLRowValues d : itemsFetched) {
            if (d.getObject("PLANNER_UID") != null) {
                final String string = d.getString("PLANNER_UID");
                if (string.length() > 3 && !plannnerUId.contains(string)) {
                    items.add(d);
                    plannnerUId.add(string);
                }
            }
        }

        TableModel model = new AbstractTableModel() {

            @Override
            public int getRowCount() {
                return items.size();
            }

            @Override
            public int getColumnCount() {
                return 5;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                SQLRowValues i = items.get(rowIndex);
                switch (columnIndex) {
                case 0:
                    // Plannif
                    String start = DateRangePlannerPanel.getDescriptionFromXML(i.getString("PLANNER_XML"));
                    return start;

                case 1:
                    // Employé
                    final String user = i.getForeign("ID_USER_COMMON").getString("NOM");
                    return user;
                case 2:
                    // Nature
                    final String type = i.getString("TYPE");
                    return type;
                case 3:
                    // Chantier
                    final String siteName = i.getForeign("ID_SITE").getString("NAME");
                    return siteName;
                case 4:
                    // Description
                    final String desc = i.getString("DESCRIPTION");
                    return desc;
                default:
                    break;
                }
                return "?";
            }

        };

        // Save the data to an ODS file and open it.
        final String templateId = ModuleOperation.OPERATIONS_REPORT_TEMPLATE2_ID;
        saveAsODS(model, templateId);

    }

    protected void export(final List<JCalendarItemDB> items) {

        TableModel model = new AbstractTableModel() {

            @Override
            public int getRowCount() {
                return items.size();
            }

            @Override
            public int getColumnCount() {
                return 7;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                JCalendarItemDB i = items.get(rowIndex);
                switch (columnIndex) {
                case 0:
                    // Date
                    Date start = i.getDtStart().getTime();
                    return start;
                case 1:
                    // Heure
                    Calendar h = i.getDtStart();
                    return h;
                case 2:
                    // Durée
                    long m = (i.getDtEnd().getTimeInMillis() - i.getDtStart().getTimeInMillis()) / (1000 * 60);
                    return m;
                case 3:
                    // Employé
                    final String user = ((SQLRowAccessor) (i.getCookie())).getString("NOM");
                    return user;
                case 4:
                    // Nature
                    final String type = i.getType();
                    return type;
                case 5:
                    // Chantier
                    final String siteName = i.getSiteName();
                    return siteName;
                case 6:
                    // Description
                    final String desc = i.getDescription();
                    return desc;
                default:
                    break;
                }
                return "?";
            }

        };

        // Save the data to an ODS file and open it.
        final String templateId = ModuleOperation.OPERATIONS_REPORT_TEMPLATE_ID;
        saveAsODS(model, templateId);

    }

    public void saveAsODS(TableModel model, final String templateId) {
        try {
            final InputStream inStream = TemplateManager.getInstance().getTemplate(templateId);
            final File templateFile = File.createTempFile(templateId, ".ods");
            if (inStream == null) {
                JOptionPane.showMessageDialog(this, "Modèle introuvable");
                return;
            }
            StreamUtils.copy(inStream, templateFile);
            inStream.close();
            final Sheet sheet = SpreadSheet.createFromFile(templateFile).getSheet(0);
            final int rowCount = model.getRowCount();
            sheet.ensureRowCount(rowCount + 1);
            final int columnCount = model.getColumnCount();
            for (int x = 0; x < columnCount; x++) {
                for (int y = 0; y < rowCount; y++) {
                    sheet.setValueAt(model.getValueAt(y, x), x, y + 1);
                }
            }

            final FileDialog d = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Exporter sous...", FileDialog.SAVE);
            d.setFile("export.ods");
            d.setVisible(true);
            String fileName = d.getFile();
            if (fileName != null) {
                fileName = fileName.trim();
                if (!fileName.toLowerCase().endsWith(".ods")) {
                    fileName += ".ods";
                }
                File outputFile = new File(d.getDirectory(), fileName);
                final File saveAs = sheet.getSpreadSheet().saveAs(outputFile);
                OOUtils.open(saveAs);
            } else {
                JOptionPane.showMessageDialog(this, "Fichier non spécifié");
            }
        } catch (Exception e) {
            ExceptionHandler.handle("Export error", e);
        }
    }

    protected void closeFrame() {
        SwingUtilities.getWindowAncestor(this).dispose();
    }

}
