
package fr.sirs.util;

import static fr.sirs.SIRS.BUNDLE_KEY_CLASS;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.Photo;
import fr.sirs.core.model.Prestation;
import static fr.sirs.util.JRUtils.ATT_BACKCOLOR;
import static fr.sirs.util.JRUtils.ATT_CLASS;
import static fr.sirs.util.JRUtils.ATT_HEIGHT;
import static fr.sirs.util.JRUtils.ATT_IS_BOLD;
import static fr.sirs.util.JRUtils.ATT_IS_ITALIC;
import static fr.sirs.util.JRUtils.ATT_IS_STRETCH_WITH_OVERFLOW;
import static fr.sirs.util.JRUtils.ATT_IS_UNDERLINE;
import static fr.sirs.util.JRUtils.ATT_KEY;
import static fr.sirs.util.JRUtils.ATT_MARKUP;
import static fr.sirs.util.JRUtils.ATT_MODE;
import static fr.sirs.util.JRUtils.ATT_NAME;
import static fr.sirs.util.JRUtils.ATT_POSITION_TYPE;
import static fr.sirs.util.JRUtils.ATT_SIZE;
import static fr.sirs.util.JRUtils.ATT_STYLE;
import static fr.sirs.util.JRUtils.ATT_SUB_DATASET;
import static fr.sirs.util.JRUtils.ATT_WIDTH;
import static fr.sirs.util.JRUtils.ATT_X;
import static fr.sirs.util.JRUtils.ATT_Y;
import static fr.sirs.util.JRUtils.BOOLEAN_PRIMITIVE_NAME;
import fr.sirs.util.JRUtils.Markup;
import fr.sirs.util.JRUtils.PositionType;
import static fr.sirs.util.JRUtils.TAG_BAND;
import static fr.sirs.util.JRUtils.TAG_COLUMN;
import static fr.sirs.util.JRUtils.TAG_COLUMN_FOOTER;
import static fr.sirs.util.JRUtils.TAG_COLUMN_HEADER;
import static fr.sirs.util.JRUtils.TAG_COMPONENT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_DATASET_RUN;
import static fr.sirs.util.JRUtils.TAG_DATA_SOURCE_EXPRESSION;
import static fr.sirs.util.JRUtils.TAG_DETAIL;
import static fr.sirs.util.JRUtils.TAG_DETAIL_CELL;
import static fr.sirs.util.JRUtils.TAG_FIELD;
import static fr.sirs.util.JRUtils.TAG_FIELD_DESCRIPTION;
import static fr.sirs.util.JRUtils.TAG_FONT;
import static fr.sirs.util.JRUtils.TAG_FRAME;
import static fr.sirs.util.JRUtils.TAG_LAST_PAGE_FOOTER;
import static fr.sirs.util.JRUtils.TAG_PAGE_FOOTER;
import static fr.sirs.util.JRUtils.TAG_PAGE_HEADER;
import static fr.sirs.util.JRUtils.TAG_REPORT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_STATIC_TEXT;
import static fr.sirs.util.JRUtils.TAG_SUBREPORT;
import static fr.sirs.util.JRUtils.TAG_SUBREPORT_EXPRESSION;
import static fr.sirs.util.JRUtils.TAG_SUB_DATASET;
import static fr.sirs.util.JRUtils.TAG_TABLE;
import static fr.sirs.util.JRUtils.TAG_TABLE_FOOTER;
import static fr.sirs.util.JRUtils.TAG_TABLE_HEADER;
import static fr.sirs.util.JRUtils.TAG_TEXT;
import static fr.sirs.util.JRUtils.TAG_TEXT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_TEXT_FIELD;
import static fr.sirs.util.JRUtils.TAG_TEXT_FIELD_EXPRESSION;
import static fr.sirs.util.JRUtils.TAG_TITLE;
import static fr.sirs.util.JRUtils.URI_JRXML;
import static fr.sirs.util.JRUtils.URI_JRXML_COMPONENTS;
import static fr.sirs.util.JRUtils.getCanonicalName;
import static fr.sirs.util.PrinterUtilities.getFieldNameFromSetter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
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
 */
public class JRDomWriterDesordreSheet extends AbstractJDomWriter {
    
