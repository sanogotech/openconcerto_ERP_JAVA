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
 
 package org.openconcerto.erp.core.edm;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.generationDoc.DocumentLocalStorageManager;
import org.openconcerto.erp.storage.StorageEngine;
import org.openconcerto.erp.storage.StorageEngines;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.sync.SyncClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

public class AttachmentUtils {

    public void uploadFile(File inFile, SQLRowAccessor rowSource) {
        try {

            // Création de la row attachment
            SQLRowValues rowValsAttachment = new SQLRowValues(rowSource.getTable().getTable("ATTACHMENT"));
            rowValsAttachment.put("SOURCE_TABLE", rowSource.getTable().getName());
            rowValsAttachment.put("SOURCE_ID", rowSource.getID());

            SQLRow rowAttachment = rowValsAttachment.insert();
            int id = rowAttachment.getID();

            String subDir = "EDM" + File.separator + String.valueOf((id / 1000) * 1000);
            String fileNameID = String.valueOf(id);
            String ext = "";

            int i = inFile.getName().lastIndexOf('.');
            if (i > 0) {
                ext = inFile.getName().substring(i + 1);
            }

            final String fileWithIDNAme = fileNameID + "." + ext;

            final ComptaPropsConfiguration config = ComptaPropsConfiguration.getInstanceCompta();
            boolean isOnCloud = config.isOnCloud();

            if (isOnCloud) {

                String remotePath = subDir;
                remotePath = remotePath.replace('\\', '/');
                List<StorageEngine> engines = StorageEngines.getInstance().getActiveEngines();
                for (StorageEngine storageEngine : engines) {
                    if (storageEngine.isConfigured() && storageEngine.allowAutoStorage()) {
                        final String path = remotePath;
                        try {
                            storageEngine.connect();
                            final BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(inFile));
                            storageEngine.store(inStream, path, fileWithIDNAme, true);
                            inStream.close();
                            storageEngine.disconnect();
                        } catch (IOException e) {
                            ExceptionHandler.handle("Impossible de sauvegarder le fichier " + inFile.getAbsolutePath() + " vers " + path + "(" + storageEngine + ")", e);
                        }
                        // if (storageEngine instanceof CloudStorageEngine) {
                        // try {
                        // storageEngine.connect();
                        // final BufferedInputStream inStream = new BufferedInputStream(new
                        // FileInputStream(generatedFile));
                        // storageEngine.store(inStream, path, generatedFile.getName(), true);
                        // inStream.close();
                        // storageEngine.disconnect();
                        // } catch (IOException e) {
                        // ExceptionHandler.handle("Impossible de sauvegarder le fichier généré " +
                        // generatedFile.getAbsolutePath() + " vers " + path + "(" + storageEngine +
                        // ")", e);
                        // }
                        // }
                    }
                }
            } else {
                // Upload File

                // Get file out
                File dirRoot = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(AttachmentSQLElement.DIRECTORY_PREFS);
                File storagePathFile = new File(dirRoot, subDir);
                storagePathFile.mkdirs();
                // TODO CHECK IF FILE EXISTS
                FileUtils.copyFile(inFile, new File(storagePathFile, fileWithIDNAme));

            }

            // Update rowAttachment
            rowValsAttachment = rowAttachment.createEmptyUpdateRow();

            // Default is without extension
            String fileName = inFile.getName();
            String name = fileName;
            int index = name.lastIndexOf('.');
            if (index > 0) {
                name = name.substring(0, index);
            }
            rowValsAttachment.put("NAME", name);
            rowValsAttachment.put("SOURCE_TABLE", rowSource.getTable().getName());
            rowValsAttachment.put("SOURCE_ID", rowSource.getID());
            Collection<MimeType> mimeTypes = MimeUtil.getMimeTypes(inFile);
            if (mimeTypes != null && !mimeTypes.isEmpty()) {
                final MimeType mimeType = (MimeType) mimeTypes.toArray()[0];
                rowValsAttachment.put("MIMETYPE", mimeType.getMediaType() + "/" + mimeType.getSubType());
            } else {
                rowValsAttachment.put("MIMETYPE", "application/octet-stream");
            }
            rowValsAttachment.put("FILENAME", fileName);
            rowValsAttachment.put("FILESIZE", inFile.length());
            rowValsAttachment.put("STORAGE_PATH", subDir);
            rowValsAttachment.put("STORAGE_FILENAME", fileWithIDNAme);
            // TODO THUMBNAIL
            // rowVals.put("THUMBNAIL", );
            // rowVals.put("THUMBNAIL_WIDTH", );
            // rowVals.put("THUMBNAIL_HEIGHT", );

            // needed for update count

            rowValsAttachment.commit();

            updateAttachmentsCountFromAttachment(rowValsAttachment);
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }

