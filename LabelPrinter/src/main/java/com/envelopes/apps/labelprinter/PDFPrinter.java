package com.envelopes.apps.labelprinter;

import com.sun.pdfview.PDFFile;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.swing.*;
import java.awt.print.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by Manu on 7/13/2016.
 */
public class PDFPrinter {
    public static final int MAX_COPIES = 100;

    public PDFPrinter(File file, int copies, Paper paper) {
        try {
            FileInputStream fis = new FileInputStream(file);

            FileChannel fc = fis.getChannel();
            ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            PDFFile pdfFile = new PDFFile(bb);
            LabelPage pages = new LabelPage(pdfFile);

            PrinterJob pjob = PrinterJob.getPrinterJob();
            PageFormat pf = PrinterJob.getPrinterJob().defaultPage();

            pf.setPaper(paper);

            pjob.setJobName(file.getName());
            Book book = new Book();
            book.append(pages, pf, pdfFile.getNumPages());
            pjob.setPageable(book);

            PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
            aset.add(new Copies(copies > MAX_COPIES ? MAX_COPIES : copies));

            // Send print job to default printer
//            if (pjob.printDialog(/*aset*/)) {
                pjob.print(aset);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(null, "Printing Error: "
                            + e.getMessage(), "Print Aborted",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