    // Template elements.
    private final Element title;
    private final Element pageHeader;
    private final Element columnHeader;
    private final Element detail;
    private final Element columnFooter;
    private final Element pageFooter;
    private final Element lastPageFooter;
    private File output;
    
    // Dynamic template parameters.
    private int fields_interline;
    
    // Static template parameters.
    private static final String FIELDS_VERTICAL_ALIGNMENT = "Middle";
    private static final String FIELDS_FONT_NAME = "Serif";
//    private static final int FIELDS_HEIGHT = 12;
    //private static final String DATE_PATTERN = "dd/MM/yyyy à hh:mm:ss";
    private static final int INDENT_LABEL = 10;
    private static final int LABEL_WIDTH = 140;
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int COLUMN_WIDTH = 555;
    private static final int LEFT_MARGIN = 20;
    private static final int RIGHT_MARGIN = 20;
    private static final int TOP_MARGIN = 20;
    private static final int BOTTOM_MARGIN = 20;
    
    private static final String NULL_REPLACEMENT = "Non renseigné";
    private static final String TRUE_REPLACEMENT = "Oui";
    private static final String FALSE_REPLACEMENT = "Non";
    
    public static final String OBSERVATIONS_DATASET = "Observations Dataset";
    public static final String OBSERVATION_TABLE_DATA_SOURCE = "OBSERVATION_TABLE_DATA_SOURCE";
    public static final String PRESTATIONS_DATASET = "Prestations Dataset";
    public static final String PRESTATION_TABLE_DATA_SOURCE = "PRESTATION_TABLE_DATA_SOURCE";
    public static final String PHOTO_TABLE_DATA_SOURCE = "PHOTO_TABLE_DATA_SOURCE";
    public static final String PHOTOS_SUBREPORT = "PHOTOS_SUBREPORT";
    
    private final List<String> avoidDesordreFields;
    private final List<String> avoidObservationFields;
    private final List<String> avoidPrestationFields;
    
    
    private JRDomWriterDesordreSheet(){
        super();
        this.title = null; 
        this.pageHeader = null;
        this.columnHeader = null;
        this.detail = null;
        this.columnFooter = null;
        this.pageFooter = null;
        this.lastPageFooter = null;
        
        this.fields_interline = 8;
        avoidDesordreFields = null;
        avoidObservationFields = null;
        avoidPrestationFields = null;
    }
    
    public JRDomWriterDesordreSheet(final InputStream stream, 
            final List<String> avoidDesordreFields,
            final List<String> avoidObservationFields,
            final List<String> avoidPrestationFields) throws ParserConfigurationException, SAXException, IOException {
        super(stream);
        title = (Element) root.getElementsByTagName(TAG_TITLE).item(0);
        pageHeader = (Element) root.getElementsByTagName(TAG_PAGE_HEADER).item(0);
        columnHeader = (Element) root.getElementsByTagName(TAG_COLUMN_HEADER).item(0);
        detail = (Element) this.root.getElementsByTagName(TAG_DETAIL).item(0);
        columnFooter = (Element) root.getElementsByTagName(TAG_COLUMN_FOOTER).item(0);
        pageFooter = (Element) root.getElementsByTagName(TAG_PAGE_FOOTER).item(0);
        lastPageFooter = (Element) root.getElementsByTagName(TAG_LAST_PAGE_FOOTER).item(0);
        
        fields_interline = 8;
        this.avoidDesordreFields = avoidDesordreFields;
        this.avoidObservationFields = avoidObservationFields;
        this.avoidPrestationFields = avoidPrestationFields;
    }
    
    /**
     * This setter changes the default fields interline.
     * @param fieldsInterline 
     */
    public void setFieldsInterline(int fieldsInterline){
        fields_interline = fieldsInterline;
    }
    
    /**
     * <p>This method sets the output to write the modified DOM in.</p>
     * @param output 
     */
    public void setOutput(final File output) {
        this.output = output;
    } 
    
