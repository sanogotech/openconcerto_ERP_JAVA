package org.openconcerto.modules.customerrelationship.lead;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ModuleElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.GlobalMapper;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLInjector;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.EditPanelListener;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.ui.EmailComposer;
import org.openconcerto.ui.FrameUtil;
import org.openconcerto.ui.group.Group;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.cc.ITransformer;

public class LeadSQLElement extends ModuleElement {
    public static final String ELEMENT_CODE = "customerrelationship.lead";
    public static String CODE_NOT_RESTRICT = "VIEW_ALL_LEADS";

    public LeadSQLElement(final AbstractModule module) {
        super(module, Module.TABLE_LEAD);
        this.setL18nLocation(LeadSQLElement.class);

        // Call
        final RowAction.PredicateRowAction addCallAction = new RowAction.PredicateRowAction(new AbstractAction("Appeler") {

            @Override
            public void actionPerformed(ActionEvent e) {
                SQLRow sRow = IListe.get(e).getSelectedRow().asRow();
                final SQLTable table = LeadSQLElement.this.getTable().getTable(Module.TABLE_LEAD_CALL);
                final SQLElement eCall = LeadSQLElement.this.getDirectory().getElement(table);
                EditFrame editFrame = new EditFrame(eCall);
                final SQLRowValues sqlRowValues = new SQLRowValues(table);
                sqlRowValues.put("ID_LEAD", sRow.getIDNumber());
                editFrame.getSQLComponent().select(sqlRowValues);
                FrameUtil.show(editFrame);
            }
        }, true) {
        };
        addCallAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(addCallAction);
        // Visit
        final RowAction.PredicateRowAction addVisitAction = new RowAction.PredicateRowAction(new AbstractAction("Enregister une visite") {

            @Override
            public void actionPerformed(ActionEvent e) {
                SQLRow sRow = IListe.get(e).getSelectedRow().asRow();
                final SQLTable table = LeadSQLElement.this.getTable().getTable(Module.TABLE_LEAD_VISIT);
                final SQLElement eCall = LeadSQLElement.this.getDirectory().getElement(table);
                EditFrame editFrame = new EditFrame(eCall);
                final SQLRowValues sqlRowValues = new SQLRowValues(table);
                sqlRowValues.put("ID_LEAD", sRow.getIDNumber());
                editFrame.getSQLComponent().select(sqlRowValues);
                FrameUtil.show(editFrame);
            }
        }, true) {
        };
        addVisitAction.setPredicate(IListeEvent.getSingleSelectionPredicate());
        getRowActions().add(addVisitAction);

        setRowActions();
    }

    @Override
    protected void _initTableSource(SQLTableModelSource source) {

        super._initTableSource(source);

        User user = UserManager.getInstance().getCurrentUser();
        if (!user.getRights().haveRight(LeadSQLElement.CODE_NOT_RESTRICT)) {
            SQLRow row = Configuration.getInstance().getRoot().findTable("USER_COMMON").getRow(UserManager.getInstance().getCurrentUser().getId());
            List<SQLRow> rows = row.getReferentRows(Configuration.getInstance().getRoot().findTable("COMMERCIAL").getField("ID_USER_COMMON"));
            final List<Integer> listComm = new ArrayList<Integer>();
            for (SQLRow sqlRow : rows) {
                listComm.add(sqlRow.getID());
            }
            if (listComm != null && listComm.size() > 0) {
                source.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {

                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {

                        SQLField field = input.getTable(Module.TABLE_LEAD).getField("ID_COMMERCIAL");
                        Where w = new Where(field, listComm);
                        w = w.or(new Where(field, "IS", (Object) null));
                        w = w.or(new Where(field, "=", getTable().getTable("COMMERCIAL").getUndefinedID()));
                        input.setWhere(w);
                        return input;
                    }
                });
            }
        }

        BaseSQLTableModelColumn adresse = new BaseSQLTableModelColumn("Adresse", String.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {
                SQLRowAccessor rAdr = r.getForeign("ID_ADRESSE");
                if (rAdr != null && !rAdr.isUndefined()) {

                    return rAdr.getString("RUE");
                }
                return "";
            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(getTable());
                p = p.add(getTable().getTable("ADRESSE"));
                return CollectionUtils.createSet(new FieldPath(p, "RUE"));
            }
        };
        source.getColumns().add(adresse);

        BaseSQLTableModelColumn cp = new BaseSQLTableModelColumn("Code Postal", String.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {
                SQLRowAccessor rAdr = r.getForeign("ID_ADRESSE");
                if (rAdr != null && !rAdr.isUndefined()) {

                    return rAdr.getString("CODE_POSTAL");
                }

                return "";

            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(getTable());
                p = p.add(getTable().getTable("ADRESSE"));

                return CollectionUtils.createSet(new FieldPath(p, "CODE_POSTAL"));
            }
        };
        source.getColumns().add(cp);

        BaseSQLTableModelColumn ville = new BaseSQLTableModelColumn("Ville", String.class) {

            @Override
            protected Object show_(SQLRowAccessor r) {

                SQLRowAccessor rAdr = r.getForeign("ID_ADRESSE");
                if (rAdr != null && !rAdr.isUndefined()) {

                    return rAdr.getString("VILLE");
                }
                return "";

            }

            @Override
            public Set<FieldPath> getPaths() {
                Path p = new Path(getTable());
                p = p.add(getTable().getTable("ADRESSE"));

                return CollectionUtils.createSet(new FieldPath(p, "VILLE"));
            }
        };
        source.getColumns().add(ville);

