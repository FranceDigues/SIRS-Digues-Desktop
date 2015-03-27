package fr.sirs.util;

import fr.sirs.core.component.PreviewLabelRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.StringConverter;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.report.JasperReportService;

/**
 * <p>This class provides utilities for two purposes:</p>
 * <ul>
 * <li>generating Jasper Reports templates mapping the classes of the model.</li>
 * <li>generating portable documents (.pdf) based on the templates on the one 
 * hand and the instances on the other hand.</li>
 * </ul>
 * <p>These are tools for printing functionnalities.</p>
 * 
 * @author Samuel Andrés (Geomatys)
 */
public class PrinterUtilities {
    
    private static final List<String> falseGetter = new ArrayList<>();
    private static final String JRXML_EXTENSION = ".jrxml";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String META_TEMPLATE = "/fr/sirs/jrxml/metaTemplate.jrxml";
    
    static{
        falseGetter.add("getClass");
        falseGetter.add("isNew");
        falseGetter.add("getAttachments");
        falseGetter.add("getRevisions");
        falseGetter.add("getConflicts");
        falseGetter.add("getDocumentId");
    }
    
    /**
     * <p>Generate the specific Jasper Reports template for a given class.
     * This method is based on a meta-template defined in 
     * src/main/resources/fr/sirs/jrxml/metaTemplate.jrxml
     * and produce a specific template : ClassName.jrxml".</p>
     * 
     * <p>Then, this specific template is used to print an object of the model.</p>
     * @param objectToPrint Pojo to print.
     * @param avoidFields Names of the fields to avoid printing.
     * @param previewLabelRepository
     * @return 
     * @throws Exception 
     */
    static public File print(final Object objectToPrint, final List<String> avoidFields, 
            final PreviewLabelRepository previewLabelRepository, final StringConverter stringConverter) throws Exception {
        
        
        // Creates the Jasper Reports specific template from the generic template.
        final JRDomWriter writer = new JRDomWriter(PrinterUtilities.class.getResourceAsStream(META_TEMPLATE));
        writer.setFieldsInterline(2);
        final File template = File.createTempFile(objectToPrint.getClass().getSimpleName(), JRXML_EXTENSION);
        template.deleteOnExit();
        writer.setOutput(template);
        writer.write(objectToPrint.getClass(), avoidFields);
        
        final JasperReport jasperReport = JasperCompileManager.compileReport(JRXmlLoader.load(template));
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("logo", PrinterUtilities.class.getResourceAsStream("/fr/sirs/images/icon-sirs.png"));
        final JRDataSource source = new ObjectDataSource(Collections.singletonList(objectToPrint), previewLabelRepository, stringConverter);
        
        final JasperPrint print = JasperFillManager.fillReport(jasperReport, parameters, source);
        
        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile(objectToPrint.getClass().getSimpleName(), PDF_EXTENSION);
        fout.deleteOnExit();
        final OutputStream out = new FileOutputStream(fout);
        final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, out);
        JasperReportService.generate(print, output);
        return fout;
    }
    
    static public File print(final Object objectToPrint, final List<String> avoidFields) throws Exception {
        return print(objectToPrint, avoidFields, null, null);
    }
    
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
    
//    static public void main(String[] arg) throws Exception {
//        
//        final File rep = new File("../core/sirs-core-store/target/generated-sources/pojos/fr/sirs/model");
//        
//        final Pattern pattern = Pattern.compile("(.*)\\.java"); 
//        for (final String s : rep.list()) {  
//            final Matcher matcher = pattern.matcher(s);
//            while(matcher.find()){
//                final String className = matcher.group(1);
//                final Class classe = Class.forName("fr.sirs.model."+className);
//                PrinterUtilities.generateJasperReportsTemplate(classe);
//            }
//        }
//    }
    
    private PrinterUtilities(){}
}