    /**
     * <p>This method writes a Jasper Reports template mapping the parameter class.</p>
     * @param desordre
     * @throws TransformerException
     * @throws IOException
     */
    public void write(final Desordre desordre) throws TransformerException, IOException, Exception {
        
        // Remove elements before inserting fields.-----------------------------
        root.removeChild(this.title);
        root.removeChild(this.pageHeader);
        root.removeChild(this.columnHeader);
        root.removeChild(this.detail);
        
        // Modifies the template, based on the given class.---------------------
        writeObject(desordre);
        
        // Serializes the document.---------------------------------------------
        //DomUtilities.write(this.document, this.output);
        final Source source = new DOMSource(this.document);
        final Result result = new StreamResult(this.output);
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer trs = factory.newTransformer();
        trs.setOutputProperty(OutputKeys.INDENT, "yes");
        trs.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        trs.transform(source, result);
    }

    /**
     * <p>This method modifies the body of the DOM.</p>
     * @param classToMap
     * @param avoidFields field names to avoid.
     * @throws Exception 
     */
    private void writeObject(final Desordre desordre) {
        
        writeSubDataset(Observation.class, avoidDesordreFields, (Element) root.getElementsByTagName(TAG_SUB_DATASET).item(0));
        writeSubDataset(Prestation.class, avoidPrestationFields, (Element) root.getElementsByTagName(TAG_SUB_DATASET).item(1));
        
        
        // Sets the initial fields used by the template.------------------------
        final Method[] methods = desordre.getClass().getMethods();
        for (final Method method : methods){
            if(PrinterUtilities.isSetter(method)){
                final String fieldName = getFieldNameFromSetter(method);
                if (avoidDesordreFields==null || !avoidDesordreFields.contains(fieldName)) {
                    writeField(method);
                }
            }
        }
        
        // Modifies the title block.--------------------------------------------
        writeTitle(desordre.getClass());
        
        // Writes the headers.--------------------------------------------------
        writePageHeader();
        writeColumnHeader();
        
        // Builds the body of the Jasper Reports template.----------------------
        writeDetail(desordre);
    }
    
    
    
    private void writeSubDataset(final Class<? extends fr.sirs.core.model.Element> elementClass, final List<String> avoidFields, final Element subDataset){
        
        final Method[] methods = elementClass.getMethods();
        for (final Method method : methods){
            if(PrinterUtilities.isSetter(method)){
                final String fieldName = getFieldNameFromSetter(method);
                if (avoidFields==null || !avoidFields.contains(fieldName)) {
                    writeSubDatasetField(method, subDataset);
                }
            }
        }
    }
        
    /**
     * <p>This method writes the fiels user by the Jasper Reports template.</p>
     * @param propertyType must be a setter method starting by "set"
     */
    private void writeSubDatasetField(final Method method, final Element subDataset) {
        
        // Builds the name of the field.----------------------------------------
        final String fieldName = method.getName().substring(3, 4).toLowerCase() 
                        + method.getName().substring(4);
        
        // Creates the field element.-------------------------------------------
        final Element field = document.createElement(TAG_FIELD);
        field.setAttribute(ATT_NAME, fieldName);
        
        final Optional<String> canonicalName = getCanonicalName(method.getParameterTypes()[0]);
        if(canonicalName.isPresent()) field.setAttribute(ATT_CLASS, canonicalName.get());
        
        final Element fieldDescription = document.createElement(TAG_FIELD_DESCRIPTION);
        final CDATASection description = document.createCDATASection("Mettre ici une description du champ.");
        
        // Builds the DOM tree.-------------------------------------------------
        fieldDescription.appendChild(description);
        field.appendChild(fieldDescription);
        subDataset.appendChild(field);
    }
        
    /**
     * <p>This method writes the fiels user by the Jasper Reports template.</p>
     * @param method must be a setter method starting by "set"
     */
    private void writeField(final Method method) {
        
        // Builds the name of the field.----------------------------------------
        final String fieldName = method.getName().substring(3, 4).toLowerCase() 
                        + method.getName().substring(4);
        
        // Creates the field element.-------------------------------------------
        final Element field = document.createElement(TAG_FIELD);
        field.setAttribute(ATT_NAME, fieldName);
        
        final Optional<String> canonicalName = getCanonicalName(method.getParameterTypes()[0]);
        if(canonicalName.isPresent()) field.setAttribute(ATT_CLASS, canonicalName.get());
        
        final Element fieldDescription = document.createElement(TAG_FIELD_DESCRIPTION);
        final CDATASection description = document.createCDATASection("Mettre ici une description du champ.");
        
        // Builds the DOM tree.-------------------------------------------------
        fieldDescription.appendChild(description);
        field.appendChild(fieldDescription);
        root.appendChild(field);
    }
    
