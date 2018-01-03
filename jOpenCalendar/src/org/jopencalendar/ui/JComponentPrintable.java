package org.jopencalendar.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.swing.JComponent;

public class JComponentPrintable implements Printable {

    private JComponent comp;
    private final String title;

    public JComponentPrintable(JComponent comp, String title) {
        this.comp = comp;
        this.title = title;
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageNumber) throws PrinterException {
        if (pageNumber > 0) {
            return Printable.NO_SUCH_PAGE;
        }
        // Get the preferred size ofthe component...
        Dimension compSize = comp.getSize();

        // Make sure we size to the preferred size
        comp.setSize(compSize);
        // Get the the print size
        Dimension printSize = new Dimension();
        printSize.setSize(pf.getImageableWidth(), pf.getImageableHeight());

        // Calculate the scale factor
        double scaleFactor = getScaleFactorToFit(compSize, printSize);
        // Don't want to scale up, only want to scale down
        if (scaleFactor > 1d) {
            scaleFactor = 1d;
        }

        // Calcaulte the scaled size...
        double scaleWidth = (compSize.width * scaleFactor) - (this.title == null ? 0 : 0.08);
        double scaleHeight = compSize.height * scaleFactor - (this.title == null ? 0 : 0.08);

        // Create a clone of the graphics context. This allows us to manipulate
        // the graphics context without begin worried about what effects
        // it might have once we're finished
        Graphics2D g2 = (Graphics2D) g.create();
        // Calculate the x/y position of the component, this will center
        // the result on the page if it can
        double x = ((pf.getImageableWidth() - scaleWidth) / 2d) + pf.getImageableX();
        double y = ((pf.getImageableHeight() - scaleHeight) / 2d) + pf.getImageableY() + (this.title == null ? 0 : 25);
        // Create a new AffineTransformation
        AffineTransform at = new AffineTransform();
        // Translate the offset to out "center" of page
        at.translate(x, y);
        // Set the scaling
        at.scale(scaleFactor, scaleFactor);
        if (title != null) {
            g2.drawString("Planning", 15, 20);
        }
        // Apply the transformation
        g2.transform(at);

        // Print the component
        comp.printAll(g2);
        // Dispose of the graphics context, freeing up memory and discarding
        // our changes
        g2.dispose();
        comp.revalidate();
        return Printable.PAGE_EXISTS;
    }

    private static final double getScaleFactorToFit(Dimension original, Dimension toFit) {
        double dScale = 1d;
        if (original != null && toFit != null) {
            double dScaleWidth = getScaleFactor(original.width, toFit.width);
            double dScaleHeight = getScaleFactor(original.height, toFit.height);
            dScale = Math.min(dScaleHeight, dScaleWidth);
        }
        return dScale;
    }

    private static final double getScaleFactor(int iMasterSize, int iTargetSize) {
        double dScale = 1;
        if (iMasterSize > iTargetSize) {
            dScale = (double) iTargetSize / (double) iMasterSize;
        } else {
            dScale = (double) iTargetSize / (double) iMasterSize;
        }
        return dScale;
    }

}
