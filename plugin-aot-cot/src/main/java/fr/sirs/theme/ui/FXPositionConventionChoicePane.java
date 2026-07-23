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

import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.SirsStringConverter;
import static java.lang.Double.max;
import static java.lang.Double.min;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

import fr.sirs.util.javafx.SpinnerUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.layout.BorderPane;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.referencing.LinearReferencing;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXPositionConventionChoicePane extends BorderPane {

    @FXML private ComboBox<Preview> ui_linear;
    @FXML private ComboBox<Class<? extends Objet>> ui_types;
    @FXML private ComboBox<SystemeReperage> ui_sr;

    @FXML private Spinner<Double> ui_prDebut;
    @FXML private Label ui_prDebutComputed;
    @FXML private Spinner<Double> ui_prFin;
    @FXML private Label ui_prFinComputed;

    @FXML private ListView<? extends Objet> ui_list;

    private final Session session = Injector.getSession();
    private final Previews previews = session.getPreviews();
    private final SystemeReperageRepository sr_repo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);
    private final AbstractSIRSRepository<BorneDigue> borneRepo = session.getRepositoryForClass(BorneDigue.class);
    private final SirsStringConverter converter = new SirsStringConverter();

    private TronconDigue currentLinear = null;
    private ObservableList<Objet> objetList;
    private final ObjectProperty<Objet> selectedObjetProperty = new SimpleObjectProperty<>();
    private final DoubleProperty prDebutProperty = new SimpleDoubleProperty();
    private final DoubleProperty prFinProperty = new SimpleDoubleProperty();
