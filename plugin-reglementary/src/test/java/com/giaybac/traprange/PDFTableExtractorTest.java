package com.giaybac.traprange;

import com.giaybac.traprange.entity.Table;
import com.giaybac.traprange.entity.TableRow;
import core.CouchDBTestCase;
import fr.sirs.plugin.reglementaire.ui.RegistreDocumentsPane;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
public class PDFTableExtractorTest extends CouchDBTestCase {

    private final PDFTableExtractor pdfTableExtractor = new PDFTableExtractor();
    @Test
    public void tes_extract() throws FileNotFoundException {
        final FileInputStream inputStream = new FileInputStream(this.getClass().getResource("tableau_synthese.pdf").getFile());
        pdfTableExtractor.setSource(inputStream);

        // two first lines of the doc : title and @SystemeEndiguement libelle
        pdfTableExtractor.exceptLine(0, new int[]{0});
        pdfTableExtractor.exceptLine(0, new int[]{1});

        // exclude last line of the last page -> corresponds to "Période : xx/xx/xxxx - xx/xx/xxxx"
        pdfTableExtractor.exceptLineInLastPage(Arrays.asList(-1));

        final List<Table> tables = pdfTableExtractor.extract();

        Assert.assertEquals(tables.size(), 1);
        final Table table = tables.get(0);
        final List<TableRow> rows = table.getRows();

        Assert.assertNotNull(rows);
        Assert.assertEquals(rows.size(), 37);

        final TableRow firstRow = rows.get(0);
        Assert.assertEquals(firstRow.getCells().size(), 10);
        final List<String> tableHeader = firstRow.getCells().stream().map(cell -> cell.getContent()).collect(Collectors.toList());

        firstRow.getCells().forEach(cell -> Assert.assertEquals(tableHeader, Arrays.asList(RegistreDocumentsPane.HEADERS)));

    }

    // TODO add tests for prestation update when loading a "tableau de synthese" file.
}
