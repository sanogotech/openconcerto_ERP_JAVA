package org.openconcerto.modules.reports.olap;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.olap4j.OlapConnection;
import org.olap4j.metadata.Schema;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;

public class OLAPPanel extends JPanel {
    public OLAPPanel() {
        this.setOpaque(false);
        this.setLayout(new GridLayout(1, 1));

        reload();

    }

    public void reload() {
        this.removeAll();
        this.invalidate();
        try {
            final File dir = new File(ComptaPropsConfiguration.getConfFile(ComptaPropsConfiguration.productInfo).getParent());
            final File f = new File(dir, "openconcerto_catalog.xml");
            String catalog = "empty";
            if (f.exists()) {
                System.out.println("OLAP: using catalog " + f.getAbsolutePath());
                catalog = FileUtils.read(f, "UTF8");
            } else {
                System.out.println("OLAP: using embedded catalog (" + f.getAbsolutePath() + " missing)");
                final InputStream in = this.getClass().getResourceAsStream("openconcerto_catalog.xml");
                catalog = FileUtils.readUTF8(in);
            }
            final String url = getOlapURL(ComptaPropsConfiguration.getInstanceCompta(), catalog);
            final Connection rConnection = DriverManager.getConnection(url);

            final OlapConnection oConnection = rConnection.unwrap(OlapConnection.class);

            final Schema schema = oConnection.getOlapSchema();
            final OLAPMainPanel p = new OLAPMainPanel(oConnection, schema, this);
            this.add(p);
        } catch (Exception e) {
            final JPanel p = new JPanel();
            p.setOpaque(false);
            p.setLayout(new FlowLayout(FlowLayout.LEFT));
            final JButton comp = new JButton("Recharger la configuration");
            p.add(comp);
            this.add(p);
            ExceptionHandler.handle("OLAP init error", e);
            comp.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    reload();
                }
            });
        }

        this.revalidate();

    }

    private String getOlapURL(ComptaPropsConfiguration instance, String catalog) {

        String url = null;
        try {
            final SQLDataSource dataSource = instance.getSystemRoot().getDataSource();
            File file = File.createTempFile("openconcerto", ".xml");
            catalog = catalog.replace("_SCHEMA_", instance.getSocieteBaseName());
            FileUtils.write(catalog, file);
            url = "jdbc:mondrian:JdbcDrivers=" + dataSource.getDriverClassName() + ";Jdbc=" + dataSource.getUrl() + "?user=" + dataSource.getUsername() + "&password=" + dataSource.getPassword()
                    + ";Catalog=file:";

            url += file.getCanonicalPath().replace('\\', '/') + ";";
            System.out.println("OLAPPanel.getOlapURL():" + url);
        } catch (Exception e) {
            ExceptionHandler.handle("OLAP configuration issue", e);
        }
        return url;
    }
}
