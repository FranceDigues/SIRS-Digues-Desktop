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
package fr.sirs.map;

import fr.sirs.SIRS;

import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.coverage.amended.AmendedCoverageStore;
import org.geotoolkit.data.FeatureStoreFactory;
import org.geotoolkit.gui.javafx.chooser.FXLayerChooser;
import org.geotoolkit.gui.javafx.chooser.FXStoreChooser;
import org.geotoolkit.gui.javafx.parameter.FXParameterGroupPane;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.geotoolkit.storage.coverage.CoverageStoreFactory;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Panel in charge of external data loading on map.
 *
 * @author Alexis Manin (Geomatys)
 */
public class FXDataImportPane extends BorderPane {

    private static final String[] COVERAGE_STORES = new String[]{"coverage-file", "wms", "wmts", "osmtms","hexagon"};

    private static final String[] FEATURE_STORES = new String[]{"shapefile", "csv", "dwg", "dxf"};

    @FXML
    protected RadioButton uiImageToggle;

    @FXML
    protected RadioButton uiVectorToggle;

    @FXML
    protected ListView uiFactoryList;

    @FXML
    protected Label uiDescriptionLabel;

    @FXML
    protected Button uiConnectionBtn;

    @FXML
    protected StackPane uiCenterPane;

    @FXML
    protected ProgressBar uiProgressBar;

    protected final FXParameterGroupPane parameterEditor = new FXParameterGroupPane();

    protected final ObservableList<FeatureStoreFactory> featureStores;

    protected final ObservableList<CoverageStoreFactory> coverageStores;

    public final ObservableList<MapLayer> mapLayers = FXCollections.observableArrayList();

    private final FXLayerChooser layerChooser = new FXLayerChooser();

    private final Stage dialog = new Stage();

    public FXDataImportPane() {
        super();
        SIRS.loadFXML(this);
        dialog.getIcons().add(SIRS.ICON);

        uiFactoryList.setCellFactory(value -> new FXStoreChooser.FactoryCell());

        coverageStores = FXCollections.observableArrayList();
        for (final String factoryName : COVERAGE_STORES) {
            try {
                final DataStoreFactory factory = DataStores.getFactoryById(factoryName);
                if(factory instanceof CoverageStoreFactory)
                    coverageStores.add((CoverageStoreFactory)factory);
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.FINE, "No factory available for name : "+factoryName, e);
            }
        }

