package org.openconcerto.modules.ocr;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.poi.hpsf.MissingSectionException;
import org.openconcerto.utils.FileUtils;

public class TesseractUtils {

    public static String DoOcr(File sourceFile) throws Exception {

        System.err.println("TesseractUtils.DoOcr() " + sourceFile.getAbsolutePath());

        String result = "";
        final File imageHOCRDirectory = new File("ImageScan/HOCR");
        imageHOCRDirectory.mkdirs();
        final File execDirectory = new File(".");
        final String destFileName = getDestFileName(sourceFile);
        final File destFile = new File(imageHOCRDirectory.getAbsolutePath(), destFileName);
        String destPath = destFile.getAbsolutePath();
        if (destPath.length() >= 5) {
            destPath = destPath.substring(0, destPath.length() - 5);
        }

        if (!destFile.exists()) {

            List<String> commands = new ArrayList<String>();
            commands.add("tesseract");
            // Add arguments
            commands.add("-v");
            System.out.println(commands);

            // Run macro on target
            try {
                runProcess(commands, execDirectory);
            } catch (Exception e) {
                throw new MissingSectionException("L'application tesseract n'est pas installée.");
            }

            commands = new ArrayList<String>();
            commands.add("tesseract");
            commands.add(sourceFile.getAbsolutePath());
            commands.add(destPath);
            commands.add("hocr");

            System.out.println(commands);

            // Run macro on target
            runProcess(commands, execDirectory);

            result = FileUtils.read(destFile);

        } else {
            final Scanner sc = new Scanner(destFile);
            final StringBuilder fileContent = new StringBuilder();
            while (sc.hasNextLine()) {
                fileContent.append(sc.nextLine());
            }
            sc.close();
            result = fileContent.toString();
        }
        return result;
    }

    public static String getDestFileName(File sourceFile) throws Exception {
        String sourceFileChecksum = "";
        String sourceFileName = "";
        String fileHOCRName = "";

        sourceFileName = sourceFile.getName();
        sourceFileChecksum = getMD5Checksum(sourceFile);
        fileHOCRName = sourceFileName.substring(0, sourceFileName.length() - 4) + "_" + sourceFileChecksum + ".html";
        return fileHOCRName;
    }

    private static byte[] createChecksum(File file) throws IOException, NoSuchAlgorithmException {
        final InputStream fis = new BufferedInputStream(new FileInputStream(file), 512 * 1024);
        final byte[] buffer = new byte[1024];
        final MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    private static String getMD5Checksum(File file) throws IOException, NoSuchAlgorithmException {
        final byte[] b = createChecksum(file);
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public static String runProcess(List<String> commands, File runDirectory) throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(commands);
        final StringBuilder output = new StringBuilder();
        Process p = null;

        pb.directory(runDirectory);
        pb.redirectErrorStream(true);
        p = pb.start();
        // Read output
        final BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null, previous = null;
        while ((line = br.readLine()) != null) {
            if (!line.equals(previous)) {
                previous = line;
                output.append(line).append('\n');
            }
        }
        // Wait process
        p.waitFor();

        return output.toString();
    }

    public static String padLeft(String str, int size, char padChar) {
        final StringBuffer padded = new StringBuffer(str);
        while (padded.length() < size) {
            padded.insert(0, padChar);
        }
        return padded.toString();
    }

    public static String padRight(String str, int size, char padChar) {
        final StringBuffer padded = new StringBuffer(str);
        while (padded.length() < size) {
            padded.append(padChar);
        }
        return padded.toString();
    }

    public static void main(String[] args) throws Exception {
        String str = TesseractUtils.DoOcr(new File("PNG/workingimage002.png"));
        System.out.println(str);
    }
}