    /**
     * <p>This method writes the title of the template.</p>
     * @param classToMap 
     */
    private void writeTitle(final Class classToMap) {
        
        // Looks for the title content.-----------------------------------------
        final Element band = (Element) this.title.getElementsByTagName(TAG_BAND).item(0);
        final Element staticText = (Element) band.getElementsByTagName(TAG_STATIC_TEXT).item(0);
        final Element text = (Element) staticText.getElementsByTagName(TAG_TEXT).item(0);
        
        // Sets the title.------------------------------------------------------
        final String className;
        final ResourceBundle resourceBundle = ResourceBundle.getBundle(classToMap.getName(), Locale.getDefault(),
                Thread.currentThread().getContextClassLoader());
        if(resourceBundle!=null){
            className = (resourceBundle.containsKey(BUNDLE_KEY_CLASS)) ?
                    resourceBundle.getString(BUNDLE_KEY_CLASS) : classToMap.getSimpleName();
        }
        else{
            className = classToMap.getSimpleName();
        }
        ((CDATASection) text.getChildNodes().item(0)).setData("Fiche détaillée de " + className);
        
        // Builds the DOM tree.-------------------------------------------------
        this.root.appendChild(this.title);
    }
    
    private void writePageHeader(){
        this.root.appendChild(this.pageHeader);
    }
    
    private void writeColumnHeader(){
        this.root.appendChild(this.columnHeader);
    }
    
    private int currentY = 0;
    /**
     * <p>This method writes the content of the detail element.</p>
     * @param classToMap
     * @throws Exception 
     */
    private void writeDetail(final Desordre desordre) {
        
        final Class classToMap = desordre.getClass();
        
        final ResourceBundle resourceBundle = ResourceBundle.getBundle(classToMap.getName(), Locale.getDefault(),
                Thread.currentThread().getContextClassLoader());
        
        /*----------------------------------------------------------------------
        ATTRIBUTS DU DESORDRE
        ----------------------------------------------------------------------*/
        // Loops over the method looking for setters (based on the field names).
        final Method[] methods = classToMap.getMethods();
        for (final Method method : methods){
            if(PrinterUtilities.isSetter(method)){
                
                // Retrives the field name from the setter name.----------------
                final String fieldName = getFieldNameFromSetter(method);
                final Class fieldClass = method.getParameterTypes()[0];
                
                // Provides a multiplied height for comment and description fields.
                final Markup markup;
                if (fieldName.contains("escript") || fieldName.contains("omment")){
                    markup = Markup.HTML;
                } else {
                    markup = Markup.NONE;
                }
                
                // Writes the field.--------------------------------------------
                // On n'écrit plus les champs dynamiquement : ceux-ci figurent en dur dans le template
//                if(avoidDesordreFields==null || !avoidDesordreFields.contains(fieldName)){
//                    writeDetailField(fieldName, fieldClass, markup, resourceBundle);
//                    // Increment Y between each field
//                    currentY+=(FIELDS_HEIGHT+fields_interline);
//                }
            }
        }
        
        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);
        currentY = Integer.valueOf(band.getAttribute(ATT_HEIGHT));
        
        /*----------------------------------------------------------------------
        TABLEAU DES OBSERVATIONS
        ----------------------------------------------------------------------*/
        if(desordre.getObservations()!=null && !desordre.getObservations().isEmpty()){
            currentY+=2;
            writeSectionTitle("Observations", 15, 2, 10, 9);
            currentY+=2;
            writeTable(Observation.class, avoidObservationFields, OBSERVATION_TABLE_DATA_SOURCE, OBSERVATIONS_DATASET, 30);
            currentY+=2;
        }
        
        /*----------------------------------------------------------------------
        TABLEAU DES PRESTATIONS
        ----------------------------------------------------------------------*/
        if(desordre.getPrestationIds()!=null && !desordre.getPrestationIds().isEmpty()){
            currentY+=2;
            writeSectionTitle("Prestations", 15, 2, 10, 9);
            currentY+=2;
            writeTable(Prestation.class, avoidPrestationFields, PRESTATION_TABLE_DATA_SOURCE, PRESTATIONS_DATASET, 30);
            currentY+=2;
        }
        
