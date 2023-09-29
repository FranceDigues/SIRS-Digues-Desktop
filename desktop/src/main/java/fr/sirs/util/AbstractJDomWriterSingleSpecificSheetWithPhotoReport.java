/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.util;

import static fr.sirs.util.JRDomWriterDesordreSheet.PHOTOS_SUBREPORT;
import static fr.sirs.util.JRDomWriterDesordreSheet.PHOTO_DATA_SOURCE;
import static fr.sirs.util.JRUtils.ATT_HEIGHT;
import static fr.sirs.util.JRUtils.ATT_POSITION_TYPE;
import static fr.sirs.util.JRUtils.ATT_WIDTH;
import static fr.sirs.util.JRUtils.ATT_X;
import static fr.sirs.util.JRUtils.ATT_Y;
import static fr.sirs.util.JRUtils.TAG_BAND;
import static fr.sirs.util.JRUtils.TAG_DATA_SOURCE_EXPRESSION;
import static fr.sirs.util.JRUtils.TAG_REPORT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_SUBREPORT;
import static fr.sirs.util.JRUtils.TAG_SUBREPORT_EXPRESSION;
import static fr.sirs.util.JRUtils.URI_JRXML;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 *
 */
public abstract class AbstractJDomWriterSingleSpecificSheetWithPhotoReport<T extends fr.sirs.core.model.Element> extends AbstractJDomWriterSingleSpecificSheet<T> {

    // Auteur du cadre de fond des titres de section.
    protected static final int TITLE_SECTION_BG_HEIGHT = 14;

    // Marge verticale entre le cadre des titres de section et le texte.
    protected static final int TITLE_SECTION_MARGIN_V = 1;

    // Indentation du texte des titres de section.
    protected static final int TITLE_SECTION_INDENT = 10;

    // Taille de police des titres de section.
    protected static final int TITLE_SECTION_FONT_SIZE = 8;

    // Hauteur réservée aux tableaux.
    protected static final int TABLE_HEIGHT = 0;

    // Taille de police dans les tableaux.
    protected static final int TABLE_FONT_SIZE = 7;

    // Hauteur des en-têtes de colonnes des tableaux.
    protected static final int TABLE_HEADER_HEIGHT = 20;

    // Hauteur des cellules des tableaux.
    protected static final int TABLE_CELL_HEIGHT = 10;

    // Indique si la dernière colonne des tableaux doit occuper la place restante dans la page.
    protected static final boolean TABLE_FILL_WIDTH = true;

    public AbstractJDomWriterSingleSpecificSheetWithPhotoReport(final Class<T> classToMap) {
        super(classToMap);
    }

    public AbstractJDomWriterSingleSpecificSheetWithPhotoReport(final Class<T> classToMap,
            final InputStream stream, final List<String> avoidFields, final String sectionTitleBackgroundColor)
            throws ParserConfigurationException, SAXException, IOException{
        super(classToMap, stream, avoidFields, sectionTitleBackgroundColor);
    }

    protected void includePhotoSubreport(final int height){

        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);

        final Element subReport = document.createElement(TAG_SUBREPORT);
        final Element reportElement = document.createElement(TAG_REPORT_ELEMENT);
        reportElement.setAttribute(ATT_X, String.valueOf(0));
        reportElement.setAttribute(ATT_Y, String.valueOf(currentY));
        reportElement.setAttribute(ATT_WIDTH, String.valueOf(802));
        reportElement.setAttribute(ATT_HEIGHT, String.valueOf(height));
        reportElement.setAttribute(ATT_POSITION_TYPE, JRUtils.PositionType.FLOAT.toString());
        subReport.appendChild(reportElement);

        final Element datasourceExpression = document.createElementNS(URI_JRXML, TAG_DATA_SOURCE_EXPRESSION);

        final CDATASection datasourceExpressionField = document.createCDATASection("(("+ObjectDataSource.class.getCanonicalName()+") $F{"+PHOTO_DATA_SOURCE+"})");

        datasourceExpression.appendChild(datasourceExpressionField);
        subReport.appendChild(datasourceExpression);

        final Element subreportExpression = document.createElementNS(URI_JRXML, TAG_SUBREPORT_EXPRESSION);
        final CDATASection subreportExpressionField = document.createCDATASection("$P{"+PHOTOS_SUBREPORT+"}");

        subreportExpression.appendChild(subreportExpressionField);
        subReport.appendChild(subreportExpression);

        band.appendChild(subReport);
        currentY+=height;
    }
}