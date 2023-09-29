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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.model.AbstractDependance;
import fr.sirs.core.model.AireStockageDependance;
import fr.sirs.core.model.AvecGeometrie;
import fr.sirs.core.model.AvecSettableGeometrie;
import fr.sirs.core.model.CheminAccesDependance;
import fr.sirs.core.model.AbstractAmenagementHydraulique;
import fr.sirs.map.FXMapTab;
import fr.sirs.plugin.dependance.map.DependanceEditHandler;
import fr.sirs.plugin.dependance.map.AbstractAmenagementHydrauliqueEditHandler;
import fr.sirs.ui.Growl;
import static fr.sirs.util.SIRSAreaComputer.getGeometryInfo;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.shapefile.ShapefileFeatureStore;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.JTSEnvelope2D;
import org.geotoolkit.gui.javafx.layer.FXFeatureTable;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

import java.io.File;
import java.net.MalformedURLException;
import java.util.logging.Level;


/**
 * Panneau de positionnement d'une dépendance, permettant d'éditer sa géométrie sur la carte ou
 * d'en importer une pour cette dépendance.
 *
 * @author Cédric Briançon (Geomatys)
 * @param <T>
 */
public class FXPositionDependancePane<T extends AvecSettableGeometrie> extends BorderPane {
    private final BooleanProperty disableFieldsProperty = new SimpleBooleanProperty(true);

    @FXML
    private Label uiGeomInfo;

    @FXML
    private Label uiGeometryPresentLbl;

    @FXML
    private Button uiDrawOnMapBtn;

    @FXML
    private Button uiImportGeometryBtn;

    /**
     * La dépendance à éditer.
     */
    private final ObjectProperty<T> dependance = new SimpleObjectProperty<>();

