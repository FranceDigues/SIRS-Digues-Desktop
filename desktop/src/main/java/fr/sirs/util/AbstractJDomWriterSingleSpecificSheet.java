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
import static fr.sirs.core.SirsCore.BUNDLE_KEY_CLASS;
import fr.sirs.core.model.ReferenceType;
import static fr.sirs.util.AbstractJDomWriter.NULL_REPLACEMENT;
import static fr.sirs.util.JRUtils.ATT_BACKCOLOR;
import static fr.sirs.util.JRUtils.ATT_CLASS;
import static fr.sirs.util.JRUtils.ATT_HEIGHT;
import static fr.sirs.util.JRUtils.ATT_IS_BOLD;
import static fr.sirs.util.JRUtils.ATT_IS_ITALIC;
import static fr.sirs.util.JRUtils.ATT_IS_STRETCH_WITH_OVERFLOW;
import static fr.sirs.util.JRUtils.ATT_IS_UNDERLINE;
import static fr.sirs.util.JRUtils.ATT_KEY;
import static fr.sirs.util.JRUtils.ATT_MODE;
import static fr.sirs.util.JRUtils.ATT_NAME;
import static fr.sirs.util.JRUtils.ATT_POSITION_TYPE;
import static fr.sirs.util.JRUtils.ATT_SIZE;
import static fr.sirs.util.JRUtils.ATT_STYLE;
import static fr.sirs.util.JRUtils.ATT_SUB_DATASET;
import static fr.sirs.util.JRUtils.ATT_VERTICAL_ALIGNMENT;
import static fr.sirs.util.JRUtils.ATT_WIDTH;
import static fr.sirs.util.JRUtils.ATT_X;
import static fr.sirs.util.JRUtils.ATT_Y;
import static fr.sirs.util.JRUtils.BOOLEAN_PRIMITIVE_NAME;
import static fr.sirs.util.JRUtils.TAG_BAND;
import static fr.sirs.util.JRUtils.TAG_BREAK;
import static fr.sirs.util.JRUtils.TAG_COLUMN;
import static fr.sirs.util.JRUtils.TAG_COLUMN_FOOTER;
import static fr.sirs.util.JRUtils.TAG_COLUMN_HEADER;
import static fr.sirs.util.JRUtils.TAG_COMPONENT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_DATASET_RUN;
import static fr.sirs.util.JRUtils.TAG_DATA_SOURCE_EXPRESSION;
import static fr.sirs.util.JRUtils.TAG_DETAIL_CELL;
import static fr.sirs.util.JRUtils.TAG_FIELD;
import static fr.sirs.util.JRUtils.TAG_FIELD_DESCRIPTION;
import static fr.sirs.util.JRUtils.TAG_FONT;
import static fr.sirs.util.JRUtils.TAG_FRAME;
import static fr.sirs.util.JRUtils.TAG_REPORT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_STATIC_TEXT;
import static fr.sirs.util.JRUtils.TAG_SUB_DATASET;
import static fr.sirs.util.JRUtils.TAG_TABLE;
import static fr.sirs.util.JRUtils.TAG_TABLE_FOOTER;
import static fr.sirs.util.JRUtils.TAG_TABLE_HEADER;
import static fr.sirs.util.JRUtils.TAG_TEXT;
import static fr.sirs.util.JRUtils.TAG_TEXT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_TEXT_FIELD;
import static fr.sirs.util.JRUtils.TAG_TEXT_FIELD_EXPRESSION;
import static fr.sirs.util.JRUtils.URI_JRXML;
import static fr.sirs.util.JRUtils.URI_JRXML_COMPONENTS;
import static fr.sirs.util.JRUtils.getCanonicalName;
import static fr.sirs.util.PrinterUtilities.getFieldNameFromSetter;
import fr.sirs.util.property.Reference;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
public abstract class AbstractJDomWriterSingleSpecificSheet<T extends fr.sirs.core.model.Element> extends AbstractJDomWriterSingleSheet {

    protected int currentY = 0;

