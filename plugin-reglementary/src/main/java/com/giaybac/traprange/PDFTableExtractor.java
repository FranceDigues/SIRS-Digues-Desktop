/**
 * Copyright (C) 2015, GIAYBAC
 *
 * Released under the MIT license
 */
package com.giaybac.traprange;

import com.giaybac.traprange.entity.Table;
import com.giaybac.traprange.entity.TableCell;
import com.giaybac.traprange.entity.TableRow;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static fr.sirs.PropertiesFileUtilities.showErrorDialog;
import static fr.sirs.util.odt.ODTUtils.askPassword;

/**
 *
 * @author Tho Mar 22, 2015 3:34:29 PM
 * @author Estelle Idée (Geomatys)
 * Modified extract() method to deal with password-protected files.
 */
public class PDFTableExtractor {
    private final Logger logger = LoggerFactory.getLogger(PDFTableExtractor.class);
    //contains pages that will be extracted table content.
    //If this variable doesn't contain any page, all pages will be extracted
    private final List<Integer> extractedPages = new ArrayList<>();
    private final List<Integer> exceptedPages = new ArrayList<>();
    //contains avoided line idx-s for each page,
    //if this multimap contains only one element and key of this element equals -1
    //then all lines in extracted pages contains in multi-map value will be avoided
    private final Multimap<Integer, Integer> pageNExceptedLinesMap = HashMultimap.create();

    //contains avoided line idx-s for the last page
    private final List<Integer> lastPageExceptedLinesList = new ArrayList<>();

    private InputStream inputStream;
    private PDDocument document;
    private String password;
    private int lastPageNo;

    /**
     * Method to be used to open a InputStream.
     * User's responsibility to close the inputStream in the class using the extractor by using a try-with-resource.
     *
     * Do not add constructor for File or String. Convert the File or String to an InputStream in the class using the extractor
     * and use it in a try-with-resource :
     *
     * try (final InputStream inputStream = new FileInputStream(new File(filePath))) {
     *     [...]
     * }
     *
     * @param inputStream the InputStream containing the data to extract.
     * @return this
     */
    public PDFTableExtractor setSource(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public PDFTableExtractor setSource(InputStream inputStream,String password) {
        this.inputStream = inputStream;
        this.password = password;
        return this;
    }

    /**
     * This page will be analyze and extract its table content
     *
     * @param pageIdx
     * @return
     */
    public PDFTableExtractor addPage(int pageIdx) {
        extractedPages.add(pageIdx);
        return this;
    }

    public PDFTableExtractor exceptPage(int pageIdx) {
        exceptedPages.add(pageIdx);
        return this;
    }

    /**
     * Avoid a specific line in a specific page. LineIdx can be negative number,
     * -1 is the last line
     *
     * @param pageIdx
     * @param lineIdxes
     * @return
     */
    public PDFTableExtractor exceptLine(int pageIdx, int[] lineIdxes) {
        for (int lineIdx : lineIdxes) {
            pageNExceptedLinesMap.put(pageIdx, lineIdx);
        }
        return this;
    }

    /**
     * Avoid a specific line in the last page. LineIdx can be negative number,
     * -1 is the last line
     *
     * @param lineIdxes the indexes of the lines to avoid in the last page of the doc.
     * @return
     */
    public PDFTableExtractor exceptLineInLastPage(List<Integer> lineIdxes) {
        lastPageExceptedLinesList.addAll(lineIdxes);
        return this;
    }

    /**
     * Avoid this line in all extracted pages. LineIdx can be negative number,
     * -1 is the last line
     *
     * @param lineIdxes
     * @return
     */
    public PDFTableExtractor exceptLine(int[] lineIdxes) {
        this.exceptLine(-1, lineIdxes);
        return this;
    }

    public List<Table> extract() {
        List<Table> retVal = new ArrayList<>();
        // TODO remove google.guava dependency
        Multimap<Integer, Range<Integer>> pageIdNLineRangesMap = LinkedListMultimap.create();
        Multimap<Integer, TextPosition> pageIdNTextsMap = LinkedListMultimap.create();
        try {
            // Load the file
            try {
                this.document = this.password != null ? PDDocument.load(inputStream, this.password) : PDDocument.load(inputStream);
            } catch (InvalidPasswordException e) {
                int passwordTry = 0;
                // Request for password if the PDF file is password protected.
                Optional<String> pwd = askPassword("Le document suivant est protégé par mot de passe. Veuillez insérer le mot de passe pour continuer.", document);
                while (pwd.isPresent() && passwordTry < 3) {
                    passwordTry++;
                    try {
                        this.document = PDDocument.load(inputStream, pwd.get());
                        break;
                    } catch (InvalidPasswordException ipe) {
                        pwd = askPassword("Le mot de passe est invalide. Veuillez recommencer.", document);
                    }
                }
                if (passwordTry >= 3) {
                    showErrorDialog("Limite de 3 essais atteinte.");
                    return null;
                }
                if (!pwd.isPresent()) {
                    throw new IOException("No password available to decode PDF file.");
                }
            }

            this.lastPageNo = document.getNumberOfPages();
            for (int pageId = 0; pageId < lastPageNo; pageId++) {
                boolean b = !exceptedPages.contains(pageId) && (extractedPages.isEmpty() || extractedPages.contains(pageId));
                if (b) {
                    List<TextPosition> texts = extractTextPositions(pageId);//sorted by .getY() ASC
                    //extract line ranges
                    List<Range<Integer>> lineRanges = getLineRanges(pageId, texts);
                    //extract column ranges
                    List<TextPosition> textsByLineRanges = getTextsByLineRanges(lineRanges, texts);

                    pageIdNLineRangesMap.putAll(pageId, lineRanges);
                    pageIdNTextsMap.putAll(pageId, textsByLineRanges);
                }
            }
            //Calculate columnRanges
            List<Range<Integer>> columnRanges = getColumnRanges(pageIdNTextsMap.values());
            for (int pageId : pageIdNTextsMap.keySet()) {
                Table table = buildTable(pageId, (List) pageIdNTextsMap.get(pageId), (List) pageIdNLineRangesMap.get(pageId), columnRanges);
                retVal.add(table);
                //debug
                logger.debug("Found " + table.getRows().size() + " row(s) and " + columnRanges.size()
                        + " column(s) of a table in page " + pageId);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Parse pdf file fail", ex);
        } finally {
            if (this.document != null) {
                try {
                    this.document.close();
                } catch (IOException ex) {
                    logger.error(null, ex);
                }
            }
        }
        //return
        return retVal;
    }

    /**
     * Texts in tableContent have been ordered by .getY() ASC
     *
     * @param pageIdx
     * @param tableContent
     * @param rowTrapRanges
     * @param columnTrapRanges
     * @return
     */
    private Table buildTable(int pageIdx, List<TextPosition> tableContent,
            List<Range<Integer>> rowTrapRanges, List<Range<Integer>> columnTrapRanges) {
        Table retVal = new Table(pageIdx, columnTrapRanges.size());
        int idx = 0;
        int rowIdx = 0;
        List<TextPosition> rowContent = new ArrayList<>();
        while (idx < tableContent.size()) {
            TextPosition textPosition = tableContent.get(idx);
            Range<Integer> rowTrapRange = rowTrapRanges.get(rowIdx);
            Range<Integer> textRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));
            if (rowTrapRange.encloses(textRange)) {
                rowContent.add(textPosition);
                idx++;
            } else {
                TableRow row = buildRow(rowIdx, rowContent, columnTrapRanges);
                retVal.getRows().add(row);
                //next row: clear rowContent
                rowContent.clear();
                rowIdx++;
            }
        }
        //last row
        if (!rowContent.isEmpty() && rowIdx < rowTrapRanges.size()) {
            TableRow row = buildRow(rowIdx, rowContent, columnTrapRanges);
            retVal.getRows().add(row);
        }
        //return
        return retVal;
    }

