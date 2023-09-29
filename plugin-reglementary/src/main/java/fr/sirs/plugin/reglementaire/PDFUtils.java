package fr.sirs.plugin.reglementaire;

import javafx.concurrent.Task;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.geotoolkit.gui.javafx.util.TaskManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */

/**
 * Utility methods used to merge PDF files, add page numbers.
 *
 * @author Estelle Idée (Geomatys)
 */
public class PDFUtils {

    /**
     * Method to create the task to merge files.
     * <p>
     * Possibility to force rotating all the pages to be portrait oriented.
     * <p>
     * Possibility to add the page numbers and choose from left or right side of the bottom of the page.
     * <p>
     *
     * @param filesToMerge the list of the files to merge in the oder to be merged.
     * @param outputFile where to save the result merged file.
     * @param forceRotatePortrait force to rotate landscape pages to portrait orientation.
     * @param addPageNumber add page number at the bottom of the pages.
     * @param pageNbLeftBottomCorner allow to choose the side of the page where to add the page numbers.
     * <ul>
     *  <li>true -> page numbers will be at the bottom left corner.</li>
     *  <li>false -> page numbers will be at the bottom right corner.</li>
     * </ul>
     * @return the creted task.
     */
    public static Task mergeFiles(final List<File> filesToMerge, final String outputFile, final boolean forceRotatePortrait,
                                  final boolean addPageNumber, final boolean pageNbLeftBottomCorner) {
        return TaskManager.INSTANCE.submit(new Task() {
            @Override
            protected Object call() throws Exception {
                //Instantiating PDFMergerUtility class
                PDFMergerUtility PDFmerger = new PDFMergerUtility();

                //Setting the destination file
                PDFmerger.setDestinationFileName(outputFile);

                //adding the source files
                try {
                    filesToMerge.forEach(file -> {
                        if (!file.exists())
                            throw new IllegalArgumentException("The file " + file + "does not exist");
                        try {
                            PDFmerger.addSource(file);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException("The file " + file + "does not exist", e);
                        }
                    });
                } catch (RuntimeException e) {
                    throw new FileNotFoundException(e.getMessage());
                }


                //Merging the two documents
                try {
                    PDFmerger.mergeDocuments();
                } catch (IOException e) {
                    throw new IllegalStateException("Error while merging documents", e);
                }

                if (addPageNumber) {
                    PDFUtils.addPageNumber(outputFile, forceRotatePortrait, pageNbLeftBottomCorner);
                } else {
                    try (PDDocument doc = PDDocument.load(new File(outputFile))) {
                        for (PDPage page : doc.getPages()) {
                            page.setRotation(0);
                        }
                        doc.save(outputFile);
                    } catch (InvalidPasswordException e) {
                        // TODO deal with password alert
                        throw new RuntimeException(e);
                    }

                }

                return true;


            }
        });
    }

    /**
     *
     * @param file
     * @param forceRotatePortrait
     * @param pageNbLeftBottomCorner
     */
    public static void addPageNumber(final String file, final boolean forceRotatePortrait, final boolean pageNbLeftBottomCorner) {
        try (PDDocument doc = PDDocument.load(new File(file))) {
            PDFont font = PDType1Font.HELVETICA;
            float fontSize = 8.0f;

            int i = 1;
            final int pageNb = doc.getNumberOfPages();
            for (PDPage page : doc.getPages()) {
                PDRectangle pageSize = page.getMediaBox();
                float stringWidth = font.getStringWidth("900 / " + pageNb) * fontSize / 1000f;
                if (forceRotatePortrait) page.setRotation(0);

                int rotation = page.getRotation();
                final int bottomDistance = 30;
                final int sideDistance = 40;
                boolean rotate = rotation == 90 || rotation == 270;
                float pageWidth = rotate ? pageSize.getHeight() : pageSize.getWidth();
                float pageHeight = rotate ? pageSize.getWidth() : pageSize.getHeight();
                float xStart;
                float yStart;

                // calculate to bottom left corner of the page or to right bottom corner.
                if (pageNbLeftBottomCorner) {
                    xStart = rotate ? pageHeight - bottomDistance : sideDistance;
                    yStart = rotate ? sideDistance : bottomDistance;
                } else {
                    xStart = rotate ? pageHeight - bottomDistance : pageWidth - stringWidth - sideDistance;
                    yStart = rotate ? pageWidth - stringWidth - sideDistance : bottomDistance;
                }

                // append the content to the existing stream
                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.beginText();
                    // set font and font size
                    contentStream.setFont(font, fontSize);
                    if (rotate) {
                        // rotate the text according to the page rotation
                        contentStream.setTextMatrix(Matrix.getRotateInstance(Math.PI / 2, xStart, yStart));
                    } else {
                        contentStream.setTextMatrix(Matrix.getTranslateInstance(xStart, yStart));
                    }
                    contentStream.showText(i + " / " + pageNb);
                    contentStream.endText();
                    i++;
                } catch (RuntimeException e) {
                    throw new IllegalStateException("Error while adding page number");
                }
            }

            doc.save(file);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e);
        } catch (InvalidPasswordException e) {
            // TODO deal with password alert
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