    // Static template parameters.
    protected static final int PAGE_WIDTH = 595;
    protected static final int PAGE_HEIGHT = 842;
    protected static final int LEFT_MARGIN = 20;
    protected static final int RIGHT_MARGIN = 20;
    protected static final int TOP_MARGIN = 20;
    protected static final int BOTTOM_MARGIN = 20;
    protected static final int UTIL_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;
    protected static final int UTIL_HEIGTH = PAGE_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN;
    protected static final int FOOTER_HEIGHT = 12;
    protected static final int DETAIL_HEIGHT = UTIL_HEIGTH-FOOTER_HEIGHT;

    // Doit être cohérent avec les paddings indiqués dans les styles des en-têtes et cellules des tableaux dans les .jrxml.
    protected static final int TABLE_CELL_PADDING_H = 4;
    protected static final int TABLE_CELL_PADDING_V = 4;
    protected static final int TABLE_HEAD_PADDING_H = 3;
    protected static final int TABLE_HEAD_PADDING_V = 5;
    protected static final boolean TABLE_HEAD_BOLD = true;

    protected static final Function<JRColumnParameter, String> FIELD_MAPPER = p -> p.getFieldName();
    protected static final String CLASS_SUB_DATASET_FIELD = BUNDLE_KEY_CLASS;

    protected final Class<T> classToMap;
    private final List<String> avoidFields;

    // Couleur d'arrière-plan des titres des sections.
    private final String sectionTitleBackgroundColor;

    public AbstractJDomWriterSingleSpecificSheet(final Class<T> classToMap) {
        super();
        avoidFields = null;
        sectionTitleBackgroundColor = "#ffffff";
        this.classToMap = classToMap;
    }

    public AbstractJDomWriterSingleSpecificSheet(final Class<T> classToMap,
            final InputStream stream, final List<String> avoidFields, final String sectionTitleBackgroundColor)
            throws ParserConfigurationException, SAXException, IOException{
        super(stream);
        this.avoidFields = avoidFields;
        this.classToMap = classToMap;
        this.sectionTitleBackgroundColor = sectionTitleBackgroundColor;
    }

    /**
     * <p>This method writes a Jasper Reports template mapping the parameter class.</p>
     *
     * @throws TransformerException
     */
    public void write() throws TransformerException {

        // Remove elements before inserting fields.-----------------------------
        root.removeChild(title);
        root.removeChild(pageHeader);
        root.removeChild(columnHeader);
        root.removeChild(detail);

        // Modifies the template, based on the given class.---------------------
        writeObject();

        // Serializes the document.---------------------------------------------
        //DomUtilities.write(this.document, this.output);
        final Source source = new DOMSource(document);
        final Result result = new StreamResult(output);
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer trs = factory.newTransformer();
        trs.setOutputProperty(OutputKeys.INDENT, "yes");
        trs.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        trs.transform(source, result);
    }

    /**
     *
     */
    protected void writeFields(){
        final Method[] methods = classToMap.getMethods();
        for (final Method method : methods){
            if(PrinterUtilities.isSetter(method)){
                final String fieldName = getFieldNameFromSetter(method);
                if (avoidFields==null || !avoidFields.contains(fieldName)) {
                    writeField(method);
                }
            }
        }
    }

    /**
     *
     * @param elementClass The class to explore fields.
     * @param fieldParameters The field list
     * @param print If true, print the field list, if false print all the fields but the ones contained into given field list
     * @param subDatasetItemNb
     */
    protected void writeSubDataset(final Class elementClass, final List<JRColumnParameter> fieldParameters, final boolean print, final int subDatasetItemNb) {

        // Extraction des noms de champs.
        final List<String> fields = fieldParameters.stream().map(FIELD_MAPPER).collect(Collectors.toList());

        // Détermination du prédicat à utiliser.
        final Predicate<String> printPredicate = print
                ? (String fieldName) -> fields==null || fields.contains(fieldName)
                : (String fieldName) -> fields==null || !fields.contains(fieldName);

        // Écriture des champs.
        final Element subDatasetItem = (Element) root.getElementsByTagName(TAG_SUB_DATASET).item(subDatasetItemNb);
        final Method[] methods = elementClass.getMethods();
        for (final Method method : methods){
            if(PrinterUtilities.isSetter(method)){
                final String fieldName = getFieldNameFromSetter(method);
                if (printPredicate.test(fieldName)) {
                    writeSubDatasetField(method, subDatasetItem);
                }
            }
        }

        // Écriture d'un champ supplémentaire pour la classe de l'objet.
        writeSubDatasetField(CLASS_SUB_DATASET_FIELD, Class.class, subDatasetItem);
    }