    public File getFile(SQLRowAccessor rowAttachment) {

        final ComptaPropsConfiguration config = ComptaPropsConfiguration.getInstanceCompta();
        boolean isOnCloud = config.isOnCloud();

        String subDir = rowAttachment.getString("STORAGE_PATH");
        String fileName = rowAttachment.getString("STORAGE_FILENAME");

        String remotePath = config.getSocieteID() + File.separator + subDir;

        File fTemp;
        try {
            fTemp = File.createTempFile("edm_", "oc");
        } catch (IOException e) {
            ExceptionHandler.handle("Impossible de créer le fichier temporaire de réception", e);
            return null;
        }
        File f = new File(fTemp.getParent(), fTemp.getName() + "-dir");
        f.mkdirs();
        fTemp.delete();

        if (isOnCloud) {
            remotePath = remotePath.replace('\\', '/');
            final SyncClient client = new SyncClient("https://" + config.getStorageServer());

            client.setVerifyHost(false);

            try {
                client.retrieveFile(f, remotePath, fileName, config.getToken());
            } catch (Exception e) {
                ExceptionHandler.handle("Impossible de récupérer le fichier depuis le cloud", e);
                return null;
            }

        } else {

            // Get file out
            File dirRoot = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(AttachmentSQLElement.DIRECTORY_PREFS);
            File storagePathFile = new File(dirRoot, subDir);
            File fileIn = new File(storagePathFile, fileName);
            if (fileIn.exists()) {
                final File outFile = new File(f, fileName);
                try {
                    FileUtils.copyFile(fileIn, outFile);
                } catch (IOException e) {
                    ExceptionHandler.handle("Impossible de copier le fichier vers le fichier temporaire de réception", e);
                    return null;
                }
            } else {
                JOptionPane.showMessageDialog(null, "Le fichier n'existe pas sur le serveur!", "Erreur fichier", JOptionPane.ERROR_MESSAGE);
            }
        }
        final File outFile = new File(f, fileName);
        outFile.setReadOnly();
        return outFile;

    }

    public void deleteFile(SQLRowAccessor rowAttachment) throws SQLException, IllegalStateException {

        final ComptaPropsConfiguration config = ComptaPropsConfiguration.getInstanceCompta();
        boolean isOnCloud = config.isOnCloud();

        // Delete File
        String subDir = rowAttachment.getString("STORAGE_PATH");
        String fileName = rowAttachment.getString("STORAGE_FILENAME");

        String remotePath = config.getSocieteID() + File.separator + subDir;
        if (isOnCloud) {
            remotePath = remotePath.replace('\\', '/');
            // final SyncClient client = new SyncClient("https://" + config.getStorageServer());
            //
            // client.setVerifyHost(false);

            // TODO DELETE FILE ON CLOUD OR RENAME?
            // client.retrieveFile(f, remotePath, fileName, config.getToken());
        } else {

            File dirRoot = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(AttachmentSQLElement.DIRECTORY_PREFS);
            File storagePathFile = new File(dirRoot, subDir);
            File f = new File(storagePathFile, fileName);
            if (f.exists()) {
                if (!f.delete()) {
                    throw new IllegalStateException("Une erreur est survenue lors de la suppression du fichier");
                }
            }
        }

        // Delete Row
        config.getDirectory().getElement(rowAttachment.getTable()).archive(rowAttachment.getID());
        updateAttachmentsCountFromAttachment(rowAttachment);
    }

    public void updateAttachmentsCountFromAttachment(SQLRowAccessor rowAttachment) {
        SQLTable table = rowAttachment.getTable().getTable(rowAttachment.getString("SOURCE_TABLE"));
        SQLRow source = table.getRow(rowAttachment.getInt("SOURCE_ID"));
        updateAttachmentsCountFromSource(source);
    }

    public void updateAttachmentsCountFromSource(SQLRow rowSource) {
        SQLTable tableSource = rowSource.getTable();
        SQLTable tableAtt = rowSource.getTable().getTable("ATTACHMENT");

        String req = "UPDATE " + tableSource.getSQLName().quote() + " SET " + tableSource.getField("ATTACHMENTS").getQuotedName() + "=(SELECT COUNT(*) FROM " + tableAtt.getSQLName().quote();
        req += " WHERE " + tableAtt.getArchiveField().getQuotedName() + "=0 AND " + tableAtt.getField("SOURCE_TABLE").getQuotedName() + "='" + tableSource.getName() + "'";
        req += " AND " + tableAtt.getField("SOURCE_ID").getQuotedName() + "=" + rowSource.getID() + ") WHERE " + tableSource.getKey().getQuotedName() + "=" + rowSource.getID();

        tableSource.getDBSystemRoot().getDataSource().execute(req);
    }

}
