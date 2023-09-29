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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.model.LeveProfilTravers;
import fr.sirs.core.model.LigneEau;
import fr.sirs.core.model.MesureLigneEauXYZ;
import fr.sirs.core.model.PointXYZ;
import fr.sirs.core.model.ProfilLong;
import fr.sirs.core.model.XYZLeveProfilTravers;
import fr.sirs.core.model.XYZProfilLong;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.EventObject;
import java.util.logging.Level;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.csv.CSVFeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.data.shapefile.ShapefileFeatureStore;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.LayerListener;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.RandomStyleBuilder;
import org.geotoolkit.util.collection.CollectionChangeEvent;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Id;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXImportXYZ extends FXAbstractImportPointLeve<PointXYZ> {

    private static final String ATT_X_KEY = "attX";
    private static final String ATT_Y_KEY = "attY";

    @FXML private ComboBox<PropertyType> uiAttX;
    @FXML private ComboBox<PropertyType> uiAttY;

    public FXImportXYZ(final PojoTable pojoTable) {
        super(pojoTable);
        uiAttX.setConverter(stringConverter);
        uiAttY.setConverter(stringConverter);
        this.initFieldValue();
        this.getScene().getWindow().setOnCloseRequest(ev -> this.saveFieldValue());
    }

    protected void initFieldValue() {
        // Init table with the previous file opened
        if ((uiPath.getText() != null && !uiPath.getText().isEmpty())) {
            // Must be done before fillFieldFromComboBox
            openFeatureStore();
        }

        // Must be done after openFeatureStore
        // Must be done before fillFieldFromComboBox
        stringConverter.registerList(uiAttX.getItems());
        stringConverter.registerList(uiAttY.getItems());
        stringConverter.registerList(uiAttZ.getItems());
        stringConverter.registerList(uiAttDesignation.getItems());

        fillFieldFromComboBox(ATT_X_KEY, uiAttX);
        fillFieldFromComboBox(ATT_Y_KEY, uiAttY);
        fillFieldFromComboBox(ATT_Z_KEY, uiAttZ);
        fillFieldFromComboBox(ATT_DESIGNATION_KEY, uiAttDesignation);
    }

    @FXML
    void openFeatureStore(ActionEvent event) {
        openFeatureStore();
    }

    private void openFeatureStore() {
        if (uiPath == null) {
            final Alert alert = new Alert(Alert.AlertType.ERROR, "Le fichier sélectionné n'est pas un shp, csv ou txt", ButtonType.OK);
            alert.setResizable(true);
            alert.showAndWait();
            return;
        }

        final String url = uiPath.getText();
        final File file = new File(uiPath.getText());

        uiPaneConfig.setDisable(true);

        selectionProperty.clear();

        try{
            if(url.toLowerCase().endsWith(".shp")){
                store = new ShapefileFeatureStore(file.toURI(), "no namespace");
                uiPaneConfig.setDisable(true);
            }else if(url.toLowerCase().endsWith(".txt") || url.toLowerCase().endsWith(".csv")){
                final char separator = (uiSeparator.getText().isEmpty()) ? ';' : uiSeparator.getText().charAt(0);
                store = new CSVFeatureStore(file, "no namespace", separator);
                uiPaneConfig.setDisable(false);
            }else{
                final Alert alert = new Alert(Alert.AlertType.ERROR, "Le fichier sélectionné n'est pas un shp, csv ou txt", ButtonType.OK);
                alert.setResizable(true);
                alert.showAndWait();
                return;
            }

            final Session session = store.createSession(true);
            final GenericName typeName = store.getNames().iterator().next();
            final FeatureCollection col = session.getFeatureCollection(QueryBuilder.all(typeName));
            final FeatureMapLayer layer = MapBuilder.createFeatureLayer(col, RandomStyleBuilder.createDefaultVectorStyle(col.getFeatureType()));
            uiTable.init(layer);

            //liste des propriétés
            final ObservableList<PropertyType> properties = getPropertiesFromFeatures(col);

            uiAttDesignation.setItems(properties);
            uiAttX.setItems(properties);
            uiAttY.setItems(properties);
            uiAttZ.setItems(properties);

            if(!properties.isEmpty()){
                uiAttDesignation.getSelectionModel().clearAndSelect(0);
                uiAttX.getSelectionModel().clearAndSelect(0);
                uiAttY.getSelectionModel().clearAndSelect(0);
                uiAttZ.getSelectionModel().clearAndSelect(0);
            }

            //on ecoute la selection
            layer.addLayerListener(new LayerListener() {
                @Override
                public void styleChange(MapLayer source, EventObject event) {}
                @Override
                public void itemChange(CollectionChangeEvent<MapItem> event) {}
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(!FeatureMapLayer.SELECTION_FILTER_PROPERTY.equals(evt.getPropertyName())) return;

                    selectionProperty.clear();
                    final Id filter = layer.getSelectionFilter();
                    try {
                        final FeatureCollection selection = layer.getCollection().subCollection(QueryBuilder.filtered(typeName, filter));
                        final FeatureIterator iterator = selection.iterator();
                        while(iterator.hasNext()){
                            selectionProperty.add(iterator.next());
                        }
                        iterator.close();
                    } catch (DataStoreException ex) {
                        SIRS.LOGGER.log(Level.WARNING, ex.getMessage(),ex);
                    }
                }
            });

        }catch(Exception ex){
            SIRS.LOGGER.log(Level.WARNING, ex.getMessage(),ex);
            final Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
            alert.setResizable(true);
            alert.showAndWait();
            return;
        }
    }

    @Override
    protected ObservableList<PointXYZ> getSelectionPoint(){
        final ObservableList<Feature> features = selectionProperty;
        final ObservableList<PointXYZ> leves = FXCollections.observableArrayList();

        for(final Feature feature : features){
            final PointXYZ leve;
            final fr.sirs.Session sirsSession = Injector.getSession();

            if(pojoTable.getParentElement() instanceof LeveProfilTravers){
                leve = sirsSession.getElementCreator().createElement(XYZLeveProfilTravers.class, false);
            } else if(pojoTable.getParentElement() instanceof ProfilLong){
                leve = sirsSession.getElementCreator().createElement(XYZProfilLong.class, false);
            } else if(pojoTable.getParentElement() instanceof LigneEau){
                leve = sirsSession.getElementCreator().createElement(MesureLigneEauXYZ.class, false);
            } else {
                throw new UnsupportedOperationException("Type d'élément parent inconnu pour les points de levé.");
            }

            Point geom;
            final CoordinateReferenceSystem dataCrs;

            // X/Y
            if(uiPaneConfig.isDisable()){
                //shapefile
                geom = ((Geometry)feature.getDefaultGeometryProperty().getValue()).getCentroid();
                dataCrs = feature.getType().getCoordinateReferenceSystem();

            }else{
                //csv
                final String attX = String.valueOf(feature.getPropertyValue(uiAttX.getValue().getName().tip().toString()));
                final String attY = String.valueOf(feature.getPropertyValue(uiAttY.getValue().getName().tip().toString()));
                geom = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(Double.valueOf(attX), Double.valueOf(attY)));
                dataCrs = uiCRS.getValue();
            }

            //transform to RGF93
            try{
                final MathTransform trs = CRS.findOperation(dataCrs, sirsSession.getProjection(), null).getMathTransform();
                geom = (Point) JTS.transform(geom, trs);
                JTS.setCRS(geom, sirsSession.getProjection());

            }catch(TransformException | FactoryException ex){
                SIRS.LOGGER.log(Level.WARNING, ex.getMessage(),ex);
                final Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                alert.setResizable(true);
                alert.showAndWait();
            }
            leve.setX(geom.getX());
            leve.setY(geom.getY());

            // Z
            leve.setZ(Double.valueOf(String.valueOf(feature.getPropertyValue(uiAttZ.getValue().getName().tip().toString()))));

            leve.setDesignation(String.valueOf(feature.getPropertyValue(uiAttDesignation.getValue().getName().tip().toString())));

            leve.setAuthor(sirsSession.getUtilisateur() == null? null : sirsSession.getUtilisateur().getId());
            leve.setValid(sirsSession.createValidDocuments().get());
            leves.add(leve);
        }
        return leves;
    }

    @Override
    protected void saveFieldValue() {
        super.saveFieldValue();
        savePreference(ATT_X_KEY, stringConverter.toString(uiAttX.getSelectionModel().getSelectedItem()));
        savePreference(ATT_Y_KEY, stringConverter.toString(uiAttY.getSelectionModel().getSelectedItem()));
    }
}