    /**
     * <p>This method writes the fiels user by the Jasper Reports template.</p>
     * @param setter
     * @param subDataset
     */
    protected void writeSubDatasetField(final Method setter, final Element subDataset) {

        // Builds the name of the field.----------------------------------------
        final String fieldName = setter.getName().substring(3, 4).toLowerCase()
                        + setter.getName().substring(4);
        writeSubDatasetField(fieldName, setter.getParameterTypes()[0], subDataset);
    }


    protected void writeSubDatasetField(final String fieldName, final Class fieldClass, final Element subDataset) {

        // Creates the field element.-------------------------------------------
        final Element field = document.createElement(TAG_FIELD);
        field.setAttribute(ATT_NAME, fieldName);

        final Optional<String> canonicalName = getCanonicalName(fieldClass);
        if(canonicalName.isPresent()) field.setAttribute(ATT_CLASS, canonicalName.get());

        final Element fieldDescription = document.createElement(TAG_FIELD_DESCRIPTION);
        final CDATASection description = document.createCDATASection("Mettre ici une description du champ.");

        // Builds the DOM tree.-------------------------------------------------
        fieldDescription.appendChild(description);
        field.appendChild(fieldDescription);
        subDataset.appendChild(field);
    }

    /**
     * <p>This method writes the title of the template.</p>
     */
    protected void writeTitle() {
        writeTitle("Fiche détaillée de ", classToMap);
    }

    /**
     * Insertion d'un titre de section.
     *
     * @param sectionTitle Titre de la section.
     * @param height Hauteur du cadre.
     * @param margin Marge entre le cadre et le texte (haut et bas).
     * @param indent Intentation du texte.
     * @param textSize Taille de la police.
     * @param bold Vrai si le texte est en gras.
     * @param italic Vrai si le texte est en italique.
     * @param underlined Vrai si le texte est souligné.
     */
    protected void writeSectionTitle(final String sectionTitle, final int height, final int margin, final int indent,
            final int textSize, final boolean bold, final boolean italic, final boolean underlined){

        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);
        final Element frame = document.createElement(TAG_FRAME);
        final Element frameReportElement = document.createElement(TAG_REPORT_ELEMENT);
        frameReportElement.setAttribute(ATT_BACKCOLOR, sectionTitleBackgroundColor);
        frameReportElement.setAttribute(ATT_HEIGHT, String.valueOf(height));
        frameReportElement.setAttribute(ATT_MODE, JRUtils.Mode.OPAQUE.toString());
        frameReportElement.setAttribute(ATT_POSITION_TYPE, JRUtils.PositionType.FLOAT.toString());
        frameReportElement.setAttribute(ATT_WIDTH, String.valueOf(UTIL_WIDTH));
        frameReportElement.setAttribute(ATT_X, String.valueOf(0));
        frameReportElement.setAttribute(ATT_Y, String.valueOf(currentY));
        frame.appendChild(frameReportElement);

        final Element staticText = document.createElement(TAG_STATIC_TEXT);
        final Element staticTextReportElement = document.createElement(TAG_REPORT_ELEMENT);
        staticTextReportElement.setAttribute(ATT_HEIGHT, String.valueOf(height-2*margin));
        staticTextReportElement.setAttribute(ATT_WIDTH, String.valueOf(UTIL_WIDTH-indent));
        staticTextReportElement.setAttribute(ATT_X, String.valueOf(indent));
        staticTextReportElement.setAttribute(ATT_Y, String.valueOf(margin));
        staticText.appendChild(staticTextReportElement);

