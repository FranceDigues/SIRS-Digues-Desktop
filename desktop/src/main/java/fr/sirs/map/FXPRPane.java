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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.BorneDigueRepository;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.AvecGeometrie;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.util.SirsStringConverter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import static javafx.beans.binding.Bindings.*;

import fr.sirs.util.javafx.SpinnerUtils;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.referencing.LinearReferencing;

/**
 * Outil de calcul de position.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXPRPane extends VBox {

    public static final Image ICON_MARKER = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_MAP_MARKER,16,FontAwesomeIcons.DEFAULT_COLOR),null);

    public static enum Direction {
        Amont,
        Aval
    }

    private final NumberFormat DF = new DecimalFormat("0.###");

    //source
    @FXML private GridPane uiGrid;
    @FXML ComboBox<Preview> uiSourceTroncon;
    @FXML private ComboBox<SystemeReperage> uiSourceSR;
    @FXML private ComboBox<SystemeReperageBorne> uiSourceBorne;
    @FXML private RadioButton uiChoosePR;
    @FXML private RadioButton uiChooseCoord;
    @FXML private RadioButton uiChooseBorne;
    @FXML ToggleButton uiPickCoord;
    @FXML ToggleButton uiPickTroncon;
    @FXML private Spinner<Double> uiSourcePR;
    @FXML private Spinner<Double> uiSourceX;
    @FXML private Spinner<Double> uiSourceY;
    @FXML private Spinner<Double> uiSourceDist;
    @FXML private ChoiceBox uiSourceAmontAval;

    @FXML private Button uiCalculate;


    //target
    @FXML private ComboBox<SystemeReperage> uiTargetSR;
    @FXML private TextField uiTargetPR;
    @FXML private CheckBox uiTargetView;
    @FXML private TextField uiTargetBorneAmont;
    @FXML private TextField uiTagetBorneAmontDist;
    @FXML private TextField uiTargetBorneAval;
    @FXML private TextField uiTargetBorneAvalDist;
    @FXML private TextField uiTargetX;
    @FXML private TextField uiTargetY;

    private final ObjectProperty<Geometry> targetPoint = new SimpleObjectProperty<>();
    private final PointCalculatorHandler handler;

    public FXPRPane(PointCalculatorHandler handler, Class typeClass) {
        SIRS.loadFXML(this, Positionable.class);

        uiSourcePR.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE,0));
        uiSourceX.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE,0));
        uiSourceY.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE,0));
        uiSourceDist.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE,0));
        SpinnerUtils.commitOnFocusLost(uiSourcePR);
        SpinnerUtils.commitOnFocusLost(uiSourceX);
        SpinnerUtils.commitOnFocusLost(uiSourceY);
        SpinnerUtils.commitOnFocusLost(uiSourceDist);
        uiSourceAmontAval.setItems(FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(Direction.values())));
        uiSourceAmontAval.setValue(Direction.Aval);

        this.handler = handler;

        final SirsStringConverter sirsStringConverter = new SirsStringConverter();

        uiSourceTroncon.setConverter(sirsStringConverter);
        uiSourceSR.setConverter(sirsStringConverter);
        uiSourceBorne.setConverter(sirsStringConverter);
        uiTargetSR.setConverter(sirsStringConverter);

        final ToggleGroup group = new ToggleGroup();
        uiChoosePR.setToggleGroup(group);
        uiChooseCoord.setToggleGroup(group);
        uiChooseBorne.setToggleGroup(group);

        uiSourcePR.disableProperty().bind(uiChoosePR.selectedProperty().not());
        final BooleanBinding coordNotSelected = uiChooseCoord.selectedProperty().not();
        uiSourceX.disableProperty().bind(coordNotSelected);
        uiSourceY.disableProperty().bind(coordNotSelected);
        uiPickCoord.disableProperty().bind(coordNotSelected);
        final BooleanBinding borneNotSelected = uiChooseBorne.selectedProperty().not();
        uiSourceBorne.disableProperty().bind(borneNotSelected);
        uiSourceDist.disableProperty().bind(borneNotSelected);
        uiSourceAmontAval.disableProperty().bind(borneNotSelected);

        final BooleanBinding canCalculate = and(
                        and(uiSourceTroncon.valueProperty().isNotNull(),
                            and(uiSourceSR.valueProperty().isNotNull(),
                                uiTargetSR.valueProperty().isNotNull())),
                        or(
                            or( uiChoosePR.selectedProperty(),
                                uiChooseCoord.selectedProperty()),
                            and(uiChooseBorne.selectedProperty(),
                                uiSourceBorne.valueProperty().isNotNull())
                            )
                        );
        uiCalculate.disableProperty().bind(canCalculate.not());

        uiChoosePR.setSelected(true);

        uiSourceTroncon.valueProperty().addListener(this::tronconChange);
        uiSourceSR.valueProperty().addListener(this::sourceSRChange);


        //on remplit la liste des troncons
        final List<Preview> previews = Injector.getSession().getPreviews().getByClass(typeClass);
        uiSourceTroncon.setItems(FXCollections.observableArrayList(previews).sorted());
        uiSourceTroncon.getSelectionModel().selectFirst();

        //on met a jour la decoration si le point cible change
        targetPoint.addListener((ObservableValue<? extends Geometry> observable, Geometry oldValue, Geometry newValue) -> updateMap());
        uiTargetView.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> updateMap());

        uiPickCoord.setText(null);
        uiPickCoord.setGraphic(new ImageView(ICON_MARKER));
        uiPickTroncon.setText(null);
        uiPickTroncon.setGraphic(new ImageView(ICON_MARKER));

    }


    private void tronconChange(ObservableValue<? extends Preview> observable, Preview oldValue, Preview newValue) {
        if(newValue!=null){
            final Session session = Injector.getSession();
            final List<SystemeReperage> srs = ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).getByLinearId(newValue.getElementId());
            uiSourceSR.setItems(FXCollections.observableArrayList(srs));
            uiTargetSR.setItems(FXCollections.observableArrayList(srs));
        }else{
            uiSourceSR.setItems(FXCollections.emptyObservableList());
            uiTargetSR.setItems(FXCollections.emptyObservableList());
        }

        uiSourceSR.getSelectionModel().selectFirst();
        uiTargetSR.getSelectionModel().selectFirst();
    }

    private void sourceSRChange(ObservableValue<? extends SystemeReperage> observable, SystemeReperage oldValue, SystemeReperage newValue){
        if(newValue!=null){
            final ObservableList bornes = FXCollections.observableList(newValue.getSystemeReperageBornes());
            uiSourceBorne.setItems(bornes);
        }else{
            uiSourceBorne.setItems(FXCollections.emptyObservableList());
        }
        uiSourceBorne.getSelectionModel().selectFirst();
    }

    private void updateMap(){
        handler.getDecoration().getGeometries().clear();
        if(uiTargetView.isSelected() && targetPoint.get()!=null){
            handler.getDecoration().getGeometries().add(targetPoint.get());
        }
    }

    /**
     * TODO : PERFORM ALL POSITION COMPUTING IN A TASK
     * @param event
     */
    @FXML
    void calculate(ActionEvent event) {
        final Session session = Injector.getSession();
        final BorneDigueRepository borneRepo = InjectorCore.getBean(BorneDigueRepository.class);

        //calcule de la position geographique dans le systeme source
        final Point pt;
        if (uiChoosePR.isSelected()) {
            pt = TronconUtils.computeCoordinate(uiSourceSR.getValue(), uiSourcePR.getValue());
        } else if (uiChooseCoord.isSelected()) {
            pt = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(uiSourceX.getValue(), uiSourceY.getValue()));
            JTS.setCRS(pt, session.getProjection());
        } else if (uiChooseBorne.isSelected()) {
            final double distance;
            final Double valueObj = uiSourceDist.getValue();
            if (valueObj == null || !Double.isFinite(valueObj)) {
                distance = 0;
            } else {
                final int coef = Direction.Amont.equals(uiSourceAmontAval.getValue()) ? -1 : 1;
                distance = valueObj * coef;
            }
            pt = TronconUtils.computeCoordinate(uiSourceSR.getValue(), uiSourceBorne.getValue(), distance);
        } else {
            pt = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(0, 0));
            JTS.setCRS(pt, session.getProjection());
        }

        //calcule de la position dans le systeme cible
        session.getElement(uiSourceTroncon.getValue())
                .map(input -> ((input instanceof AvecGeometrie) ? ((AvecGeometrie) input).getGeometry() : null))
                .map(geom -> LinearReferencingUtilities.buildSegments(LinearReferencing.asLineString(geom)))
                .ifPresent(segments -> {
                    LinearReferencing.ProjectedPoint pos = LinearReferencingUtilities.projectReference(segments, pt);
                    final Point projPt = GO2Utilities.JTS_FACTORY.createPoint(pos.projected);
                    projPt.setUserData(pt.getUserData());

                    targetPoint.set(projPt);

                    uiTargetX.setText(DF.format(pos.projected.x));
                    uiTargetY.setText(DF.format(pos.projected.y));

                    //calcule du PR cible
                    final float targetPR = TronconUtils.computePR(segments, uiTargetSR.getValue(), pt, borneRepo);
                    uiTargetPR.setText(DF.format(targetPR));

                    //calcule de la position par rapport aux bornes
                    final StringConverter strCvt = new SirsStringConverter();
                    final Map.Entry<Double, SystemeReperageBorne>[] nearest = TronconUtils.findNearest(segments, uiTargetSR.getValue(), pt, borneRepo);
                    if (nearest[0] != null) {
                        uiTargetBorneAmont.setText(strCvt.toString(nearest[0].getValue()));
                        uiTagetBorneAmontDist.setText(DF.format(nearest[0].getKey()));
                    } else {
                        uiTargetBorneAmont.setText("");
                        uiTagetBorneAmontDist.setText("");
                    }
                    if (nearest[1] != null) {
                        uiTargetBorneAval.setText(strCvt.toString(nearest[1].getValue()));
                        uiTargetBorneAvalDist.setText(DF.format(nearest[1].getKey()));
                    } else {
                        uiTargetBorneAval.setText("");
                        uiTargetBorneAvalDist.setText("");
                    }
                });
    }

    @FXML
    void pickCoord(ActionEvent event) {
        uiPickTroncon.setSelected(false);
        if(uiPickCoord.isSelected()){
            handler.setPickType(2);
        }else{
            handler.setPickType(0);
        }
    }

    @FXML
    void pickTroncon(ActionEvent event) {
        uiPickCoord.setSelected(false);
        if(uiPickTroncon.isSelected()){
            handler.setPickType(1);
        }else{
            handler.setPickType(0);
        }
    }

    public void setPosition(Coordinate position) {
        uiSourceX.getValueFactory().setValue(position.x);
        uiSourceY.getValueFactory().setValue(position.y);
    }
}