    /**
     *
     * @param rowIdx
     * @param rowContent
     * @param columnTrapRanges
     * @return
     */
    private TableRow buildRow(int rowIdx, List<TextPosition> rowContent, List<Range<Integer>> columnTrapRanges) {
        TableRow retVal = new TableRow(rowIdx);
        //Sort rowContent
        sortContentByX(rowContent);
        int idx = 0;
        int columnIdx = 0;
        List<TextPosition> cellContent = new ArrayList<>();
        while (idx < rowContent.size()) {
            TextPosition textPosition = rowContent.get(idx);
            Range<Integer> columnTrapRange = columnTrapRanges.get(columnIdx);
            Range<Integer> textRange = Range.closed((int) textPosition.getX(),
                    (int) (textPosition.getX() + textPosition.getWidth()));
            if (columnTrapRange.encloses(textRange)) {
                cellContent.add(textPosition);
                idx++;
            } else {
                TableCell cell = buildCell(columnIdx, cellContent);
                retVal.getCells().add(cell);
                //next column: clear cell content
                cellContent.clear();
                columnIdx++;
            }
        }
        if (!cellContent.isEmpty() && columnIdx < columnTrapRanges.size()) {
            TableCell cell = buildCell(columnIdx, cellContent);
            retVal.getCells().add(cell);
        }
        //return
        return retVal;
    }

    private static void sortContentByX(List<TextPosition> content) {
        Collections.sort(content, (o1, o2) -> {
            int retVal = 0;
            if (o1.getX() < o2.getX()) {
                retVal = -1;
            } else if (o1.getX() > o2.getX()) {
                retVal = 1;
            }
            return retVal;
        });
    }

