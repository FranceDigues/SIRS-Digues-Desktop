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
import com.vividsolutions.jts.io.WKTReader;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.PositionableVegetation;
import fr.sirs.plugin.vegetation.PluginVegetation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import org.geotoolkit.geometry.jts.JTS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Edition en WKT des geometries
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXPositionableExplicitMode extends BorderPane implements FXPositionableMode {

    public static final String MODE = PluginVegetation.Mode.EXPLICIT.getValue();

    private final CoordinateReferenceSystem baseCrs = Injector.getSession().getProjection();

    private final ObjectProperty<Positionable> posProperty = new SimpleObjectProperty<>();
    private final BooleanProperty disableProperty = new SimpleBooleanProperty(true);

    @FXML
    private TextArea uiText;

    private boolean reseting = false;

    public FXPositionableExplicitMode() {
        SIRS.loadFXML(this, Positionable.class);

        uiText.disableProperty().bind(disableProperty);

        final ChangeListener<Geometry> geomListener = new ChangeListener<Geometry>() {
            @Override
            public void changed(ObservableValue<? extends Geometry> observable, Geometry oldValue, Geometry newValue) {
                if(reseting) return;
                updateFields();
            }
        };

        posProperty.addListener(new ChangeListener<Positionable>() {
            @Override
            public void changed(ObservableValue<? extends Positionable> observable, Positionable oldValue, Positionable newValue) {
                if(oldValue!=null){
                    oldValue.geometryProperty().removeListener(geomListener);
                }
                if(newValue!=null){
                    newValue.geometryProperty().addListener(geomListener);
                    updateFields();
                }
            }
        });

        uiText.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> coordChange() );

    }

    @Override
    public String getID() {
        return MODE;
    }

    @Override
    public String getTitle() {
        return "Géométrie";
    }

    @Override
    public Node getFXNode() {
        return this;
    }

    @Override
    public ObjectProperty<Positionable> positionableProperty() {
        return posProperty;
    }

    @Override
    public BooleanProperty disablingProperty() {
        return disableProperty;
    }

    @Override
    public void updateFields(){
        reseting = true;

        final Positionable pos = posProperty.get();
        final Geometry geom = pos.getGeometry();
        final String wkt;
        if(geom==null){
            wkt = "";
        }else{
            wkt = pos.getGeometry().toText();
        }

        uiText.setText(wkt);

        reseting = false;
    }

    @Override
    public void buildGeometry(){

        final String wkt = uiText.getText();
        final WKTReader reader = new WKTReader();

        try {
            final Geometry geom = reader.read(wkt);
            JTS.setCRS(geom, baseCrs);

            final PositionableVegetation positionable = (PositionableVegetation) posProperty.get();
            positionable.geometryModeProperty().set(MODE);
            positionable.setGeometry(geom);
            positionable.setExplicitGeometry(geom);

            uiText.getStyleClass().remove("unvalid");
            uiText.getStyleClass().add("valid");

        } catch (Exception ex) {
            //JTS raise many other kind of exceptions, not on ParseException
            uiText.getStyleClass().remove("valid");
            uiText.getStyleClass().add("unvalid");
        }
    }

    private void coordChange(){
        if(reseting) return;

        reseting = true;
        buildGeometry();
        reseting = false;
    }


}
