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
import fr.sirs.core.model.DZLeveProfilTravers;
import fr.sirs.core.model.LeveProfilTravers;
import fr.sirs.core.model.LigneEau;
import fr.sirs.core.model.MesureLigneEauPrZ;
import fr.sirs.core.model.PointDZ;
import fr.sirs.core.model.PrZProfilLong;
import fr.sirs.core.model.ProfilLong;
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
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.csv.CSVFeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.data.shapefile.ShapefileFeatureStore;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.LayerListener;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.RandomStyleBuilder;
import org.geotoolkit.util.collection.CollectionChangeEvent;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Id;
import org.opengis.util.GenericName;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXImportDZ extends FXAbstractImportPointLeve<PointDZ> {

    private static final String ATT_D_KEY = "attD";

    @FXML private ComboBox<PropertyType> uiAttD;

    public FXImportDZ(final PojoTable pojoTable) {
        super(pojoTable);

        uiAttD.setConverter(stringConverter);
        uiCRS.setDisable(true);
        this.initFieldValue();
        this.getScene().getWindow().setOnCloseRequest(ev -> this.saveFieldValue());
    }

    private void initFieldValue() {
        // Init table with the previous file opened
        if ((uiPath.getText() != null && !uiPath.getText().isEmpty())) {
            // Must be done before fillFieldFromComboBox
            openFeatureStore();
        }

        // Must be done after openFeatureStore
        // Must be done before fillFieldFromComboBox
        stringConverter.registerList(uiAttD.getItems());
        stringConverter.registerList(uiAttZ.getItems());
        stringConverter.registerList(uiAttDesignation.getItems());

        // Init coordinate value with the previous one saved
        fillFieldFromComboBox(ATT_D_KEY, uiAttD);
        fillFieldFromComboBox(ATT_Z_KEY, uiAttZ);
        fillFieldFromComboBox(ATT_DESIGNATION_KEY, uiAttDesignation);
    }

    @FXML
    void openFeatureStore(ActionEvent event) {
        openFeatureStore();
    }

    private void openFeatureStore() {
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

            stringConverter.registerList(properties);

            uiAttDesignation.setItems(properties);
            uiAttD.setItems(properties);
            uiAttZ.setItems(properties);

            if(!properties.isEmpty()){
                uiAttDesignation.getSelectionModel().clearAndSelect(0);
                uiAttD.getSelectionModel().clearAndSelect(0);
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

        } catch(Exception ex){
            SIRS.LOGGER.log(Level.WARNING, ex.getMessage(),ex);
            final Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
            alert.setResizable(true);
            alert.showAndWait();
        }
    }

    @Override
    protected void saveFieldValue() {
        super.saveFieldValue();
        savePreference(ATT_D_KEY, stringConverter.toString(uiAttD.getSelectionModel().getSelectedItem()));
    }

    @Override
    protected ObservableList<PointDZ> getSelectionPoint(){
        final ObservableList<Feature> features = selectionProperty;
        final ObservableList<PointDZ> leves = FXCollections.observableArrayList();

        for(final Feature feature : features){
            final PointDZ leve;

            if(pojoTable.getParentElement() instanceof LeveProfilTravers){
                leve = Injector.getSession().getElementCreator().createElement(DZLeveProfilTravers.class, false);
            } else if(pojoTable.getParentElement() instanceof ProfilLong){
                leve = Injector.getSession().getElementCreator().createElement(PrZProfilLong.class, false);
            } else if(pojoTable.getParentElement() instanceof LigneEau){
                leve = Injector.getSession().getElementCreator().createElement(MesureLigneEauPrZ.class, false);
            } else {
                throw new UnsupportedOperationException("Type d'élément parent inconnu pour les points de levé.");
            }

            // DZ
            leve.setD(Double.valueOf(String.valueOf(feature.getPropertyValue(uiAttD.getValue().getName().tip().toString()))));
            leve.setZ(Double.valueOf(String.valueOf(feature.getPropertyValue(uiAttZ.getValue().getName().tip().toString()))));

            leve.setDesignation(String.valueOf(feature.getPropertyValue(uiAttDesignation.getValue().getName().tip().toString())));

            leves.add(leve);
        }
        return leves;
    }
}
