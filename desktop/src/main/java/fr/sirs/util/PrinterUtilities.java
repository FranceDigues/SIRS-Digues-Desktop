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

import fr.sirs.SIRS;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import static fr.sirs.util.JRDomWriterDesordreSheet.PHOTOS_SUBREPORT;
import fr.sirs.util.property.DocumentRoots;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import javafx.collections.FXCollections;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.report.CollectionDataSource;
import org.geotoolkit.report.JasperReportService;
import org.xml.sax.SAXException;

/**
 * <p>This class provides utilities for two purposes:</p>
 * <ul>
 * <li>generating Jasper Reports templates mapping the classes of the model.</li>
 * <li>generating portable documents (.pdf) based on the templates on the one
 * hand and the instances on the other hand.</li>
 * </ul>
 * <p>These are tools for printing functionnalities.</p>
 * @author Samuel Andrés (Geomatys)
 */
public class PrinterUtilities {

    private static final String JRXML_EXTENSION = ".jrxml";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String LOGO_PATH = "/fr/sirs/images/icon-sirs.png";
    private static final String IMG_NOT_FOUND_PATH = "/fr/sirs/images/imgNotFound.png";

    private static final List<String> falseGetter = new ArrayList<>();
    static{
        falseGetter.add("getClass");
        falseGetter.add("isNew");
        falseGetter.add("getAttachments");
        falseGetter.add("getRevisions");
        falseGetter.add("getConflicts");
        falseGetter.add("getDocumentId");
    }

    private static final String TEMPLATE_PHOTOS = "/fr/sirs/jrxml/photoTemplate.jrxml";

    ////////////////////////////////////////////////////////////////////////////
    // FICHES DÉTAILLÉES DE DESORDRES
    ////////////////////////////////////////////////////////////////////////////
    private static final String META_TEMPLATE_RESEAU_FERME = "/fr/sirs/jrxml/metaTemplateReseauFerme.jrxml";

    public static File printReseauFerme(final List<String> avoidReseauFields,
            final List<String> avoidObservationFields,
            final List<String> reseauFields,
            final Previews previewLabelRepository,
            final SirsStringConverter stringConverter,
            final List<ReseauHydrauliqueFerme> reseaux,
            final boolean printPhoto, final boolean printReseauOuvrage) throws IOException, ParserConfigurationException, SAXException, TransformerException, JRException {

        // Creates the Jasper Reports specific template from the generic template.
        final File templateFile = File.createTempFile(ReseauHydrauliqueFerme.class.getName(), JRXML_EXTENSION);
        templateFile.deleteOnExit();

        final JasperPrint print;
        try(final InputStream logoStream = PrinterUtilities.class.getResourceAsStream(LOGO_PATH);
                final InputStream metaTemplateStream = PrinterUtilities.class.getResourceAsStream(META_TEMPLATE_RESEAU_FERME);
                final InputStream photoTemplateStream = PrinterUtilities.class.getResourceAsStream(TEMPLATE_PHOTOS)){
            final JRDomWriterReseauFermeSheet templateWriter = new JRDomWriterReseauFermeSheet(
                    metaTemplateStream,
                    avoidReseauFields, avoidObservationFields,
                    reseauFields, printPhoto, printReseauOuvrage);
            templateWriter.setOutput(templateFile);
            templateWriter.write();

            final JasperReport jasperReport = JasperCompileManager.compileReport(JRXmlLoader.load(templateFile));

            final JRDataSource source = new ReseauHydrauliqueFermeDataSource(reseaux, previewLabelRepository, stringConverter);

            final Map<String, Object> parameters = new HashMap<>();

            parameters.put("logo", logoStream);

            final JasperReport photosReport = net.sf.jasperreports.engine.JasperCompileManager.compileReport(photoTemplateStream);
            parameters.put(PHOTOS_SUBREPORT, photosReport);

            print = JasperFillManager.fillReport(jasperReport, parameters, source);
        }

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile("RESEAU_HYDRAULIQUE_FERME_OBSERVATION", PDF_EXTENSION);
        fout.deleteOnExit();
        try (final FileOutputStream outStream = new FileOutputStream(fout)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            JasperReportService.generate(print, output);
        }
        return fout;
    }