        featureStores = FXCollections.observableArrayList();
        for (final String factoryName : FEATURE_STORES) {
            try {
                final DataStoreFactory factory = DataStores.getFactoryById(factoryName);
                if (factory instanceof FeatureStoreFactory)
                    featureStores.add((FeatureStoreFactory) factory);
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.FINE, "No factory available for name : "+factoryName, e);
            }
        }

        uiImageToggle.getToggleGroup().selectedToggleProperty().addListener(this::updateFactoryList);
        uiFactoryList.getSelectionModel().selectedItemProperty().addListener(this::updateFactoryForm);

        parameterEditor.managedProperty().bind(parameterEditor.visibleProperty());
        parameterEditor.visibleProperty().bind(uiFactoryList.getSelectionModel().selectedItemProperty().isNotNull());

        uiCenterPane.getChildren().add(parameterEditor);
        uiProgressBar.visibleProperty().bind(disableProperty());

        /*
         * INIT DIALOG FOR LAYER CHOICE.
         */
        dialog.setTitle("Choix des données à importer");
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(null);

        final Button finishBtn = new Button("Valider");
        final Button cancelBtn = new Button("Annuler");
        cancelBtn.setCancelButton(true);

        final ButtonBar babar = new ButtonBar();
        babar.setPadding(new Insets(5, 5, 5, 5));
        babar.getButtons().addAll(cancelBtn, finishBtn);

        final BorderPane dialogContent = new BorderPane();
        dialogContent.setCenter(layerChooser);
        dialogContent.setBottom(babar);

        cancelBtn.setOnAction((ActionEvent e) -> dialog.hide());

        finishBtn.setOnAction((ActionEvent e) ->  {
            try {
                mapLayers.addAll(layerChooser.getLayers());
            } catch (DataStoreException ex) {
                SIRS.LOGGER.log(Level.WARNING, null, ex);
                GeotkFX.newExceptionDialog("Impossible de lire les couches demandées.", ex).showAndWait();
            }
            dialog.hide();
        });

        finishBtn.disableProperty().bind(layerChooser.layerNames.getSelectionModel().selectedItemProperty().isNull());
        dialog.setScene(new Scene(dialogContent));
    }

    public SimpleObjectProperty<ParameterValueGroup> configurationProperty() {
        return parameterEditor.inputGroup;
    }

    @FXML
    protected void connect(ActionEvent event) {
        final Object selectedItem = uiFactoryList.getSelectionModel().getSelectedItem();
        final ParameterValueGroup parameters = parameterEditor.inputGroup.get();

        setDisable(true);

        // Try to connnect on data source with given parameter.
        final Task submit = TaskManager.INSTANCE.submit("Connexion à une source de données", () -> {
            final DataStore store;
            if (selectedItem instanceof CoverageStoreFactory) {
                final CoverageStoreFactory f = (CoverageStoreFactory) selectedItem;
                store = new AmendedCoverageStore((CoverageStore)f.open(parameters));
            } else if (selectedItem instanceof FeatureStoreFactory) {
                store = ((FeatureStoreFactory) selectedItem).open(parameters);
            } else {
                throw new UnsupportedOperationException("Type de donnée inconnu");
            }
            // Set list values, and wait FX thread to return them to us.
            layerChooser.setSource(store);
            final Task getItems = new Task() {
                @Override
                protected Object call() throws Exception {
                    return layerChooser.layerNames.getItems();
                }
            };
            Platform.runLater(getItems);
            return getItems.get();
        });

        submit.setOnFailed(e -> {
            Platform.runLater(() -> {
                setDisable(false);
                if (submit.getException() != null) {
                    GeotkFX.newExceptionDialog("Impossible de se connecter à la source de donnée spécifiée.", submit.getException()).show();
                }
            });
        });

        submit.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                setDisable(false);
                // Choose layer to import, or import directly if there's only one in given file.
                if (layerChooser.layerNames.getItems().size() == 1) {
                    layerChooser.layerNames.getSelectionModel().selectFirst();
                    try {
                        mapLayers.addAll(layerChooser.getLayers());
                    } catch (DataStoreException ex) {
                        SIRS.LOGGER.log(Level.WARNING, null, ex);
                        GeotkFX.newExceptionDialog("Impossible de lire les couches demandées.", ex).showAndWait();
                    }
                } else if (layerChooser.layerNames.getItems().size() > 1) {
                    dialog.showAndWait();
                } else {
                    final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune donnée trouvée pour les paramètres définis.", ButtonType.OK);
                    alert.setResizable(true);
                    alert.showAndWait();
                }
            });
        });
    }

    protected void updateFactoryList(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
        if (newValue == uiImageToggle) {
            uiFactoryList.setItems(coverageStores);
        } else if (newValue == uiVectorToggle) {
            uiFactoryList.setItems(featureStores);
        } else {
            uiFactoryList.setItems(null);
        }
    }

    protected void updateFactoryForm(ObservableValue observable, Object oldValue, Object newValue) {
        forceRecreateParameters(newValue);
    }

    /**
     * Hack to force reset the parameter object.
     * Avoid having same parameter object in all layers added on a same session leading to layer incoherence everytime the parameter's url changes,
     * and thus to corrupted context carto with wrong value in field <ns4:Parameter key="url" type="java.net.URL"/>
     *
     * @param selectedItem can be null. In this case, the value will be taken from the uiFactoryList selectedItem.
     */
    protected void forceRecreateParameters(Object selectedItem) {
        if (selectedItem == null) {
            selectedItem = uiFactoryList.getSelectionModel().getSelectedItem();
        }
        if (selectedItem instanceof DataStoreFactory) {
            ParameterValueGroup factoryParameters = ((DataStoreFactory) selectedItem).getParametersDescriptor().createValue();
            parameterEditor.inputGroup.set(factoryParameters);
        }
    }
}
