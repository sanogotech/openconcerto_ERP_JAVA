package org.openconcerto.modules.label;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.SwingWorker;

import org.openconcerto.erp.generationDoc.provider.AdresseFullClientValueProvider;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.StringUtils;

public final class ModuleLabel extends AbstractModule {

    public ModuleLabel(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {
        final String actionName = "Imprimer les étiquettes";
        final PredicateRowAction aArticle = new PredicateRowAction(new AbstractAction(actionName) {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final IListe list = IListe.get(arg0);
                final List<Integer> selectedIDs = list.getSelection().getSelectedIDs();
                final SwingWorker<List<SQLRowValues>, String> wworker = new SwingWorker<List<SQLRowValues>, String>() {

                    @Override
                    protected List<SQLRowValues> doInBackground() throws Exception {
                        final SQLTable tArticle = list.getSelectedRows().get(0).getTable();
                        final SQLRowValues graph = new SQLRowValues(tArticle);
                        graph.putNulls("NOM", "PV_TTC");
                        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(graph);
                        final List<SQLRowValues> values = fetcher.fetch(new Where(tArticle.getKey(), selectedIDs));
                        return values;
                    }

                    protected void done() {
                        try {
                            final List<SQLRowValues> values = get();

                            final LabelFrame f = new LabelFrame(values, new LabelRenderer() {

                                @Override
                                public void paintLabel(Graphics g, SQLRowAccessor row, int x, int y, int gridWith, int gridHeight, float fontSize) {
                                    g.setColor(Color.BLACK);
                                    g.setFont(g.getFont().deriveFont(fontSize));
                                    // Labels borders
                                    final int hBorder = 12;
                                    final int vBorder = 8;
                                    // Product name
                                    final String text = row.getString("NOM");
                                    final List<String> l = StringUtils.wrap(text, g.getFontMetrics(), gridWith - 2 * hBorder);
                                    final int lineHeight = g.getFontMetrics().getHeight();
                                    int lineY = y;
                                    final int margin = gridHeight - l.size() * lineHeight;
                                    if (margin > 0) {
                                        lineY += (int) (margin / 2);
                                    }
                                    for (String line : l) {
                                        g.drawString(line, x + hBorder, lineY);
                                        lineY += lineHeight;
                                    }
                                    // Price
                                    g.setFont(g.getFont().deriveFont(fontSize + 2));
                                    final String price = GestionDevise.currencyToString(row.getBigDecimal("PV_TTC")) + " € TTC";
                                    final Rectangle2D r2 = g.getFont().getStringBounds(price, g.getFontMetrics().getFontRenderContext());
                                    g.drawString(price, x + (int) (gridWith - hBorder - r2.getWidth()), y + gridHeight - vBorder);

                                }
                            });
                            f.setTitle(actionName);
                            f.setLocationRelativeTo(null);
                            f.pack();
                            f.setResizable(false);
                            f.setVisible(true);
                        } catch (Exception e) {
                            ExceptionHandler.handle("Erreur d'impression", e);
                        }
                    };
                };
                wworker.execute();

            }
        }, true, false);
        final PredicateRowAction aClient = new PredicateRowAction(new AbstractAction(actionName) {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                final IListe list = IListe.get(arg0);
                final List<Integer> selectedIDs = list.getSelection().getSelectedIDs();
                final SwingWorker<List<SQLRowValues>, String> wworker = new SwingWorker<List<SQLRowValues>, String>() {

                    @Override
                    protected List<SQLRowValues> doInBackground() throws Exception {
                        final SQLTable tClient = list.getSelectedRows().get(0).getTable();
                        final SQLRowValues graph = new SQLRowValues(tClient);
                        graph.putNulls("NOM");
                        final SQLRowValues a1 = graph.putRowValues("ID_ADRESSE");
                        a1.putNulls(a1.getTable().getFieldsName());
                        final SQLRowValues a2 = graph.putRowValues("ID_ADRESSE_L");
                        a2.putNulls(a2.getTable().getFieldsName());
                        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(graph);
                        final List<SQLRowValues> values = fetcher.fetch(new Where(tClient.getKey(), selectedIDs));
                        return values;
                    }

                    protected void done() {
                        try {
                            final List<SQLRowValues> values = get();
                            final LabelFrame f = new LabelFrame(values, new LabelRenderer() {

                                @Override
                                public void paintLabel(Graphics g, SQLRowAccessor row, int x, int y, int gridWith, int gridHeight, float fontSize) {
                                    SQLRowAccessor rAddr = row.getForeign("ID_ADRESSE_L");
                                    if (rAddr == null || rAddr.isUndefined()) {
                                        rAddr = row.getForeign("ID_ADRESSE");
                                    }
                                    if (rAddr == null || rAddr.isUndefined()) {
                                        return;
                                    }

                                    String text = AdresseFullClientValueProvider.getFormattedAddress(rAddr, row.getString("NOM") + "\n");

                                    // Default font at 10pt black
                                    g.setColor(Color.BLACK);
                                    g.setFont(g.getFont().deriveFont(fontSize));
                                    // Labels borders
                                    final int hBorder = 12;
                                    // Product name
                                    final List<String> l = StringUtils.wrap(text, g.getFontMetrics(), gridWith - 2 * hBorder);
                                    final int lineHeight = g.getFontMetrics().getHeight();
                                    int lineY = y + lineHeight + 4;
                                    for (String line : l) {
                                        g.drawString(line, x + hBorder, lineY);
                                        lineY += lineHeight;
                                    }

                                }
                            });
                            f.setTitle(actionName);
                            f.setLocationRelativeTo(null);
                            f.pack();
                            f.setResizable(false);
                            f.setVisible(true);

                        } catch (Exception e) {
                            ExceptionHandler.handle("Erreur d'impression", e);
                        }
                    };
                };
                wworker.execute();

            }
        }, true, false);

        aArticle.setPredicate(IListeEvent.createSelectionCountPredicate(1, Integer.MAX_VALUE));
        aClient.setPredicate(IListeEvent.createSelectionCountPredicate(1, Integer.MAX_VALUE));
        ctxt.getElement("ARTICLE").getRowActions().add(aArticle);
        ctxt.getElement("CLIENT").getRowActions().add(aClient);
    }

    @Override
    protected void start() {
    }

    @Override
    protected void stop() {
    }
}
