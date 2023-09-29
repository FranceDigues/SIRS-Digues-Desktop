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
package fr.sirs.ui;

import fr.sirs.SIRS;
import fr.sirs.util.SaveableConfiguration;
import fr.sirs.util.property.SirsPreferences;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.beans.DefaultProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.apache.sis.util.ArgumentChecks;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class FXSirsPreferenceEditor extends ScrollPane implements SaveableConfiguration {

    /**
     * Put needed custom overrides in this map. Allow to edit an entry of {@link SirsPreferences}
     * with a personalized component.
     */
    private static final HashMap<SirsPreferences.PROPERTIES, Node> EDITOR_OVERRIDES = new HashMap<>();
    static {
        EDITOR_OVERRIDES.put(SirsPreferences.PROPERTIES.DESIGNATION_AUTO_INCREMENT, new IncrementCheckBox());

        EDITOR_OVERRIDES.put(SirsPreferences.PROPERTIES.ABSTRACT_SHOWCASE, new ShowCaseComboBox());

        EDITOR_OVERRIDES.put(SirsPreferences.PROPERTIES.HIDE_ARCHIVED_PARENT, new ArchivedParentCheckBox());
    }

    final ObservableMap<SirsPreferences.PROPERTIES, String> editedProperties = FXCollections.observableHashMap();

    public FXSirsPreferenceEditor() {
        final GridPane propertyPane = new GridPane();
        propertyPane.setAlignment(Pos.CENTER);
        propertyPane.setMaxSize(Double.MAX_VALUE, USE_COMPUTED_SIZE);

        // label column, we always want it to fit content size.
        propertyPane.getColumnConstraints().add(new ColumnConstraints(30, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.SOMETIMES, HPos.LEFT, true));
        // Propery editor. We make it fit available space.
        propertyPane.getColumnConstraints().add(new ColumnConstraints(0, USE_COMPUTED_SIZE, Double.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true));
        // Undo button, unresizable.
        propertyPane.getColumnConstraints().add(new ColumnConstraints(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE, Priority.NEVER, HPos.RIGHT, true));
        propertyPane.setVgap(10);
        propertyPane.setHgap(10);
        propertyPane.setPadding(new Insets(10));

        // We don't want basemap properties appear in general property configuration.
        final List<SirsPreferences.PROPERTIES> withoutBasemapProp = Arrays.stream(SirsPreferences.PROPERTIES.values())
                .filter(p ->
                        !SirsPreferences.PROPERTIES.BASEMAP_CHOICE.equals(p) &&
                        !SirsPreferences.PROPERTIES.BASEMAP_FILE_TYPE.equals(p) &&
                        !SirsPreferences.PROPERTIES.BASEMAP_LOCAL_FILE.equals(p) &&
                        !SirsPreferences.PROPERTIES.BASEMAP_OSM_TILE_URL.equals(p) &&
                        !SirsPreferences.PROPERTIES.BASEMAP_WM_TYPE.equals(p) &&
                        !SirsPreferences.PROPERTIES.BASEMAP_WM_URL.equals(p)
                )
                .collect(Collectors.toList());

        int row = 0;
        for (final SirsPreferences.PROPERTIES p : withoutBasemapProp) {
            final Label propLibelle = new Label(p.title);
            propLibelle.setTooltip(new Tooltip(p.description));
            propertyPane.add(propLibelle, 0, row);

            try {
                final Node propEditor = getValidEditor(p);
                final Property targetProperty = getNodeProperty(propEditor);

                final String propertySafe = SirsPreferences.INSTANCE.getPropertySafe(p.name());
                if (propertySafe != null) {
                    targetProperty.setValue(propertySafe);
                } else {
                    targetProperty.setValue(p.getDefaultValue());
                }

                targetProperty.addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
                    if (!newValue.equals(SirsPreferences.INSTANCE.getPropertySafe(p.name()))) {
                        editedProperties.put(p, newValue.toString());
                    }
                });

                propertyPane.add(propEditor, 1, row);

                final Button resetValueBtn = new Button();
                resetValueBtn.setGraphic(new ImageView(SIRS.ICON_UNDO_BLACK));
                resetValueBtn.setPrefSize(16, 16);
                resetValueBtn.setOnAction((ActionEvent e) -> {
                    final String propertySafe1 = SirsPreferences.INSTANCE.getPropertySafe(p.name());
                    if (propertySafe1 != null) {
                        targetProperty.setValue(propertySafe1);
                    } else if (p.getDefaultValue() != null) {
                        targetProperty.setValue(p.getDefaultValue());
                    } else {
                        targetProperty.setValue(null);
                    }
                });
                propertyPane.add(resetValueBtn, 2, row);
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.WARNING, "No editor can be displayed for property " + p.name(), e);
                propertyPane.add(new Label("Une erreur s'est produite pendant la création de l'éditeur."), 2, row, 2, 1);
            }
            row++;
        }

        setFitToWidth(true);
        setContent(propertyPane);
    }

    @Override
    public void save() throws IOException {
        SirsPreferences.INSTANCE.store(editedProperties);
        editedProperties.clear();
    }

    @Override
    public String getTitle() {
        return "Général";
    }

    protected static Node getValidEditor(SirsPreferences.PROPERTIES p) {
        Node editor = EDITOR_OVERRIDES.get(p);
        if (editor != null && editor.getClass().isAnnotationPresent(DefaultProperty.class)) {
            return editor;
        } else {
            return new TextField();
        }
    }

    protected static Property getNodeProperty(final Node inputNode) throws IllegalArgumentException {
        ArgumentChecks.ensureNonNull("Input node", inputNode);
        if (inputNode instanceof TextInputControl) {
            return ((TextInputControl) inputNode).textProperty();
        } else if (inputNode instanceof CheckBox) {
            return ((CheckBox) inputNode).selectedProperty();
        }

        // Fallback case. We'll try to get node default property.
        final Class<? extends Node> nodeClass = inputNode.getClass();
        DefaultProperty annotation = nodeClass.getAnnotation(DefaultProperty.class);
        if (annotation == null) {
            throw new IllegalArgumentException("No valid default property for input node.");
        }

        final String propertyName = annotation.value() + "Property";
        try {
            Field field = nodeClass.getField(propertyName);
            return (Property) field.get(inputNode);
        } catch (Exception e) {
            try {
                Method method = nodeClass.getMethod(propertyName);
                return (Property) method.invoke(inputNode);
            } catch (Exception eBis) {
                IllegalArgumentException toThrow = new IllegalArgumentException("No valid default property for input node.", eBis);
                toThrow.addSuppressed(e);
                throw toThrow;
            }
        }
    }
}