    public FXPositionDependancePane() {
        SIRS.loadFXML(this);

        setGeomLabel(false);
        dependance.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                setGeomLabel(false);
            } else {
                final Geometry geom = newValue.getGeometry();
                setGeomLabel(geom != null);
                uiGeomInfo.setText(getGeometryInfoWrapped(geom));
                newValue.geometryProperty().addListener((observable1, oldValue1, newValue1) -> {
                    setGeomLabel(newValue1 != null);
                    uiGeomInfo.setText(getGeometryInfoWrapped((Geometry) newValue1));
                });
            }
        });
    }

    private void setGeomLabel(final boolean geom) {
        if (geom) {
            uiGeometryPresentLbl.setText("Géométrie présente");
            uiDrawOnMapBtn.setText("Modifier le tracé");
            uiImportGeometryBtn.setText("Réimporter une géométrie");
        } else {
            uiGeometryPresentLbl.setText("Pas de géométrie");
            uiDrawOnMapBtn.setText("Tracer sur la carte");
            uiImportGeometryBtn.setText("Importer une géométrie");
        }
    }

    public AvecGeometrie getDependance() {
        return dependance.get();
    }

    public ObjectProperty<T> dependanceProperty() {
        return dependance;
    }

    @FXML
    public void drawOnMap() {
        final FXMapTab tab = Injector.getSession().getFrame().getMapTab();
        tab.show();

        if (dependance.get().getGeometry() != null) {
            final JTSEnvelope2D env = JTS.toEnvelope(dependance.get().getGeometry());
            final Envelope selectionEnvelope = SIRS.pseudoBuffer(env);
            Platform.runLater(() -> {
                try {
                    tab.getMap().getUiMap().getCanvas().setVisibleArea(selectionEnvelope);
                } catch (Exception e) {
                    GeotkFX.newExceptionDialog("Impossible de zoomer sur l'étendue de la géométrie", e);
                    SIRS.LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
                }
            });
        }
        if(dependance.get() instanceof AbstractDependance){
            tab.getMap().getUiMap().setHandler(new DependanceEditHandler((AbstractDependance) dependance.get()));
        }else if(dependance.get() instanceof AbstractAmenagementHydraulique){
            tab.getMap().getUiMap().setHandler(new AbstractAmenagementHydrauliqueEditHandler((AbstractAmenagementHydraulique) dependance.get()));
        }
    }

    /**
     * Importe une géométrie provenant d'un fichier SHP comme géométrie de la dépendance.
     */
    @FXML
    public void importGeometry() {
        final FileChooser fileChooser = new FileChooser();
        // Demande du fichier SHP à considérer
        final File shpFile = fileChooser.showOpenDialog(null);
        if(shpFile==null) return;
        final FXFeatureTable shpTable = new FXFeatureTable();
        shpTable.setLoadAll(true);

        try {
            final FeatureStore shpStore = new ShapefileFeatureStore(shpFile.toURI());

            final GenericName name = shpStore.getNames().iterator().next();
            final FeatureMapLayer mapLayer = MapBuilder.createFeatureLayer(
                    shpStore.createSession(true).getFeatureCollection(QueryBuilder.all(name)));
            shpTable.init(mapLayer);

            // Affichage d'une popup présentant les features contenues dans le SHP
            final Stage stage = new Stage();
            final HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_RIGHT);
            hBox.setPadding(new Insets(10));
            HBox.setHgrow(hBox, Priority.ALWAYS);
            final Button validateBtn = new Button("Valider");
            validateBtn.setOnAction(event -> stage.close());
            hBox.getChildren().add(validateBtn);
            final BorderPane mainPane = new BorderPane(shpTable, null, null, hBox, null);
            final Scene scene = new Scene(mainPane);
            stage.setScene(scene);
            stage.setTitle("Choisir une géométrie à importer");
            stage.getIcons().add(SIRS.ICON);
            stage.showAndWait();

            // La feature sélectionnée dans la table de la popup précédente sera utilisée comme géométrie de cette dépendance.
            final FeatureCollection ids = shpStore.createSession(true).getFeatureCollection(
                    QueryBuilder.filtered(name, mapLayer.getSelectionFilter()));
            try (final FeatureIterator it = ids.iterator()) {
                if (it.hasNext()) {
                    final Feature feature = it.next();
                    Geometry geom = (Geometry) feature.getDefaultGeometryProperty().getValue();
                    geom = JTS.transform(geom, Injector.getSession().getProjection());

                    //check the geometry type
                    AvecGeometrie absDep = dependance.get();
                    if(absDep instanceof AireStockageDependance){
                        //doit etre un polygon
                        if(!(geom instanceof Polygon || geom instanceof MultiPolygon)){
                            final Growl successGrowl = new Growl(Growl.Type.ERROR, "Mauvais import, la géométrie doit etre un polygone.");
                            successGrowl.showAndFade();
                            return;
                        }

                    }else if(absDep instanceof CheminAccesDependance){
                        //doit etre un linestring
                        if(!(geom instanceof LineString || geom instanceof MultiLineString)){
                            final Growl successGrowl = new Growl(Growl.Type.ERROR, "Mauvais import, la géométrie doit etre une polyligne.");
                            successGrowl.showAndFade();
                            return;
                        }
                    }

                    dependance.get().setGeometry(geom);

                    final Growl successGrowl = new Growl(Growl.Type.INFO, "Géométrie importée avec succès");
                    successGrowl.showAndFade();
                }
            }
        } catch (DataStoreException | TransformException | FactoryException | MalformedURLException ex) {
            GeotkFX.newExceptionDialog(ex.getLocalizedMessage(), ex);
            SIRS.LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
        }
    }

    public BooleanProperty disableFieldsProperty(){
        return disableFieldsProperty;
    }

    private String getGeometryInfoWrapped(final Geometry geometry) {
        try {
            return getGeometryInfo(geometry);
        } catch (IllegalArgumentException ex) {
            return "calcul impossible avec la projection actuelle";
        }
    }
}
