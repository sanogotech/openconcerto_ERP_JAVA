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
 
 package org.openconcerto.erp.core.sales.pos.io;

import org.openconcerto.erp.core.sales.pos.ui.POSDisplay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class ESCSerialDisplay extends POSDisplay {
    private static final int CR = 0x0D;
    private String port;
    private int columns;
    private int lines;
    protected static final int ESC = 0x1B;
    protected static final int STX = 0x02;
    private static final byte CLR = (byte) 0x0C;

    public ESCSerialDisplay(String serialPort) {
        this(serialPort, 20, 2);
    }

    public int getColumnCount() {
        return columns;
    }

    public int getLineCount() {
        return lines;
    }

    public ESCSerialDisplay(String serialPort, int columns, int lines) {
        if (serialPort == null) {
            serialPort = "";
        }
        this.port = serialPort.trim();
        if (this.port.endsWith(":")) {
            this.port = this.port.substring(0, this.port.length() - 1);
        }
        this.columns = columns;
        this.lines = lines;
    }

    private SerialPort getSerialPort() throws Exception {
        if (port == null || port.length() == 0) {
            throw new IllegalStateException("Invalid serial port name: " + port);
        }
        final CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(this.port);
        if (portIdentifier.isCurrentlyOwned()) {
            throw new IllegalAccessError("Port " + this.port + " is currently in use");
        }
        final int timeOutMs = 2000;
        final CommPort commPort = portIdentifier.open("ESCSerialDisplay", timeOutMs);
        if (!(commPort instanceof SerialPort)) {
            throw new IllegalStateException("Invalid serial port: " + port);
        }

        final SerialPort serialPort = (SerialPort) commPort;
        serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        return serialPort;
    }

    public synchronized void setMessage(String line1, String line2) throws Exception, IOException {
        if (this.port == null || this.port.trim().length() < 3) {
            return;
        }
        if (line1 == null) {
            line1 = "";
        }
        if (line2 == null) {
            line2 = "";
        }

        line1 = escape(line1);
        line2 = escape(line2);
        final SerialPort serialPort = getSerialPort();

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(CLR);
        //
        out.write(defaultInternationnalCharacterSet('F'));
        out.write(overwriteMode());
        out.write(overrideO());

        if (line1.length() > columns) {
            line1 = line1.substring(0, columns);
        }
        if (line2.length() > columns) {
            line2 = line2.substring(0, columns);
        }
        // upper line
        out.write(ESC);
        out.write(0x51);
        out.write(0x41);

        for (int i = 0; i < line1.length(); i++) {
            out.write((byte) line1.charAt(i));
        }
        out.write(CR);
        if (lines > 1) {
            // bottom line
            out.write(ESC);
            out.write(0x51);
            out.write(0x42);
            for (int i = 0; i < line2.length(); i++) {
                out.write((byte) line2.charAt(i));
            }
            out.write(CR);
        }
        // move cursor to home position
        out.write(new byte[] { ESC, 0x5B, 0x48 });
        out.close();

        final OutputStream outputStream = serialPort.getOutputStream();
        outputStream.write(out.toByteArray());
        outputStream.close();
        serialPort.close();
    }

    public String escape(String line) {
        line = line.replace('é', 'e');
        line = line.replace('è', 'e');
        line = line.replace('ê', 'e');
        line = line.replace('à', 'a');
        line = line.replace('ç', 'c');
        line = line.replace('î', 'i');
        line = line.replace('ô', 'o');
        return line;
    }

    private byte[] overrideO() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Use user defined
        out.write(new byte[] { ESC, 0x25, 1 });
        // Define
        out.write(ESC);
        out.write(0x26);
        out.write(0x01);
        // range
        out.write((byte) 'O');
        out.write((byte) 'O');
        //
        out.write(5);
        // for (int i = 0; i < 7; i++)
        out.write(new byte[] { 58, 65, 64, 65, 62 });

        return out.toByteArray();
    }

    public byte[] horizontalScrollMode() {
        return new byte[] { 0x1B, 0x13 };
    }

    public byte[] overwriteMode() {
        return new byte[] { 0x1B, 0x11 };
    }

    private byte[] defaultInternationnalCharacterSet(char c) {
        byte[] b = new byte[] { ESC, 0x66, (byte) c };
        return b;
    }

    public static void main(String[] args) throws Exception {
        ESCSerialDisplay d = new ESCSerialDisplay("COM15:", 20, 2);
        d.setMessage("OpenConcerto", "ILM Informatique");

    }
}
