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
 
 package org.openconcerto.erp.core.sales.pos;

import org.openconcerto.erp.core.sales.pos.io.ESCSerialPrinter;
import org.openconcerto.erp.core.sales.pos.io.ESCStandardPrinter;
import org.openconcerto.erp.core.sales.pos.io.JPOSTicketPrinter;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;

public class TicketPrinterConfiguration {
    public static final String STANDARD_PRINTER = "std";
    public static final String SERIAL_PRINTER = "serial";
    public static final String JPOS_PRINTER = "jpos";
    private String type = STANDARD_PRINTER;
    private int ticketWidth = 42;

    private int copyCount = 2;
    private String name = "";
    private String folder = "";

    public TicketPrinterConfiguration() {

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (STANDARD_PRINTER.equals(type) || SERIAL_PRINTER.equals(type) || JPOS_PRINTER.equals(type)) {
            this.type = type;
        } else {
            throw new IllegalArgumentException("Unknown type " + type);
        }
    }

    public int getTicketWidth() {
        return ticketWidth;
    }

    public void setTicketWidth(int ticketWidth) {
        if (ticketWidth < 10) {
            throw new IllegalArgumentException("Invalid " + ticketWidth);
        }
        this.ticketWidth = ticketWidth;
    }

    public int getCopyCount() {
        return copyCount;
    }

    public void setCopyCount(int copyCount) {
        if (copyCount < 0) {
            throw new IllegalArgumentException("Negative copy count " + copyCount);
        }
        this.copyCount = copyCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public TicketPrinter createTicketPrinter() {
        final TicketPrinter prt;
        if (STANDARD_PRINTER.equals(getType())) {
            prt = new ESCStandardPrinter(getName());
        } else if (SERIAL_PRINTER.equals(getType())) {
            prt = new ESCSerialPrinter(getName());
        } else if (JPOS_PRINTER.equals(getType())) {
            prt = new JPOSTicketPrinter(getName());
        } else {
            throw new IllegalStateException("Unknown type " + getType());
        }
        return prt;
    }

    public boolean isValid() {
        return this.getName() != null && !this.getName().isEmpty();
    }
}