        final Element textElement = document.createElement(TAG_TEXT_ELEMENT);
        textElement.setAttribute(ATT_VERTICAL_ALIGNMENT, JRUtils.VerticalAlignment.MIDDLE.toString());
        final Element font = document.createElement(TAG_FONT);
        font.setAttribute(ATT_IS_BOLD, String.valueOf(bold));
        font.setAttribute(ATT_IS_ITALIC, String.valueOf(italic));
        font.setAttribute(ATT_IS_UNDERLINE, String.valueOf(underlined));
        font.setAttribute(ATT_SIZE, String.valueOf(textSize));
        textElement.appendChild(font);
        staticText.appendChild(textElement);

        final Element text = document.createElement(TAG_TEXT);
        final CDATASection textField = document.createCDATASection(sectionTitle);
        text.appendChild(textField);
        staticText.appendChild(text);
        frame.appendChild(staticText);
        band.appendChild(frame);
        currentY+=height;
    }

    /**
     * Insertion d'un saut de page.
     */
    protected void writeDetailPageBreak(){

        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);
        final Element pageBreak = document.createElement(TAG_BREAK);
        final Element pageBreakReportElement = document.createElement(TAG_REPORT_ELEMENT);
        pageBreakReportElement.setAttribute(ATT_HEIGHT, String.valueOf(1));
        pageBreakReportElement.setAttribute(ATT_WIDTH, String.valueOf(UTIL_WIDTH));
        pageBreakReportElement.setAttribute(ATT_X, String.valueOf(0));
        pageBreakReportElement.setAttribute(ATT_Y, String.valueOf(currentY));

        currentY++;
        pageBreak.appendChild(pageBreakReportElement);
        band.appendChild(pageBreak);
    }

    /**
     *
     * @param clazz Type d'objets contenus dans le tableau.
     * @param fields Liste de noms de champs. Il peut s'agir d'une liste restrictive des noms de champs à énumérer
     * dans le tableau à l'exclusion des autres champs. Il peut également s'agir d'une liste de champs à exclure du
     * tableau. Dans le premier cas uniquement, l'ordre dans lequel sont énumérés les noms de champs détermine l'ordre
     * des colonnes dans le tableau. Dans tous les cas, les noms de champs doivent être cohérents avec le type d'objet
     * du tableau.
     * @param print Détermine si les noms de champs énumérés doivent être considérés comme une liste restrictive (vrai)
     * ou comme une liste de champs à éviter (faux).
     * @param datasourceParameter Nom de la source de données, tel que connu par JasperReports.
     * @param datasetName
     * @param height Hauteur réservée pour le tableau.
     * @param fontSize Taille de police dans le tableau.
     * @param headerHeight Hauteur des en-têtes de colonnes.
     * @param detailCellHeight Hauteur des cellules.
     * @param fillWidth Détermine si la dernière colonne doit exploiter le maximum d'espace disponible.
     */
    protected void writeTable(final Class clazz, final Collection<JRColumnParameter> fields, final boolean print,
            final String datasourceParameter, final String datasetName, final int height,
            final int fontSize, final int headerHeight, final int detailCellHeight,
            final boolean fillWidth){

        // Extraction des noms de champs et des largeurs de colonnes.
        final Collection<String> fieldNames = new ArrayList<>();

        /*
        Suite de coefficients de pondération de largeur à appliquer à chaque champ dans l'ordre de leur mention.

        1- Le coefficient de pondération de base d'une colonne vaut 1.f par défaut.

        2- Si les coefficients de toutes les colonnes sont identiques (quelle que soit leur valeur), toutes les colonnes
        auront la même largeur.

        3- Si le coefficient d'une colonne A vaut 1.f, celui d'une colonne B vaut .25f et celui d'une colonne C vaut 2.f,
        la colonne C sera environ deux fois plus large que la colonne A et huit fois plus large que la colonne B.

        4- Les coefficients ne sont pris en compte que lorsque la liste des champs spécifie ceux qui doivent affichés
        exclusivement ; ils sont ignorés si la liste contient des champs à exclure.
        */
        final float[] widthCoeffs = new float[fields.size()];
        float coeffSum = 0.f;
        int i = 0;
        for(final JRColumnParameter fieldParam : fields){
            fieldNames.add(fieldParam.getFieldName());
            widthCoeffs[i] = fieldParam.getColumnWidthCoeff();
            coeffSum+=widthCoeffs[i];
            i++;
        }

        final Predicate<String> printPredicate = print
                ? (String fieldName) -> fieldNames.contains(fieldName)
                : (String fieldName) -> !fieldNames.contains(fieldName);

        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);

        final Element componentElement = document.createElement(TAG_COMPONENT_ELEMENT);
        final Element componentElementReportElement = document.createElement(TAG_REPORT_ELEMENT);
        componentElementReportElement.setAttribute(ATT_KEY, "table");
