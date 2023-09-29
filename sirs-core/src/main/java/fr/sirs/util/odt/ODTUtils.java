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
package fr.sirs.util.odt;

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.report.AbstractSectionRapport;
import fr.sirs.core.model.report.ModeleRapport;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.sis.measure.Units;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.image.io.XImageIO;
import org.odftoolkit.odfdom.dom.OdfSchemaDocument;
import org.odftoolkit.odfdom.dom.element.text.TextUserFieldDeclElement;
import org.odftoolkit.odfdom.dom.element.text.TextUserFieldDeclsElement;
import org.odftoolkit.odfdom.dom.element.text.TextUserFieldGetElement;
import org.odftoolkit.odfdom.dom.element.text.TextVariableDeclElement;
import org.odftoolkit.odfdom.dom.element.text.TextVariableDeclsElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.pkg.ElementVisitor;
import org.odftoolkit.odfdom.pkg.OdfElement;
import org.odftoolkit.odfdom.type.Color;
import org.odftoolkit.simple.Document;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.common.field.AbstractVariableContainer;
import org.odftoolkit.simple.common.field.Field.FieldType;
import org.odftoolkit.simple.common.field.Fields;
import org.odftoolkit.simple.common.field.VariableField;
import org.odftoolkit.simple.draw.FrameRectangle;
import org.odftoolkit.simple.draw.Image;
import org.odftoolkit.simple.style.MasterPage;
import org.odftoolkit.simple.style.StyleTypeDefinitions;
import org.odftoolkit.simple.style.TableCellProperties;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;
import org.odftoolkit.simple.table.TableContainer;
import org.odftoolkit.simple.text.Paragraph;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Utility methods used to create ODT templates, or fill ODT templates.
 *
 * Note : For now, we do not use real ODT varaibles (see {@link VariableField})
 * in our templates, because iterating through does not look simple. We just use
 * text with special formatting.
 *
 * @author Alexis Manin (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class ODTUtils {

    /**
     * Width of an ODT documentt page in portrait mode. Unit is millimeter.
     */
    private static final int PORTRAIT_WIDTH = 210;

    /**
     * Height of an ODT documentt page in portrait mode. Unit is millimeter.
     */
    private static final int PORTRAIT_HEIGHT = 297;

    private static final int DEFAULT_MARGIN = 20;

    public static final String CLASS_KEY = "The.Sirs.Class";

    private static final String ASK_PASSWORD = "Le document suivant est protégé par mot de passe. Veuillez insérer le mot de passe pour continuer.";

    private static final String ELEMENT_MODEL_NOTE = "Note : Ci-dessous se trouve"
            + " la liste des champs utilisés par SIRS-Digues lors de la création"
            + " d'une fiche. Vous pouvez compléter ce modèle (Ajout de contenu,"
            + " mise en forme) et déplacer / copier les variables (les textes"
            + " surlignés de gris) où vous voulez dans le document. Elles seront"
            + " automatiquement remplacées lors de la génération du rapport.";

    private static Field USER_FIELD;
    private static Field SIMPLE_FIELD;

    /**
     * Generate a new template which will put "variables" into it to be easily
     * replaced when creating a report.
     *
     * Note : For the moment, we do not use real {@link VariableField}, because
     * iterating through does not look simple. We just use text with special
     * formatting.
     *
     * @param title Title for the document to create.
     * @param properties A map whose keys are properties to put in template, and
     * values are titles for them.
     * @return A new template document with prepared variables.
     * @throws java.lang.Exception If we fail creating a new document.
     */
    public static TextDocument newSimplePropertyModel(final String title, final Map<String, String> properties) throws Exception {
        final TextDocument result = TextDocument.newTextDocument();
        result.addParagraph(title).applyHeading();
        result.addParagraph(ELEMENT_MODEL_NOTE);
        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            appendUserVariable(result, entry.getKey(), entry.getValue(), entry.getValue());
        }

        return result;
    }

    /**
     * Save given class reference in input document, setting it as the class to
     * use when generating a report.
     *
     * @param input The document to modify.
     * @param targetClass Class to put, or null to remove information from
     * document.
     */
    public static void setTargetClass(final TextDocument input, final Class targetClass) {
        ArgumentChecks.ensureNonNull("Input document", input);
        if (targetClass == null) {
            removeVariable(input.getVariableFieldByName(CLASS_KEY));
        } else {
            Fields.createUserVariableField(input, CLASS_KEY, targetClass.getCanonicalName());
        }
    }

    /**
     * Analyze input document to find which type of object it is planned for.
     *
     * @param source Source document to analyze.
     * @return The class which must be used for report creation, or null if we
     * cannot find information in given document.
     * @throws ReflectiveOperationException If we fail analyzing document.
     */
    public static Class getTargetClass(final TextDocument source) throws ReflectiveOperationException {
        final VariableField var = source.getVariableFieldByName(CLASS_KEY);
        if (var != null && FieldType.USER_VARIABLE_FIELD.equals(var.getFieldType())) {
            Object value = getVariableValue(var);
            if (value instanceof String) {
                return Thread.currentThread().getContextClassLoader().loadClass((String) value);
            }
        }

        return null;
    }

    /**
     * Replace all variables defined in input document template with the one
     * given in parameter. For variables in document also present in given
     * property mapping, they're left as is. New properties to be put are added
     * in paragraphs at the end of the document.
     *
     * @param source Document template to modify.
     * @param properties Properties to set in given document. Keys are property
     * names, and values are associated title. If null, all user variables will
     * be deleted from input document.
     */
    public static void setVariables(final TextDocument source, final Map<String, String> properties) {
        Map<String, VariableField> vars = findAllVariables(source, VariableField.VariableType.USER);

        if (properties != null) {
            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                if (vars.remove(entry.getKey()) == null) {
                    appendUserVariable(source, entry.getKey(), entry.getValue(), entry.getValue());
                }
            }
        }

        for (final VariableField var : vars.values()) {
            removeVariable(var);
        }
    }

    /**
     * Remove given variable from its holding document.
     *
     * @param var the variable field to get rid of.
     * @return true if we succeeded, false otherwise.
     */
    public static boolean removeVariable(final VariableField var) {
        if (var == null)
            return false;
        final OdfElement varElement = var.getOdfElement();
        if (varElement == null)
            return false;
        return varElement.getParentNode().removeChild(varElement) != null;
    }

    /**
     * Fill given template with data originating from candidate object.
     *
     * @param template Source template to fill.
     * @param candidate The object to get data from to fill given template.
     *
     * @throws java.beans.IntrospectionException If input candidate cannot be
     * analyzed.
     * @throws java.lang.ReflectiveOperationException If we fail reading
     * candidate properties.
     */
    public static void fillTemplate(final TextDocument template, final Element candidate) throws IntrospectionException, ReflectiveOperationException {
        // We iterate through input properties to extract all mappable attributes.
        final PropertyDescriptor[] descriptors = Introspector.getBeanInfo(candidate.getClass()).getPropertyDescriptors();
        VariableField var;
        String pName;
        for (final PropertyDescriptor desc : descriptors) {
            pName = desc.getName();
            var = template.getVariableFieldByName(pName);
            if (var != null) {
                var.updateField(Printers.getPrinter(pName).print(candidate, desc), null);
            } else {
                SirsCore.LOGGER.fine("No variable found for name " + pName);
            }
        }
    }


    /**
     * Fill given template with data originating from candidate object.
     *
     * @param template Source template to fill.
     * @param candidate The object to get data from to fill given template.
     *
     * @throws java.beans.IntrospectionException If input candidate cannot be
     * analyzed.
     * @throws java.lang.ReflectiveOperationException If we fail reading
     * candidate properties.
     */
    public static void fillTemplate(final TextDocument template, final Feature candidate) {
        // We iterate through input properties to extract all mappable attributes.
        final Collection<? extends PropertyType> properties = candidate.getType().getProperties(true);
        VariableField var;
        String pName;
        for (final PropertyType desc : properties) {
            pName = desc.getName().tip().toString();
            var = template.getVariableFieldByName(pName);
            if (var != null) {
                var.updateField(Printers.getPrinter(pName).print(candidate, pName), null);
            } else {
                SirsCore.LOGGER.fine("No variable found for name " + pName);
            }
        }
    }

    /**
     * Extract properties from given candidate, and for each node registered for
     * it, we set its text using property value.
     *
     * @param candidate Element to extract properties from.
     * @param propertyContainers A registry of all elements to modify, sorted by
     * property name.
     * @throws IntrospectionException If input candidate cannot be analyzed.
     * @throws ReflectiveOperationException If we fail reading candidate
     * properties.
     */
    public static void replaceTextContent(final Element candidate, final Map<String, List<? extends Node>> propertyContainers) throws IntrospectionException, ReflectiveOperationException {
        // We iterate through input properties to extract all mappable attributes.
        final PropertyDescriptor[] descriptors = Introspector.getBeanInfo(candidate.getClass()).getPropertyDescriptors();
        List<? extends Node> containers;
        String pName;
        for (final PropertyDescriptor desc : descriptors) {
            pName = desc.getName();
            containers = propertyContainers.get(pName);
            if (containers != null) {
                for (final Node n : containers) {
                    n.setTextContent(Printers.getPrinter(pName).print(candidate, desc));
                }
            }
        }
    }


    /**
     * Extract properties from given candidate, and for each node registered for
     * it, we set its text using property value.
     *
     * @param candidate Feature to extract properties from.
     * @param propertyContainers A registry of all elements to modify, sorted by
     * property name.
     * @throws IntrospectionException If input candidate cannot be analyzed.
     * @throws ReflectiveOperationException If we fail reading candidate
     * properties.
     */
    public static void replaceTextContent(final Feature candidate, final Map<String, List<? extends Node>> propertyContainers) throws IntrospectionException, ReflectiveOperationException {
        // We iterate through input properties to extract all mappable attributes.
        Property prop;
        for (final Map.Entry<String, List<? extends Node>> entry : propertyContainers.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            prop = candidate.getProperty(entry.getKey());
            if (prop == null) {
                for (final Node n : entry.getValue()) {
                    n.setTextContent(" - ");
                }
            } else for (final Node n : entry.getValue()) {
                n.setTextContent(Printers.getPrinter(entry.getKey()).print(candidate, entry.getKey()));
            }
        }
    }

    /**
     * Find variable with given name into input document, and update its value
     * with specified one. If we cannot find a matching variable into given
     * document, this method just returns.
     *
     * @param doc The document to search into.
     * @param varName Name of the variable to find.
     * @param newValue Value to put into found variable.
     */
    public static void findAndReplaceVariable(final TextDocument doc, final String varName, final Object newValue) {
        try {
            VariableField var = doc.getVariableFieldByName(varName);
            if (var != null) {
                var.updateField(newValue == null ? "N/A" : newValue.toString(), null);
            }
        } catch (IllegalArgumentException e) {
            // No variable found for given name.
        }
    }

    /**
     * Add a new variable in given document, and put a new paragraph containing
     * it at the end of the document.
     *
     * @param doc The document to add variable into.
     * @param varName Name of the variable to create. Should not be null.
     * @param value Default value to set to the variable. Null accepted.
     * @param text Text to put in the paragraph to create. If null, no paragraph
     * will be created, so variabl will not be displayed in document body.
     * @return The created variable.
     */
    public static VariableField appendUserVariable(final TextDocument doc, final String varName, final String value, final String text) {
        final VariableField field = Fields.createUserVariableField(doc, varName, value);
        if (text != null) {
            field.displayField(doc.addParagraph(text + " : ").getOdfElement());
        }
        return field;
    }

    /**
     * Search in input document for all declared variables of a given type.
     *
     * For algorithm, see {@link AbstractVariableContainer#getVariableFieldByName(java.lang.String)
     * }
     *
     * @param source Document to search into.
     * @param type Type of variable to retrieve. If null, all type of variables
     * will be returned.
     * @return A map of all found variables. Keys are variable names, values are
     * concrete variables. Never null, but can be empty.
     */
    public static Map<String, VariableField> findAllVariables(final TextDocument source, final VariableField.VariableType type) {
        final OdfElement variableContainer = source.getVariableContainerElement();
        final HashMap<String, VariableField> result = new HashMap<>();

        VariableField tmpField;
        // First, find all user variable methods.
        if (type == null || VariableField.VariableType.USER.equals(type)) {
            TextUserFieldDeclsElement userVariableElements = OdfElement.findFirstChildNode(TextUserFieldDeclsElement.class, variableContainer);
            if (userVariableElements != null) {
                TextUserFieldDeclElement userVariable = OdfElement.findFirstChildNode(TextUserFieldDeclElement.class, userVariableElements);
                Object value;
                while (userVariable != null) {
                    // really crappy...
                    value = getValue(userVariable);

                    // even crappier ...
                    tmpField = Fields.createUserVariableField(source, userVariable.getTextNameAttribute(), value.toString());
                    result.put(tmpField.getVariableName(), tmpField);

                    userVariable = OdfElement.findNextChildNode(TextUserFieldDeclElement.class, userVariable);
                }
            }
        }

        // then look for simple variables.
        if (type == null || VariableField.VariableType.SIMPLE.equals(type)) {
            TextVariableDeclsElement userVariableElements = OdfElement.findFirstChildNode(TextVariableDeclsElement.class, variableContainer);
            if (userVariableElements != null) {
                TextVariableDeclElement variable = OdfElement.findFirstChildNode(TextVariableDeclElement.class, userVariableElements);
                while (variable != null) {
                    tmpField = Fields.createSimpleVariableField(source, variable.getTextNameAttribute());
                    result.put(tmpField.getVariableName(), tmpField);
                    variable = OdfElement.findNextChildNode(TextVariableDeclElement.class, variable);
                }
            }
        }

        return result;
    }

    /**
     * Rename user variables listed in input map. /!\ It does not rename user
     * variable occurrences in content dom, only their declarations !
     *
     * @param source Document containing user variables to rename.
     * @param replacements Map of variables to rename. Key is the original
     * variable name, and value its replacement.
     */
    public static void renameUserVariables(final TextDocument source, final Map<String, String> replacements) {
        final OdfElement variableContainer = source.getVariableContainerElement();
        TextUserFieldDeclsElement userVariableElements = OdfElement.findFirstChildNode(TextUserFieldDeclsElement.class, variableContainer);
        if (userVariableElements != null) {
            TextUserFieldDeclElement userVariable = OdfElement.findFirstChildNode(TextUserFieldDeclElement.class, userVariableElements);
            String oldName, newName;
            while (userVariable != null) {
                oldName = userVariable.getTextNameAttribute();
                if (oldName != null) {
                    newName = replacements.get(oldName);
                    if (newName != null) {
                        userVariable.setTextNameAttribute(newName);
                    }
                }

                userVariable = OdfElement.findNextChildNode(TextUserFieldDeclElement.class, userVariable);
            }
        }
    }

    public static Map<String, List<Text>> replaceUserVariablesWithText(final TextDocument source) throws Exception {
        final HashMap<String, List<Text>> replaced = new HashMap<>();
        final Map<String, VariableField> vars = findAllVariables(source, VariableField.VariableType.USER);
        if (vars.isEmpty())
            return replaced;

        source.getContentRoot().accept(new ElementVisitor() {

            @Override
            public void visit(OdfElement element) {
                if (element instanceof TextUserFieldGetElement) {
                    final String varName = ((TextUserFieldGetElement) element).getTextNameAttribute();
                    // Replace variable
                    final Text text;
                    try {
                        text = source.getContentDom().createTextNode(getVariableValue(vars.get(varName)).toString());
                    } catch (Exception ex) {
                        throw new SirsCoreRuntimeException(ex);
                    }
                    final Node papa = element.getParentNode();
                    papa.replaceChild(text, element);
                    // hack : paragraphs cannot embed other paragraphs.
                    List<Text> tmpList = replaced.get(varName);
                    if (tmpList == null) {
                        tmpList = new ArrayList<>();
                        replaced.put(varName, tmpList);
                    }
                    tmpList.add(text);

                } else if (element.hasChildNodes()) {
                    // Browse tree
                    final NodeList children = element.getChildNodes();
                    Node child;
                    for (int i = 0; i < children.getLength(); i++) {
                        child = children.item(i);
                        if (child instanceof OdfElement) {
                            ((OdfElement) child).accept(this);
                        }
                    }
                }
            }
        });

        // TODO : remove declared variables
        return replaced;
    }

    /**
     * Try to extract value from given field.
     *
     * IMPORTANT ! For now, only fields of
     * {@link VariableField.VariableType#USER} type are supported.
     *
     * @param field Object to extract value from.
     * @return Found value, or null if we cannot find any.
     * @throws ReflectiveOperationException If an error occurred while analyzing
     * input variable.
     * @throws UnsupportedOperationException If input variable type is not
     * {@link VariableField.VariableType#USER}
     */
    public static Object getVariableValue(final VariableField field) throws ReflectiveOperationException {
        ArgumentChecks.ensureNonNull("input variable field", field);
        if (FieldType.USER_VARIABLE_FIELD.equals(field.getFieldType())) {
            Field userVariableField = getUserVariableField();
            return getValue((TextUserFieldDeclElement) userVariableField.get(field));
        } else {
            throw new UnsupportedOperationException("Not done yet.");
        }
    }

    private static Field getUserVariableField() {
        if (USER_FIELD == null) {
            try {
                USER_FIELD = VariableField.class.getDeclaredField("userVariableElement");
            } catch (NoSuchFieldException ex) {
                throw new IllegalStateException("Cannot access user field.", ex);
            }
            USER_FIELD.setAccessible(true);
        }
        return USER_FIELD;
    }

    private static Field getSimpleVariableField() {
        if (SIMPLE_FIELD == null) {
            try {
                SIMPLE_FIELD = VariableField.class.getDeclaredField("simpleVariableElement");
            } catch (NoSuchFieldException ex) {
                throw new IllegalStateException("Cannot access user field.", ex);
            }
            SIMPLE_FIELD.setAccessible(true);
        }
        return SIMPLE_FIELD;
    }

    /**
     * Analyze input element to find contained value.
     *
     * @return value hold by given object, or null.
     */
    private static Object getValue(TextUserFieldDeclElement userVariable) {
        // really crappy...
        Object value = userVariable.getOfficeStringValueAttribute();
        if (value == null) {
            value = userVariable.getOfficeTimeValueAttribute();
            if (value == null) {
                value = userVariable.getOfficeDateValueAttribute();
                if (value == null) {
                    value = userVariable.getOfficeBooleanValueAttribute();
                    if (value == null) {
                        value = userVariable.getOfficeValueAttribute();
                    }
                }
            }
        }

        return value;
    }

    /**
     * Get master page with same orientation / margin properties as inputs, or
     * create a new one if we cannot find any.
     *
     * TODO : Check footnote settings
     *
     * @param doc Document to search for existing master pages.
     * @param orientation Orientation wanted for the returned page
     * configuration. If null, portrait orientation is used.
     * @param margin Margins to set to the master page. If null, default style
     * margins are used.
     * @return Found master page, or a new one.
     * @throws Exception If we cannot read given document.
     */
    public static MasterPage getOrCreateOrientationMasterPage(Document doc, StyleTypeDefinitions.PrintOrientation orientation, Insets margin) throws Exception {
        if (orientation == null) {
            orientation = StyleTypeDefinitions.PrintOrientation.PORTRAIT;
        }

        final String masterName = orientation.name() + (margin == null ? "" : " " + margin.toString());

        final MasterPage masterPage = MasterPage.getOrCreateMasterPage(doc, masterName);
        masterPage.setPrintOrientation(orientation);
        switch (orientation) {
            case LANDSCAPE:
                masterPage.setPageHeight(PORTRAIT_WIDTH);
                masterPage.setPageWidth(PORTRAIT_HEIGHT);
                break;
            case PORTRAIT:
                masterPage.setPageWidth(PORTRAIT_WIDTH);
                masterPage.setPageHeight(PORTRAIT_HEIGHT);
        }
        if (margin != null) {
            masterPage.setMargins(margin.getTop(), margin.getBottom(), margin.getLeft(), margin.getRight());
            if (margin.getBottom() <= 0) {
                masterPage.setFootnoteMaxHeight(0);
            }
        }

        return masterPage;
    }

    /**
     * Aggregate all provided data into one ODT document.
     *
     * Recognized data : - {@link TextDocument}
     * - {@link RenderedImage}
     * - {@link PDDocument}
     * - {@link PDPage}
     * - {@link CharSequence}
     *
     * Also, if a data reference is given as : - {@link URI}
     * - {@link URL}
     * - {@link File}
     * - {@link Path}
     * - {@link InputStream}
     *
     * We'll try to read it and convert it into one of the managed objects.
     *
     * @param output output ODT file. If it already exists, we try to open it
     * and add content at the end of the document. If it does not exists, we
     * create a new file.
     * @param candidates Objects to concatenate.
     * @throws java.lang.Exception If output
     */
    public static void concatenate(Path output, Object... candidates) throws Exception {
        try (final TextDocument doc = Files.exists(output)
                ? TextDocument.loadDocument(output.toFile()) : TextDocument.newTextDocument()) {

            for (Object candidate : candidates) {
                append(doc, candidate);
            }

            try (final OutputStream stream = Files.newOutputStream(output, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                doc.save(stream);
            } catch (Exception e) {
                try {
                    Files.deleteIfExists(output);
                } catch (Exception ignored) {
                    e.addSuppressed(ignored);
                }
            }
        }
    }

    /**
     * Try to read input object and put it into given document.
     *
     * Recognized data : - {@link TextDocument}
     * - {@link RenderedImage}
     * - {@link PDDocument}
     * - {@link PDPage}
     * - {@link CharSequence}
     *
     * Also, if a data reference is given as : - {@link URI}
     * - {@link URL}
     * - {@link File}
     * - {@link Path}
     * - {@link InputStream}
     *
     * We'll try to read it and convert it into one of the managed objects.
     *
     * @param holder Document to append data into.
     * @param candidate Object to read and put at the end of given document.
     * @throws Exception If we cannot read object or it is not a supported
     * format.
     */
    public static void append(TextDocument holder, Object candidate) throws Exception {
        if (candidate == null) {
            return;
        }

        boolean deletePath = false;

        // Unify all reference APIs to Path.
        if (candidate instanceof File) {
            candidate = ((File) candidate).toPath();
        } else if (candidate instanceof URI) {
            candidate = Paths.get((URI) candidate);
        } else if (candidate instanceof URL) {
            candidate = Paths.get(((URL) candidate).toURI());
        } else if (candidate instanceof InputStream) {
            final Path tmpFile = Files.createTempFile("candidate", ".tmp");
            Files.copy((InputStream) candidate, tmpFile);
            candidate = tmpFile;
            deletePath = true;
        }

        // If we've got a reference to an external data, we try to read it.
        if (candidate instanceof Path) {
            final Path tmpPath = (Path) candidate;
            if (!Files.isReadable(tmpPath)) {
                throw new IllegalArgumentException("Path given for document concatenation is not readable : " + tmpPath);
            }

            try (final TextDocument tmpDoc = TextDocument.loadDocument(tmpPath.toFile())) {

                holder.insertContentFromDocumentAfter(tmpDoc, holder.addParagraph(""), true);

            } catch (Exception e) {
                try {

                    appendPDF(holder, tmpPath);

                } catch (Exception e1) {
                    try {

                        appendImage(holder, tmpPath);

                    } catch (Exception e2) {
                        try {

                            appendTextFile(holder, tmpPath);

                        } catch (Exception e3) {
                            e.addSuppressed(e1);
                            e.addSuppressed(e2);
                            e.addSuppressed(e3);
                            throw e;
                        }
                    }
                }
            } finally {
                if (deletePath) {
                    Files.deleteIfExists(tmpPath);
                }
            }

        } else if (candidate instanceof TextDocument) {
            holder.insertContentFromDocumentAfter((TextDocument) candidate, holder.addParagraph(""), true);
        } else if (candidate instanceof PDDocument) {
            appendPDF(holder, (PDDocument) candidate);
        } else if (candidate instanceof PDPage) {
            // Migration to PdfBox 2.0.6 does not include PDPage.convertToImage() method anymore. Can only convert PDDocument
//            appendPage(holder, (PDPage) candidate);
            throw new UnsupportedOperationException("Object type not supported for insertion in ODT : " + candidate.getClass().getCanonicalName());
        } else if (candidate instanceof RenderedImage) {
            appendImage(holder, (RenderedImage) candidate);
        } else if (candidate instanceof CharSequence) {
            holder.addParagraph(((CharSequence) candidate).toString());
        } else {
            throw new UnsupportedOperationException("Object type not supported for insertion in ODT : " + candidate.getClass().getCanonicalName());
        }
    }

    /**
     * Put content of a text file into given document. We assume that input text
     * file paragraphs are separated by a blank line.
     *
     * Note : all lines are trimmed and all blank line are suppressed.
     *
     * @param holder Document to append data into.
     * @param txtFile Text file to read and concatenate.
     * @throws IOException If an error occurs while reading input file.
     */
    public static void appendTextFile(final TextDocument holder, final Path txtFile) throws IOException {
        try (final BufferedReader reader = Files.newBufferedReader(txtFile)) {
            String line = reader.readLine();
            if (line != null) {
                StringBuilder txtBuilder = new StringBuilder(line);
                final String sep = System.lineSeparator();
                while ((line = reader.readLine().trim()) != null) {
                    if (line.isEmpty()) {
                        // skip all blank lines
                        if (txtBuilder.length() <= 0)
                            continue;
                        holder.addParagraph(txtBuilder.toString());
                        line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        txtBuilder = new StringBuilder(line);
                    } else {
                        txtBuilder.append(sep).append(line);
                    }
                }
            }
        }
    }

    /**
     * Read a PDF document and put its content into given document.
     *
     * Note : Each PDF page is transformed into an image.
     *
     * @param target Document to put pdf content into.
     * @param input Location of the PDF document to read.
     * @throws IOException If we fail reading input pdf, or writing image
     * generated from it.
     */
    public static void appendPDF(final TextDocument target, final Path input) throws IOException {
        ArgumentChecks.ensureNonNull("Output document", target);
        ArgumentChecks.ensureNonNull("Input document", input);
        try (final InputStream in = Files.newInputStream(input, StandardOpenOption.READ);
                final PDDocument loaded = PDDocument.load(in)) {
            appendPDF(target, loaded);
        }
    }

    /**
     * Read a PDF document and put its content into given document.
     *
     * Note : Each PDF page is transformed into an image.
     *
     * @param target Document to put pdf content into.
     * @param input PDF document to read.
     * @throws IOException If we fail reading input pdf, or writing image
     * generated from it.
     */
    public static void appendPDF(final TextDocument target, final PDDocument input) throws IOException {
        ArgumentChecks.ensureNonNull("Output document", target);
        ArgumentChecks.ensureNonNull("Input document", input);
        PDFRenderer pdfRenderer = null;
        if (input.isEncrypted()) {
            Optional<String> pwd = askPassword(ASK_PASSWORD, input);
            while (pwd.isPresent()) {
                try {
                    // TODO find a way to reload the doc with pwd as openProtection()
                    //  does not exist anymore in PDFBox 2.x
//                    input.openProtection(new StandardDecryptionMaterial(pwd.get()));

                    // TODO proposed alternative
//                    final File file = File.createTempFile("filename", ".pdf");
//                    input.save(file);
//                    final PDDocument inputWithPwd = PDDocument.load(file, pwd.get());
//                    pdfRenderer = new PDFRenderer(inputWithPwd);
//                    input.close();
//                    break;
                    throw new IOException("Since the PdfBox migration to version 2.0.6, documents protected by password cannot be opened anymore.");
                }  catch (InvalidPasswordException e) {
                    pwd = askPassword("Le mot de passe est invalide. Veuillez recommencer.", input);
                }
//                catch (CryptographyException | BadSecurityHandlerException e) {
//                    pwd = askPassword("Le mot de passe est invalide. Veuillez recommencer.", input);
//                }

            }

            if (!pwd.isPresent()) {
                throw new IOException("No password available to decode PDF file.");
            }
        }
        if (pdfRenderer == null) {
            pdfRenderer = new PDFRenderer(input);
        }
        for (int page = 0; page < input.getNumberOfPages(); ++page) {
            appendPage(target, pdfRenderer, page);
        }
    }

    /**
     * Pops a JavaFX alert to query document password.
     *
     * @param headerText A quick sentence about what is asked to user.
     * @param input Document to get password for.
     * @return An optional containing user password, or an empty optional if
     * user cancelled the alert.
     */
    public static Optional<String> askPassword(final String headerText, final PDDocument input) {
        final String sep = System.lineSeparator();
        final StringBuilder builder = new StringBuilder(headerText).append(sep).append("Informations sur le document : ");

        final PDDocumentInformation docInfo = input.getDocumentInformation();
        builder.append("Titre : ").append(valueOrUnknown(docInfo.getTitle())).append(sep);
        builder.append("Auteur : ").append(valueOrUnknown(docInfo.getAuthor())).append(sep);
        builder.append("Editeur : ").append(valueOrUnknown(docInfo.getProducer())).append(sep);
        builder.append("Sujet : ").append(valueOrUnknown(docInfo.getSubject()));

        final Callable<Optional<String>> query = () -> {
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.CANCEL, ButtonType.OK);
            alert.setHeaderText(builder.toString());

            final PasswordField field = new PasswordField();
            HBox.setHgrow(field, Priority.ALWAYS);
            alert.getDialogPane().setContent(new HBox(5, new Label("Mot de passe : "), field));
            alert.setResizable(true);
            if (ButtonType.OK.equals(alert.showAndWait().orElse(ButtonType.CANCEL))) {
                final String pwd = field.getText();
                return Optional.of(pwd == null ? "" : pwd);
            } else {
                return Optional.empty();
            }
        };

        if (Platform.isFxApplicationThread()) {
            try {
                return query.call();
            } catch (Exception ex) {
                throw new SirsCoreRuntimeException(ex);
            }
        } else {
            final TaskManager.MockTask<Optional<String>> mockTask = new TaskManager.MockTask("Demande de mot de passe", query);
            Platform.runLater(mockTask);
            try {
                return mockTask.get();
            } catch (InterruptedException ex) {
                // No response for too long
                return Optional.empty();
            } catch (ExecutionException ex) {
                throw new SirsCoreRuntimeException(ex);
            }
        }
    }

    /**
     * Adapt input string value.
     *
     * @param input The string to test.
     * @return input string if not null nor empty, or "Inconnu".
     */
    private static String valueOrUnknown(final String input) {
        if (input == null || input.isEmpty()) {
            return "Inconnu";
        } else {
            return input;
        }
    }

    /**
     * Put content of the given PDF page at the end of input ODT document.
     *
     * Note: As processing pdf text is hell (no, really), we just convert it
     * into an image and put it into given document.
     *
     * @param holder Document to append content to.
     * @param pdfRenderer the PDDocument containing the page to convert.
     * @param page the page number of the document to convert into image.
     * @throws java.io.IOException If we fail reading input page, or writing
     * generated image.
     */
    public static void appendPage(final TextDocument holder, final PDFRenderer pdfRenderer, final int page) throws IOException {
        final BufferedImage img = pdfRenderer.renderImageWithDPI(page, 200);
        ODTUtils.appendImage(holder, img, true);
    }

    /**
     * Print given photograph into input document. Created image title and
     * description will be filled with input {@link AbstractPhoto} information
     * (libelle, comment, author and date).
     *
     * @param holder Document to append content to.
     * @param anchor Paragraph to insert image into. If null, image is append at
     * the end of the document.
     * @param toPrint Photo to insert in document.
     * @param fullPage Used only if image is not inserted in a paragraph.
     * Specifies if we should dedicate a page for image display.
     * @return Created image.
     * @throws IllegalArgumentException If we cannot find image pointed by input {@link AbstractPhoto}.
     */
    public static Image appendImage(final TextDocument holder, final Paragraph anchor, final AbstractPhoto toPrint, final boolean fullPage) throws IllegalArgumentException {
        final String chemin = toPrint.getChemin();
        if (chemin == null || chemin.isEmpty())
            throw new IllegalArgumentException("Input photograph path is invalid !");
        final Path imgPath = SirsCore.getDocumentAbsolutePath(toPrint);

        if (!Files.isRegularFile(imgPath)) {
            throw new IllegalArgumentException("Input photograph file cannot be found !");
        }

        final Image img;
        if (anchor == null) {
            img = appendImage(holder, imgPath, fullPage);
        } else {
            img = Image.newImage(anchor, imgPath.toUri());
        }

        final String title = toPrint.getLibelle();
        if (title == null || title.isEmpty()) {
            img.setTitle(title);
        }

        final StringBuilder description = new StringBuilder();
        final LocalDate date = toPrint.getDate();
        description.append(System.lineSeparator());
        if (date == null) {
            description.append("date inconnue");
        } else {
            description.append("Prise le : ").append(date);
        }

        final String photographeId = toPrint.getPhotographeId();
        description.append(System.lineSeparator());
        if (photographeId == null) {
            description.append("Photographe inconnu");
        } else {
            description.append("par : ").append(InjectorCore.getBean(SessionCore.class).getPreviews().get(photographeId).getLibelle());
        }

        final String commentaire = toPrint.getCommentaire();
        if (commentaire != null && !commentaire.isEmpty()) {
            description.append(System.lineSeparator()).append("Commentaire : ").append(commentaire);
        }

        return img;
    }

    /**
     * Insert given image into input document. Image is inserted in a new empty
     * paragraph, to avoid overlapping with another content.
     *
     * @param holder Document to insert image into.
     * @param image Image to put.
     * @return Created image in document.
     * @throws java.io.IOException If we cannot write input image.
     */
    public static Image appendImage(final TextDocument holder, final RenderedImage image) throws IOException {
        return appendImage(holder, image, false);
    }

    /**
     * Insert an image into given document.
     *
     * @param holder Document to insert image into.
     * @param image Image to put in
     * @param fullPage True if given image should be placed on a new page,
     * alone. alse to integrate it just below previous element in the document.
     * @return Created image in document.
     * @throws IOException If input image cannot be written in given document.
     */
    public static Image appendImage(final TextDocument holder, final RenderedImage image, final boolean fullPage) throws IOException {
        final Path tmpImage = Files.createTempFile("img", ".png");
        if (!ImageIO.write(image, "png", tmpImage.toFile())) {
            throw new IllegalStateException("No valid writer found for image format png !");
        }

        try {
            final StyleTypeDefinitions.PrintOrientation orientation = (image.getWidth() < image.getHeight())
                    ? StyleTypeDefinitions.PrintOrientation.PORTRAIT : StyleTypeDefinitions.PrintOrientation.LANDSCAPE;
            return appendImage(holder, tmpImage, orientation, fullPage);
        } finally {
            try {
                Files.delete(tmpImage);
            } catch (IOException e) {
                SirsCore.LOGGER.log(Level.FINE, "A temporary file cannot be deleted !", e);
            }
        }
    }

    /**
     * Insert given image into input document. Image is inserted in a new empty
     * paragraph, to avoid overlapping with another content.
     *
     * @param holder Document to insert into
     * @param imagePath Location off the image to insert.
     * @return Created image in document.
     */
    public static Image appendImage(final TextDocument holder, final Path imagePath) {
        return appendImage(holder, imagePath, null, false);
    }

    /**
     * Insert given image into input document. Image is inserted in a new empty
     * paragraph, to avoid overlapping with another content.
     *
     * @param holder Document to insert into
     * @param imagePath Location off the image to insert.
     * @param fullPage If true, we will create a new page (no margin,
     * orientation set according to image dimension) in which the image will be
     * rendered.
     * @return Created image in document.
     */
    public static Image appendImage(final TextDocument holder, final Path imagePath, final boolean fullPage) {
        return appendImage(holder, imagePath, null, fullPage);
    }

    /**
     * Insert given image into input document. Image is inserted in a new empty
     * paragraph, to avoid overlapping with another content.
     *
     * @param holder Document to insert into
     * @param imagePath Location off the image to insert.
     * @param orientation Orientation of the image to insert. Only used if a
     * full page rendering is queried. If null, we will try to determine the
     * best orientation by analyzing image dimension.
     * @param fullPage If true, we will create a new page (no margin,
     * orientation set by previous parameter) in which the image will be
     * rendered.
     * @return Created image in document.
     */
    public static Image appendImage(
            final TextDocument holder,
            final Path imagePath,
            StyleTypeDefinitions.PrintOrientation orientation,
            final boolean fullPage) {

        Paragraph tmpParagraph = holder.addParagraph("");
        Dimension2D pageDim = null;
        Insets margin = null;
        if (fullPage) {
            if (orientation == null) {
                try {
                    ImageReader reader = XImageIO.getReader(imagePath.toFile(), false, false);
                    orientation = (reader.getWidth(0) < reader.getHeight(0))
                            ? StyleTypeDefinitions.PrintOrientation.PORTRAIT : StyleTypeDefinitions.PrintOrientation.LANDSCAPE;
                } catch (IOException e) {
                    SirsCore.LOGGER.log(Level.WARNING, "Cannot read image attributes : " + imagePath, e);
                    orientation = StyleTypeDefinitions.PrintOrientation.PORTRAIT;
                }
            }

            try {
                final MasterPage masterPage = getOrCreateOrientationMasterPage(holder, orientation, Insets.EMPTY);
                OdfOfficeStyles styles = holder.getStylesDom().getOfficeStyles();
                OdfStyle toUse = null;
                for (final OdfStyle style : styles.getStylesForFamily(OdfStyleFamily.Paragraph)) {
                    if (masterPage.getName().equals(style.getStyleMasterPageNameAttribute())) {
                        toUse = style;
                        break;
                    }
                }
                if (toUse == null) {
                    toUse = styles.newStyle(UUID.randomUUID().toString(), OdfStyleFamily.Paragraph);
                    toUse.setStyleMasterPageNameAttribute(masterPage.getName());
                }
                /* BUG ! Cannot set paragraph style name directly, see :
                 * http://stackoverflow.com/questions/26574174/setting-style-on-a-paragraph-using-odf-toolkit
                 */
                tmpParagraph.getOdfElement().setStyleName(toUse.getStyleNameAttribute());
                margin = Insets.EMPTY;
                pageDim = new Dimension2D(masterPage.getPageWidth(), masterPage.getPageHeight());
            } catch (Exception ex) {
                SirsCore.LOGGER.log(Level.WARNING, "Cannot add a page break in a text document", ex);
            }
        }

        final Image newImage = Image.newImage(tmpParagraph, imagePath.toUri());
        try {
            resizeImage(newImage, pageDim, margin, Units.MILLIMETRE, true, false);
        } catch (UnconvertibleException | IncommensurableException ex) {
            SirsCore.LOGGER.log(Level.WARNING, "Cannot resize an image.", ex);
        }

        return newImage;
    }

    /**
     * Resize input image to fit given page requirements.
     *
     * TODO : find page parameters by analyzing input image.
     *
     * @param toResize The image to resize.
     * @param pageDim Dimension of the page containing image. If null {@link #PORTRAIT_WIDTH} and {@link #PORTRAIT_HEIGHT} are assumed as dimension.
     * @param margin Margins of the page to put image into. If null, {@link #DEFAULT_MARGIN} is assumed for each border.
     * @param measureUnit Unit in which page dimension/margin are expressed. If null, milimeter is assumed.
     * @param keepRatio True if we should keep image Ratio, or false to distort it to fit strictly the page.
     * @param forceLower True to resize image even if the page is bigger, false to left image untouched if it already fit into page.
     * @throws UnconvertibleException If we cannot convert given page dimension into image rectangle unit.
     * @throws IncommensurableException If we cannot convert units.
     */
    public static void resizeImage(
            final Image toResize,
            Dimension2D pageDim,
            Insets margin,
            Unit measureUnit,
            final boolean keepRatio,
            final boolean forceLower) throws UnconvertibleException, IncommensurableException {

        ArgumentChecks.ensureNonNull("Image to resize", toResize);
        final FrameRectangle rectangle = toResize.getRectangle();
        final Unit rectUnit = Units.valueOf(rectangle.getLinearMeasure().toString());

        if (measureUnit == null) {
            measureUnit = Units.MILLIMETRE;
        }

        final UnitConverter pageConverter, marginConverter;
        if (pageDim == null) {
            pageDim = new Dimension2D(PORTRAIT_WIDTH, PORTRAIT_HEIGHT);
            pageConverter = Units.MILLIMETRE.getConverterToAny(rectUnit);
        } else {
            pageConverter = measureUnit.getConverterTo(rectUnit);
        }
        if (margin == null) {
            margin = new Insets(DEFAULT_MARGIN);
            marginConverter = Units.MILLIMETRE.getConverterToAny(rectUnit);
        } else {
            marginConverter = measureUnit.getConverterTo(rectUnit);
        }

        ArgumentChecks.ensurePositive("Page width", pageDim.getWidth());
        ArgumentChecks.ensurePositive("Page height", pageDim.getHeight());

        ArgumentChecks.ensurePositive("Margin top", margin.getTop());
        ArgumentChecks.ensurePositive("Margin right", margin.getRight());
        ArgumentChecks.ensurePositive("Margin bottom", margin.getBottom());
        ArgumentChecks.ensurePositive("Margin left", margin.getLeft());

        // Initialize output dimension to available page space.
        double width = pageConverter.convert(pageDim.getWidth()) - (marginConverter.convert(margin.getLeft()) + marginConverter.convert(margin.getRight()) + rectangle.getX());
        double height = pageConverter.convert(pageDim.getHeight()) - (marginConverter.convert(margin.getTop()) + marginConverter.convert(margin.getBottom()) + rectangle.getY());

        // No need for rescale if more little image is accepted.
        if (!forceLower && rectangle.getWidth() <= width && rectangle.getHeight() <= height)
            return;

        if (keepRatio) {
            final double ratio = Math.min(width / rectangle.getWidth(), height / rectangle.getHeight());
            width = rectangle.getWidth() * ratio;
            height = rectangle.getHeight() * ratio;
        }

        rectangle.setWidth(width);
        rectangle.setHeight(height);
        toResize.setRectangle(rectangle);
    }

    /**
     * Create a table at the end of the given {@link TableContainer}, and fill
     * it with input data.
     *
     * Note : if no property listing is given, no ordering will be performed on
     * columns.
     *
     * @param target Document or section to put table into.
     * @param data List of elements to extract properties from in order to fill
     * table. Each element of the iterator is a row in output table.
     * @param propertyNames List of properties (as returned by {@link PropertyDescriptor#getName()
     * } to use for table columns. If null or empty, all properties of input
     * objects will be used.
     * @param printMapping
     * @throws java.beans.IntrospectionException If an error occurs while
     * analyzing properties of an element.
     * @throws java.lang.ReflectiveOperationException If an error occurs while
     * accessing an element property.
     */
    public static void appendTable(final TableContainer target, final Iterator<Element> data, final List<String> propertyNames,
            final Map<String, Function<Element, String>> printMapping)
            throws IntrospectionException, ReflectiveOperationException {

        ArgumentChecks.ensureNonNull("Target document", target);
        if (data == null || !data.hasNext()) {
            return; // No elements, do not create table
        }

        Element element = data.next();
        Class<? extends Element> elementClass = element.getClass();
        Map<String, PropertyDescriptor> elementProperties = SirsCore.listSimpleProperties(elementClass);
        final Map<String, Map<String, PropertyDescriptor>> descriptorsByClass = new HashMap<>();
        descriptorsByClass.put(elementClass.getCanonicalName(), elementProperties);

        final List<String> headers;
        if (propertyNames == null) {
            headers = new ArrayList<>(elementProperties.keySet());
        } else {
            headers = propertyNames;
        }

        // Create table and headers
        final Table table = target.addTable(1, headers.size());
        final OdfStyle headerStyle;
        if (target instanceof OdfSchemaDocument) {
            headerStyle = getOrCreateTableHeaderStyle((OdfSchemaDocument) target);
        } else {
            headerStyle = null;
        }

        final LabelMapper lMapper_0 = LabelMapper.get(elementClass);
        final Row dataRow_0 = table.getRowByIndex(0);
        Cell currentCell;
        for (int i = 0; i < headers.size(); i++) {
            currentCell = dataRow_0.getCellByIndex(i);
            currentCell.addParagraph(lMapper_0.mapPropertyName(headers.get(i)))
                    .getFont().setFontStyle(StyleTypeDefinitions.FontStyle.BOLD);
            if (headerStyle != null) {
                currentCell.setCellStyleName(headerStyle.getStyleNameAttribute());
            }
        }

        // Fill first line
        final Row dataRow_1 = table.appendRow();
        for (int i = 0; i < headers.size(); i++) {
            final String propertyName = headers.get(i);
            final PropertyDescriptor desc = elementProperties.get(propertyName);
            final PropertyPrinter printer = Printers.getPrinter(propertyName);
            if (desc != null) {
                dataRow_1.getCellByIndex(i).addParagraph(printer.print(element, desc));
            }
            else if(printMapping.get(propertyName)!=null){
                dataRow_1.getCellByIndex(i).addParagraph(printer.print(element, propertyName, printMapping.get(propertyName)));
            }
            else {
                SirsCore.LOGGER.log(Level.INFO, "Cannot hangle {0} column (first line)", propertyName);
            }
        }

        // Fill remaining lines
        while (data.hasNext()) {
            element = data.next();
            elementClass = element.getClass();
            final LabelMapper lMapper = LabelMapper.get(elementClass);
            /*
             * If current object is a new type of element, we retrieve its properties
             * and add necessary columns in case none have been specified as input.
             */
            elementProperties = descriptorsByClass.get(elementClass.getCanonicalName());
            if (elementProperties == null) {
                elementProperties = SirsCore.listSimpleProperties(elementClass);
                descriptorsByClass.put(elementClass.getCanonicalName(), elementProperties);
                // If no properties have been specified, we put all properties of the current object in the table.
                if (propertyNames == null) {
                    final Set<String> tmpKeys = elementProperties.keySet();
                    for (final String key : tmpKeys) {
                        if (!headers.contains(key)) {
                            headers.add(key);
                            table.appendColumn().getCellByIndex(0).addParagraph(lMapper.mapPropertyName(key))
                                    .getFont().setFontStyle(StyleTypeDefinitions.FontStyle.BOLD);
                        }
                    }
                }
            }

            // Fill data
            final Row dataRow = table.appendRow();
            for (int i = 0; i < headers.size(); i++) {
                final String propertyName = headers.get(i);
                final PropertyDescriptor desc = elementProperties.get(propertyName);
                final PropertyPrinter printer = Printers.getPrinter(propertyName);
                if (desc != null) {
                    dataRow.getCellByIndex(i).addParagraph(printer.print(element, desc));
                }
                else if(printMapping.get(propertyName)!=null){
                    dataRow.getCellByIndex(i).addParagraph(printer.print(element, propertyName, printMapping.get(propertyName)));
                }
                else {
                    SirsCore.LOGGER.log(Level.INFO, "Cannot handle {0} column", propertyName);
                }
            }
        }
    }

    public static void appendTable(final TableContainer target, final FeatureIterator data, List<String> propertyNames) {
        ArgumentChecks.ensureNonNull("Target document", target);
        if (data == null || !data.hasNext()) {
            return; // No elements, do not create table
        }

        Feature next = data.next();
        if (propertyNames == null) {
            propertyNames = next.getType().getProperties(true).stream().map(type -> type.getName().tip().toString()).collect(Collectors.toList());
        }

        // Create table and headers
        final Table table = target.addTable(1, propertyNames.size());
        final OdfStyle headerStyle;
        if (target instanceof OdfSchemaDocument) {
            headerStyle = getOrCreateTableHeaderStyle((OdfSchemaDocument) target);
        } else {
            headerStyle = null;
        }
        Row dataRow = table.getRowByIndex(0);
        for (int i = 0; i < propertyNames.size(); i++) {
            final Cell currentCell = dataRow.getCellByIndex(i);
            currentCell.setStringValue(propertyNames.get(i));
            if (headerStyle != null) {
                currentCell.setCellStyleName(headerStyle.getStyleNameAttribute());
            }
        }

        // Fill first line
        dataRow = table.appendRow();
        String propertyName;
        for (int i = 0; i < propertyNames.size(); i++) {
            propertyName = propertyNames.get(i);
            dataRow.getCellByIndex(i).setStringValue(Printers.getPrinter(propertyName).print(next, propertyName));
        }

        // remaining lines
        while (data.hasNext()) {
            next = data.next();
            dataRow = table.appendRow();
            for (int i = 0; i < propertyNames.size(); i++) {
                propertyName = propertyNames.get(i);
                dataRow.getCellByIndex(i).setStringValue(Printers.getPrinter(propertyName).print(next, propertyName));
            }
        }
    }

    public static OdfStyle getOrCreateTableHeaderStyle(final OdfSchemaDocument source) {
        OdfOfficeStyles styles = source.getOrCreateDocumentStyles();
        OdfStyle style = styles.getStyle("table.header", OdfStyleFamily.TableCell);
        if (style == null) {
            style = styles.newStyle("table.header", OdfStyleFamily.TableCell);
            final TableCellProperties cellProps = TableCellProperties.getOrCreateTableCellProperties(style);
            cellProps.setBackgroundColor(new Color(109, 149, 182));
            cellProps.setVerticalAlignment(StyleTypeDefinitions.VerticalAlignmentType.MIDDLE);
            //ParagraphProperties.getOrCreateParagraphProperties(style).setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.CENTER);
        }
        return style;
    }

    /**
     * Génération d'un rapport.
     *
     * @param reportModel modèle de rapport contenant la description des différentes sections à inclure
     * @param elements éléments candidats à l'inclusion dans le rapport
     * @param output
     * @param title titre du rapport, si le titre est null ou vide, c'est le titre du modèle de rapport qui sera utilisé
     * @return
     */
    public static Task generateReport(final ModeleRapport reportModel, final Collection<? extends Element> elements, final Path output, String title) {

        /*
        A- détermination d'un titre
        ===========================*/

        final String titre;
        if (title == null || title.isEmpty()) {
            titre = reportModel.getLibelle();
        } else {
            titre = title;
        }


        /*
        B- construction et lancement de la tâche de génération
        =====================================================*/

        return TaskManager.INSTANCE.submit(new Task() {
            @Override
            protected Object call() throws Exception {


                updateTitle("Génération de rapport" + (titre != null ? " (" + titre + ")" : ""));


                /*
                1- récupération des différentes sections du rapport
                --------------------------------------------------*/

                final ObservableList<AbstractSectionRapport> sections = reportModel.getSections();


                /*
                2- initialisation des paramètres de suivi de la progression de la tâche
                -----------------------------------------------------------------------*/
                final long totalWork = elements == null? -1 : elements.size() * sections.size();
                final AtomicLong currentWork = new AtomicLong(-1);


                /*
                3- création et remplissage du document section par section
                ---------------------------------------------------------*/

                // on crée le document de rapport
                try (final TextDocument headerDoc = TextDocument.newTextDocument()) {

                    // a- titre du rapport
                    if (titre != null && !titre.isEmpty()) {
                        final Paragraph paragraph = headerDoc.addParagraph(titre);
                        paragraph.applyHeading();
                    }

                    // b- parcours des sections pour impression au fur et à mesure
                    // on aggrege chaque section
                    AbstractSectionRapport section;
                    long expectedWork;
                    for (int i = 0; i < sections.size(); i++) {

                        // récupération de la i-ème section
                        section = sections.get(i);

                        // mise à jour du message d'information indiquant l'impression de la section en cours
                        String libelle = section.getLibelle();
                        if (libelle == null || libelle.isEmpty()) {
                            libelle = "sans nom";
                        }
                        updateMessage("Génération de la section : " + libelle);

                        // impression des éléments par la section : l'impression est gérée au cas par cas par le type de section
                        if (elements == null || elements.isEmpty()) {
                            // cas particulier : on force à null si aucun élément n'est fourni
                            section.print(headerDoc, null);
                        } else {
                            try (final Stream dataStream = elements.stream().peek(input -> updateProgress(currentWork.incrementAndGet(), totalWork))) {

                                // délégaton de l'impression au type de section
                                section.print(headerDoc, dataStream);

                                // In case section printing has not used provided stream, we have to update progress manually.
                                expectedWork = ((long)i + 1) * elements.size() - 1;
                                if (currentWork.get() != expectedWork) {
                                    currentWork.set(expectedWork);
                                    updateProgress(expectedWork, totalWork);
                                }
                            }
                        }
                    }

                    // on sauvegarde le tout
                    updateProgress(-1, -1);
                    updateMessage("Sauvegarde du rapport");

                    try (final OutputStream out = Files.newOutputStream(output)) {
                        headerDoc.save(out);
                    }

                    return true;
                }
            }
        });
    }
}
