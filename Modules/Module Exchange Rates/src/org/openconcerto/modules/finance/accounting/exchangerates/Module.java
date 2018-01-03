package org.openconcerto.modules.finance.accounting.exchangerates;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.JOptionPane;

import org.apache.commons.dbutils.ResultSetHandler;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.core.finance.accounting.model.CurrencyConverter;
import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.DBContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.erp.modules.ModuleManager;
import org.openconcerto.erp.modules.ModulePackager;
import org.openconcerto.erp.modules.ModulePreferencePanel;
import org.openconcerto.erp.modules.ModulePreferencePanelDesc;
import org.openconcerto.erp.modules.ModuleVersion;
import org.openconcerto.erp.modules.RuntimeModuleFactory;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRequestLog;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.ui.ConnexionPanel;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.PrefType;

public final class Module extends AbstractModule {

    public static final String TABLE_EXPENSE = "EXPENSE";
    public static final String TABLE_EXPENSE_STATE = "EXPENSE_STATE";
    public static final String EXPENSE_RATE_NOT_DOWNLOAD_PREF = "EXPENSE_RATE_NOT_DOWNLOAD_PREF";
    public static ModuleVersion MODULE_VERSION = new ModuleVersion(1, 0);
    private CurrencyConverter commercialConverter = null;

    public Module(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void install(DBContext ctxt) {

    }

    public void setCommercialConverter(CurrencyConverter commercialConverter) {
        this.commercialConverter = commercialConverter;
    }

    @Override
    protected void start() {
        final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();
        Boolean b = ModuleManager.getInstance().getFactories().get("org.openconcerto.modules.finance.accounting.exchangerates").get(Module.MODULE_VERSION).getSQLPreferences(root)
                .getBoolean(EXPENSE_RATE_NOT_DOWNLOAD_PREF, false);
        if (!b) {
            final Thread t = new Thread() {
                public void run() {
                    // Wait 1s to prevent too many internet access at startup
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    updateRates(root, ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getForeign("ID_DEVISE").getString("CODE"));
                };
            };
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);
            t.start();
        }

    }

