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
import static fr.sirs.SIRS.CRS_WGS84;
import fr.sirs.util.SirsStringConverter;
import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.gui.javafx.layer.FXFeatureTable;
import org.opengis.feature.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class FXAbstractImportCoordinate extends BorderPane {

    protected static String SEPARATOR_KEY = "separator";
    protected static String PATH_KEY = "path";
    protected static String CRS_KEY = "crs";

    @FXML protected TextField uiPath;
    @FXML protected TextField uiSeparator;

    @FXML protected ComboBox<CoordinateReferenceSystem> uiCRS;
    @FXML protected FXFeatureTable uiTable;

    @FXML protected GridPane uiPaneConfig;
    @FXML protected GridPane uiPaneImport;

    protected FeatureStore store;

    final SirsStringConverter stringConverter = new SirsStringConverter();

    public FXAbstractImportCoordinate() {
        SIRS.loadFXML(this);

        final ObservableList<CoordinateReferenceSystem> crsList = FXCollections.observableArrayList(Injector.getSession().getProjection(), CRS_WGS84);
        uiCRS.setItems(crsList);
        stringConverter.registerList(crsList);
        uiCRS.setConverter(stringConverter);
        uiCRS.getSelectionModel().clearAndSelect(0);

        uiPaneConfig.setDisable(true);
        uiTable.setEditable(false);
        uiTable.setLoadAll(true);
        initFieldValue();
    }

    private void initFieldValue() {
        // Path
        final File prevPath = getPreviousPath();
        if (prevPath != null) {
            uiPath.setText(prevPath.getAbsolutePath());
        }
        // Separator
        final String separator = previousFieldValue(SEPARATOR_KEY);
        if (separator != null && separator != "") uiSeparator.setText(separator);
        // CRS
        final String crsString = previousFieldValue(CRS_KEY);
        if (crsString != null && crsString != "") {
            final Object o = stringConverter.fromString(crsString);
            if (o instanceof CoordinateReferenceSystem) {
                uiCRS.getSelectionModel().select((CoordinateReferenceSystem) o);
            } else {
                SIRS.LOGGER.log(Level.WARNING, "Cannot convert " + crsString + " to CoordinateReferenceSystem object.");
            }
        }
    }

    protected void saveFieldValue() {
        savePreference(SEPARATOR_KEY, uiSeparator.getText());
        savePreference(CRS_KEY, stringConverter.toString(uiCRS.getSelectionModel().getSelectedItem()));
    }

    protected String previousFieldValue(final String key) {
        final Preferences prefs = Preferences.userNodeForPackage(FXAbstractImportCoordinate.class);
        return prefs.get(key, null);
    }

    protected void savePreference(final String key, final String value) {
        final Preferences prefs = Preferences.userNodeForPackage(FXAbstractImportCoordinate.class);
        prefs.put(key, value);
    }

    @FXML
    protected void openFileChooser(ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();
        final File prevPath = getPreviousPath();
        if (prevPath != null) {
            fileChooser.setInitialDirectory(prevPath.getParentFile());
        }
        final File file = fileChooser.showOpenDialog(getScene().getWindow());
        if(file!=null){
            setPreviousPath(file);
            uiPath.setText(file.getAbsolutePath());
        }
    }

    private static File getPreviousPath() {
        final Preferences prefs = Preferences.userNodeForPackage(FXAbstractImportCoordinate.class);
        final String str = prefs.get(PATH_KEY, null);
        if(str!=null){
            final File file = new File(str);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    private static void setPreviousPath(final File path) {
        final Preferences prefs = Preferences.userNodeForPackage(FXAbstractImportCoordinate.class);
        prefs.put(PATH_KEY, path.getAbsolutePath());
    }

    protected ObservableList<PropertyType> getPropertiesFromFeatures(final FeatureCollection col) {
        return FXCollections
                .observableArrayList((Collection<PropertyType>) col.getFeatureType().getProperties(true))
                .sorted((o1, o2) -> o1.getName().compareTo(o2.getName()));
    }

    protected void fillFieldFromComboBox(final String key, final ComboBox<PropertyType> combo) {
        final String str = previousFieldValue(key);
        if (str != null) {
            final Object o = stringConverter.fromString(str);
            if (o instanceof PropertyType) {
                combo.getSelectionModel().select((PropertyType) o);
            } else {
                SIRS.LOGGER.log(Level.WARNING, "Cannot convert " + str + " to PropertyType object.");
            }
        }
    }
}