        /*----------------------------------------------------------------------
        SOUS-RAPPORTS DES PHOTOS
        ----------------------------------------------------------------------*/
        final List<Photo> photos = new ArrayList<>();
        for(final Observation observation : desordre.getObservations()){
            final List<Photo> obsPhotos = observation.getPhotos();
            if(obsPhotos!=null && !obsPhotos.isEmpty()){
                photos.addAll(obsPhotos);
            }
        }
        if(!photos.isEmpty()){
            currentY+=2;
            includePhotoSubreport(64);
        }
        
        // Sizes the detail element given to the field number.------------------
        band.setAttribute(ATT_HEIGHT, String.valueOf(currentY));
        
        // Builds the DOM tree.-------------------------------------------------
        root.appendChild(detail);
    }
    
    private void writeSectionTitle(final String sectionTitle, final int height, final int margin, final int indent, final int textSize){
        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);
        final Element frame = document.createElement(TAG_FRAME);
        final Element frameReportElement = document.createElement(TAG_REPORT_ELEMENT);
        frameReportElement.setAttribute(ATT_BACKCOLOR, "#CB5C5C");
        frameReportElement.setAttribute(ATT_HEIGHT, String.valueOf(height));
        frameReportElement.setAttribute(ATT_MODE, JRUtils.Mode.OPAQUE.toString());
        frameReportElement.setAttribute(ATT_POSITION_TYPE, JRUtils.PositionType.FLOAT.toString());
        frameReportElement.setAttribute(ATT_WIDTH, String.valueOf(PAGE_WIDTH-LEFT_MARGIN-RIGHT_MARGIN));
        frameReportElement.setAttribute(ATT_X, String.valueOf(0));
        frameReportElement.setAttribute(ATT_Y, String.valueOf(currentY));
        frame.appendChild(frameReportElement);
        
        final Element staticText = document.createElement(TAG_STATIC_TEXT);
        final Element staticTextReportElement = document.createElement(TAG_REPORT_ELEMENT);
        staticTextReportElement.setAttribute(ATT_HEIGHT, String.valueOf(height-2*margin));
        staticTextReportElement.setAttribute(ATT_WIDTH, String.valueOf(PAGE_WIDTH-LEFT_MARGIN-RIGHT_MARGIN-indent));
        staticTextReportElement.setAttribute(ATT_X, String.valueOf(indent));
        staticTextReportElement.setAttribute(ATT_Y, String.valueOf(margin));
        staticText.appendChild(staticTextReportElement);
        
        final Element textElement = document.createElement(TAG_TEXT_ELEMENT);
        final Element font = document.createElement(TAG_FONT);
        font.setAttribute(ATT_IS_BOLD, String.valueOf(true));
        font.setAttribute(ATT_IS_ITALIC, String.valueOf(true));
        font.setAttribute(ATT_IS_UNDERLINE, String.valueOf(true));
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
    
    private void includePhotoSubreport(final int height){
        
        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);
        
        final Element subReport = document.createElement(TAG_SUBREPORT);
        final Element reportElement = document.createElement(TAG_REPORT_ELEMENT);
//        reportElement.setAttribute(ATT_KEY, "table");
//        reportElement.setAttribute(ATT_STYLE, "table");
        reportElement.setAttribute(ATT_X, String.valueOf(0));
        reportElement.setAttribute(ATT_Y, String.valueOf(currentY));
//        componentElementReportElement.setAttribute(ATT_Y, String.valueOf(0));
        reportElement.setAttribute(ATT_WIDTH, String.valueOf(802));
        reportElement.setAttribute(ATT_HEIGHT, String.valueOf(height));
        reportElement.setAttribute(ATT_POSITION_TYPE, PositionType.FLOAT.toString());