//    private final Predicate<Objet> inclusivePrPredicate = (Objet t) -> t.getPrDebut()<prDebutProperty.doubleValue() && t.getPrFin()>prFinProperty.doubleValue();
    private final Predicate<Objet> exclusivePrPredicate = (Objet t) -> t.getPrDebut()>prDebutProperty.doubleValue() && t.getPrFin()<prFinProperty.doubleValue();

    public ObjectProperty<Objet> selectedObjetProperty(){return selectedObjetProperty;}
    public DoubleProperty prDebutProperty(){return prDebutProperty;}
    public DoubleProperty prFinProperty(){return prFinProperty;}

    private class PrChangeListener implements ChangeListener<Double> {

        private final DoubleProperty pr;
        private final Label ui_prComputed;

        private PrChangeListener(DoubleProperty pr, Label ui_prComputed){
            this.pr = pr;
            this.ui_prComputed = ui_prComputed;
        }

        @Override
        public void changed(ObservableValue<? extends Double> observable, Double oldValue, Double newValue) {

            final Class<? extends Objet> selectedClass = ui_types.getSelectionModel().getSelectedItem();
            final SystemeReperage initialSR = ui_sr.getSelectionModel().getSelectedItem();
            final SystemeReperage targetSR = sr_repo.get(currentLinear.getSystemeRepDefautId());

            if(selectedClass!=null && newValue!=null && initialSR!=null && targetSR!=null){
                final AbstractSIRSRepository repo = session.getRepositoryForClass(selectedClass);
                if(repo instanceof AbstractPositionableRepository){
                    if(targetSR.equals(initialSR)){
                        pr.set(newValue);
                    }
                    else {
                        final LinearReferencing.SegmentInfo[] segments = LinearReferencingUtilities.buildSegments(LinearReferencing.asLineString(currentLinear.getGeometry()));
                        pr.set(TronconUtils.switchSRForPR(segments, newValue, initialSR, targetSR, borneRepo));
                    }

                    ui_prComputed.setText(String.format("%.2f", pr.get()));

                    if(objetList!=null && !objetList.isEmpty()){
                        ui_list.setItems((ObservableList) objetList.filtered(exclusivePrPredicate));
                    } else {
                        ui_list.setItems(FXCollections.emptyObservableList());
                    }

                } else {
                    SIRS.LOGGER.log(Level.WARNING, "Repo not positionable");
                    ui_list.setItems(FXCollections.emptyObservableList());
                }
            } else {
                SIRS.LOGGER.log(Level.WARNING, "Impossible de calculer les objets concernés");
                ui_list.setItems(FXCollections.emptyObservableList());
            }
        }

    }


    public FXPositionConventionChoicePane() {

        SIRS.loadFXML(this, (Class)null);

        final List<Class<? extends Objet>> concreteObjetClasses = Session.getConcreteSubTypes(Objet.class);

        ui_types.setItems(FXCollections.observableArrayList(concreteObjetClasses));
        ui_types.setConverter(converter);
        ui_types.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends Class<? extends Objet>> observable, Class<? extends Objet> oldValue, Class<? extends Objet> newValue) ->
                {
                    updateObjetList(newValue, currentLinear);
                });

        SIRS.initCombo(ui_linear, FXCollections.observableArrayList(previews.getByClass(TronconDigue.class)), null);
        ui_linear.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Preview>() {

            @Override
            public void changed(ObservableValue<? extends Preview> observable, Preview oldValue, Preview newValue) {

                try {
                    // Mise à jour de la liste des SRs
                    currentLinear = Injector.getSession().getRepositoryForClass(TronconDigue.class).get(newValue.getElementId());
                    if(currentLinear!=null){
                        SIRS.initCombo(ui_sr, FXCollections.observableArrayList(sr_repo.getByLinear(currentLinear)), null);

                        // Mise à jour de la liste des objets
                        updateObjetList(ui_types.getSelectionModel().getSelectedItem(), currentLinear);
                    }

                } catch (Exception e) {
                    SIRS.LOGGER.log(Level.WARNING, "SR document not found. {0}", e);
                }
            }
        });

        ui_prDebut.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0, 1));
        ui_prFin.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-Double.MAX_VALUE, Double.MAX_VALUE, 0, 1));
        SpinnerUtils.commitOnFocusLost(ui_prDebut);
        SpinnerUtils.commitOnFocusLost(ui_prFin);

        ui_sr.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SystemeReperage>() {

            @Override
            public void changed(ObservableValue<? extends SystemeReperage> observable, SystemeReperage oldValue, SystemeReperage newValue) {
                if(currentLinear!=null){
                    final LinearReferencing.SegmentInfo[] segments = LinearReferencingUtilities.buildSegments(LinearReferencing.asLineString(currentLinear.getGeometry()));
                    final Point pt0 = GO2Utilities.JTS_FACTORY.createPoint(segments[0].segmentCoords[0]);
                    final double pr0 = TronconUtils.computePR(segments, newValue, pt0, borneRepo);
                    final Point pt1 = GO2Utilities.JTS_FACTORY.createPoint(segments[segments.length - 1].segmentCoords[1]);
                    final double pr1 = TronconUtils.computePR(segments, newValue, pt1, borneRepo);

                    ui_prDebut.getValueFactory().setValue(min(pr0, pr1));
                    ui_prFin.getValueFactory().setValue(max(pr0, pr1));
                }
            }
        });

        ui_prDebut.valueProperty().addListener(new PrChangeListener(prDebutProperty, ui_prDebutComputed));
        ui_prFin.valueProperty().addListener(new PrChangeListener(prFinProperty, ui_prFinComputed));
        ui_prDebut.setEditable(true); ui_prFin.setEditable(true);
        ui_list.setCellFactory(ComboBoxListCell.forListView(converter));
        selectedObjetProperty.bind(ui_list.getSelectionModel().selectedItemProperty());
    }


    private void updateObjetList(final Class<? extends Objet> clazz, final TronconDigue linear){

        if(clazz==null) {
            objetList = FXCollections.emptyObservableList();
        }

        else {
            final AbstractSIRSRepository repo = Injector.getSession().getRepositoryForClass(clazz);
            if(repo instanceof AbstractPositionableRepository){
                final AbstractPositionableRepository positionableRepo = (AbstractPositionableRepository) repo;
                final List result;
                if(linear!=null){
                    result = positionableRepo.getByLinear(linear);
                }
                else {
                    result = positionableRepo.getAll();
                }
                objetList = FXCollections.observableList(result);
            }
            else {
                SIRS.LOGGER.log(Level.WARNING, "Repo not positionable");
                objetList = FXCollections.emptyObservableList();
            }
        }
        ui_list.setItems((ObservableList) objetList);
    }
}
