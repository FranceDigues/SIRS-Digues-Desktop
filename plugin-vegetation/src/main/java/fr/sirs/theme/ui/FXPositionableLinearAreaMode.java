/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.GeometryType;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.PositionableVegetation;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.model.ZoneVegetation;
import fr.sirs.plugin.vegetation.PluginVegetation;
import fr.sirs.util.ConvertPositionableCoordinates;
import java.util.List;
import java.util.Map;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/**
 * Edition des bornes d'un {@link Positionable}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXPositionableLinearAreaMode extends FXPositionableAbstractLinearMode {

    public static final String MODE = PluginVegetation.Mode.LINEAR_AREA.getValue();

    //area
    @FXML private Spinner<Double> uiStartNear;
    @FXML private Spinner<Double> uiStartFar;
    @FXML private Spinner<Double> uiEndNear;
    @FXML private Spinner<Double> uiEndFar;

    // Libellés à cacher si c'est un ponctuel
    @FXML private Label lblFin;
    @FXML private Label lblStartNear;
    @FXML private Label lblStartFar;
    @FXML private Label lblEndNear;
    @FXML private Label lblEndFar;

    private final String originalLblStartNear;

    private final BooleanProperty pctProp = new SimpleBooleanProperty(false);

    final ChangeListener<String> typeCoteChangeListener = (ObservableValue<? extends String> observable, String oldValue, String newValue) -> buildGeometry();

    public FXPositionableLinearAreaMode() {
        super();

        uiStartNear.disableProperty().bind(disableProperty);
        uiStartFar.disableProperty().bind(disableProperty);
        uiEndNear.disableProperty().bind(disableProperty);
        uiEndFar.disableProperty().bind(disableProperty);
        uiStartNear.setEditable(true);
        uiStartFar.setEditable(true);
        uiEndNear.setEditable(true);
        uiEndFar.setEditable(true);

        uiStartNear.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE, 0,1));
        uiStartFar.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE, 0,1));
        uiEndNear.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE, 0,1));
        uiEndFar.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE, 0,1));

        final ChangeListener chgListener = (ObservableValue observable, Object oldValue, Object newValue) -> coordChange(false);
        uiStartNear.valueProperty().addListener(chgListener);
        uiStartFar.valueProperty().addListener(chgListener);
        uiEndNear.valueProperty().addListener(chgListener);
        uiEndFar.valueProperty().addListener(chgListener);

        //on cache certain elements quand c'est un ponctuel
        lblFin.visibleProperty().bind(pctProp);
        uiBorneEnd.visibleProperty().bind(pctProp);
        uiAvalEnd.visibleProperty().bind(pctProp);
        uiDistanceEnd.visibleProperty().bind(pctProp);
        uiAmontEnd.visibleProperty().bind(pctProp);
        uiEndNear.visibleProperty().bind(pctProp);
        uiEndFar.visibleProperty().bind(pctProp);
        lblStartFar.visibleProperty().bind(pctProp);
        uiStartFar.visibleProperty().bind(pctProp);
        lblEndNear.visibleProperty().bind(pctProp);
        lblEndFar.visibleProperty().bind(pctProp);
        lblFin.managedProperty().bind(pctProp);
        uiBorneEnd.managedProperty().bind(pctProp);
        uiAvalEnd.managedProperty().bind(pctProp);
        uiDistanceEnd.managedProperty().bind(pctProp);
        uiAmontEnd.managedProperty().bind(pctProp);
        uiEndNear.managedProperty().bind(pctProp);
        uiEndFar.managedProperty().bind(pctProp);
        lblStartFar.managedProperty().bind(pctProp);
        uiStartFar.managedProperty().bind(pctProp);
        lblEndNear.managedProperty().bind(pctProp);
        lblEndFar.managedProperty().bind(pctProp);

        originalLblStartNear = lblStartNear.getText();
        lblStartNear.textProperty().bind(new StringBinding() {
            {bind(pctProp);}
            @Override
            protected String computeValue() {
                if(pctProp.get()){
                    return originalLblStartNear;
                }
                else return "Point unique";
            }
        });

        final ChangeListener posListener = (ObservableValue observable, Object oldValue, Object newValue) -> updateFields();

        // Listener permettant d'indiquer si la géométrie a été créée via la carte
        // ou bien a été calculée à partir des coordonnées (linéaire ou géo)
        final ChangeListener<Boolean> updateCartoEditedDisplay = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) setCoordinatesLabel(null, positionableProperty().get().getEditedGeoCoordinate());
        };

        positionableProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue instanceof ZoneVegetation) {
                ZoneVegetation zone = (ZoneVegetation) oldValue;
                zone.typeCoteIdProperty().removeListener(typeCoteChangeListener);
                zone.cartoEditedProperty().removeListener(updateCartoEditedDisplay);
                zone.distanceDebutMinProperty().removeListener(posListener);
                zone.distanceDebutMaxProperty().removeListener(posListener);
                zone.distanceFinMinProperty().removeListener(posListener);
                zone.distanceFinMaxProperty().removeListener(posListener);
            }
            if (newValue instanceof ZoneVegetation) {
                ZoneVegetation zone = (ZoneVegetation) newValue;
                zone.typeCoteIdProperty().addListener(typeCoteChangeListener);
                zone.cartoEditedProperty().addListener(updateCartoEditedDisplay);
                // Listeners to update values in the pane when they are updated via the pojoTable.
                zone.distanceDebutMinProperty().addListener(posListener);
                zone.distanceDebutMaxProperty().addListener(posListener);
                zone.distanceFinMinProperty().addListener(posListener);
                zone.distanceFinMaxProperty().addListener(posListener);
            }
        });
    }

    /**
     * Méthode permettant de mettre à jour le label (FXML) indiquant si les
     * coordonnées du mode ont été calculées ou éditées.
     *
     * @param oldEditedGeoCoordinate ancienne valeur de la propriété
     * editedGeoCoordinate du positionable courant. Null si on l'ignore.
     * @param newEditedGeoCoordinate nouvelle valeur.
     */
    @Override
    final protected void setCoordinatesLabel(Boolean oldEditedGeoCoordinate, Boolean newEditedGeoCoordinate){
        ZoneVegetation zone = (ZoneVegetation) positionableProperty().get();
        PluginVegetation.setCoordinatesLabel(oldEditedGeoCoordinate, newEditedGeoCoordinate, uiLinearCoordLabel, zone, false);
    }

    @Override
    public String getID() {
        return MODE;
    }

    @Override
    public void updateFields(){
        super.updateFields();
        setReseting(true);

        final PositionableVegetation pos = (PositionableVegetation) positionableProperty().get();

        updateDistanceSpinners(pos);

        //on cache certains champs si c'est un ponctuel
        pctProp.unbind();
        pctProp.bind(pos.geometryTypeProperty().isNotEqualTo(GeometryType.PONCTUAL));

        setReseting(false);
    }

    private void updateSpinnerWithValue(final Spinner<Double> spinner, final Double value, final Double defaultValue) {
        if (spinner != null && spinner.getValueFactory() != null)
            spinner.getValueFactory().setValue(value == null ? defaultValue : value);
    }

    private void updateDistanceSpinners(final PositionableVegetation pos) {
        updateSpinnerWithValue(uiStartNear, pos.getDistanceDebutMin(), 0.0);
        updateSpinnerWithValue(uiStartFar, pos.getDistanceDebutMax(), 0.0);
        updateSpinnerWithValue(uiEndNear, pos.getDistanceFinMin(), 0.0);
        updateSpinnerWithValue(uiEndFar, pos.getDistanceFinMax(), 0.0);
    }

    @Override
    public void buildGeometry(){
        //sauvegarde des propriétés
        final ZoneVegetation positionable = (ZoneVegetation) positionableProperty().get();

        // On ne met la géométrie à jour depuis ce panneau que si on est dans son mode.
        if (!MODE.equals(positionable.getGeometryMode())) return;

        setValuesFromUi(positionable);

        PluginVegetation.buildLinearGeometry(positionable, uiSRs.getValue(), PluginVegetation.Mode.LINEAR_AREA);
    }

    @Override
    public void setValuesFromUi(final Positionable positionable) {
        super.setValuesFromUi(positionable);
        PositionableVegetation pos = (PositionableVegetation) positionable;
        pos.setDistanceDebutMin(uiStartNear.getValue());
        pos.setDistanceDebutMax(uiStartFar.getValue());
        pos.setDistanceFinMin(uiEndNear.getValue());
        pos.setDistanceFinMax(uiEndFar.getValue());
    }

}
