package com.envelopes.apps.labelprinter;

import com.sun.pdfview.PDFFile;
import org.apache.commons.lang3.StringUtils;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.PrinterResolution;
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

    public PDFPrinter(File file, int copies, Paper paper, String preferredPrinterName, boolean miniLabel) {
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
            aset.add(new Copies(copies > LabelHelper.MAX_COPIES ? LabelHelper.MAX_COPIES : copies));
            if(StringUtils.isNotEmpty(preferredPrinterName)) {
                pjob.setPrintService(getPrintService(preferredPrinterName));
            }
            aset.add(javax.print.attribute.standard.PrintQuality.HIGH);
            if(miniLabel) {
                aset.add(new PrinterResolution(175, 200, ResolutionSyntax.DPI));
            }

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

    protected PrintService getPrintService(String printerName) {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        for(int i = 0; i < printServices.length; i ++) {
            if(printServices[i].getName().equalsIgnoreCase(printerName)) {
                return printServices[i];
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }
}
