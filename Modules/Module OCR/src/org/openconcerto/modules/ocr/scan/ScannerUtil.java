package org.openconcerto.modules.ocr.scan;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JOptionPane;

import org.openconcerto.modules.ocr.TesseractUtils;

public class ScannerUtil {
    public static int MODE_ICOPY = 0;
    public static int MODE_NAPS2 = 1;
    public static int MODE_SANE = 2;

    static public boolean scanTo(File directory, int mode) throws Exception {

        Boolean result = true;

        List<String> commands = new ArrayList<String>();
        List<File> filesScan = null;

        Process p = null;

        File imageTIFFPrinterDirectory = new File(directory, "Printer");

        if (!createDirectory(imageTIFFPrinterDirectory)) {

            if (mode == MODE_ICOPY) {
                p = Runtime.getRuntime().exec("icopy/iCopy.exe /file /silent /path \"" + imageTIFFPrinterDirectory.getCanonicalPath() + "\"");
                p.waitFor();
                result = (p.exitValue() == 0);
            } else if (mode == MODE_NAPS2) {
                p = Runtime.getRuntime().exec("naps2/NAPS2.Console.exe -o \"" + imageTIFFPrinterDirectory.getCanonicalPath() + "\" -p wia");
                p.waitFor();
                result = (p.exitValue() == 0);
            } else if (mode == MODE_SANE) {
                String output = "";
                Integer nbFileScan = 0;

                commands.add("scanimage");
                commands.add("--batch");
                commands.add("--format=tiff");
                commands.add("--resolution");
                commands.add("300");
                commands.add("--mode");
                commands.add("Gray");
                commands.add("--source");
                commands.add("adf");

                output = TesseractUtils.runProcess(commands, imageTIFFPrinterDirectory);

                String[] split = output.split("\\s+");
                for (int i = 0; i < split.length; i++) {
                    if (split[i].equals("Scanned")) {
                        nbFileScan++;
                    }
                }

                if (nbFileScan.equals(0)) {
                    JOptionPane.showConfirmDialog(null, "Aucun document n'a été scanné", "Alert", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showConfirmDialog(null, nbFileScan.toString() + " documents ont été scannés", "Information", JOptionPane.DEFAULT_OPTION);
                }

                filesScan = scanDirectory(imageTIFFPrinterDirectory);

                for (int i = 0; i < filesScan.size(); i++) {
                    File fileScan = filesScan.get(i);

                    Calendar c = Calendar.getInstance();
                    Integer year = c.get(Calendar.YEAR);
                    Integer month = c.get(Calendar.MONTH) + 1;
                    Integer day = c.get(Calendar.DATE);
                    Integer hour = c.get(Calendar.HOUR);
                    Integer minute = c.get(Calendar.MINUTE);
                    Integer second = c.get(Calendar.SECOND);
                    Integer millis = c.get(Calendar.MILLISECOND);
                    Integer counter = 0;

                    String finalName = TesseractUtils.padLeft(year.toString(), 4, '0') + TesseractUtils.padLeft(month.toString(), 2, '0') + TesseractUtils.padLeft(day.toString(), 2, '0')
                            + TesseractUtils.padLeft(hour.toString(), 2, '0') + TesseractUtils.padLeft(minute.toString(), 2, '0') + TesseractUtils.padLeft(second.toString(), 2, '0')
                            + TesseractUtils.padLeft(millis.toString(), 3, '0') + "_" + TesseractUtils.padLeft(counter.toString(), 3, '0') + ".tiff";

                    File outputFile = new File(directory, finalName);

                    while (outputFile.exists()) {
                        counter++;
                        finalName = finalName.substring(0, finalName.length() - 8) + TesseractUtils.padLeft(counter.toString(), 3, '0') + ".tiff";

                        outputFile = new File(directory, finalName);
                    }

                    if (fileScan.renameTo(new File(directory, finalName))) {
                        System.out.println("File is moved successful!");
                    } else {
                        result = Boolean.FALSE;
                        throw new Exception("Echec du déplacement du fichier:" + fileScan.getName());
                    }
                }
            } else {
                result = Boolean.FALSE;
                throw new IllegalArgumentException("Mode " + mode + " not supported");
            }
        }

        return result;
    }

    static public boolean scanTo(File imageFile) throws Exception {
        try {
            String os_name = System.getProperty("os.name");
            if (os_name.equals("Linux")) {
                return scanTo(imageFile, MODE_SANE);
            } else {
                return scanTo(imageFile, MODE_ICOPY);
            }
        } catch (Exception ex) {
            throw ex;
        }

    }

    static public List<File> scanDirectory(File directory) {
        List<File> res = new ArrayList<File>();

        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!child.isDirectory()) {
                    res.add(child);
                }
            }
        }

        return res;
    }

    public static Boolean createDirectory(File directory) {
        Boolean error = Boolean.FALSE;

        if (!directory.exists()) {
            System.out.println("Creating directory: " + directory.getName());

            try {
                directory.mkdir();
            } catch (SecurityException se) {
                error = Boolean.TRUE;
            }
            if (!error) {
                System.out.println("Directory created");
            }
        }
        return error;
    }

    public static void convertFile(File sourceFile, File destFile) throws Exception {
        List<String> commands = new ArrayList<String>();

        commands.add("convert");
        commands.add(sourceFile.getAbsolutePath());
        commands.add(destFile.getAbsolutePath());

        try {
            TesseractUtils.runProcess(commands, new File("."));
            System.out.println("Image convert successfully! " + sourceFile.getAbsolutePath() + " " + destFile.getAbsolutePath());
        } catch (Exception ex) {
            throw new Exception("L'application ImageMagik n'est pas installé.");
        }

        if (!destFile.exists()) {
            throw new Exception("Erreur lors de la conversion en PNG, le fichier de destination n'a pas été créé (" + destFile.getAbsolutePath() + ").\nL'application ImageMagik n'est pas installée?");
        }
    }
}