    private TableCell buildCell(int columnIdx, List<TextPosition> cellContent) {
        sortContentByX(cellContent);
        //String cellContentString = Joiner.on("").join(cellContent.stream().map(e -> e.getCharacter()).iterator());
        StringBuilder cellContentBuilder = new StringBuilder();
        for (TextPosition textPosition : cellContent) {
            cellContentBuilder.append(textPosition.getUnicode());
        }
        String cellContentString = cellContentBuilder.toString();
        return new TableCell(columnIdx, cellContentString);
    }

    private List<TextPosition> extractTextPositions(int pageId) throws IOException {
        TextPositionExtractor extractor = new TextPositionExtractor(document, pageId);
        return extractor.extract();
    }

    private boolean isExceptedLine(int pageIdx, int lineIdx) {
        boolean isLastPage = pageIdx == lastPageNo - 1;
        boolean retVal = this.pageNExceptedLinesMap.containsEntry(pageIdx, lineIdx)
                        || this.pageNExceptedLinesMap.containsEntry(-1, lineIdx)
                        || (isLastPage && this.lastPageExceptedLinesList.contains(lineIdx));
        return retVal;
    }

    /**
     *
     * Remove all texts in excepted lines
     *
     * TexPositions are sorted by .getY() ASC
     *
     * @param lineRanges
     * @param textPositions
     * @return
     */
    private List<TextPosition> getTextsByLineRanges(List<Range<Integer>> lineRanges, List<TextPosition> textPositions) {
        List<TextPosition> retVal = new ArrayList<>();
        int idx = 0;
        int lineIdx = 0;
        while (idx < textPositions.size() && lineIdx < lineRanges.size()) {
            TextPosition textPosition = textPositions.get(idx);
            Range<Integer> textRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));
            Range<Integer> lineRange = lineRanges.get(lineIdx);
            if (lineRange.encloses(textRange)) {
                retVal.add(textPosition);
                idx++;
            } else if (lineRange.upperEndpoint() < textRange.lowerEndpoint()) {
                lineIdx++;
            } else {
                idx++;
            }
        }
        //return
        return retVal;
    }

    /**
     * @param texts
     * @return
     */
    private List<Range<Integer>> getColumnRanges(Collection<TextPosition> texts) {
        TrapRangeBuilder rangesBuilder = new TrapRangeBuilder();
        for (TextPosition text : texts) {
            Range<Integer> range = Range.closed((int) text.getX(), (int) (text.getX() + text.getWidth()));
            rangesBuilder.addRange(range);
        }
        return rangesBuilder.build();
    }

    private List<Range<Integer>> getLineRanges(int pageId, List<TextPosition> pageContent) {
        TrapRangeBuilder lineTrapRangeBuilder = new TrapRangeBuilder();
        for (TextPosition textPosition : pageContent) {
            Range<Integer> lineRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));
            //add to builder
            lineTrapRangeBuilder.addRange(lineRange);
        }
        List<Range<Integer>> lineTrapRanges = lineTrapRangeBuilder.build();
        List<Range<Integer>> retVal = removeExceptedLines(pageId, lineTrapRanges);
        return retVal;
    }

    private List<Range<Integer>> removeExceptedLines(int pageIdx, List<Range<Integer>> lineTrapRanges) {
        List<Range<Integer>> retVal = new ArrayList<>();
        for (int lineIdx = 0; lineIdx < lineTrapRanges.size(); lineIdx++) {
            boolean isExceptedLine = isExceptedLine(pageIdx, lineIdx)
                    || isExceptedLine(pageIdx, lineIdx - lineTrapRanges.size());
            if (!isExceptedLine) {
                retVal.add(lineTrapRanges.get(lineIdx));
            }
        }
        //return
        return retVal;
    }

    private static class TextPositionExtractor extends PDFTextStripper {

        private final List<TextPosition> textPositions = new ArrayList<>();
        private final int pageId;

        private TextPositionExtractor(PDDocument document, int pageId) throws IOException {
            super();
            super.setSortByPosition(true);
            super.document = document;
            this.pageId = pageId;
        }

        public void stripPage(int pageId) throws IOException {
            this.setStartPage(pageId + 1);
            this.setEndPage(pageId + 1);
            try (Writer writer = new OutputStreamWriter(new ByteArrayOutputStream())) {
                writeText(document, writer);
            }
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            this.textPositions.addAll(textPositions);
        }

        /**
         * and order by textPosition.getY() ASC
         *
         * @return
         * @throws IOException
         */
        private List<TextPosition> extract() throws IOException {
            this.stripPage(pageId);
            //sort
            Collections.sort(textPositions, (o1, o2) -> {
                int retVal = 0;
                if (o1.getY() < o2.getY()) {
                    retVal = -1;
                } else if (o1.getY() > o2.getY()) {
                    retVal = 1;
                }
                return retVal;

            });
            return this.textPositions;
        }
    }
}