    ////////////////////////////////////////////////////////////////////////////
    // FICHES DÉTAILLÉES DE DESORDRES
    ////////////////////////////////////////////////////////////////////////////
    private static final String META_TEMPLATE_DESORDRE = "/fr/sirs/jrxml/metaTemplateDesordre.jrxml";

    public static File printDisorders(final List<String> avoidDesordreFields,
            final List<String> avoidObservationFields,
            final List<String> avoidPrestationFields,
            final List<String> reseauFields,
            final Previews previewLabelRepository,
            final SirsStringConverter stringConverter,
            final List<Desordre> desordres,
            final boolean printPhoto, final boolean printReseauOuvrage, final boolean printVoirie)
        throws ParserConfigurationException, SAXException, JRException, TransformerException, IOException {

        // Creates the Jasper Reports specific template from the generic template.
        final File templateFile = File.createTempFile(Desordre.class.getName(), JRXML_EXTENSION);
        templateFile.deleteOnExit();

        final JasperPrint print;
        try(final InputStream logoStream = PrinterUtilities.class.getResourceAsStream(LOGO_PATH);
                final InputStream metaTemplateStream = PrinterUtilities.class.getResourceAsStream(META_TEMPLATE_DESORDRE);
                final InputStream photoTemplateStream = PrinterUtilities.class.getResourceAsStream(TEMPLATE_PHOTOS)){

            final JRDomWriterDesordreSheet templateWriter = new JRDomWriterDesordreSheet(
                    metaTemplateStream,
                    avoidDesordreFields, avoidObservationFields, avoidPrestationFields,
                    reseauFields, printPhoto, printReseauOuvrage, printVoirie);
            templateWriter.setOutput(templateFile);
            templateWriter.write();

            final JasperReport jasperReport = JasperCompileManager.compileReport(JRXmlLoader.load(templateFile));

            final JRDataSource source = new DesordreDataSource(FXCollections.observableList(desordres).sorted((Comparator)new PRComparator()), previewLabelRepository, stringConverter);

            final Map<String, Object> parameters = new HashMap<>();

            final JasperReport photosReport = net.sf.jasperreports.engine.JasperCompileManager.compileReport(photoTemplateStream);
            parameters.put(PHOTOS_SUBREPORT, photosReport);

            print = JasperFillManager.fillReport(jasperReport, parameters, source);
        }

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile("DESORDRE_OBSERVATION", PDF_EXTENSION);
        fout.deleteOnExit();
        try (final FileOutputStream outStream = new FileOutputStream(fout)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            JasperReportService.generate(print, output);
        }
        return fout;
    }

    ////////////////////////////////////////////////////////////////////////////
    // FICHES DE RESULTATS DE REQUETES
    ////////////////////////////////////////////////////////////////////////////
    private static final String META_TEMPLATE_QUERY = "/fr/sirs/jrxml/metaTemplateQuery.jrxml";

    /**
     *
     * @param avoidFields
     * @param featureCollection
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws JRException
     * @throws TransformerException
     */
    public static File print(List<String> avoidFields, final FeatureCollection featureCollection)
        throws IOException, ParserConfigurationException, SAXException, JRException, TransformerException {

        if(avoidFields==null) avoidFields=new ArrayList<>();

        // Creates the Jasper Reports specific template from the generic template.
        final File template;
        try(final InputStream templateInputStream =PrinterUtilities.class.getResourceAsStream(META_TEMPLATE_QUERY)){
            final JRDomWriterQueryResultSheet writer = new JRDomWriterQueryResultSheet(templateInputStream);
            writer.setFieldsInterline(2);
            template = File.createTempFile(featureCollection.getFeatureType().getName().tip().toString(), JRXML_EXTENSION);
            template.deleteOnExit();
            writer.setOutput(template);
            writer.write(featureCollection.getFeatureType(), avoidFields);
        }

        // Retrives the compiled template and the feature type -----------------
        final Map.Entry<JasperReport, FeatureType> entry = JasperReportService.prepareTemplate(template);
        final JasperReport report = entry.getKey();

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile(featureCollection.getFeatureType().getName().tip().toString(), PDF_EXTENSION);
        fout.deleteOnExit();
        try (final FileOutputStream outStream = new FileOutputStream(fout);
                final InputStream logoStream = PrinterUtilities.class.getResourceAsStream(LOGO_PATH)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("logo", logoStream);
            parameters.put(JRDomWriterQueryResultSheet.TABLE_DATA_SOURCE, new CollectionDataSource(featureCollection));

            final JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
            JasperReportService.generate(print, output);
//        JasperReportService.generateReport(report, featureCollection, parameters, output);
        }
        return fout;
    }

