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

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

public class ESCSerialPrinter extends AbstractESCPrinter {

    private String port;

    /**
     * Valid port are COM1: , COM1 , /dev/ttyS0 or /dev/tty.mydevice
     * */
    public ESCSerialPrinter(String port) {
        port = port.trim();
        if (port.endsWith(":")) {
            port = port.substring(0, port.length() - 1);
        }
        this.port = port;
    }

    public synchronized void openDrawer() throws Exception {
        final SerialPort serialPort = getSerialPort();

        OutputStream out = serialPort.getOutputStream();
        boolean useESCP = false;
        if (useESCP) {
            // Pin 2, 200ms min
            out.write(ESC);
            out.write(0x70);
            out.write(0x00); // Pin 2
            out.write(100); // 2x100ms On
            out.write(100); // 2x100ms Off
            try {
                // 300ms to ensure opening
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Pin 2, 200ms min
            out.write(ESC);
            out.write(0x70);
            out.write(0x01); // Pin 5
            out.write(100); // 2x100ms On
            out.write(100); // 2x100ms Off
            try {
                // 300ms to ensure opening
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        // Pin 2, 200ms min
        out.write(0x10);// DLE
        out.write(0x14);// DC4
        out.write(0x01);
        out.write(0x00);// Pin 2
        out.write(0x02);
        // Vista 32bits bug: out.flush(); // Crash, works fine without

        try {
            // 300ms to ensure opening
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Pin 5, 200ms
        out.write(0x10);// DLE
        out.write(0x14);// DC4
        out.write(0x01);
        out.write(0x01);// Pin 5
        out.write(0x02);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Vista 32bits bug: out.flush(); // Crash, works fine without
        out.close();

        serialPort.close();
    }

    public synchronized void printBuffer() throws Exception {
        final byte[] byteArray = getPrintBufferBytes();
        // Do NOT flush or use BufferedOutputStream !
        final SerialPort serialPort = getSerialPort();
        final OutputStream out = serialPort.getOutputStream();
        out.write(byteArray);
        out.close();
        serialPort.close();
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
        final CommPort commPort = portIdentifier.open("ESCSerialPrinter", timeOutMs);
        if (!(commPort instanceof SerialPort)) {
            throw new IllegalStateException("Invalid serial port: " + port);
        }

        final SerialPort serialPort = (SerialPort) commPort;
        serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        return serialPort;
    }

    public static void main(String[] args) {
        listPorts();
        final ESCSerialPrinter prt = new ESCSerialPrinter("COM1");
        prt.setPort(getSerialPortNames().get(0));
        int col = 42;
        prt.addToBuffer("ILM INFORMATIQUE", BOLD_LARGE);
        prt.addToBuffer("");
        prt.addToBuffer("22 place de la liberation");
        prt.addToBuffer("80100 ABBEVILLE");
        prt.addToBuffer("Tél: 00 00 00 00 00");
        prt.addToBuffer("Fax: 00 00 00 00 00");
        prt.addToBuffer("");
        final SimpleDateFormat df = new SimpleDateFormat("EEEE d MMMM yyyy à HH:mm");
        prt.addToBuffer(formatRight(42, "Le " + df.format(Calendar.getInstance().getTime())));
        prt.addToBuffer("");
        prt.addToBuffer(formatRight(5, "3") + " " + formatLeft(col - 6 - 9, "ILM Informatique") + " " + formatRight(8, "3.00"));
        prt.addToBuffer(formatLeft(col, "      ======================================="));
        prt.addToBuffer(formatRight(col - 8, "Total") + formatRight(8, "3.00"), BOLD);
        prt.addToBuffer("");
        prt.addToBuffer("Merci de votre visite, à bientôt.");
        prt.addToBuffer("");
        prt.addToBuffer("01 05042010 00002", BARCODE);

        try {
            prt.printBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setPort(String string) {
        this.port = string;

    }

    public static void listPorts() {
        @SuppressWarnings("unchecked")
        final Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            System.out.println("Port: " + portIdentifier.getName() + " Type: " + getPortTypeName(portIdentifier.getPortType()));
        }
    }

    public static List<String> getSerialPortNames() {
        ArrayList<String> r = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portEnum.nextElement();
            System.out.println("Port: " + portIdentifier.getName() + " Type: " + getPortTypeName(portIdentifier.getPortType()));
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                r.add(portIdentifier.getName());
            }
        }
        return r;
    }

    private static String getPortTypeName(int portType) {
        switch (portType) {
        case CommPortIdentifier.PORT_I2C:
            return "I2C";
        case CommPortIdentifier.PORT_PARALLEL:
            return "Parallel";
        case CommPortIdentifier.PORT_RAW:
            return "Raw";
        case CommPortIdentifier.PORT_RS485:
            return "RS485";
        case CommPortIdentifier.PORT_SERIAL:
            return "Serial";
        default:
            return "unknown type";
        }
    }

}