//        componentElementReportElement.setAttribute(ATT_IS_STRETCH_WITH_OVERFLOW, String.valueOf(true));
        subReport.appendChild(reportElement);
        
        final Element datasourceExpression = document.createElementNS(URI_JRXML, TAG_DATA_SOURCE_EXPRESSION);
        
        final CDATASection datasourceExpressionField = document.createCDATASection("(("+ObjectDataSource.class.getCanonicalName()+") $P{"+PHOTO_TABLE_DATA_SOURCE+"})");
        
        datasourceExpression.appendChild(datasourceExpressionField);
        subReport.appendChild(datasourceExpression);
        
        final Element subreportExpression = document.createElementNS(URI_JRXML, TAG_SUBREPORT_EXPRESSION);
        final CDATASection subreportExpressionField = document.createCDATASection("$P{"+PHOTOS_SUBREPORT+"}");
        
        subreportExpression.appendChild(subreportExpressionField);
        subReport.appendChild(subreportExpression);
        
        band.appendChild(subReport);
        currentY+=height;
    }
    
    private void writeTable(final Class clazz, final List<String> avoidFields, final String datasourceParameter, final String datasetName, final int height){
        
        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);
        
        final Element componentElement = document.createElement(TAG_COMPONENT_ELEMENT);
        final Element componentElementReportElement = document.createElement(TAG_REPORT_ELEMENT);
        componentElementReportElement.setAttribute(ATT_KEY, "table");
        componentElementReportElement.setAttribute(ATT_STYLE, "table");
        componentElementReportElement.setAttribute(ATT_X, String.valueOf(0));
        componentElementReportElement.setAttribute(ATT_Y, String.valueOf(currentY));
//        componentElementReportElement.setAttribute(ATT_Y, String.valueOf(0));
        componentElementReportElement.setAttribute(ATT_WIDTH, String.valueOf(802));
        componentElementReportElement.setAttribute(ATT_HEIGHT, String.valueOf(height));
        componentElementReportElement.setAttribute(ATT_POSITION_TYPE, PositionType.FLOAT.toString());
//        componentElementReportElement.setAttribute(ATT_IS_STRETCH_WITH_OVERFLOW, String.valueOf(true));
        
        // Set the table element
        final Element table = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE);
        
        final Element datasetRun = document.createElementNS(URI_JRXML, TAG_DATASET_RUN);
        datasetRun.setAttribute(ATT_SUB_DATASET, datasetName);
        final Element datasourceExpression = document.createElementNS(URI_JRXML, TAG_DATA_SOURCE_EXPRESSION);
        
        final CDATASection datasourceExpressionField = document.createCDATASection("(("+ObjectDataSource.class.getCanonicalName()+") $P{"+datasourceParameter+"})");//.cloneDataSource()
        
        datasourceExpression.appendChild(datasourceExpressionField);
        datasetRun.appendChild(datasourceExpression);
        
        table.appendChild(datasetRun);
        
        int nbColumns=0;
        // Premier parcours pour calculer le nombre de colonnes
        for(final Method method : clazz.getMethods()){
            
            if(PrinterUtilities.isSetter(method)){
                
                // Retrives the field name from the setter name.----------------
                final String fieldName = getFieldNameFromSetter(method);
                if(avoidFields==null || !avoidFields.contains(fieldName))
                    nbColumns++;
            }
        }
        
        final int columnWidth = (PAGE_WIDTH - (LEFT_MARGIN+LEFT_MARGIN))/nbColumns;
        for(final Method method : clazz.getMethods()){
            
            if(PrinterUtilities.isSetter(method)){
                
                // Retrives the field name from the setter name.----------------
                final String fieldName = getFieldNameFromSetter(method);
//                final Class fieldClass = method.getParameterTypes()[0];
                if(avoidFields==null || !avoidFields.contains(fieldName))
                    writeColumn(clazz, method, table, columnWidth, 7, 1, 20, 10);
            }
        }
        
        componentElement.appendChild(componentElementReportElement);
        componentElement.appendChild(table);
        
        band.appendChild(componentElement);
        currentY+=height;
    }
    
    private void writeColumn(final Class clazz, final Method setter, final Element table, final int columnWidth, final int fontSize, final int padding, final int headerHeight, final int detailCellHeight){
       
        final String fieldName = getFieldNameFromSetter(setter);
        final Class fieldClass = setter.getParameterTypes()[0];
        
        final Element column = document.createElementNS(URI_JRXML_COMPONENTS, TAG_COLUMN);
        column.setAttribute(ATT_WIDTH, String.valueOf(columnWidth));
        
        // Table header and footer
        final Element tableHeader = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE_HEADER);
        tableHeader.setAttribute(ATT_STYLE, "table_TH");
        tableHeader.setAttribute(ATT_HEIGHT, String.valueOf(0));
        
        final Element tableFooter = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE_FOOTER);
        tableFooter.setAttribute(ATT_STYLE, "table_TH");
        tableFooter.setAttribute(ATT_HEIGHT, String.valueOf(0));
        
        // Column header
        final Element jrColumnHeader = document.createElementNS(URI_JRXML_COMPONENTS, TAG_COLUMN_HEADER);
        jrColumnHeader.setAttribute(ATT_STYLE, "table_CH");
        jrColumnHeader.setAttribute(ATT_HEIGHT, String.valueOf(headerHeight));

        final Element staticText = document.createElementNS(URI_JRXML, TAG_STATIC_TEXT);
            
        final Element staticTextReportElement = document.createElementNS(URI_JRXML, TAG_REPORT_ELEMENT);
        staticTextReportElement.setAttribute(ATT_X, String.valueOf(padding));
        staticTextReportElement.setAttribute(ATT_Y, String.valueOf(padding));
        staticTextReportElement.setAttribute(ATT_WIDTH, String.valueOf(columnWidth-2*padding));
        staticTextReportElement.setAttribute(ATT_HEIGHT, String.valueOf(headerHeight-2*padding));