    ////////////////////////////////////////////////////////////////////////////
    // FICHES SYNOPTIQUES D'ELEMENTS DU MODELE
    ////////////////////////////////////////////////////////////////////////////
    private static final String META_TEMPLATE_ELEMENT = "/fr/sirs/jrxml/metaTemplateElement.jrxml";

    /**
     * <p>Generate the specific Jasper Reports template for a given class.
     * This method is based on a meta-template defined in
     * src/main/resources/fr/sirs/jrxml/metaTemplate.jrxml
     * and produce a specific template : ClassName.jrxml".</p>
     *
     * <p>This specific template is used to print objects of the model.</p>
     *
     * @param elements Pojos to print. The list must contain at least one element.
     * If it contains more than one, they must be all of the same class.
     * @param avoidFields Names of the fields to avoid printing.
     * @param previewLabelRepository
     * @param stringConverter
     * @return
     * @throws java.io.IOException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     * @throws net.sf.jasperreports.engine.JRException
     * @throws javax.xml.transform.TransformerException
     */
    public static File print(final List<String> avoidFields,
            final Previews previewLabelRepository, final SirsStringConverter stringConverter, final List<? extends Element> elements)
            throws IOException, ParserConfigurationException, SAXException, JRException, TransformerException {

        // Creates the Jasper Reports specific template from the generic template.
        final File templateFile = File.createTempFile(elements.get(0).getClass().getSimpleName(), JRXML_EXTENSION);
        templateFile.deleteOnExit();

        final JRDomWriterElementSheet templateWriter = new JRDomWriterElementSheet(PrinterUtilities.class.getResourceAsStream(META_TEMPLATE_ELEMENT));
        templateWriter.setFieldsInterline(2);
        templateWriter.setOutput(templateFile);
        templateWriter.write(elements.get(0).getClass(), avoidFields);

        final JasperReport jasperReport = JasperCompileManager.compileReport(JRXmlLoader.load(templateFile));

        JasperPrint finalPrint = null;
        for(final Element element : elements){
            final JRDataSource source = new ObjectDataSource(Collections.singletonList(element), previewLabelRepository, stringConverter);

            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("logo", PrinterUtilities.class.getResourceAsStream(LOGO_PATH));
            final JasperPrint print = JasperFillManager.fillReport(jasperReport, parameters, source);
            if(finalPrint==null) finalPrint=print;
            else{
                for(final JRPrintPage page : print.getPages()){
                    finalPrint.addPage(page);
                }
            }
        }

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile(elements.get(0).getClass().getSimpleName(), PDF_EXTENSION);
        try (final FileOutputStream outStream = new FileOutputStream(fout)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            JasperReportService.generate(finalPrint, output);
        }
        return fout;
    }

