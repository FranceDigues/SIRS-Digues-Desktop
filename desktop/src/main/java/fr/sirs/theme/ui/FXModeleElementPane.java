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
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.report.ModeleElement;
import fr.sirs.ui.Growl;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.odt.ODTUtils;
import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.text.Collator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.common.field.VariableField;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class FXModeleElementPane extends AbstractFXElementPane<ModeleElement> {

    /**
     * Attributs que l'on ne souhaite pas garder dans le formulaire.
     */
    private static final List<String> FIELDS_TO_IGNORE = Arrays.asList(new String[]{SIRS.AUTHOR_FIELD, SIRS.VALID_FIELD, SIRS.FOREIGN_PARENT_ID_FIELD, SIRS.GEOMETRY_MODE_FIELD, SIRS.SELECTED_CRS_NAME, ODTUtils.CLASS_KEY});

    private static String DESKTOP_UNSUPPORTED = "Impossible de dialoguer avec le système. Pour éditer le modèle, vous pouvez cependant utiliser la fonction d'export, "
            + "puis ré-importer votre ficher lorsque vous aurez terminé vos modifications.";

    @FXML
    private TextField uiTitle;

    @FXML
    private ComboBox<Class> uiTargetClass;

    @FXML
    private Button uiImportODT;

    @FXML
    private Button uiExportODT;

    @FXML
    private Label uiNoModelLabel;

    @FXML
    private Label uiModelPresentLabel;

    @FXML
    private Label uiSizeLabel;

    @FXML
    private HBox uiEditBar;

    @FXML
    private BorderPane uiPropertyPane;

    @FXML
    private ListView<String> uiAvailableProperties;

    @FXML
    private ListView<String> uiUsedProperties;

    @FXML
    private Button uiGenerate;

    @FXML
    private Label uiProgressLabel;

    @FXML
    private ProgressIndicator uiProgress;

    /**
     * A temporary file used for ODT modifications happening before save.
     */
    private final Path tempODT;
    private final SimpleBooleanProperty tempODTexists = new SimpleBooleanProperty(false);

    /**
     * Property holding current performed task. It allow us to centralize
     * progress behavior definition, by binding it to this property.
     */
    private final ObjectProperty<Task> taskProperty = new SimpleObjectProperty<>();

    private final ObservableList<String> availableProperties = FXCollections.observableArrayList();
    private final ObservableList<String> usedProperties = FXCollections.observableArrayList();

    public FXModeleElementPane() {
        super();
        SIRS.loadFXML(this);

        uiAvailableProperties.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiAvailableProperties.setItems(availableProperties);
        uiUsedProperties.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiUsedProperties.setItems(usedProperties);

        // Build ordered list of possible target classes.
        final SirsStringConverter converter = new SirsStringConverter();
        final Collator collator = Collator.getInstance();
        final ObservableList<Class> classes = FXCollections.observableArrayList(
                Injector.getSession().getAvailableModels());
        classes.sort((o1, o2) -> collator.compare(converter.toString(o1), converter.toString(o2)));
        SIRS.initCombo(uiTargetClass, classes, null);

        taskProperty.addListener(this::taskChanged);

        // Modify available properties when target class is modified.
        uiTargetClass.valueProperty().addListener(this::updateAvailableProperties);
        uiExportODT.setDisable(true);
        uiEditBar.managedProperty().bind(visibleProperty());
        uiEditBar.setVisible(false);
        uiSizeLabel.setVisible(false);
        uiModelPresentLabel.setVisible(false);
        uiGenerate.setDisable(true);

        // Activation rules.
        uiTitle.disableProperty().bind(disableFieldsProperty());
        uiTargetClass.disableProperty().bind(disableFieldsProperty());
        uiImportODT.disableProperty().bind(disableFieldsProperty());
        uiEditBar.disableProperty().bind(disableFieldsProperty());
        uiPropertyPane.disableProperty().bind(disableFieldsProperty());
        uiGenerate.disableProperty().bind(disableFieldsProperty());
        uiExportODT.disableProperty().bind(tempODTexists.not());

        uiEditBar.visibleProperty().bind(tempODTexists);
        uiEditBar.managedProperty().bind(uiEditBar.visibleProperty());
        uiNoModelLabel.visibleProperty().bind(tempODTexists.not());
        uiNoModelLabel.managedProperty().bind(uiNoModelLabel.visibleProperty());
        uiModelPresentLabel.visibleProperty().bind(tempODTexists);
        uiModelPresentLabel.managedProperty().bind(uiModelPresentLabel.visibleProperty());
        uiSizeLabel.visibleProperty().bind(tempODTexists);
        uiSizeLabel.managedProperty().bind(uiSizeLabel.visibleProperty());

        // Prepare reference to temporary file used for ODT edition. Keep it suppressed until we need it.
        try {
            tempODT = Files.createTempFile("sirs", ".odt");
            Files.delete(tempODT);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create temporary file !", ex);
        }

        // When ODT document is updated, it change label displaying its size.
        elementProperty.addListener(this::elementChanged);
            tempODTexists.addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                try {
                    uiSizeLabel.setText(SIRS.toReadableSize(
                            Files.getFileAttributeView(tempODT, BasicFileAttributeView.class).readAttributes().size()));
                } catch (IOException ex) {
                    SirsCore.LOGGER.log(Level.WARNING, "Unable to read file size : "+tempODT, ex);
                    uiSizeLabel.setText("illisible");
                }
            } else {
                uiSizeLabel.setText("Modèle inexistant");
            }
        });
    }

    public FXModeleElementPane(final ModeleElement input) {
        this();
        setElement(input);
    }

    private void taskChanged(ObservableValue<? extends Task> obs, Task oldTask, Task newTask) {
        if (oldTask != null) {
            uiProgress.visibleProperty().unbind();
            uiProgressLabel.visibleProperty().unbind();
            uiProgressLabel.textProperty().unbind();
        }

        if (newTask != null) {
            uiProgressLabel.textProperty().bind(newTask.titleProperty());
            uiProgressLabel.visibleProperty().bind(newTask.runningProperty());
            uiProgress.visibleProperty().bind(newTask.runningProperty());
            uiProgress.progressProperty().bind(newTask.progressProperty());

        } else {
            uiProgressLabel.setVisible(false);
            uiProgress.setVisible(false);
        }
    }

    private void elementChanged(ObservableValue<? extends ModeleElement> obs, ModeleElement oldElement, ModeleElement newElement) {
        if (oldElement != null) {
            uiTitle.textProperty().unbindBidirectional(oldElement.libelleProperty());
        }

        try {
            Files.deleteIfExists(tempODT);
            tempODTexists.set(false);
        } catch (IOException ex) {
            throw new SirsCoreRuntimeException("Cannot delete temporary file !", ex);
        }

        if (newElement != null) {
            uiTitle.textProperty().bindBidirectional(newElement.libelleProperty());
            final String targetClassName = newElement.getTargetClass();
            if (targetClassName == null || targetClassName.isEmpty()) {
                uiTargetClass.setValue(null);
            } else {
                try {
                    Class<?> tClass = Thread.currentThread().getContextClassLoader().loadClass(targetClassName);
                    uiTargetClass.setValue(tClass);
                } catch (ClassNotFoundException e) {
                    SirsCore.LOGGER.log(Level.WARNING, "Invalid target class in model "+newElement.getId(), e);
                    uiTargetClass.setValue(null);
                }
            }
            final byte[] odt = newElement.getOdt();

            // Temporary file for ODT edition
            final Task<Boolean> loadTask = new Task() {

                @Override
                protected Object call() throws Exception {
                    updateTitle("Chargement d'un modèle");
                    if (odt == null || odt.length < 1) {
                        // deletion has been done before.
                        return false;
                    } else {
                        // Check saved document is a real odt, + analyze contained properties.
                        try (final InputStream stream = new ByteArrayInputStream(odt)) {
                            updateProperties(stream);
                        }
                        Files.write(tempODT, odt, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        return true;
                    }
                }
            };

            taskProperty.set(loadTask);
            loadTask.setOnSucceeded((success) -> {
                Platform.runLater(() -> {
                    tempODTexists.set(loadTask.getValue());
                    new Growl(Growl.Type.INFO, "Chargement terminé").showAndFade();
                });
            });
            loadTask.setOnFailed((fail) -> {
                Platform.runLater(() -> {
                    tempODTexists.set(false);
                    GeotkFX.newExceptionDialog("Imposssible de charger le modèle", loadTask.getException()).show();
                });
            });

            TaskManager.INSTANCE.submit(loadTask);

        } else {
            uiTitle.setText(null);
            uiTargetClass.setValue(null);
        }
    }

    /**
     * Called when model class is changed. It reloads property lists.
     * WARNING : Does not update ODT template.
     *
     * @param obs Originating property which has changed.
     * @param oldValue Old model type.
     * @param newValue New model type.
     */
    private void updateAvailableProperties(ObservableValue<? extends Class> obs, Class oldValue, Class newValue) {
        if (newValue == null) {
            availableProperties.clear();
            usedProperties.clear();
            return;
        }

        final Set<String> properties;
        try {
            properties = SIRS.listSimpleProperties(newValue).keySet();
        } catch (IntrospectionException ex) {
            GeotkFX.newExceptionDialog("Une erreur inattendue est survenue lors de la sélection du type d'objet.", ex);
            uiTargetClass.setValue(oldValue);
            return;
        }
        properties.removeAll(FIELDS_TO_IGNORE);

        // refresh available property list
        availableProperties.setAll(properties);
        // Remove all selected properties not present into selected type.
        final Iterator<String> usedProps = usedProperties.iterator();
        while (usedProps.hasNext()) {
            if (!properties.remove(usedProps.next())) {
                usedProps.remove();
            }
        }

        // Finally, try to configure translations to display properties.
        final LabelMapper mapper = LabelMapper.get(newValue);
        if (mapper != null) {
            final Callback<ListView<String>, ListCell<String>> converter = (ListView<String> param) -> {
                return new ListCell<String>() {

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(mapper.mapPropertyName(item));
                        }
                    }

                };
            };
            uiAvailableProperties.setCellFactory(converter);
            uiUsedProperties.setCellFactory(converter);
        }
    }

    @FXML
    void addProperties(ActionEvent event) {
        final ObservableList<String> selectedItems = uiAvailableProperties.getSelectionModel().getSelectedItems();
        // TODO : null pointer check.
        usedProperties.addAll(selectedItems);
        availableProperties.removeAll(selectedItems);
    }

    @FXML
    void deleteODT(ActionEvent event) {
        try {
            Files.deleteIfExists(tempODT);
            tempODTexists.set(false);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot delete temporary file !", ex);
        }
    }

    @FXML
    void editODT(ActionEvent event) {
        final Task<Boolean> editTask = SIRS.openFile(tempODT);
        taskProperty.set(editTask);
        editTask.setOnFailed(taskEvent -> {
            Platform.runLater(() -> {
                final Alert alert = new Alert(AlertType.WARNING, DESKTOP_UNSUPPORTED, ButtonType.OK);
                alert.setResizable(true);
                alert.show();
            });
        });
    }

    @FXML
    void exportODT(ActionEvent event) {
        final FileChooser outputChooser = new FileChooser();
        File output = outputChooser.showSaveDialog(getScene().getWindow());
        if (output != null) {
            final Task exportTask = TaskManager.INSTANCE.submit("Export de modèle", () -> {
                if (Files.isRegularFile(tempODT)) {
                    Files.copy(tempODT, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.write(output.toPath(), elementProperty.get().getOdt(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                }
                return true;
            });

            taskProperty.set(exportTask);
            exportTask.setOnSucceeded((success) -> Platform.runLater(() -> new Growl(Growl.Type.INFO, "Export terminé").showAndFade()));
            exportTask.setOnCancelled((cancelled) -> Platform.runLater(() -> new Growl(Growl.Type.WARNING, "Export annulé").showAndFade()));
            exportTask.setOnFailed((failed) -> Platform.runLater(() ->
                GeotkFX.newExceptionDialog("L'export a échoué suite à une erreur inattendue.", exportTask.getException()).show()
            ));
        }
    }

    @FXML
    void updateODT(ActionEvent event) {
        // Get a title for each wanted property.
        final HashMap<String, String> properties = new HashMap<>(usedProperties.size());
        final Class targetClass = uiTargetClass.getValue();
        final Function<String, String> nameMapper = createPropertyNameMapper(targetClass);
        for (final String prop : usedProperties) {
            properties.put(prop, nameMapper.apply(prop));
        }

        final Task generationTask;
        if (Files.isRegularFile(tempODT)) {
            final Alert alert = new Alert(AlertType.WARNING, "Attention, le modèle existant sera modifié, êtes-vous sûr ?", ButtonType.NO, ButtonType.YES);
            alert.setResizable(true);
            if (ButtonType.YES.equals(alert.showAndWait().orElse(null))) {
                generationTask = TaskManager.INSTANCE.submit("Génération d'un modèle", () -> {
                    final TextDocument tmpDoc;
                    try (final InputStream docInput = Files.newInputStream(tempODT, StandardOpenOption.READ)) {
                        tmpDoc = TextDocument.loadDocument(docInput);
                        ODTUtils.setVariables(tmpDoc, properties);
                        ODTUtils.setTargetClass(tmpDoc, targetClass);
                    }
                    try (OutputStream docOutput = Files.newOutputStream(tempODT, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                        tmpDoc.save(docOutput);
                    }
                    return true;
                });
            } else {
                generationTask = null;
            }
        } else {
            generationTask = TaskManager.INSTANCE.submit("Génération d'un modèle", () -> {
                try (final OutputStream outputStream = Files.newOutputStream(tempODT, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                    final TextDocument newTemplate = ODTUtils.newSimplePropertyModel(uiTitle.getText(), properties);
                    ODTUtils.setTargetClass(newTemplate, targetClass);
                    newTemplate.save(outputStream);
                }
                return true;
            });
        }

        if (generationTask != null) {
            taskProperty.set(generationTask);
            generationTask.setOnSucceeded((success) -> Platform.runLater(() -> {
                tempODTexists.set(true);
                new Growl(Growl.Type.INFO, "Génération terminée").showAndFade();
            }));
            generationTask.setOnCancelled((cancel) -> Platform.runLater(() -> new Growl(Growl.Type.WARNING, "Génération annulée").showAndFade()));
            generationTask.setOnFailed((success) -> Platform.runLater(() ->
                    GeotkFX.newExceptionDialog("La génération du modèle a échoué !", generationTask.getException()).show()));
        }
    }

    @FXML
    void importODT(ActionEvent event) {
        final FileChooser inputChooser = new FileChooser();
        inputChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous", "*"));
        inputChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OpenOffice document", "*.odt", "*.ODT"));
        File tmpInput = inputChooser.showOpenDialog(getScene().getWindow());
        if (tmpInput != null) {
            final Path input = tmpInput.toPath();
            final Task importTask = TaskManager.INSTANCE.submit("Import de modèle", () -> {
                // Check saved document is a real odt, + analyze contained properties.
                try (final InputStream stream = Files.newInputStream(input, StandardOpenOption.READ)) {
                    updateProperties(stream);
                }

                Files.copy(input, tempODT, StandardCopyOption.REPLACE_EXISTING);
                return true;
            });
            taskProperty.set(importTask);
            importTask.setOnSucceeded((success) -> Platform.runLater(() -> {
                tempODTexists.set(true);
                new Growl(Growl.Type.INFO, "Import terminé").showAndFade();
            }));
            importTask.setOnCancelled((cancelled) -> Platform.runLater(() -> new Growl(Growl.Type.WARNING, "Import annulé").showAndFade()));
            importTask.setOnFailed((failed) -> Platform.runLater(() -> {
                final Throwable exception = importTask.getException();
                if (exception instanceof IOException) {
                    GeotkFX.newExceptionDialog("Une erreur est survenue lors de l'accès aux données", exception).show();
                } else {
                    GeotkFX.newExceptionDialog("Le fichier fourni n'est pas reconnu en tant que modèle ODT.", exception).show();
                }
            }));
        }
    }

    @FXML
    void removeProperties(ActionEvent event) {
        final ObservableList<String> selectedItems = uiUsedProperties.getSelectionModel().getSelectedItems();
        availableProperties.addAll(selectedItems);
        usedProperties.removeAll(selectedItems);
    }

    @Override
    public void preSave() throws Exception {
        ModeleElement result = elementProperty.get();
        final Class targetClass = uiTargetClass.valueProperty().get();
        if (targetClass != null) {
            // Check if model needs to be updated.
            result.setTargetClass(targetClass.getCanonicalName());
        } else {
            result.setTargetClass(null);
        }

        // User have cleared model
        if (usedProperties.isEmpty()) {
            Files.deleteIfExists(tempODT);
        } else {
            boolean mustUpdate = false;
            if (!Files.isRegularFile(tempODT)) {
                mustUpdate = true;
            } else {
                // Analyze document content to know if it needs to be updated.
                try (final InputStream stream = Files.newInputStream(tempODT, StandardOpenOption.READ)) {
                    final TextDocument doc = TextDocument.loadDocument(stream);
                    final Set<String> keySet = ODTUtils.findAllVariables(doc, VariableField.VariableType.USER).keySet();
                    keySet.removeAll(FIELDS_TO_IGNORE);
                    if (keySet.size() != usedProperties.size()) {
                        mustUpdate = true;
                    } else {
                        for (final String name : usedProperties) {
                            if (!keySet.contains(name)) {
                                mustUpdate = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (mustUpdate) {
                updateODT(null);
            }
        }

        if (Files.isRegularFile(tempODT)) {
            result.setOdt(Files.readAllBytes(tempODT));
        } else {
            result.setOdt(null);
        }
    }

    /**
     * Read document from given stream, and analyze it to find class / properties
     * information. If we found it, UI is updated.
     * @param inputDocument Stream pointing to the document to analyze.
     * @throws Exception If we cannot read given document.
     */
    private void updateProperties(final InputStream inputDocument) throws Exception {
        final TextDocument tmpDoc = TextDocument.loadDocument(inputDocument);
        final Map<String, VariableField> vars = ODTUtils.findAllVariables(tmpDoc, VariableField.VariableType.USER);

        // First, we try to retrieve target class from document.
        try {
            final Class foundClass = ODTUtils.getTargetClass(tmpDoc);
            if (foundClass != null) {
                if (Platform.isFxApplicationThread()) {
                    uiTargetClass.setValue(foundClass);
                } else {
                    TaskManager.MockTask<Object> mockTask = new TaskManager.MockTask<>(() -> uiTargetClass.setValue(foundClass));
                    Platform.runLater(mockTask);
                    // Ensure available property list is updated before going further.
                    mockTask.get();
                }
            }
        } catch (ReflectiveOperationException e) {
            SirsCore.LOGGER.log(Level.FINE, "Document variable 'class' does not contains a valid class information.", e);
        }

        // Now, we build list of used properties
        final Set<String> keys = vars.keySet();
        keys.removeAll(FIELDS_TO_IGNORE);
        final Runnable r = () -> {
            availableProperties.removeAll(keys);
            usedProperties.setAll(keys);
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    /**
     * Create a function giving display name for properties of a given class.
     *
     * @param propertyHolder Class holding properties to translate.
     * @return A function which take a property name, and return a display name
     * for it.
     */
    public static Function<String, String> createPropertyNameMapper(final Class propertyHolder) {
        final LabelMapper mapper = LabelMapper.get(propertyHolder);
        if (mapper == null) {
            /*
             * If we cannot find any mapper, we try to build a decent name by putting space on word end / beginning.
             */
            return (input) -> input.replaceAll("([A-Z0-9][^A-Z0-9])", " $1").replaceAll("([^A-Z0-9\\s])([A-Z0-9])", "$1 $2");
        } else {
            return (input) -> mapper.mapPropertyName(input);
        }
    }
}