    public void updateRates(DBRoot root, String companyCurrencyCode) {
        // Check if update need to be done
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (c.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            c.set(Calendar.HOUR, -72);
        } else {
            c.add(Calendar.HOUR, -24);
        }
        final Date yesterday = c.getTime();

        final CurrencyConverter converter = new CurrencyConverter(root, companyCurrencyCode, "EUR");
        final SQLSelect select = new SQLSelect();
        final SQLTable t = root.getTable("DEVISE_HISTORIQUE");
        select.addAllSelect(t, Arrays.asList("DATE", "SRC", "ID"));
        Where w = new Where(t.getField("SRC"), "=", "EUR");
        w = w.and(new Where(t.getField("DATE"), "=", yesterday));
        select.setWhere(w);
        select.setLimit(1);
        final List<SQLRow> rows = SQLRowListRSH.execute(select);
        if (!rows.isEmpty()) {
            // Nothing to update
            System.out.println("No exchange rates to download");
            return;
        }
        // Get conversion info
        final ExchangeRatesDownloader d = new ExchangeRatesDownloader();
        try {
            d.downloadAndParse();
            Set<Date> dates = getMissingDates(converter, root);
            System.out.println("Missing date:" + dates);
            List<Report> reports = d.getReports();

            for (Report report : reports) {
                if (dates.contains(report.getDate())) {
                    importReport(d, report, converter, root);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Impossible de mettre à jour les taux de change");
        }
    }

    @Override
    public List<ModulePreferencePanelDesc> getPrefDescriptors() {
        return Arrays.<ModulePreferencePanelDesc> asList(new ModulePreferencePanelDesc("Gestion des devises") {
            @Override
            protected ModulePreferencePanel createPanel() {
                return new ModulePreferencePanel("Gestion des devises") {
                    @Override
                    protected void addViews() {

                        final SQLPrefView<Boolean> view = new SQLPrefView<Boolean>(PrefType.BOOLEAN_TYPE, "Ne pas télécharger les nouveaux taux au lancement du logiciel",
                                EXPENSE_RATE_NOT_DOWNLOAD_PREF);
                        this.addView(view);
                    }
                };
            }
        }.setLocal(false).setKeywords("devise"));
    }

    protected void importReport(ExchangeRatesDownloader d, Report report, CurrencyConverter converter, DBRoot root) {

        System.out.println("Module.importReport() " + report.getDate());

        SQLTable tDeviseHisto = root.getTable("DEVISE_HISTORIQUE");

        final List<String> queries = new ArrayList<String>();
        final List<ResultSetHandler> handlers = new ArrayList<ResultSetHandler>();
        final ResultSetHandler handler = new ResultSetHandler() {

            @Override
            public Object handle(ResultSet rs) throws SQLException {
                if (rs.next()) {
                    return rs.getObject(1);
                }
                return null;
            }
        };
        Date date = Calendar.getInstance().getTime();
        final int size = ExchangeRatesDownloader.supportedCurrencyCodes.size();
        for (int i = 0; i < size; i++) {
            String code = ExchangeRatesDownloader.supportedCurrencyCodes.get(i);
            BigDecimal tauxCommercial;
            if (this.commercialConverter != null) {
                tauxCommercial = commercialConverter.convert(BigDecimal.ONE, report.getMainCurrencyCode(), code, date, true);
            } else {
                tauxCommercial = converter.convert(BigDecimal.ONE, report.getMainCurrencyCode(), code, date, true);
            }
            if (tauxCommercial == null) {
                tauxCommercial = BigDecimal.ONE;
            }
            tauxCommercial = tauxCommercial.setScale(4, BigDecimal.ROUND_HALF_UP);
            String query = "INSERT INTO " + tDeviseHisto.getSQLName().quote();
            final BigDecimal rate = report.getRate(code);
            if (rate == null) {
                System.err.println("RATE " + code + " NOT Supported");
                // Thread.dumpStack();
            } else {
                final BigDecimal taux = rate.setScale(4, BigDecimal.ROUND_HALF_UP);

                final java.sql.Date date2 = new java.sql.Date(report.getDate().getTime());
                query += " (\"DATE\", \"SRC\", \"DST\", \"TAUX\", \"TAUX_COMMERCIAL\", \"ORDRE\") select " + SQLBase.quoteStringStd(date2.toString()) + ", "
                        + SQLBase.quoteStringStd(report.getMainCurrencyCode()) + ", " + SQLBase.quoteStringStd(code) + ", " + taux + ", " + tauxCommercial + ", COALESCE(MAX(\"ORDRE\"), 0) + 1 ";
                query += "FROM " + tDeviseHisto.getSQLName().quote() + " RETURNING \"ID\"";

                queries.add(query);
                handlers.add(handler);
            }
        }

        try {
            SQLUtils.executeMultiple(tDeviseHisto.getDBSystemRoot(), queries, handlers);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected Set<Date> getMissingDates(CurrencyConverter converter, DBRoot root) {
        final SQLSelect select = new SQLSelect();

        final SQLTable t = root.getTable("DEVISE_HISTORIQUE");
        select.addAllSelect(t, Arrays.asList("DATE"));
        Where w = new Where(t.getField("SRC"), "=", converter.getCompanyCurrencyCode());
        w = w.and(new Where(t.getField("DATE"), "<", new Date(System.currentTimeMillis())));
        select.setWhere(w);
        select.addGroupBy(t.getField("DATE"));
        select.setLimit(30);
        final List<Date> dates = t.getDBSystemRoot().getDataSource().executeCol(select.asString());

        final Set<Date> result = new HashSet<Date>();
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < 20; i++) {
            c.add(Calendar.HOUR_OF_DAY, -24);
            final Date time = c.getTime();
            System.out.println("::" + time);
            result.add(time);
        }

        for (Date date : dates) {
            Calendar c2 = Calendar.getInstance();
            c2.setTime(date);
            c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.set(Calendar.YEAR, c2.get(Calendar.YEAR));
            c.set(Calendar.DAY_OF_YEAR, c2.get(Calendar.DAY_OF_YEAR));
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            Date dd = c.getTime();
            result.remove(dd);
            System.out.println("--" + dd);
        }
        System.out.println(result.size());

        return result;
    }

    @Override
    protected void stop() {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty(ConnexionPanel.QUICK_LOGIN, "true");
        final File propsFile = new File("module.properties");
        System.out.println(propsFile.getAbsolutePath());
        final ModuleFactory factory = new RuntimeModuleFactory(propsFile);
        SQLRequestLog.setEnabled(true);
        SQLRequestLog.showFrame();
        // uncomment to create and use the jar
        final ModulePackager modulePackager = new ModulePackager(propsFile, new File("bin/"));
        modulePackager.writeToDir(new File("../OpenConcerto/Modules"));
        // final ModuleFactory factory = new JarModuleFactory(jar);
        ModuleManager.getInstance().addFactories(new File("../OpenConcerto/Modules"));
        ModuleManager.getInstance().addFactoryAndStart(factory, false);
        Gestion.main(args);
    }

}