//        componentElementReportElement.setAttribute(ATT_STYLE, "table");
        componentElementReportElement.setAttribute(ATT_X, String.valueOf(0));
        componentElementReportElement.setAttribute(ATT_Y, String.valueOf(currentY));
        componentElementReportElement.setAttribute(ATT_WIDTH, String.valueOf(UTIL_WIDTH));
        componentElementReportElement.setAttribute(ATT_HEIGHT, String.valueOf(height));
        componentElementReportElement.setAttribute(ATT_POSITION_TYPE, JRUtils.PositionType.FLOAT.toString());
//        componentElementReportElement.setAttribute(ATT_IS_STRETCH_WITH_OVERFLOW, String.valueOf(true));

        // Set the table element
        final Element table = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE);
        int cumulatedWidth = 0;

        final Element datasetRun = document.createElementNS(URI_JRXML, TAG_DATASET_RUN);
        datasetRun.setAttribute(ATT_SUB_DATASET, datasetName);
        final Element datasourceExpression = document.createElementNS(URI_JRXML, TAG_DATA_SOURCE_EXPRESSION);

        final CDATASection datasourceExpressionField = document.createCDATASection("(("+ObjectDataSource.class.getCanonicalName()+") $F{"+datasourceParameter+"})");//.cloneDataSource()

        datasourceExpression.appendChild(datasourceExpressionField);
        datasetRun.appendChild(datasourceExpression);

        table.appendChild(datasetRun);

        ////////////////////////////////////////////////////////////////////////
        // COMPUTE NUMBER OF COLUMNS AND COLUMN WIDTH
        ////////////////////////////////////////////////////////////////////////
        int nbColumns=0;
        /*
        Si la liste des champs contient les champs à imprimer et non pas à éviter alors le nombre de champs est
        directement donnée par la taille de la liste.
        */
        if(print) {
            nbColumns=fieldNames.size();
        }
        // Sinon il faut faire un premier parcours pour calculer le nombre de colonnes
        else {
            for(final Method method : clazz.getMethods()){
                if(PrinterUtilities.isSetter(method)){
                    // Retrives the field name from the setter name.----------------
                    final String fieldName = getFieldNameFromSetter(method);
                    if(printPredicate.test(fieldName)) {
                        nbColumns++;
                    }
                }
            }
        }

        // If class is abstract, add one column to print class name
        if(Modifier.isAbstract(clazz.getModifiers())){
            nbColumns++;
        }

        // Calcul de la largeur de base d'une colonne en fonction du nombre, sans tenir compte des coefficients de largeur.
        final int baseColumnWidth = UTIL_WIDTH/nbColumns;

        if(Modifier.isAbstract(clazz.getModifiers())){
            coeffSum+=1.f;
            cumulatedWidth+=baseColumnWidth;
            writeColumn("Type",
                    () -> document.createCDATASection("$F{class}==null ? \""+NULL_REPLACEMENT+"\" : java.util.ResourceBundle.getBundle($F{class}.getName()).getString(\""+BUNDLE_KEY_CLASS+"\")"),
                    table, baseColumnWidth, fontSize, TABLE_HEAD_BOLD, true, headerHeight, detailCellHeight);
        }

        ////////////////////////////////////////////////////////////////////////
        // BUILD COLUMNS
        ////////////////////////////////////////////////////////////////////////
        final ResourceBundle rb = ResourceBundle.getBundle(clazz.getName());
        /*
        Si la liste des champs contient les champs à imprimer et non pas à éviter on se base sur l'ordre de la liste
        pour générer l'ordre des colonnes.
        */
        if(print){
            // Indexation des initialiseurs par les noms de champs.
            final Map<String, Method> settersByFieldName = new HashMap<>();
            for(final Method method : clazz.getMethods()){
                if(PrinterUtilities.isSetter(method)){
                    settersByFieldName.put(getFieldNameFromSetter(method), method);
                }
            }

            int coeffIndex = 0; // Index dans le tableau des coefficients de pondération des largeurs de colonnes.
            int fieldIndex = 0;
            for(final JRColumnParameter field : fields){
                fieldIndex++;
                final String fieldName = field.getFieldName();
                final String headerName = field.getHeaderName();
                final JRColumnParameter.DisplayPolicy displayPolicy = field.getDisplayPolicy();
                final float coeff;
                if(coeffSum!=0.f) {
                    coeff = widthCoeffs[coeffIndex]*nbColumns/coeffSum;
                    SirsCore.LOGGER.log(Level.FINEST, "raw coeff = {0}", widthCoeffs[coeffIndex]);
                    coeffIndex++;
                }
                else {
                    coeff = 1.f;
                }
                SirsCore.LOGGER.log(Level.FINEST, "c = {0} sum = {1} width coeff = {2}", new Object[]{coeffIndex, coeffSum, coeff});

                /*
                On calcule la largeur de la colonne.
                Dans le cas général, le calcul est obtenu avec le coefficient, mais pour la dernière colonne, on ajuste
                à la largeur de la page si cela est demandé.
                */
                final int ajustedColumnWidth = (fillWidth && fieldIndex==fieldNames.size()) ? (UTIL_WIDTH-cumulatedWidth) : Math.round(baseColumnWidth*coeff);

                cumulatedWidth+=ajustedColumnWidth;
                writeColumn(headerName == null ? rb.getString(fieldName) : headerName, getCDATASupplierFromSetter(settersByFieldName.get(fieldName), displayPolicy),
                        table, ajustedColumnWidth, fontSize, TABLE_HEAD_BOLD, field.isBold(), headerHeight, detailCellHeight);
            }
        }
        /*
        Sinon, on n'a pas d'ordre particulier sur lequel se baser et on parcours donc en premier les méthodes pour trouver
        les noms des champs à imprimer. Dans ce cas, on ne peut pas paramettrer les noms de colonnes
        */
        else{
            for(final Method method : clazz.getMethods()){
                if(PrinterUtilities.isSetter(method)){
                    // Retrives the field name from the setter name.----------------
                    final String fieldName = getFieldNameFromSetter(method);
                    if(printPredicate.test(fieldName)){
                        cumulatedWidth+=baseColumnWidth;
                        writeColumn(rb.getString(fieldName), getCDATASupplierFromSetter(method, JRColumnParameter.DisplayPolicy.LABEL),
                                table, baseColumnWidth, fontSize, TABLE_HEAD_BOLD, false, headerHeight, detailCellHeight);
                    }
                }
            }
        }

        // Centrage de la table
        componentElementReportElement.setAttribute(ATT_X, String.valueOf(Math.round((UTIL_WIDTH - cumulatedWidth)/2.f)));

        componentElement.appendChild(componentElementReportElement);
        componentElement.appendChild(table);

        band.appendChild(componentElement);
        currentY+=height;
    }

    private Supplier<CDATASection> getCDATASupplierFromSetter(final Method setter, final JRColumnParameter.DisplayPolicy displayPolicy) {

        final String fieldName = getFieldNameFromSetter(setter);
        final Class fieldClass = setter.getParameterTypes()[0];

        return () -> {

            if(fieldClass==Boolean.class || (fieldClass!=null && BOOLEAN_PRIMITIVE_NAME.equals(fieldClass.getName()))){
                return document.createCDATASection("$F{"+fieldName+"}==null ? \""+NULL_REPLACEMENT+"\" : ($F{"+fieldName+"} ? \""+TRUE_REPLACEMENT+"\" : \""+FALSE_REPLACEMENT+"\")");
            }
            else {
                try {
                    // Récupération de la classe dans laquelle le champ est déclaré.
                    final Class declaringClass = setter.getDeclaringClass();

                    // On vérifie s'il s'agit d'un identifiant de référence. L'annotation est portée par le getter (pas par le champ ni par le setter).
                    final Method getter = declaringClass.getMethod(setter.getName().replaceFirst("set", "get"));
                    if(getter!=null && fieldClass!=null){
                        final Reference annotation = getter.getDeclaredAnnotation(Reference.class);
                        if(annotation!=null){
                            // Si c'est en fait une collection de références.
                            if(Iterable.class.isAssignableFrom(fieldClass)){
                                return document.createCDATASection(JRXMLUtil.dynamicDisplayLabels(fieldName));
                            }
                            // Si c'est une référence unique.
                            else if(String.class.isAssignableFrom(fieldClass)) {
                                switch(displayPolicy){
                                    case REFERENCE_LABEL_AND_CODE:
                                        if(ReferenceType.class.isAssignableFrom(annotation.ref())){
                                            return document.createCDATASection("$F{"+fieldName+"}==null ? \""+NULL_REPLACEMENT+"\" : $F{"+fieldName+"}");
                                        }
                                    case REFERENCE_CODE:
                                        if(ReferenceType.class.isAssignableFrom(annotation.ref())){
                                            return document.createCDATASection(JRXMLUtil.dynamicDisplayReferenceCode(fieldName));
                                        }
                                    default:
                                        return document.createCDATASection(JRXMLUtil.dynamicDisplayLabel(fieldName));
                                }
                            }
                        }
                    }

                    // S'il ne s'agit pas d'une référence :
                    return document.createCDATASection("$F{"+fieldName+"}==null ? \""+NULL_REPLACEMENT+"\" : $F{"+fieldName+"}");
                } catch (NoSuchMethodException | SecurityException ex) {
                    // Si pour une raison quelconque l'analyse du champ échoue, on utilise un comportement par défaut.
                    SirsCore.LOGGER.log(Level.INFO, "Impossible d''analyser le champ {0}. On utilisera le comportement par d\u00e9faut.", fieldName);
                    return document.createCDATASection("$F{"+fieldName+"}==null ? \""+NULL_REPLACEMENT+"\" : $F{"+fieldName+"}");
                }
            }
        };
    };


    private void writeColumn(final String header, final Supplier<CDATASection> cellSupplier, final Element table, final int columnWidth,
            final int fontSize, final boolean headerBold, final boolean cellBold, final int headerHeight, final int detailCellHeight) {

        final Element column = document.createElementNS(URI_JRXML_COMPONENTS, TAG_COLUMN);
        column.setAttribute(ATT_WIDTH, String.valueOf(columnWidth));

        // Table header and footer
        final Element tableHeader = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE_HEADER);
        tableHeader.setAttribute(ATT_HEIGHT, String.valueOf(0));

        final Element tableFooter = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE_FOOTER);
        tableFooter.setAttribute(ATT_HEIGHT, String.valueOf(0));

        // Column header
        final Element jrColumnHeader = document.createElementNS(URI_JRXML_COMPONENTS, TAG_COLUMN_HEADER);
        jrColumnHeader.setAttribute(ATT_STYLE, "table_CH");
        jrColumnHeader.setAttribute(ATT_HEIGHT, String.valueOf(headerHeight));

        final Element staticText = document.createElementNS(URI_JRXML, TAG_STATIC_TEXT);

        final Element staticTextReportElement = document.createElementNS(URI_JRXML, TAG_REPORT_ELEMENT);
        staticTextReportElement.setAttribute(ATT_X, String.valueOf(0));
        staticTextReportElement.setAttribute(ATT_Y, String.valueOf(0));
        staticTextReportElement.setAttribute(ATT_WIDTH, String.valueOf(columnWidth-2*TABLE_HEAD_PADDING_H));
        staticTextReportElement.setAttribute(ATT_HEIGHT, String.valueOf(headerHeight-2*TABLE_HEAD_PADDING_V));
        staticText.appendChild(staticTextReportElement);

        final Element textElement = document.createElement(TAG_TEXT_ELEMENT);
        final Element font = document.createElement(TAG_FONT);
        font.setAttribute(ATT_IS_BOLD, String.valueOf(headerBold));
        font.setAttribute(ATT_SIZE, String.valueOf(fontSize));
        textElement.appendChild(font);
        staticText.appendChild(textElement);

        final Element text = document.createElementNS(URI_JRXML, TAG_TEXT);
        final CDATASection labelField = document.createCDATASection(header);
        text.appendChild(labelField);

        staticText.appendChild(text);
        jrColumnHeader.appendChild(staticText);

        // Column footer
        final Element jrColumnFooter = document.createElementNS(URI_JRXML_COMPONENTS, TAG_COLUMN_FOOTER);
        jrColumnFooter.setAttribute(ATT_STYLE, "table_CH");
        jrColumnFooter.setAttribute(ATT_HEIGHT, String.valueOf(0));


        // Detail cell
        final Element detailCell = document.createElementNS(URI_JRXML_COMPONENTS, TAG_DETAIL_CELL);
        detailCell.setAttribute(ATT_STYLE, "table_TD");
        detailCell.setAttribute(ATT_HEIGHT, String.valueOf(detailCellHeight));

        final Element textField = document.createElementNS(URI_JRXML, TAG_TEXT_FIELD);
        textField.setAttribute(ATT_IS_STRETCH_WITH_OVERFLOW, String.valueOf(true));

        final Element textFieldReportElement = document.createElement(TAG_REPORT_ELEMENT);
        textFieldReportElement.setAttribute(ATT_X, String.valueOf(0));
        textFieldReportElement.setAttribute(ATT_Y, String.valueOf(0));
        textFieldReportElement.setAttribute(ATT_WIDTH, String.valueOf(columnWidth-2*TABLE_CELL_PADDING_H));
        textFieldReportElement.setAttribute(ATT_HEIGHT, String.valueOf(detailCellHeight-2*TABLE_CELL_PADDING_V));
        textField.appendChild(textFieldReportElement);

        final Element detailTextElement = document.createElement(TAG_TEXT_ELEMENT);
        final Element detailFont = document.createElement(TAG_FONT);
        detailFont.setAttribute(ATT_IS_BOLD, String.valueOf(cellBold));
        detailFont.setAttribute(ATT_SIZE, String.valueOf(fontSize));
        detailTextElement.appendChild(detailFont);
        //detailTextElement.setAttribute(ATT_MARKUP, (markup==null ? JRUtils.Markup.NONE : markup).toString());
        textField.appendChild(detailTextElement);

        final Element textFieldExpression = document.createElement(TAG_TEXT_FIELD_EXPRESSION);

        textFieldExpression.appendChild(cellSupplier.get());

        textField.appendChild(textFieldExpression);
        detailCell.appendChild(textField);

        column.appendChild(tableHeader);
        column.appendChild(tableFooter);
        column.appendChild(jrColumnHeader);
        column.appendChild(jrColumnFooter);
        column.appendChild(detailCell);

        table.appendChild(column);
    }

    protected abstract void writeObject();
}