    /*

    Pour l'impression de plusieurs documents, plusieurs solutions (http://stackoverflow.com/questions/24115885/combining-two-jasper-reports)


    solution 1

    List<JasperPrint> jasperPrints = new ArrayList<JasperPrint>();
// Your code to get Jasperreport objects
JasperReport jasperReportReport1 = JasperCompileManager.compileReport(jasperDesignReport1);
jasperPrints.add(jasperReportReport1);
JasperReport jasperReportReport2 = JasperCompileManager.compileReport(jasperDesignReport2);
jasperPrints.add(jasperReportReport2);
JasperReport jasperReportReport3 = JasperCompileManager.compileReport(jasperDesignReport3);
jasperPrints.add(jasperReportReport3);

JRPdfExporter exporter = new JRPdfExporter();
//Create new FileOutputStream or you can use Http Servlet Response.getOutputStream() to get Servlet output stream
// Or if you want bytes create ByteArrayOutputStream
ByteArrayOutputStream out = new ByteArrayOutputStream();
exporter.setParameter(JRExporterParameter.JASPER_PRINT_LIST, jasperPrints);
exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, out);
exporter.exportReport();
byte[] bytes = out.toByteArray();


    solution 2 (adoptée pour le moment) :

    JasperPrint jp1 = JasperFillManager.fillReport(url.openStream(), parameters,
                    new JRBeanCollectionDataSource(inspBean));
JasperPrint jp2 = JasperFillManager.fillReport(url.openStream(), parameters,
                    new JRBeanCollectionDataSource(inspBean));

List pages = jp2 .getPages();
        for (int j = 0; j < pages.size(); j++) {
        JRPrintPage object = (JRPrintPage)pages.get(j);
        jp1.addPage(object);

}
JasperViewer.viewReport(jp1,false);

    */

    ////////////////////////////////////////////////////////////////////////////
    // METHODES UTILITAIRES
    ////////////////////////////////////////////////////////////////////////////
    /**
     * <p>This method detects if a method is a getter.</p>
     * @param method
     * @return true if the method is a getter.
     */
    static public boolean isGetter(final Method method){
        if (method == null)
            return false;
        else
            return (method.getName().startsWith("get")
                || method.getName().startsWith("is"))
                && method.getParameterTypes().length == 0
                && !falseGetter.contains(method.getName());
    }

    /**
     * <p>This method detects if a method is a setter.</p>
     * @param method
     * @return true if the method is a setter.
     */
    static public boolean isSetter(final Method method){
        if (method == null)
            return false;
        else
            return method.getName().startsWith("set")
                && method.getParameterTypes().length == 1
                && void.class.equals(method.getReturnType());
    }

    public static String getFieldNameFromSetter(final Method setter) {
        return setter.getName().substring(3, 4).toLowerCase()
                            + setter.getName().substring(4);
    }

    /**
     * Find photograph pointed by given path (must be relative to configured
     * {@link DocumentRoots#getPhotoRoot(java.lang.Class, boolean) }.
     * Note : Used in .jrxml files.
     * @param inputText Relative path to the wanted image.
     * @return The found file, as a stream.
     */
    public static InputStream getPhotoStream(final String inputText) {
        Optional<Path> root = DocumentRoots.getRoot(AbstractPhoto.class, false);
        if (root.isPresent()) {
            try {
                return Files.newInputStream(SIRS.concatenatePaths(root.get(), inputText));
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.WARNING, "Cannot access a photograph file.", e);
            }
        }

        return PrinterUtilities.class.getResourceAsStream(IMG_NOT_FOUND_PATH);
    }

    ////////////////////////////////////////////////////////////////////////////
    private PrinterUtilities(){}

    /**
     * Compare objects according to their PRs. They're sorted by ascending order
     * of their {@link Positionable#getPrDebut() } value, and then by {@link Positionable#getPrFin() } (length).
     */
    private static class PRComparator implements Comparator<Positionable> {

        @Override
        public int compare(Positionable o1, Positionable o2) {
            if (o1 == null || Float.isNaN(o1.getPrDebut())) {
                return 1;
            } else if (o2 == null || Float.isNaN(o2.getPrDebut())) {
                return -1;
            } else {
                final int compare = Float.compare(o1.getPrDebut(), o2.getPrDebut());
                if (compare == 0) {
                    return Float.compare(o1.getPrFin(), o2.getPrFin());
                } else {
                    return compare;
                }
            }
        }
    }
}