        if (getTable().contains("REMIND_DATE")) {
            BaseSQLTableModelColumn dateRemind = new BaseSQLTableModelColumn("Date de rappel", Date.class) {

                @Override
                protected Object show_(SQLRowAccessor r) {

                    Calendar c = r.getDate("REMIND_DATE");
                    if (c == null) {
                        return null;
                    } else {
                        return c.getTime();
                    }

                }

                @Override
                public Set<FieldPath> getPaths() {
                    Path p = new Path(getTable());
                    return CollectionUtils.createSet(new FieldPath(p, "REMIND_DATE"));
                }
            };

            dateRemind.setRenderer(new RemindDateRenderer());
            source.getColumns().add(dateRemind);

        }
    }

    private void setRowActions() {

        AbstractAction action = new AbstractAction("Transférer en client") {

            @Override
            public void actionPerformed(ActionEvent e) {

                final SQLRow row = IListe.get(e).getSelectedRow().asRow();

                SQLRowAccessor foreign = row.getForeign("ID_CLIENT");
                if (foreign == null || foreign.isUndefined()) {

                    // Client
                    SQLRowValues rowVals = SQLInjector.getInjector(row.getTable(), row.getTable().getTable("CLIENT")).createRowValuesFrom(row);

                    SQLRowAccessor adresse = row.getForeign("ID_ADRESSE");
                    if (adresse != null && !adresse.isUndefined()) {
                        SQLRowValues rowValsAdr = new SQLRowValues(adresse.asRowValues());
                        // rowValsAdr.clearPrimaryKeys();
                        rowVals.put("ID_ADRESSE", rowValsAdr);
                    }

                    // Contact
                    SQLRowValues rowValsContact = SQLInjector.getInjector(row.getTable(), row.getTable().getTable("CONTACT")).createRowValuesFrom(row);
                    rowValsContact.put("ID_CLIENT", rowVals);

                    EditFrame frame = new EditFrame(Configuration.getInstance().getDirectory().getElement("CLIENT"), EditMode.CREATION);
                    frame.getSQLComponent().select(rowVals);
                    frame.setVisible(true);

                    frame.addEditPanelListener(new EditPanelListener() {

                        @Override
                        public void modified() {
                        }

                        @Override
                        public void inserted(int id) {
                            SQLRowValues rowVals = row.asRowValues();
                            rowVals.put("ID_CLIENT", id);
                            rowVals.put("STATUS", "Acquis");
                            try {
                                rowVals.commit();
                            } catch (SQLException exn) {
                                // TODO Bloc catch auto-généré
                                exn.printStackTrace();
                            }
                        }

                        @Override
                        public void deleted() {
                        }

                        @Override
                        public void cancelled() {
                        }
                    });
                } else {
                    JOptionPane.showMessageDialog(null, "Ce prospect a déjà été transféré en client!");
                }
            }
        };
        PredicateRowAction transfertClient = new PredicateRowAction(action, true);
        transfertClient.setPredicate(IListeEvent.getSingleSelectionPredicate());
        this.getRowActions().add(transfertClient);

        AbstractAction actionMail = new AbstractAction("Envoyer un e-mail") {

            @Override
            public void actionPerformed(ActionEvent e) {

                sendMail(IListe.get(e).getSelectedRows());

            }
        };
        PredicateRowAction mail = new PredicateRowAction(actionMail, true);
        mail.setPredicate(IListeEvent.getNonEmptySelectionPredicate());
        this.getRowActions().add(mail);
    }

    protected void sendMail(List<SQLRowValues> l) {

        String mail = "";

        // #endif
        for (SQLRowAccessor rowCli : l) {
            String string = rowCli.getString("EMAIL");
            if (string != null && string.trim().length() > 0) {
                mail += string + ";";
            }
        }

        try {
            EmailComposer.getInstance().compose(mail, "", "");
        } catch (Exception exn) {
            ExceptionHandler.handle(null, "Impossible de créer le courriel", exn);
        }

    }

    @Override
    protected String createCode() {
        return ELEMENT_CODE;
    }

    @Override
    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_COMMERCIAL");
        l.add("DATE");
        l.add("COMPANY");
        l.add("LOCALISATION");
        l.add("ID_TITRE_PERSONNEL");
        l.add("NAME");
        l.add("FIRSTNAME");
        l.add("PHONE");
        l.add("MOBILE");
        l.add("EMAIL");
        l.add("SOURCE");
        l.add("INFORMATION");
        l.add("INFOS");
        // l.add("REMIND_DATE");
        return l;
    }

    @Override
    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("COMPANY");
        l.add("FIRSTNAME");
        l.add("NAME");
        return l;
    }

    @Override
    protected List<String> getPrivateFields() {
        final List<String> l = new ArrayList<String>();
        l.add("ID_ADRESSE");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        return ListMap.singleton(null, getComboFields());
    }

    @Override
    public SQLComponent createComponent() {
        final String groupId = this.getCode() + ".default";
        final Group group = GlobalMapper.getInstance().getGroup(groupId);
        if (group == null) {
            throw new IllegalStateException("No group found for id " + groupId);
        }
        return createComponent(group);
    }

    protected SQLComponent createComponent(final Group group) {
        return new LeadSQLComponent(this, group);
    }
}
