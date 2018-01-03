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

import java.io.ByteArrayOutputStream;

public abstract class AbstractESCPrinter extends DefaultTicketPrinter {
    protected static final int GS = 0x1D;
    protected static final int ESC = 0x1B;

    public void addToBuffer(String t) {
        addToBuffer(t, NORMAL);
    }

    public void addToBuffer(String t, int mode) {
        this.strings.add(t);
        this.modes.add(mode);
    }

    protected byte[] getPrintBufferBytes() {
        final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        // Init
        bOut.write(ESC);
        bOut.write(0x40);
        // French characters
        bOut.write(ESC);
        bOut.write(0x52);
        bOut.write(0x01);
        //

        final int size = this.strings.size();
        for (int i = 0; i < size; i++) {
            String string = this.strings.get(i);
            int mode = modes.get(i);

            if (mode == BARCODE) {
                //
                bOut.write(GS);
                bOut.write(0x48);
                bOut.write(0x02); // en bas

                //
                bOut.write(GS);
                bOut.write(0x77);
                bOut.write(0x02); // Zoom 2

                //
                bOut.write(GS);
                bOut.write(0x68);
                bOut.write(60); // Hauteur
                // Code 39
                bOut.write(GS);
                bOut.write(0x6B);
                bOut.write(0x04); // Code 39
                for (int k = 0; k < string.length(); k++) {
                    char c = string.charAt(k);

                    bOut.write(c);
                }
                bOut.write(0x00); // End
            } else {
                if (mode == NORMAL) {
                    bOut.write(ESC);
                    bOut.write(0x21);
                    bOut.write(0);// Default
                } else if (mode == BOLD) {
                    bOut.write(ESC);
                    bOut.write(0x21);
                    bOut.write(8);// Emphasis
                } else if (mode == BOLD_LARGE) {
                    bOut.write(GS);
                    bOut.write(0x21);
                    bOut.write(0x11);//
                }

                for (int k = 0; k < string.length(); k++) {
                    char c = string.charAt(k);
                    if (c == 'é') {
                        c = 130;
                    } else if (c == 'è') {
                        c = 138;
                    } else if (c == 'ê') {
                        c = 136;
                    } else if (c == 'ù') {
                        c = 151;
                    } else if (c == 'à') {
                        c = 133;
                    } else if (c == 'ç') {
                        c = 135;
                    } else if (c == 'ô') {
                        c = 147;
                    }
                    bOut.write(c);
                }
            }
            bOut.write(0x0A);// Retour a la ligne

        }
        // Eject
        bOut.write(0x0A);
        bOut.write(0x0A);
        bOut.write(0x0A);
        bOut.write(0x0A);
        // Coupe
        bOut.write(GS);
        bOut.write(0x56); // V
        bOut.write(0x01);
        final byte[] byteArray = bOut.toByteArray();
        return byteArray;
    }

    @Override
    public abstract void printBuffer() throws Exception;

    @Override
    public abstract void openDrawer() throws Exception;

}
