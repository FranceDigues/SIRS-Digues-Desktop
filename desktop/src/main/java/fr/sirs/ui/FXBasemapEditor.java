/**
 *
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

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.map.FXMapTab;
import fr.sirs.util.SaveableConfiguration;
import fr.sirs.util.property.SirsPreferences;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.geotoolkit.map.CoverageMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;

/**
 * An editor used to configure the default basemap. Located in user preferences.
 *
 * @author maximegavens
 */
public class FXBasemapEditor extends BorderPane implements SaveableConfiguration {

    public final static String WMS_WMTS_CHOICE = "wms/wmts";
    public final static String OSM_TILE_CHOICE = "OSMTileMap";
    public final static String FILE_CHOICE = "ficher";
    public final static String DEFAULT_CHOICE = "defaut";
    public final static String WMS111 = "WMS - 1.1.1";
    public final static String WMS130 = "WMS - 1.3.0";
    public final static String WMTS100 = "WMTS - 1.0.0";
    public final static String COVERAGE_FILE_TYPE = "File coverage";
    public final static String HEXAGON_TYPE = "ECW/JPEG-2000";

    @FXML private RadioButton uiRadioButtonWM;
    @FXML private RadioButton uiRadioButtonOsmTile;
    @FXML private RadioButton uiRadioButtonLocalFile;
    @FXML private RadioButton uiRadioButtonDefault;

    @FXML private ChoiceBox uiChoiceService;
    @FXML private ChoiceBox uiChoiceFileType;

    @FXML private TextField uiBasemapUrlWM;
    @FXML private TextField uiBasemapUrlOsmTile;
    @FXML private TextField uiBasemapFile;

    public FXBasemapEditor() {
        SIRS.loadFXML(this);
        initField();
    }

    private void initField() {
        final String previousWMUrl = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_WM_URL);
        final String previousOsmTileUrl = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_OSM_TILE_URL);
        final String previousLocalFile = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_LOCAL_FILE);
        final String previousFileType = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_FILE_TYPE);
        final String previousChoice = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_CHOICE);
        final String previousWMType = SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.BASEMAP_WM_TYPE);

        uiBasemapUrlWM.setText(previousWMUrl);
        uiBasemapUrlOsmTile.setText(previousOsmTileUrl);
        uiBasemapFile.setText(previousLocalFile);

        ToggleGroup group = new ToggleGroup();
        uiRadioButtonWM.setToggleGroup(group);
        uiRadioButtonOsmTile.setToggleGroup(group);
        uiRadioButtonLocalFile.setToggleGroup(group);
        uiRadioButtonDefault.setToggleGroup(group);

        if (previousChoice == null) {
            // default choice
            uiRadioButtonDefault.selectedProperty().setValue(true);
        } else {
            switch (previousChoice) {
                case WMS_WMTS_CHOICE:
                    uiRadioButtonWM.selectedProperty().setValue(true);
                    break;
                case OSM_TILE_CHOICE:
                    uiRadioButtonOsmTile.selectedProperty().setValue(true);
                    break;
                case FILE_CHOICE:
                    uiRadioButtonLocalFile.selectedProperty().setValue(true);
                    break;
                case DEFAULT_CHOICE:
                    uiRadioButtonDefault.selectedProperty().setValue(true);
                    break;
                default:
                    // default choice
                    uiRadioButtonDefault.selectedProperty().setValue(true);
                    break;
            }
        }

        uiChoiceService.setItems(FXCollections.observableList(Arrays.asList(new String[]{WMS111, WMS130, WMTS100})));
        uiChoiceService.getSelectionModel().select(previousWMType);
        uiChoiceFileType.setItems(FXCollections.observableList(Arrays.asList(new String[]{COVERAGE_FILE_TYPE, HEXAGON_TYPE})));
        uiChoiceFileType.getSelectionModel().select(previousFileType);
    }

    @FXML
    protected void openFileChooser(ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();
        String str = uiBasemapFile.getText();
        // retrieve current folder
        File prevFolder = null;
        if (str != null) {
            File file2 = new File(str);
            if (file2.isDirectory()) {
                prevFolder = file2;
            } else if (file2.isFile()) {
                prevFolder = file2.getParentFile();
            }
        }
        if (prevFolder != null) {
            fileChooser.setInitialDirectory(prevFolder);
        }
        final File choosenFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (choosenFile != null) {
            uiBasemapFile.setText(choosenFile.getAbsolutePath());
        }
    }

    @Override
    public void save() throws Exception {
        final Map<SirsPreferences.PROPERTIES, String> properties = new HashMap<>();
        properties.put(SirsPreferences.PROPERTIES.BASEMAP_WM_URL, uiBasemapUrlWM.getText());
        properties.put(SirsPreferences.PROPERTIES.BASEMAP_OSM_TILE_URL, uiBasemapUrlOsmTile.getText());
        properties.put(SirsPreferences.PROPERTIES.BASEMAP_LOCAL_FILE, uiBasemapFile.getText());
        properties.put(SirsPreferences.PROPERTIES.BASEMAP_WM_TYPE, (String) uiChoiceService.getSelectionModel().getSelectedItem());
        properties.put(SirsPreferences.PROPERTIES.BASEMAP_FILE_TYPE, (String) uiChoiceFileType.getSelectionModel().getSelectedItem());
        if (uiRadioButtonWM.isSelected()) {
            properties.put(SirsPreferences.PROPERTIES.BASEMAP_CHOICE, WMS_WMTS_CHOICE);
        } else if (uiRadioButtonOsmTile.isSelected()) {
            properties.put(SirsPreferences.PROPERTIES.BASEMAP_CHOICE, OSM_TILE_CHOICE);
        } else if (uiRadioButtonLocalFile.isSelected()) {
            properties.put(SirsPreferences.PROPERTIES.BASEMAP_CHOICE, FILE_CHOICE);
        } else if (uiRadioButtonDefault.isSelected()) {
            properties.put(SirsPreferences.PROPERTIES.BASEMAP_CHOICE, DEFAULT_CHOICE);
        }
        SirsPreferences.INSTANCE.store(properties);
        reloadBasemap();
    }

    private void reloadBasemap() {
        final Session session = Injector.getSession();
        if (session != null) {
            session.getMapContext();
            final FXMapTab mapTab = session.getFrame().getMapTab();
            final Collection<MapItem> root = mapTab.getMap().getUiMap().getContainer().getContext().items();
            final CoverageMapLayer basemapLayer = Session.getBasemapLayer();

            if (basemapLayer != null) {
                // Looking for basemap group
                MapItem parent = null;
                for (MapItem mi : root) {
                    if ("Fond de plan".equals(mi.getName())) {
                        parent = mi;
                        break;
                    }
                }
                if (parent == null) {
                    parent = MapBuilder.createItem();
                    parent.setName("Fond de plan");
                    root.add(parent);
                }
                parent.items().clear();
                parent.items().add(basemapLayer);
            }
        }
    }

    @Override
    public String getTitle() {
        return "Fond de carte par défaut";
    }
}
