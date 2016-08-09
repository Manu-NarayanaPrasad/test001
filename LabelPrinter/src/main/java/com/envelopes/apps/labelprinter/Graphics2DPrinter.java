package com.envelopes.apps.labelprinter;

import com.envelopes.apps.labelprinter.paper.Label3x2;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

/**
 * Created by Manu on 8/9/2016.
 */
public class Graphics2DPrinter implements Printable {

    public void print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        boolean ok = job.printDialog();
        if (ok) {
            try {
                job.print();
            } catch (PrinterException ex) {
                  /* The job did not successfully complete */
            }
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if(pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

        pageFormat.setPaper(new Label3x2());
        graphics.drawString("6 x 9 Open End Envelopes", 10, 50);

        return PAGE_EXISTS;
    }
}
