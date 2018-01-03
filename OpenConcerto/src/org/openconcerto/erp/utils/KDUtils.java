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
 
 package org.openconcerto.erp.utils;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement.Month;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.utils.OSFamily;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KDUtils {
    private static final Map<Integer, String> map = new HashMap<Integer, String>();

    public static enum Folder {
        RAPPORT_SIGNE("Rapports signés.IDs"), RAPPORT_FORMULAIRE("Rapports formulaires.IDs");
        String folderName;

        private Folder(String folderName) {
            this.folderName = folderName;
        }

        public String getFolderName() {
            return this.folderName;
        }
    };

    static {
        // Société utilisant le système de classement
        map.put(54, "KD");
        map.put(55, "CTP");
        map.put(44, "NONNENMACHER");

    }

    public static String getExtranetClientDirectory(SQLRowAccessor rowClient) {

        // Dossier davs://user@groupe-cadet.fr/webdav
        final boolean windows = System.getProperty("os.name").startsWith("Windows");
        String path;
        String stringClient = rowClient.getString("NOM") + " [" + rowClient.getString("SIRET") + "]";
        if (!windows) {
            String username = System.getProperty("user.name");
            path = "davs://" + username + "@groupe-cadet.fr/webdav/extranet/" + stringClient;
        } else {
            try {
                path = new URI("https", "groupe-cadet.fr/webdav/extranet/" + stringClient, null).toString();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                path = "";
            }
        }

        return path;

    }

    public static File getRapportDirectory() {

        int idSociete = ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getID();

        if (!map.containsKey(idSociete)) {
            throw new IllegalArgumentException("Cette société n'est pas prise en charge dans le système de classement automatique des rapports");
        }

        // Récupération du dossier RAPPORTS en fonction du système (Windows (X|Y|Z):/RAPPORTS ;
        // Linux /mnt/username/Serveur/RAPPORTS)
        String path = null;
        if (OSFamily.getInstance() == OSFamily.Windows) {
            // TODO call "net use | grep -F '\\kdfs.alphacadet.fr\Serveur'"
            File f = new File("Z:" + File.separator + "RAPPORTS");
            if (f.exists()) {
                path = f.getAbsolutePath();
            } else {
                f = new File("Y:" + File.separator + "RAPPORTS");
                if (f.exists()) {
                    path = f.getAbsolutePath();
                } else {
                    f = new File("X:" + File.separator + "RAPPORTS");
                    if (f.exists()) {
                        path = f.getAbsolutePath();
                    }
                }
            }
            if (path == null) {
                throw new IllegalArgumentException("Impossible de trouver le dossier RAPPORTS. Vérifiez que le serveur est bien sur une des lettres suivantes (X,Y ou Z).");
            }
        } else {
            String username = System.getProperty("user.name");
            File f = new File(File.separator + "mnt" + File.separator + username + "-ILM-server" + File.separator + "RAPPORTS");
            path = f.getAbsolutePath();
        }

        return new File(path + File.separator + map.get(idSociete));

    }

    public static List<File> getDevisFolders(SQLRowAccessor rowDevis, Folder folder) {
        File root = new File(getRapportDirectory(), folder.getFolderName());

        final Number idClient = rowDevis.getForeign("ID_CLIENT").getForeignIDNumber("ID_CLIENT");

        // Récupération du dossier client

        File[] clientFolders = { new File(root, String.valueOf(idClient)) };

        // Recherche du dossier associé au devis
        Calendar date = rowDevis.getDate("DATE");
        final String numero = rowDevis.getString("NUMERO");

        List<File> foldersToOpen = new ArrayList<File>();
        final int devisYear = date.get(Calendar.YEAR);
        for (File clientFolder : clientFolders) {
            // System.err.println(clientFolder.getAbsolutePath());
            for (int year = devisYear; year <= devisYear + 1; year++) {
                for (Month m : Month.values()) {
                    File testFolder = new File(clientFolder, year + File.separator + m.getPath() + File.separator + numero);
                    if (testFolder.exists()) {
                        foldersToOpen.add(testFolder);
                    }
                }
            }
        }

        if (foldersToOpen.isEmpty()) {
            throw new IllegalArgumentException("Aucun dossier associé au devis n'a été trouvé!");
        }

        return foldersToOpen;
    }

}
