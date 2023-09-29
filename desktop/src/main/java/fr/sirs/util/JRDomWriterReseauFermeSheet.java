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

import fr.sirs.core.SirsCore;
import fr.sirs.core.model.ObjetReseau;
import fr.sirs.core.model.ObservationReseauHydrauliqueFerme;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import static fr.sirs.util.JRUtils.ATT_HEIGHT;
import static fr.sirs.util.JRUtils.TAG_BAND;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class JRDomWriterReseauFermeSheet extends AbstractJDomWriterSingleSpecificSheetWithPhotoReport<ReseauHydrauliqueFerme> {

    public static final String OBSERVATION_DATASET = "Observation Dataset";
    public static final String OBSERVATION_TABLE_DATA_SOURCE = "OBSERVATION_TABLE_DATA_SOURCE";

    public static final String OBSERVATION_SPEC_DATASET = "Observation Specification Dataset";
    public static final String OBSERVATION_SPEC_TABLE_DATA_SOURCE = "OBSERVATION_SPEC_TABLE_DATA_SOURCE";

    public static final String RESEAU_OUVRAGE_DATASET = "ReseauOuvrage Dataset";
    public static final String RESEAU_OUVRAGE_TABLE_DATA_SOURCE = "RESEAU_OUVRAGE_TABLE_DATA_SOURCE";

    public static final String DESORDRE_DATASET = "Desordre Dataset";
    public static final String DESORDRE_TABLE_DATA_SOURCE = "DESORDRE_TABLE_DATA_SOURCE";

    public static final String PHOTO_DATA_SOURCE = "PHOTO_DATA_SOURCE";
    public static final String PHOTOS_SUBREPORT = "PHOTO_SUBREPORT";

    public static final String IMAGE_DATA_SOURCE = "IMAGE_DATA_SOURCE";

    public static final String SECURITE_ID_FIELD = "SECURITE_ID_FIELD";

    private final List<JRColumnParameter> observationFields;
    private final List<JRColumnParameter> observationSpecFields;
    private final List<JRColumnParameter> reseauFields;
    private final List<JRColumnParameter> desordreFields;

    private final boolean printPhoto;
    private final boolean printReseauOuvrage;
    private final boolean printObservationsSpec;

    private JRDomWriterReseauFermeSheet(final Class<ReseauHydrauliqueFerme> classToMap){
        super(classToMap);

        observationFields = null;
        observationSpecFields = null;
        reseauFields = null;
        desordreFields = null;
        printPhoto = printReseauOuvrage = printObservationsSpec = true;
    }

    public JRDomWriterReseauFermeSheet(final InputStream stream,
                                       final List<String> avoidFields,
                                       final List<JRColumnParameter> observationFields,
                                       final List<JRColumnParameter> observationSpecFields,
                                       final List<JRColumnParameter> reseauFields,
                                       final List<JRColumnParameter> desordreFields,
                                       final boolean printPhoto,
                                       final boolean printReseauOuvrage,
                                       boolean printObservationsSpec)
            throws ParserConfigurationException, SAXException, IOException {
        super(ReseauHydrauliqueFerme.class, stream, avoidFields, "#b2a1c7");

        this.observationFields = observationFields;
        this.observationSpecFields = observationSpecFields;
        this.reseauFields = reseauFields;
        this.printPhoto = printPhoto;
        this.printReseauOuvrage = printReseauOuvrage;
        this.desordreFields = desordreFields;
        this.printObservationsSpec = printObservationsSpec;
    }

    /**
     * <p>This method modifies the body of the DOM.</p>
     */
    @Override
    protected void writeObject() {

        writeSubDataset(ObservationReseauHydrauliqueFerme.class, observationFields, true, 0);
        writeSubDataset(ObservationReseauHydrauliqueFerme.class, observationSpecFields, true, 1);
        writeSubDataset(ObjetReseau.class, reseauFields, true, 2);
        writeSubDataset(JRDesordreTableRow.class, desordreFields, true, 3);

        // Sets the initial fields used by the template.------------------------
        writeFields();
        writeField(String.class, SirsCore.DIGUE_ID_FIELD, "Champ ajouté de force pour prendre en compte l'intitulé de la digue.");// Ajout d'un champ pour l'intitulé de la digue.
        if(printPhoto) writeField(ObjectDataSource.class, PHOTO_DATA_SOURCE, "Source de données des photos");
        writeField(ObjectDataSource.class, OBSERVATION_TABLE_DATA_SOURCE, "Source de données des observations");
        writeField(ObjectDataSource.class, OBSERVATION_SPEC_TABLE_DATA_SOURCE, "Source de données des spécifications d'observations");
        if(printReseauOuvrage) writeField(ObjectDataSource.class, RESEAU_OUVRAGE_TABLE_DATA_SOURCE, "Source de données des réseaux");
        writeField(ObjectDataSource.class, DESORDRE_TABLE_DATA_SOURCE, "Source de données des désordres");
        writeField(Image.class, IMAGE_DATA_SOURCE, "Image de l'élément");
        writeField(ObjectDataSource.class, SECURITE_ID_FIELD, "Champ calculé pour le niveau de sécurité de l'ouvrage associé.");

        // Modifies the title block.--------------------------------------------
        writeTitle();

        // Writes the headers.--------------------------------------------------
        writePageHeader();
        writeColumnHeader();

        // Builds the body of the Jasper Reports template.----------------------
        writeDetail(printObservationsSpec);

        // Writes the footers
        writeColumnFooter();
        writePageFooter();
    }

    /**
     * <p>This method writes the content of the detail element.</p>
     * @param printObservationsSpec
     * @throws Exception
     */
    private void writeDetail(boolean printObservationsSpec) {

        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);
        currentY = Integer.valueOf(band.getAttribute(ATT_HEIGHT));

        /*----------------------------------------------------------------------
        TABLEAU DES OBSERVATIONS
        ----------------------------------------------------------------------*/
        currentY+=24;
        writeSectionTitle("Observations", TITLE_SECTION_BG_HEIGHT, TITLE_SECTION_MARGIN_V, TITLE_SECTION_INDENT, TITLE_SECTION_FONT_SIZE, true, false, false);
        currentY+=2;
        writeTable(ObservationReseauHydrauliqueFerme.class, observationFields, true, OBSERVATION_TABLE_DATA_SOURCE, OBSERVATION_DATASET,
                TABLE_HEIGHT, TABLE_FONT_SIZE, TABLE_HEADER_HEIGHT, TABLE_CELL_HEIGHT, TABLE_FILL_WIDTH);

        /*----------------------------------------------------------------------
        TABLEAU DES SPECIFICATIONS D'OBSERVATIONS
        ----------------------------------------------------------------------*/
        if (printObservationsSpec) {
            currentY += 24;
            writeSectionTitle("Observations (spécification ouvrage/réseau)", TITLE_SECTION_BG_HEIGHT, TITLE_SECTION_MARGIN_V, TITLE_SECTION_INDENT, TITLE_SECTION_FONT_SIZE, true, false, false);
            currentY += 2;
            writeTable(ObservationReseauHydrauliqueFerme.class, observationSpecFields, true, OBSERVATION_SPEC_TABLE_DATA_SOURCE, OBSERVATION_SPEC_DATASET,
                    TABLE_HEIGHT, TABLE_FONT_SIZE, 36, TABLE_CELL_HEIGHT, TABLE_FILL_WIDTH);
        }

        /*----------------------------------------------------------------------
        SOUS-RAPPORTS DES PHOTOS
        ----------------------------------------------------------------------*/
        if(printPhoto){
            currentY+=24;
            includePhotoSubreport(0);
        }

        /*----------------------------------------------------------------------
        TABLEAU DES DESORDRES
        ----------------------------------------------------------------------*/
        currentY+=24;
        writeSectionTitle("Desordres", TITLE_SECTION_BG_HEIGHT, TITLE_SECTION_MARGIN_V, TITLE_SECTION_INDENT, TITLE_SECTION_FONT_SIZE, true, false, false);
        currentY+=2;
        writeTable(JRDesordreTableRow.class, desordreFields, true, DESORDRE_TABLE_DATA_SOURCE, DESORDRE_DATASET,
                TABLE_HEIGHT, TABLE_FONT_SIZE, TABLE_HEADER_HEIGHT, TABLE_CELL_HEIGHT, TABLE_FILL_WIDTH);

        /*----------------------------------------------------------------------
        TABLEAU DES OUVRAGES ET RÉSEAUX
        ----------------------------------------------------------------------*/
        if(printReseauOuvrage){
            currentY+=24;
            writeSectionTitle("Réseaux et ouvrages", TITLE_SECTION_BG_HEIGHT, TITLE_SECTION_MARGIN_V, TITLE_SECTION_INDENT, TITLE_SECTION_FONT_SIZE, true, false, false);
            currentY+=2;
            writeTable(ObjetReseau.class, reseauFields, true, RESEAU_OUVRAGE_TABLE_DATA_SOURCE, RESEAU_OUVRAGE_DATASET,
                    TABLE_HEIGHT, TABLE_FONT_SIZE, TABLE_HEADER_HEIGHT, TABLE_CELL_HEIGHT, TABLE_FILL_WIDTH);
        }

//        writeDetailPageBreak();

        // Sizes the detail element given to the field number.------------------
        band.setAttribute(ATT_HEIGHT, String.valueOf(DETAIL_HEIGHT));

        // Builds the DOM tree.-------------------------------------------------
        root.appendChild(detail);
    }
}