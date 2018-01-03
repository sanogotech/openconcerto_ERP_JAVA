/*
 * Créé le 6 nov. 2012
 */
package org.openconcerto.modules.customerrelationship.lead;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.common.ui.IListFilterDatePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.BaseSQLTableModelColumn;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.PanelFrame;
import org.openconcerto.utils.CollectionUtils;

public class LeadListAction extends CreateFrameAbstractAction {

    private SQLElement eltProspect = Configuration.getInstance().getDirectory().getElement(Module.TABLE_LEAD);

    @Override
    public JFrame createFrame() {
        PanelFrame frame = new PanelFrame(getPanel(), "Liste des prospects");
        return frame;
    }

    public LeadListAction() {
        super("Liste des propects");
    }

    public JPanel getPanel() {

        JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;

        JTabbedPane tabbedPane = new JTabbedPane();

        // Tous
        Map<IListe, SQLField> listeFilter = new HashMap<IListe, SQLField>();
        ListeAddPanel panelAll = createPanel(null);
        listeFilter.put(panelAll.getListe(), panelAll.getListe().getModel().getTable().getField("DATE"));

        tabbedPane.add("Tous", panelAll);

        // Sans statut
        ListeAddPanel panelEmpty = createPanel("");
        listeFilter.put(panelEmpty.getListe(), panelEmpty.getListe().getModel().getTable().getField("DATE"));

        tabbedPane.add("Sans statut", panelEmpty);

        SQLSelect selStatus = new SQLSelect();
        selStatus.addSelect(this.eltProspect.getTable().getField("STATUS"));
        selStatus.addGroupBy(this.eltProspect.getTable().getField("STATUS"));
        selStatus.addFieldOrder(this.eltProspect.getTable().getField("STATUS"));

        System.err.println(selStatus.asString());

        List<String> listStatus = Configuration.getInstance().getBase().getDataSource().executeCol(selStatus.asString());

        // Date panel
        for (String status : listStatus) {
            if (status != null && status.trim().length() > 0) {
                ListeAddPanel p = createPanel(status);
                listeFilter.put(p.getListe(), p.getListe().getModel().getTable().getField("DATE"));
                tabbedPane.add(status, p);
            }
        }

        // final SQLElement eltDevis =
        // Configuration.getInstance().getDirectory().getElement(this.eltProspect.getTable().getTable("DEVIS"));
        // if (eltDevis.getTable().getTable("CLIENT").contains("PROSPECT")) {
        // // Filter
        // final SQLTableModelSourceOnline lAttente = eltDevis.getTableSource(true);
        // if (eltDevis.getTable().contains("REMIND_DATE")) {
        // BaseSQLTableModelColumn dateRemind = new BaseSQLTableModelColumn("Date de rappel",
        // Date.class) {
        //
        // @Override
        // protected Object show_(SQLRowAccessor r) {
        //
        // Calendar c = r.getDate("REMIND_DATE");
        // if (c == null) {
        // return null;
        // } else {
        // return c.getTime();
        // }
        //
        // }
        //
        // @Override
        // public Set<FieldPath> getPaths() {
        // Path p = new Path(eltDevis.getTable());
        // return CollectionUtils.createSet(new FieldPath(p, "REMIND_DATE"));
        // }
        // };
        //
        // dateRemind.setRenderer(new RemindDateRenderer());
        // lAttente.getColumns().add(dateRemind);
        //
        // }
        // lAttente.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
        //
        // @Override
        // public SQLSelect transformChecked(SQLSelect input) {
        // Where wAttente = new Where(input.getTable("DEVIS").getField("ID_ETAT_DEVIS"), "=",
        // EtatDevisSQLElement.EN_ATTENTE);
        // wAttente = wAttente.and(new
        // Where(input.getAlias(eltDevis.getTable().getTable("CLIENT")).getField("PROSPECT"), "=",
        // Boolean.TRUE));
        //
        // User user = UserManager.getInstance().getCurrentUser();
        // if (!user.getRights().haveRight(LeadSQLElement.CODE_NOT_RESTRICT)) {
        // SQLRow row =
        // Configuration.getInstance().getRoot().findTable("USER_COMMON").getRow(UserManager.getInstance().getCurrentUser().getId());
        // List<SQLRow> rows =
        // row.getReferentRows(Configuration.getInstance().getRoot().findTable("COMMERCIAL").getField("ID_USER_COMMON"));
        // final List<Integer> listComm = new ArrayList<Integer>();
        // for (SQLRow sqlRow : rows) {
        // listComm.add(sqlRow.getID());
        // }
        // if (listComm != null && listComm.size() > 0) {
        // SQLField field = input.getTable("DEVIS").getField("ID_COMMERCIAL");
        // Where w = new Where(field, listComm);
        // w = w.or(new Where(field, "IS", (Object) null));
        // w = w.or(new Where(field, "=",
        // eltDevis.getTable().getTable("COMMERCIAL").getUndefinedID()));
        // wAttente = wAttente.and(w);
        // }
        // }
        //
        // input.setWhere(wAttente);
        // return input;
        // }
        // });
        //
        // BaseSQLTableModelColumn mb = new BaseSQLTableModelColumn("Informations commerciales",
        // String.class) {
        //
        // @Override
        // protected Object show_(SQLRowAccessor r) {
        //
        // return r.getString("INFOS_COM");
        // }
        //
        // @Override
        // public Set<FieldPath> getPaths() {
        //
        // Path p = new Path(eltDevis.getTable());
        //
        // return Collections.singleton(new FieldPath(p, "INFOS_COM"));
        // }
        // };
        //
        // lAttente.getColumns().add(mb);
        //
        // final ListeAddPanel paneDevis = new ListeAddPanel(eltDevis, new IListe(lAttente),
        // "Prospect");
        // tabbedPane.add("Devis en attente", paneDevis);
        // }
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(tabbedPane, c);
        IListFilterDatePanel panelDate = new IListFilterDatePanel(listeFilter, IListFilterDatePanel.getDefaultMap());

        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 0;
        c.gridy++;
        panel.add(panelDate, c);

        return panel;
    }

    private ListeAddPanel createPanel(String status) {
        // Filter
        final SQLTableModelSourceOnline source = this.eltProspect.getTableSource(true);

        if (status != null) {
            Where wAttente = new Where(this.eltProspect.getTable().getField("STATUS"), "=", status);
            source.getReq().setWhere(wAttente);
        } else {
            source.getColumns().add(new BaseSQLTableModelColumn("Transférer en client", Boolean.class) {
                @Override
                protected Object show_(SQLRowAccessor r) {
                    return (r.getForeign("ID_CLIENT") != null && !r.isForeignEmpty("ID_CLIENT"));
                }

                @Override
                public Set<FieldPath> getPaths() {

                    Path p = new Path(eltProspect.getTable());
                    return CollectionUtils.createSet(new FieldPath(p, "ID_CLIENT"));
                }
            });
        }
        final ListeAddPanel pane = new ListeAddPanel(this.eltProspect, new IListe(source), "Status" + status);
        pane.getListe().setOpaque(false);
        pane.setOpaque(false);
        return pane;
    }
}