//        staticTextReportElement.setAttribute(ATT_POSITION_TYPE, PositionType.FLOAT.toString());
        staticText.appendChild(staticTextReportElement);
        
        final Element textElement = document.createElement(TAG_TEXT_ELEMENT);
        final Element font = document.createElement(TAG_FONT);
        font.setAttribute(ATT_SIZE, String.valueOf(fontSize));
        textElement.appendChild(font);
        staticText.appendChild(textElement);

        final Element text = document.createElementNS(URI_JRXML, TAG_TEXT);
        final ResourceBundle rb = ResourceBundle.getBundle(clazz.getName());
        final CDATASection labelField = document.createCDATASection(rb.getString(fieldName));
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
        textFieldReportElement.setAttribute(ATT_X, String.valueOf(padding));
        textFieldReportElement.setAttribute(ATT_Y, String.valueOf(padding));
        textFieldReportElement.setAttribute(ATT_WIDTH, String.valueOf(columnWidth-2*padding));
        textFieldReportElement.setAttribute(ATT_HEIGHT, String.valueOf(detailCellHeight-2*padding));
//        textFieldReportElement.setAttribute(ATT_POSITION_TYPE, PositionType.FLOAT.toString());
        textField.appendChild(textFieldReportElement);
        
        final Element detailTextElement = document.createElement(TAG_TEXT_ELEMENT);
        final Element detailFont = document.createElement(TAG_FONT);
        detailFont.setAttribute(ATT_SIZE, String.valueOf(fontSize));
        detailTextElement.appendChild(detailFont);
        final Markup markup;
        if (fieldName.contains("escript") || fieldName.contains("omment")){
            markup = Markup.HTML;
        } else {
            markup = Markup.NONE;
        }
        detailTextElement.setAttribute(ATT_MARKUP, markup.toString());
        textField.appendChild(detailTextElement);

        final Element textFieldExpression = document.createElement(TAG_TEXT_FIELD_EXPRESSION);
        final CDATASection valueField;
        if(fieldClass==Boolean.class || (fieldClass!=null && BOOLEAN_PRIMITIVE_NAME.equals(fieldClass.getName()))){
            valueField = document.createCDATASection("$F{"+fieldName+"}==null ? \""+NULL_REPLACEMENT+"\" : ($F{"+fieldName+"} ? \""+TRUE_REPLACEMENT+"\" : \""+FALSE_REPLACEMENT+"\")");
        }
        else{
            valueField = document.createCDATASection("$F{"+fieldName+"}==null ? \""+NULL_REPLACEMENT+"\" : $F{"+fieldName+"}");
        }
        textFieldExpression.appendChild(valueField);

        textField.appendChild(textFieldExpression);
        detailCell.appendChild(textField);
        
        column.appendChild(tableHeader);
        column.appendChild(tableFooter);
        column.appendChild(jrColumnHeader);
        column.appendChild(jrColumnFooter);
        column.appendChild(detailCell);
        
        table.appendChild(column);
    }
    
}