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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.MultiPoint;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import static fr.sirs.SIRS.CRS_WGS84;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;

import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.SystemeReperageRepository;
import org.geotoolkit.gui.javafx.util.TaskManager;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.SirsStringConverter;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.data.shapefile.ShapefileFeatureStore;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.GeometryDescriptor;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.layer.FXFeatureTable;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.LayerListener;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.referencing.LinearReferencing;
import org.geotoolkit.referencing.LinearReferencing.ProjectedPoint;
import org.geotoolkit.style.RandomStyleBuilder;
import org.geotoolkit.util.collection.CollectionChangeEvent;
import org.opengis.feature.AttributeType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Id;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.GenericName;

/**
 * Un panneau permettant d'ouvrir un fichier Shapefile (.shp) pour en extraire
 * des points puis les convertir en bornes.
 *
 * TODO : Etendre la gestion aux format CSV (Ré-activation du choix du CRS, etc.)
 *
 * TODO : Fermer Feature store / nettoyer les ressources quand on ferme la fenêtre.
 *
 * @author Alexis Manin (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class FXImportBornesPane extends BorderPane {

    @FXML private TextField uiPath;
    @FXML private TextField uiSeparator;
    @FXML private Label uiSeparatorLabel;
    @FXML private ComboBox<CoordinateReferenceSystem> uiCRS;
    @FXML private ComboBox<PropertyType> uiAttX;
    @FXML private ComboBox<PropertyType> uiAttY;
    @FXML private FXFeatureTable uiTable;
    @FXML private ComboBox uiLibelleBox;
    @FXML private ComboBox<SystemeReperage> uiSRBox;
    @FXML private Label uiPRLabel;
    @FXML private ComboBox<PropertyType> uiPRBox;
    @FXML private ComboBox<PropertyType> uiCodeBox;
    @FXML private ComboBox<Preview> uiTronconBox;
    @FXML private GridPane uiPaneConfig;
    @FXML private GridPane uiPaneImport;
    @FXML private Button uiImportButton;

    private FeatureMapLayer loadedData;
    private FeatureCollection selection;

    private final String typeName;
    private final Class<? extends TronconDigue> typeClass;
    
    private final BooleanBinding prVisibilityBinding;

    public FXImportBornesPane(final String typeName, final Class typeClass) {
        SIRS.loadFXML(this);

        this.typeName  = typeName;
        this.typeClass = typeClass;

        final SirsStringConverter stringConverter = new SirsStringConverter();
        uiCRS.setItems(FXCollections.observableArrayList(Injector.getSession().getProjection(), CRS_WGS84));
        uiCRS.setConverter(stringConverter);
        uiCRS.getSelectionModel().clearAndSelect(0);
        uiAttX.setConverter(stringConverter);
        uiAttY.setConverter(stringConverter);
        uiLibelleBox.setConverter(stringConverter);
        uiSRBox.setConverter(stringConverter);
        uiPRBox.setConverter(stringConverter);
        
        prVisibilityBinding = new BooleanBinding() {
            
            {bind(uiSRBox.valueProperty());}
            
            @Override
            protected boolean computeValue() {
                // Si le SR sélectionné est le SR élémentaire, on n'affichera pas le choix de la colonne pour le PR car ce dernier sera calculé.
                return uiSRBox.getValue()==null || !SirsCore.SR_ELEMENTAIRE.equals(uiSRBox.getValue().getLibelle());
            }
        };
        
        uiPRLabel.managedProperty().bind(prVisibilityBinding);
        uiPRBox.managedProperty().bind(prVisibilityBinding);
        uiPRLabel.visibleProperty().bind(prVisibilityBinding);
        uiPRBox.visibleProperty().bind(prVisibilityBinding);
        
        uiCodeBox.setConverter(stringConverter);

        // TODO : make visible if we activate back csv import.
        uiSeparator.setVisible(false);
        uiSeparator.managedProperty().bind(uiSeparator.visibleProperty());
        uiSeparatorLabel.setVisible(false);
        uiSeparatorLabel.managedProperty().bind(uiSeparatorLabel.visibleProperty());

        uiPaneConfig.setVisible(false);
        uiPaneConfig.managedProperty().bind(uiPaneConfig.visibleProperty());

        uiTable.setEditable(false);
        uiTable.setLoadAll(true);

        uiImportButton.setTooltip(new Tooltip("Importer la séléction"));
        uiImportButton.setDisable(true);

        uiTronconBox.setItems(SIRS.observableList(
                Injector.getSession().getPreviews().getByClass(typeClass)).sorted());
        uiTronconBox.setConverter(stringConverter);
        uiTronconBox.valueProperty().addListener(this::updateSrList);

        uiSRBox.disableProperty().bind(uiTronconBox.valueProperty().isNull());
        uiPRBox.disableProperty().bind(uiSRBox.valueProperty().isNull());
    }

    private void updateSrList(ObservableValue<? extends Preview> observable, Preview oldValue, Preview newValue){
        if(newValue==null){
            uiSRBox.setItems(FXCollections.emptyObservableList());
        } else {
            final fr.sirs.Session session = Injector.getSession();
            final TronconDigue troncon = session.getRepositoryForClass(typeClass).get(newValue.getElementId());
            final List<SystemeReperage> srs = ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).getByLinear(troncon);
            uiSRBox.setItems(FXCollections.observableArrayList(srs));
            uiSRBox.getItems().add(null);
            uiSRBox.getSelectionModel().selectFirst();

            final String defaultSRID = troncon.getSystemeRepDefautId();
            if (defaultSRID != null) {
                for (final SystemeReperage sr : srs) {
                    if (defaultSRID.equals(sr.getId())) {
                        uiSRBox.getSelectionModel().select(sr);
                        break;
                    }
                }
            }
        }
    }

    @FXML
    void openFileChooser(ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();
        final File prevPath = getPreviousPath();
        if (prevPath != null) {
            fileChooser.setInitialDirectory(prevPath);
        }
        final File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            setPreviousPath(file.getParentFile());
            uiPath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    void openFeatureStore(ActionEvent event) {
        final File file = new File(uiPath.getText());

        uiPaneConfig.setDisable(true);
        uiImportButton.setDisable(true);

        final Task<FeatureCollection> openTask = new Task<FeatureCollection>() {
            @Override
            protected FeatureCollection call() throws Exception {
                updateTitle("Lecture d'un fichier vectoriel.");
//            TODO : Uncomment if we want to activate back csv / txt import
//            if(url.toLowerCase().endsWith(".shp")){
                final FeatureStore store = new ShapefileFeatureStore(file.toURI(), "no namespace");
//            }else if(url.toLowerCase().endsWith(".txt") || url.toLowerCase().endsWith(".csv")){
//                final char separator = (uiSeparator.getText().isEmpty()) ? ';' : uiSeparator.getText().charAt(0);
//                store = new CSVFeatureStore(file, "no namespace", separator);
//                uiPaneConfig.setDisable(false);
//            }else{
//                new Alert(Alert.AlertType.ERROR, "Le fichier sélectionné n'est pas un shp, csv ou txt", ButtonType.OK).showAndWait();
//                return;
//            }
                final Session session = store.createSession(true);
                final Set<GenericName> names = store.getNames();
                if (names == null || names.isEmpty()) {
                    throw new IllegalArgumentException("Aucune donnée vectorielle trouvée dans le fichier.");
                }

                // On s'assure que le fichier en entrée contient des points.
                final GenericName n = names.iterator().next();
                GeometryDescriptor geometryDescriptor = store.getFeatureType(n).getGeometryDescriptor();
                if (geometryDescriptor == null || !Geometry.class.isAssignableFrom(geometryDescriptor.getType().getBinding())) {
                    throw new IllegalArgumentException("Aucune donnée vectorielle trouvée dans le fichier.");
                }

                return session.getFeatureCollection(QueryBuilder.all(names.iterator().next()));
            }
        };

        setDisable(true);
        openTask.setOnCancelled((cancelledEvent) -> setDisable(false));

        openTask.setOnFailed((failureEvent) -> Platform.runLater(() -> {
            setDisable(false);
            GeotkFX.newExceptionDialog("Impossible d'ouvrir le fichier séléctionné.", openTask.getException()).show();
        }));

        openTask.setOnSucceeded((successEvent) -> Platform.runLater(() -> {
            setDisable(false);
            final FeatureCollection col = openTask.getValue();
            FeatureType fType = col.getFeatureType();
            loadedData = MapBuilder.createFeatureLayer(col, RandomStyleBuilder.createDefaultVectorStyle(fType));
            uiTable.init(loadedData);

            final Comparator<PropertyType> nameComparator = (o1, o2) -> o1.getName().compareTo(o2.getName());

            //liste des propriétés
            final ObservableList<PropertyType> properties = FXCollections.observableArrayList(fType.getProperties(true)).sorted((Comparator)nameComparator);
            uiAttX.setItems(properties);
            uiAttY.setItems(properties);

            final ObservableList stringProperties = properties.filtered((PropertyType p) -> {
                return (p instanceof AttributeType)
                        && CharSequence.class.isAssignableFrom(
                                ((AttributeType) p).getValueClass());
            }).sorted(nameComparator);
            final ObservableList numberProperties = properties.filtered((PropertyType p) -> {
                return (p instanceof AttributeType)
                        && Number.class.isAssignableFrom(
                                ((AttributeType) p).getValueClass());
            }).sorted(nameComparator);
            uiLibelleBox.setItems(stringProperties);
            uiCodeBox.setItems(stringProperties);
            uiPRBox.setItems(numberProperties);

            if (!properties.isEmpty()) {
                uiAttX.getSelectionModel().clearAndSelect(0);
                uiAttY.getSelectionModel().clearAndSelect(0);
            }

            //on ecoute la selection
            loadedData.addLayerListener(new LayerListener() {
                @Override
                public void styleChange(MapLayer source, EventObject event) {
                }

                @Override
                public void itemChange(CollectionChangeEvent<MapItem> event) {
                }

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (!FeatureMapLayer.SELECTION_FILTER_PROPERTY.equals(evt.getPropertyName())) {
                        return;
                    }

                    final Id filter = loadedData.getSelectionFilter();
                    try {
                        selection = loadedData.getCollection().subCollection(QueryBuilder.filtered(fType.getName(), filter));
                    if (selection == null || selection.isEmpty() || uiTronconBox.getValue() == null) {
                        uiImportButton.setDisable(true);
                    } else {
                            uiImportButton.setDisable(false);
                        }
                    } catch (DataStoreException ex) {
                        GeotkFX.newExceptionDialog("Une erreur est survenue lors de la mise à jour de la sélection.", ex).show();
                    }
                }
            });
        }));

        TaskManager.INSTANCE.submit(openTask);
    }

    @FXML
    void cancelImport(ActionEvent event) {
        this.getScene().getWindow().hide();
    }

    @FXML
    void importBornes(ActionEvent event) {
        if (selection == null || selection.isEmpty()) {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune borne à importer.", ButtonType.OK);
            alert.setResizable(true);
            alert.showAndWait();
            return;
        }

        final Object selectedTd = uiTronconBox.getSelectionModel().getSelectedItem();
        if (selectedTd == null) {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Veuillez sélectionner un " + typeName + ".", ButtonType.OK);
            alert.setResizable(true);
            alert.showAndWait();
            return;
        }

        Object selectedCode = uiCodeBox.getSelectionModel().getSelectedItem();
        final String codeProperty;
        if (selectedCode instanceof PropertyType) {
            codeProperty = ((PropertyType) selectedCode).getName().head().toString();
        } else {
            codeProperty = null;
        }

        Object selectedLibelle = uiLibelleBox.getSelectionModel().getSelectedItem();
        final String libelleProperty;
        if (selectedLibelle instanceof PropertyType) {
            libelleProperty = ((PropertyType) selectedLibelle).getName().head().toString();
        } else {
            libelleProperty = null;
        }

        Object selectedPR = uiPRBox.getSelectionModel().getSelectedItem();
        final String prProperty;
        if (selectedPR instanceof PropertyType) {
            prProperty = ((PropertyType) selectedPR).getName().head().toString();
        } else {
            prProperty = null;
        }

        final SystemeReperage sr = uiSRBox.getValue();
        if (sr != null){
            if(prProperty == null && !SirsCore.SR_ELEMENTAIRE.equals(sr.getLibelle())){
                final Alert alert = new Alert(
                        Alert.AlertType.WARNING,
                        "Vous allez affecter les bornes importées à un système de repérage sans leur associer de PR. Êtes-vous sûr ?",
                        ButtonType.NO, ButtonType.YES);
                alert.setResizable(true);

                // Prevent import if no PR property is chosen, and user did not click on yes button (cancelled or quit alert).
                // L'alerte est lancée uniquement si le SR sélectionné n'est pas le SR élémentaire.
                if (!ButtonType.YES.equals(alert.showAndWait().orElse(null))) {
                    return;
                }
            }
        }

        final Task importTask = new Task() {
            @Override
            protected Object call() throws Exception {
                updateTitle("Import de bornes");
                final fr.sirs.Session session = Injector.getSession();

                final TronconDigue troncon;
                final AbstractSIRSRepository tdRepo = Injector.getSession().getRepositoryForClass(typeClass);
                if (selectedTd instanceof TronconDigue) {
                    troncon = (TronconDigue) selectedTd;
                } else if (selectedTd instanceof Preview) {
                    troncon = (TronconDigue) tdRepo.get(((Preview) selectedTd).getDocId());
                } else {
                    throw new IllegalStateException("Unknown object type for parameter " + typeName);
                }
                
                // Le cas échéant, on calcule les paramètres nécessaires à la détermination des PRs dans le SR Elémentaire.
                LinearReferencing.SegmentInfo[] buildSegments = null;
                float prStart = 0.f;
                if(sr!=null && SirsCore.SR_ELEMENTAIRE.equals(sr.getLibelle())){
                    buildSegments = LinearReferencing.buildSegments(LinearReferencingUtilities.asLineString(troncon.getGeometry()));
                    prStart  = TronconUtils.getPRStart(troncon, sr, session);
                }
                
                final MathTransform trs = CRS.findOperation(
                        selection.getFeatureType().getCoordinateReferenceSystem(), // TODO : replace CRS with the one in uiCRS for CSV files.
Injector.getSession().getProjection(),
                        null).getMathTransform();
                final boolean isIdentity = trs.isIdentity();

                boolean mustUpdateTroncon = false;
                try (final FeatureIterator it = selection.iterator()) {
                    final AbstractSIRSRepository<BorneDigue> borneRepo = Injector.getSession().getRepositoryForClass(BorneDigue.class);
                    while (it.hasNext()) {
                        Feature current = it.next();
                        // We can cast here because we checked property type at loading.
                        Geometry value = (Geometry) current.getDefaultGeometryProperty().getValue();
//                // TODO : use following code for CSV files.
//            final String attX = String.valueOf(feature.getPropertyValue(uiAttX.getValue().getName().tip().toString()));
//            final String attY = String.valueOf(feature.getPropertyValue(uiAttY.getValue().getName().tip().toString()));
//            geom = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(Double.valueOf(attX), Double.valueOf(attY)));
                        // REDMINE 8012 - Fix import when shapefile uses Multipoints instead of Points
                        // REDMINE 8012 - Fix import when shapefile uses Multipoints instead of Points
                        // The import shapefile must contain a code and a libelle for each borne to import. We thus don't allow multipoints containing several bornes / points.
                        if (value instanceof MultiPoint) {
                            final MultiPoint value1 = (MultiPoint) value;
                            if (value1.getNumGeometries() == 1) {
                                value = value1.getGeometryN(0);
                            }
                        }
                        if (value instanceof Point) {
                            final Point borneGeom;
                            if (isIdentity) {
                                borneGeom = (Point) value;
                            } else {
                                borneGeom = (Point) JTS.transform((Point) value, trs);
                                JTS.setCRS(borneGeom, Injector.getSession().getProjection());
                            }
                            BorneDigue newBorn = borneRepo.create();
                            newBorn.setGeometry(borneGeom);
                            if (codeProperty != null) {
                                newBorn.setDesignation(current.getPropertyValue(codeProperty).toString());
                            }
                            if (libelleProperty != null) {
                                newBorn.setLibelle(current.getPropertyValue(libelleProperty).toString());
                            }

                            borneRepo.add(newBorn);
                            mustUpdateTroncon = (troncon.getBorneIds().add(newBorn.getId()) || mustUpdateTroncon);

                            if(sr!=null){
                                // Dans le cas du SR élémentaire, on calcule le PR de la borne automatiquement
                                if(SirsCore.SR_ELEMENTAIRE.equals(sr.getLibelle())){
                                    final SystemeReperageBorne srb = session.getElementCreator().createElement(SystemeReperageBorne.class);
                                    srb.borneIdProperty().set(newBorn.getDocumentId());
                                    
                                    final ProjectedPoint proj = LinearReferencing.projectReference(buildSegments, newBorn.getGeometry());

                                    // Pour obtenir le PR calculé dans le SR élémentaire, il faut ajouter le PR de la borne de départ à la distance du point projeté sur le linéaire.
                                    srb.setValeurPR((float) proj.distanceAlongLinear + prStart);
                                    
                                    sr.systemeReperageBornes.add(srb);
                                } 
                                else if(prProperty!=null){
                                    //reference dans le SR
                                    final SystemeReperageBorne srb = session.getElementCreator().createElement(SystemeReperageBorne.class);
                                    srb.borneIdProperty().set(newBorn.getDocumentId());
                                    srb.setValeurPR(((Number)current.getPropertyValue(prProperty)).floatValue());
                                    sr.systemeReperageBornes.add(srb);
                                }
                            }

                        } else {
                    // TODO : store unmanaged feature libelle to alert user at
                            // the end of import.
                        }
                    }
                } finally {
                    if (mustUpdateTroncon) {
                        tdRepo.update(troncon);
                    }
                }

                //sauvegarde du SR
                if(sr!=null){
                    session.getApplicationContext().getBean(SystemeReperageRepository.class).update(sr, troncon);
                }

                return mustUpdateTroncon;
            }
        };

        importTask.setOnSucceeded((successEvent) -> Platform.runLater(() -> {
            final Alert alert;
            if (Boolean.TRUE.equals(TaskManager.INSTANCE.submit(importTask).getValue())) {
                alert = new Alert(Alert.AlertType.INFORMATION, "L'import est terminé.", ButtonType.OK);
            } else {
                alert = new Alert(Alert.AlertType.WARNING, "Aucune borne n'a pu être importée.", ButtonType.OK);
            }
            alert.setResizable(true);
            alert.showAndWait();
        }));

        importTask.setOnFailed((successEvent) -> Platform.runLater(() -> {
            GeotkFX.newExceptionDialog("Une erreur s'est produite pendant l'import des bornes.", importTask.getException()).show();
        }));

        TaskManager.INSTANCE.submit(importTask);
        getScene().getWindow().hide();
    }

    private static File getPreviousPath() {
        final Preferences prefs = Preferences.userNodeForPackage(FXImportBornesPane.class);
        final String str = prefs.get("path", null);
        if (str != null) {
            final File file = new File(str);
            if (file.isDirectory()) {
                return file;
            }
        }
        return null;
    }

    private static void setPreviousPath(final File path) {
        final Preferences prefs = Preferences.userNodeForPackage(FXImportBornesPane.class);
        prefs.put("path", path.getAbsolutePath());
    }

    public static void showImportDialog(String typeName, Class typeClass) {
        final FXImportBornesPane panel = new FXImportBornesPane(typeName, typeClass);
        final Stage dialog = new Stage();
        dialog.setTitle("Import de bornes");
        dialog.getIcons().add(SIRS.ICON);
        dialog.setResizable(true);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setScene(new Scene(panel));
        dialog.initOwner(Injector.getSession().getFrame().getScene().getWindow());
        dialog.sizeToScene();
        dialog.showAndWait();
    }
}
