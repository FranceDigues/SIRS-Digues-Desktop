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
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import static fr.sirs.SIRS.CRS_WGS84;
import static fr.sirs.SIRS.ICON_IMPORT_WHITE;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.TronconDigue;
import static fr.sirs.theme.ui.FXPositionableMode.fxNumberValue;

import fr.sirs.util.ConvertPositionableCoordinates;
import fr.sirs.util.FormattedDoubleConverter;
import fr.sirs.util.SirsStringConverter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.internal.GeotkFX;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Edition des coordonées géographique d'un {@link Positionable}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class FXPositionableAbstractCoordMode extends BorderPane implements FXPositionableMode {

    private static final double BUFFER_DISTANCE = 0.001;

    private static final StringConverter<Double> SEVEN_DIGITS_CONVERTER = new FormattedDoubleConverter(new DecimalFormat("#.#######"));
    private static final StringConverter<Double> TWO_DIGITS_CONVERTER = new FormattedDoubleConverter(new DecimalFormat("#.##"));

    private final CoordinateReferenceSystem baseCrs = Injector.getSession().getProjection();

    private final ObjectProperty<Positionable> posProperty = new SimpleObjectProperty<>();
    protected final BooleanProperty disableProperty = new SimpleBooleanProperty(true);

    @FXML
    protected ComboBox<CoordinateReferenceSystem> uiCRSs;
    @FXML
    protected Spinner<Double> uiLongitudeStart;
    @FXML
    protected Spinner<Double> uiLongitudeEnd;
    @FXML
    protected Spinner<Double> uiLatitudeStart;
    @FXML
    protected Spinner<Double> uiLatitudeEnd;

    @FXML
    protected Label uiGeoCoordLabel;

    private final Button uiImport;

    private boolean reseting = false;

    public void setReseting(boolean reseting) {
        this.reseting = reseting;
    }

    public FXPositionableAbstractCoordMode() {
        SIRS.loadFXML(this, Positionable.class);

        //bouton d'import
        uiImport = new Button();
        uiImport.setGraphic(new ImageView(ICON_IMPORT_WHITE));
        uiImport.getStyleClass().add("buttonbar-button");
        uiImport.setOnAction(this::importCoord);
        uiImport.visibleProperty().bind(disableProperty.not());

        uiLongitudeStart.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0, 1));
        uiLatitudeStart.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0, 1));
        uiLongitudeEnd.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0, 1));
        uiLatitudeEnd.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0, 1));
        uiLongitudeStart.setEditable(true);
        uiLatitudeStart.setEditable(true);
        uiLongitudeEnd.setEditable(true);
        uiLatitudeEnd.setEditable(true);
        uiLongitudeStart.disableProperty().bind(disableProperty);
        uiLatitudeStart.disableProperty().bind(disableProperty);
        uiLongitudeEnd.disableProperty().bind(disableProperty);
        uiLatitudeEnd.disableProperty().bind(disableProperty);

        //liste par défaut des systemes de coordonnées
        ObservableList<CoordinateReferenceSystem> crss = FXCollections.unmodifiableObservableList(
                FXCollections.observableList(
                        Arrays.asList(CRS_WGS84, baseCrs)
                )
        );

        uiCRSs.setItems(crss);

        // JIRA SYM-1638 : adapt decimal representation to provide consistent precision.
        uiCRSs.valueProperty().addListener(this::replaceConverter);

        uiCRSs.getSelectionModel().select(baseCrs);
        uiCRSs.disableProperty().bind(disableProperty);
        uiCRSs.setConverter(new SirsStringConverter());

        final ChangeListener<Geometry> geomListener = (observable, oldValue, newValue) -> {
            if (reseting) {
                return;
            }
            updateFields();
        };

        // Listener pour les changements sur les points de début et de fin, dans le cadre de l'import de bornes par exemple.
        final ChangeListener<Point> pointListener = (obs, oldVal, newVal) -> updateFields();

        //Listener permettant d'indiquer si les coordonnées sont calculées ou éditées
        final ChangeListener<Boolean> updateEditedGeoCoordinatesDisplay = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                setCoordinatesLabel(oldValue, newValue);

        posProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.geometryProperty().removeListener(geomListener);
                oldValue.positionDebutProperty().removeListener(pointListener);
                oldValue.positionFinProperty().removeListener(pointListener);
                oldValue.editedGeoCoordinateProperty().removeListener(updateEditedGeoCoordinatesDisplay);
            }
            if (newValue != null) {
                newValue.geometryProperty().addListener(geomListener);
                // Listeners to update values in the pane when they are updated via the pojoTable.
                newValue.positionDebutProperty().addListener(pointListener);
                newValue.positionFinProperty().addListener(pointListener);
                newValue.editedGeoCoordinateProperty().addListener(updateEditedGeoCoordinatesDisplay);
                setCoordinatesLabel(null, newValue.getEditedGeoCoordinate());
                updateFields();
            }
        });

        final ChangeListener<Double> valListener = (ObservableValue<? extends Double> observable, Double oldValue, Double newValue) -> coordChange();
        uiLongitudeStart.valueProperty().addListener(valListener);
        uiLatitudeStart.valueProperty().addListener(valListener);
        uiLongitudeEnd.valueProperty().addListener(valListener);
        uiLatitudeEnd.valueProperty().addListener(valListener);

        uiCRSs.valueProperty().addListener(this::crsChange);
    }

    /**
     * Méthode permettant de mettre à jour le label (FXML) indiquant si les
     * coordonnées du mode ont été calculées ou éditées.
     *
     * @param oldEditedGeoCoordinate ancienne valeur de la propriété
     * editedGeoCoordinate du positionable courant. Null si ont l'ignore.
     * @param newEditedGeoCoordinate nouvelle valeur.
     */
    protected void setCoordinatesLabel(Boolean oldEditedGeoCoordinate, Boolean newEditedGeoCoordinate) {
        if (newEditedGeoCoordinate == null) {
            uiGeoCoordLabel.setText(COORDS_TYPE_UNKNOWN);
            return;
        } else if (newEditedGeoCoordinate.equals(oldEditedGeoCoordinate)) {
            return;
        }

        if (uiGeoCoordLabel != null) { //Occurred for vegetation
            if (newEditedGeoCoordinate) {
                uiGeoCoordLabel.setText(COORDS_TYPE_ENTERED);
            } else {
                uiGeoCoordLabel.setText(COORDS_TYPE_COMPUTED);
            }
        }
    }

    @Override
    public String getTitle() {
        return "Coordonnées";
    }

    public List<Node> getExtraButton() {
        return Collections.singletonList(uiImport);
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

    private void importCoord(ActionEvent event) {
        final FXImportCoordinate importCoord = new FXImportCoordinate(posProperty.get());
        final Dialog dialog = new Dialog();
        final DialogPane pane = new DialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setContent(importCoord);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Import de coordonnées");
        dialog.setOnCloseRequest(event1 -> dialog.hide());
        dialog.show();
    }

    @Override
    public void updateFields() {
        reseting = true;

        final Positionable pos = posProperty.get();
        final String mode = pos.getGeometryMode();
        Point startPoint, endPoint;
        if (mode == null || getID().equals(mode)) {
            //on peut réutiliser les points enregistrés dans la position
            startPoint = pos.getPositionDebut();
            endPoint = pos.getPositionFin();
        } else {
            startPoint = endPoint = null;
        }

        if (startPoint == null && endPoint == null) {
            final Geometry geom = pos.getGeometry();
            //on refait les points a partir de la géométrie
            if (geom != null) {
                Coordinate[] coords = geom.getCoordinates();
                if (coords.length > 0) {
                    startPoint = GO2Utilities.JTS_FACTORY.createPoint(coords[0]);
                }
                if (coords.length > 1) {
                    endPoint = GO2Utilities.JTS_FACTORY.createPoint(coords[coords.length - 1]);
                }
            }
        }

        final CoordinateReferenceSystem crs = uiCRSs.getValue();
        if (baseCrs != crs && (startPoint != null || endPoint != null)) {
            try {
                final MathTransform tr = CRS.findOperation(baseCrs, crs, null).getMathTransform();
                if (startPoint != null) {
                    startPoint = JTS.transform(startPoint, tr).getInteriorPoint();
                }
                if (endPoint != null) {
                    endPoint = JTS.transform(endPoint, tr).getInteriorPoint();
                }

            } catch (FactoryException | MismatchedDimensionException | TransformException ex) {
                GeotkFX.newExceptionDialog("La conversion des positions a échoué.", ex).show();
                throw new RuntimeException("La conversion des positions a échoué.", ex);
            }
        }

        updateLatLongFor(startPoint, endPoint);
        reseting = false;
    }

    /**
     * Update the spinners' Longitude and Latitude values for start and end points.
     * @param startPoint the start point coordinates
     * @param endPoint the end point coordinates
     */
    public void updateLatLongFor(final Point startPoint, final Point endPoint) {
        updateLatLonSpinnerWithPoint(startPoint, uiLongitudeStart, uiLatitudeStart);
        updateLatLonSpinnerWithPoint(endPoint, uiLongitudeEnd, uiLatitudeEnd);
    }

    /**
     * Update the spinners' Longitude and Latitude values with point's coordinates.
     * @param point point with coordinates to update the spinners
     * @param lonSpinner spinner for longitude
     * @param latSpinner spinner for latitude
     */
    private void updateLatLonSpinnerWithPoint(final Point point, final Spinner<Double> lonSpinner, final Spinner<Double> latSpinner) {
        if (point != null) {
            if (lonSpinner != null && lonSpinner.getValueFactory() != null)
                lonSpinner.getValueFactory().valueProperty().set(point.getX());
            if (latSpinner != null && latSpinner.getValueFactory() != null)
                latSpinner.getValueFactory().valueProperty().set(point.getY());
        }
        // if the user erase the value, the old value is set back.
        else {
            SIRS.LOGGER.log(Level.WARNING,"test Value null for spinner " );
        }
    }

    @Override
    public void buildGeometry() {

        final Positionable positionable = posProperty.get();

        // On ne met la géométrie à jour depuis ce panneau que si on est dans son mode.
        if (!getID().equals(positionable.getGeometryMode())) return;

        // Si un CRS est défini, on essaye de récupérer les positions géographiques depuis le formulaire.
        final CoordinateReferenceSystem crs = uiCRSs.getValue();
        if (crs == null) return;

        Point startPoint = null;
        Point endPoint = null;
        if (uiLongitudeStart.getValue() != null && uiLatitudeStart.getValue() != null) {
            startPoint = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(
                    uiLongitudeStart.getValue(), uiLatitudeStart.getValue()));
            JTS.setCRS(startPoint, crs);
        }

        if (uiLongitudeEnd.getValue() != null && uiLatitudeEnd.getValue() != null) {
            endPoint = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(
                    uiLongitudeEnd.getValue(), uiLatitudeEnd.getValue()));
            JTS.setCRS(endPoint, crs);
        }

        if (startPoint == null) {
            if (endPoint == null) return;
            startPoint = endPoint;
        }
        if (endPoint == null) endPoint = startPoint;

        //on sauvegarde les points dans le crs de la base
        if (!Utilities.equalsIgnoreMetadata(crs, baseCrs)) {
            try {
                final MathTransform trs = CRS.findOperation(crs, baseCrs, null).getMathTransform();
                startPoint = (Point) JTS.transform(startPoint, trs);
                endPoint = (Point) JTS.transform(endPoint, trs);

            } catch (FactoryException | MismatchedDimensionException | TransformException ex) {
                GeotkFX.newExceptionDialog("La conversion des positions a échoué.", ex).show();
                throw new RuntimeException("La conversion des positions a échoué.", ex);
            }
        }

        final TronconDigue troncon = ConvertPositionableCoordinates.getTronconFromPositionable(positionable);
        LineString geometry = LinearReferencingUtilities.buildGeometryFromGeo(troncon.getGeometry(), startPoint, endPoint);

        /* SYM-1658 : If user has inverted start and end point, we must try to
         * detect it and reverse his seizure. To do so, we ensure that user typed
         * two distinct point (not overlapping), and that the generated geometry
         * consists of a single point. If so, we inverse start and end point,
         * then ask back geometry computing.
         */
        final Geometry proximityBuf = startPoint.buffer(BUFFER_DISTANCE);
        if (!proximityBuf.contains(endPoint)) {
            if (geometry.getNumPoints() < 3) {
                final Point geomStart = geometry.getStartPoint();
                final Point geomEnd = geometry.getEndPoint();
                if (geomStart.equals(geomEnd) || geomStart.buffer(BUFFER_DISTANCE).contains(geomEnd)) {
                    final Point tmp = startPoint;
                    startPoint = endPoint;
                    endPoint = tmp;
                    geometry = LinearReferencingUtilities.buildGeometryFromGeo(troncon.getGeometry(), startPoint, endPoint);
                }
            }
        }

        positionable.setPositionDebut(startPoint);
        positionable.setPositionFin(endPoint);
        positionable.geometryModeProperty().set(getID());
        positionable.setGeometry(geometry);

        ConvertPositionableCoordinates.computePositionableLinearCoordinate(positionable);

    }

    protected void coordChange() {
        if (reseting) {
            return;
        }
        reseting = true;
        try {
            buildGeometry();
            posProperty.get().setEditedGeoCoordinate(Boolean.TRUE);
        } catch (Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Echec de la construction de la géométrie lors du changement de coordonnées.", e);
        } finally {
            reseting = false;
        }
    }

    /**
     * Update geographic/projected coordinate fields when current CRS change.
     * Note : listener method, should always be launched from FX-thread.
     *
     * @param oldValue Previous value into {@link #uiCRSs}
     * @param newValue Current value into {@link #uiCRSs}
     */
    private void crsChange(ObservableValue<? extends CoordinateReferenceSystem> observable,
            CoordinateReferenceSystem oldValue, CoordinateReferenceSystem newValue) {
        if (reseting) {
            return;
        }

        // There's no available null value in CRS combobox, so old value will be
        // null only at first allocation, no transform needed in this case.
        if (oldValue == null || newValue == null) {
            return;
        }

        reseting = true;

        final Point ptStart, ptEnd;
        // On a un point de début valide
        if (uiLongitudeStart.valueProperty().get() != null && uiLatitudeStart.valueProperty().get() != null) {
            ptStart = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(
                    fxNumberValue(uiLongitudeStart.getValueFactory().valueProperty()),
                    fxNumberValue(uiLatitudeStart.getValueFactory().valueProperty())
            ));
        } else {
            ptStart = null;
        }

        // On a un point de fin valide
        if (uiLongitudeEnd.valueProperty().get() != null && uiLatitudeEnd.valueProperty().get() != null) {
            ptEnd = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(
                    fxNumberValue(uiLongitudeEnd.getValueFactory().valueProperty()),
                    fxNumberValue(uiLatitudeEnd.getValueFactory().valueProperty())
            ));
        } else {
            ptEnd = null;
        }

        // If we've got at least one valid point, we transform it. Otherwise, just return.
        if (ptStart != null || ptEnd != null) {
            try {
                final MathTransform conversion = CRS.findOperation(oldValue, newValue, null).getMathTransform();

                if (ptStart != null) {
                    final Point tmpStart = (Point) JTS.transform(ptStart, conversion);
                    Platform.runLater(() -> {
                        uiLongitudeStart.getValueFactory().valueProperty().set(tmpStart.getX());
                        uiLatitudeStart.getValueFactory().valueProperty().set(tmpStart.getY());
                    });
                }
                if (ptEnd != null) {
                    final Point tmpEnd = (Point) JTS.transform(ptEnd, conversion);
                    Platform.runLater(() -> {
                        uiLongitudeEnd.getValueFactory().valueProperty().set(tmpEnd.getX());
                        uiLatitudeEnd.getValueFactory().valueProperty().set(tmpEnd.getY());
                    });
                }
            } catch (Exception ex) {
                GeotkFX.newExceptionDialog("La conversion des positions a échoué.", ex).show();
                throw new RuntimeException("La conversion des positions a échoué.", ex);
            }
        }

        buildGeometry();
        reseting = false;
    }

    private void replaceConverter(final Observable obs, final CoordinateReferenceSystem oldValue, final CoordinateReferenceSystem newValue) {
        boolean isMeter = false;
        if (newValue != null) {
            final SingleCRS hcrs = CRS.getHorizontalComponent(newValue);
            final CoordinateSystemAxis axis = hcrs.getCoordinateSystem().getAxis(0);
            if (Units.METRE.equals(axis.getUnit())) {
                isMeter = true;
            }
        }

        // If we express meter coordinates, we keep JavaFX default behavior, cause two decimal are enough (cm precision)
        final StringConverter<Double> converter = isMeter ? TWO_DIGITS_CONVERTER : SEVEN_DIGITS_CONVERTER;
        getSpinners()
                .map(Spinner::getValueFactory)
                .forEach(vf -> {
                    if (vf != null) {
                        vf.setConverter(converter);
                    } else {
                        SIRS.LOGGER.log(Level.WARNING,"Value factory null for spinner");
                    }
                });
    }

    protected Stream<Spinner> getSpinners() {
        return Stream.of(uiLongitudeStart, uiLongitudeEnd, uiLatitudeStart, uiLatitudeEnd);
    }
}
