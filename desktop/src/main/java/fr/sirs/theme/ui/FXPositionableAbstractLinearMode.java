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

import com.vividsolutions.jts.geom.Geometry;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.ConvertPositionableCoordinates;
import fr.sirs.util.FormattedDoubleConverter;
import fr.sirs.util.SirsStringConverter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import org.geotoolkit.gui.javafx.util.ComboBoxCompletion;
import org.geotoolkit.referencing.LinearReferencing;

/**
 * Edition des bornes d'un {@link Positionable}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class FXPositionableAbstractLinearMode extends BorderPane implements FXPositionableMode {

    private final ObjectProperty<Positionable> posProperty = new SimpleObjectProperty<>();
    protected final BooleanProperty disableProperty = new SimpleBooleanProperty(true);
    protected LinearReferencing.SegmentInfo[] tronconSegments;

    @FXML
    protected ComboBox<SystemeReperage> uiSRs;
    @FXML
    protected ComboBox<BorneDigue> uiBorneStart;
    @FXML
    protected ComboBox<BorneDigue> uiBorneEnd;
    @FXML
    protected RadioButton uiAvalStart;
    @FXML
    protected RadioButton uiAvalEnd;
    @FXML
    protected RadioButton uiAmontStart;
    @FXML
    protected RadioButton uiAmontEnd;
    @FXML
    protected Spinner<Double> uiDistanceStart;
    @FXML
    protected Spinner<Double> uiDistanceEnd;

    //Indicatif de la coordonnée saisie/calculée.
    @FXML
    protected Label uiLinearCoordLabel;

    private boolean reseting = false;

    public void setReseting(boolean reseting) {
        this.reseting = reseting;
    }

    public FXPositionableAbstractLinearMode() {
        SIRS.loadFXML(this, Positionable.class);

        final SirsStringConverter sirsStringConverter = new SirsStringConverter();
        uiSRs.setConverter(sirsStringConverter);
        uiBorneStart.setConverter(sirsStringConverter);
        uiBorneStart.setEditable(true);
        uiBorneEnd.setConverter(sirsStringConverter);
        uiBorneEnd.setEditable(true);

        ComboBoxCompletion.autocomplete(uiBorneStart);
        ComboBoxCompletion.autocomplete(uiBorneEnd);

        uiSRs.disableProperty().bind(disableProperty);
        uiBorneStart.disableProperty().bind(disableProperty);
        uiBorneEnd.disableProperty().bind(disableProperty);
        uiAvalStart.disableProperty().bind(disableProperty);
        uiAmontStart.disableProperty().bind(disableProperty);
        uiAvalEnd.disableProperty().bind(disableProperty);
        uiAmontEnd.disableProperty().bind(disableProperty);
        uiDistanceStart.disableProperty().bind(disableProperty);
        uiDistanceEnd.disableProperty().bind(disableProperty);
        uiDistanceStart.setEditable(true);
        uiDistanceEnd.setEditable(true);

        final ToggleGroup groupStart = new ToggleGroup();
        uiAmontStart.setToggleGroup(groupStart);
        uiAvalStart.setToggleGroup(groupStart);
        uiAvalStart.setSelected(true);

        final ToggleGroup groupEnd = new ToggleGroup();
        uiAmontEnd.setToggleGroup(groupEnd);
        uiAvalEnd.setToggleGroup(groupEnd);
        uiAvalEnd.setSelected(true);

        /*
         * A converter displaying 3 decimals for numbers. Hack because of hard-coded
         * and unmodifiable decimal formats in both {@link Spinner} and {@link FXNumberCell}.
         *
         * Jira task : SYM-1133
         */
        final StringConverter conv = new FormattedDoubleConverter(new DecimalFormat("#.###"));
        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE, 0, 1);
        valueFactory.setConverter(conv);
        uiDistanceStart.setValueFactory(valueFactory);

        valueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE, 0, 1);
        valueFactory.setConverter(conv);
        uiDistanceEnd.setValueFactory(valueFactory);

        final ChangeListener<Geometry> geomListener = (observable, oldValue, newValue) -> {
            if (reseting) {
                return;
            }
            if (newValue == null) {
                throw new IllegalArgumentException("New geometry is null");
            }
            updateFields();
        };

        //Listener permettant d'indiquer si les coordonnées sont calculées ou éditées
        final ChangeListener<Boolean> updateEditedGeoCoordinatesDisplay = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            setCoordinatesLabel(oldValue, newValue);
        };
        // Listener pour les changements sur les paramètres de début et de fin, dans le cadre de l'import de bornes par exemple ou de changement dans la pojoTable.
        final ChangeListener pointListener = (obs, oldVal, newVal) -> updateFields();

        posProperty.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.geometryProperty().removeListener(geomListener);
                oldValue.editedGeoCoordinateProperty().removeListener(updateEditedGeoCoordinatesDisplay);
                oldValue.borneFinIdProperty().removeListener(pointListener);
                oldValue.borneDebutIdProperty().removeListener(pointListener);
                oldValue.borne_debut_avalProperty().removeListener(pointListener);
                oldValue.borne_debut_distanceProperty().removeListener(pointListener);
                oldValue.borne_fin_avalProperty().removeListener(pointListener);
                oldValue.borne_fin_distanceProperty().removeListener(pointListener);
            }
            if (newValue != null) {
                newValue.geometryProperty().addListener(geomListener);
                newValue.editedGeoCoordinateProperty().addListener(updateEditedGeoCoordinatesDisplay);
                setCoordinatesLabel(null, posProperty.get().getEditedGeoCoordinate());
                // Listeners to update values in the pane when they are updated via the pojoTable.
                newValue.borneFinIdProperty().addListener(pointListener);
                newValue.borneDebutIdProperty().addListener(pointListener);
                newValue.borne_debut_avalProperty().addListener(pointListener);
                newValue.borne_debut_distanceProperty().addListener(pointListener);
                newValue.borne_fin_avalProperty().addListener(pointListener);
                newValue.borne_fin_distanceProperty().addListener(pointListener);
                updateFields();
            }
        });

        uiSRs.getSelectionModel().selectedItemProperty().addListener(this::srsChange);

        final ChangeListener chgListener = (ObservableValue observable, Object oldValue, Object newValue) -> coordChange(false);
        final ChangeListener borneChgListener = (ObservableValue observable, Object oldValue, Object newValue) -> coordChange(true);
        groupStart.selectedToggleProperty().addListener(chgListener);
        groupEnd.selectedToggleProperty().addListener(chgListener);
        uiBorneStart.valueProperty().addListener(borneChgListener);
        uiBorneEnd.valueProperty().addListener(borneChgListener);
        uiDistanceStart.valueProperty().addListener(chgListener);
        uiDistanceEnd.valueProperty().addListener(chgListener);
    }

    /**
     * Méthode permettant de mettre à jour le label (FXML) indiquant si les
     * coordonnées du mode ont été calculées ou éditées.
     *
     * @param oldEditedGeoCoordinate ancienne valeur de la propriété
     * editedGeoCoordinate du positionable courant. Null si on l'ignore.
     * @param newEditedGeoCoordinate nouvelle valeur.
     */
    protected void setCoordinatesLabel(Boolean oldEditedGeoCoordinate, Boolean newEditedGeoCoordinate){
        if (newEditedGeoCoordinate == null) {
            uiLinearCoordLabel.setText(COORDS_TYPE_UNKNOWN);
            return;
        } else if (newEditedGeoCoordinate.equals(oldEditedGeoCoordinate)) {
            return;
        }
        if (uiLinearCoordLabel != null) { //Occurred for vegetation
            if (newEditedGeoCoordinate) {
                uiLinearCoordLabel.setText(COORDS_TYPE_COMPUTED);
            } else {
                uiLinearCoordLabel.setText(COORDS_TYPE_ENTERED);
            }
        }
    }

    @Override
    public Node getFXNode() {
        return this;
    }

    @Override
    public String getTitle() {
        return "Borne";
    }

    @Override
    public ObjectProperty<Positionable> positionableProperty() {
        return posProperty;
    }

    @Override
    public BooleanProperty disablingProperty() {
        return disableProperty;
    }

    /**
     * Cette méthode ne doit s'occuper que de mettre à jour les champs et non de
     * la mise à jour de l'information géométrique du positionable.
     */
    @Override
    public void updateFields() {
        updateFields(false);
    }

    /**
     * Le paramètre isBorne est nécéssaire pour empécher l'écrasement de la
     * liste calculée par l'autocomplétion, qui est déclenché juste avant la
     * mise à jour des champs. isBorne a pour valeur vrai uniquement pour les
     * listeners des champs borneDebut et borneFin.
     * @param isBorne
     */
    private void updateFields(final boolean isBorne) {
        reseting = true;
        try {
            final Positionable pos = posProperty.get();
            final String mode = pos.getGeometryMode();

            final TronconDigue t = ConvertPositionableCoordinates.getTronconFromPositionable(pos);
            final SystemeReperageRepository srRepo = (SystemeReperageRepository) Injector.getSession().getRepositoryForClass(SystemeReperage.class);
            final SystemeReperage defaultSR;
            boolean sameSR = false;
            if (pos.getSystemeRepId() != null) {
                defaultSR = srRepo.get(pos.getSystemeRepId());
                sameSR = true;
            } else if (t.getSystemeRepDefautId() != null) {
                defaultSR = srRepo.get(t.getSystemeRepDefautId());
            } else {
                defaultSR = null;
            }
            uiSRs.setValue(defaultSR);

            /*
            Init list of bornes and SRs : must be done all the time to allow the user
            to change/choose the positionable SR and bornes among list elements.
             */
            final Map<String, BorneDigue> borneMap = initSRBorneLists(t, defaultSR, isBorne);

            if ((mode == null || getID().equals(mode)) && sameSR) {
                //on assigne les valeurs sans changement
                uiAmontStart.setSelected(pos.getBorne_debut_aval());
                uiAvalStart.setSelected(!pos.getBorne_debut_aval());
                uiAmontEnd.setSelected(pos.getBorne_fin_aval());
                uiAvalEnd.setSelected(!pos.getBorne_fin_aval());

                uiDistanceStart.getValueFactory().setValue(pos.getBorne_debut_distance());
                uiDistanceEnd.getValueFactory().setValue(pos.getBorne_fin_distance());

                uiBorneStart.valueProperty().set(borneMap.get(pos.borneDebutIdProperty().get()));
                uiBorneEnd.valueProperty().set(borneMap.get(pos.borneFinIdProperty().get()));

            } else if (pos.getGeometry() != null && pos.getBorneDebutId() != null) {
                    //on calcule les valeurs en fonction des points de debut et fin
                    final TronconUtils.PosInfo ps = new TronconUtils.PosInfo(pos, t);
                    updateFromPosSRInfo(defaultSR, ps);
            } else {
                updateWithDefaultValues();
            }

        } finally {
            reseting = false;
        }
    }

    /**
     * Init SRs, borneStart and borneEnd UI lists.
     *
     * Return a map of BorneDigue accessible by their id.
     *
     * @param t
     * @param defaultSR
     * @return
     */
    protected Map<String, BorneDigue> initSRBorneLists(final TronconDigue t, final SystemeReperage defaultSR, final boolean isBorne) {
        final List<SystemeReperage> srs = ((SystemeReperageRepository) Injector.getSession().getRepositoryForClass(SystemeReperage.class)).getByLinear(t);
        uiSRs.setItems(SIRS.observableList(srs));
        uiSRs.getSelectionModel().select(defaultSR);

        // Init list of bornes
        final Map<String, BorneDigue> borneMap = new HashMap<>();
        ObservableList<BorneDigue> bornes = FXCollections.observableArrayList();
        if (defaultSR != null) {
            final AbstractSIRSRepository<BorneDigue> borneRepo = Injector.getSession().getRepositoryForClass(BorneDigue.class);
            for (SystemeReperageBorne srb : defaultSR.systemeReperageBornes) {
                borneMap.put(srb.getBorneId(), borneRepo.get(srb.getBorneId()));
            }
            bornes.addAll(borneMap.values());
        }
        if (!isBorne) {
            bornes = bornes.sorted(new BorneComparator());
            uiBorneStart.setItems(bornes);
            uiBorneEnd.setItems(bornes);
        }
        return borneMap;
    }

    /**
     * Cette méthode ne doit s'occuper que de la mise à jour de l'information
     * géométrique du positionable et non de la mise à jour des champs.
     */
    @Override
    public void buildGeometry() {
        //sauvegarde des propriétés
        final Positionable positionable = posProperty.get();

        // On ne met la géométrie à jour depuis ce panneau que si on est dans son mode.
        if (!getID().equals(positionable.getGeometryMode())) {
            return;
        }

        setValuesFromUi(positionable);

        //on recalcule la geométrie et les coordonnées Géo du positionable.
        ConvertPositionableCoordinates.computePositionableGeometryAndCoord(positionable);
    }

    public void setValuesFromUi(final Positionable positionable) {
        final SystemeReperage sr = uiSRs.getValue();
        final BorneDigue startBorne = uiBorneStart.getValue();
        final BorneDigue endBorne = uiBorneEnd.getValue();
        positionable.setSystemeRepId(sr == null ? null : sr.getDocumentId());
        positionable.setBorneDebutId(startBorne == null ? null : startBorne.getDocumentId());
        positionable.setBorneFinId(endBorne == null ? null : endBorne.getDocumentId());
        positionable.setBorne_debut_aval(uiAmontStart.isSelected());
        positionable.setBorne_fin_aval(uiAmontEnd.isSelected());
        positionable.setBorne_debut_distance(uiDistanceStart.getValue());
        positionable.setBorne_fin_distance(uiDistanceEnd.getValue());
    }

    protected void coordChange(final boolean isBorne) {
        if (reseting) return;
        reseting = true;
        try {
            buildGeometry();
            posProperty.get().setEditedGeoCoordinate(Boolean.FALSE);
        } catch (Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Echec de la construction de la géométrie lors du changement de coordonnées.", e);
        } finally {
            // Disjonction nécéssaire pour éviter l'écrasement de la liste
            // calculée par l'autocomplétion des champs borneDebut et borneFin.
            if (isBorne) {
                updateFields(true);
            } else {
                updateFields();
            }
            reseting = false;
        }
    }

    protected void srsChange(ObservableValue<? extends SystemeReperage> observable,
            SystemeReperage oldValue, SystemeReperage newSR) {
        if (reseting) {
            return;
        }

        reseting = true;
        try {
            // Mise à jour de la liste des bornes
            final ArrayList<BorneDigue> bornes = new ArrayList<>();
            final AbstractSIRSRepository<BorneDigue> borneRepo = Injector.getSession().getRepositoryForClass(BorneDigue.class);
            for (final SystemeReperageBorne srb : newSR.systemeReperageBornes) {
                final BorneDigue bd = borneRepo.get(srb.getBorneId());
                if (bd != null) {
                    bornes.add(bd);
                }
            }

            final ObservableList<BorneDigue> observableBornes = FXCollections.observableList(bornes).sorted(new BorneComparator());
            uiBorneStart.setItems(observableBornes);
            uiBorneEnd.setItems(observableBornes);

            //calcul de la position relative dans le nouveau SR
            try {
                //on calcule les valeurs en fonction des points de debut et fin
                final TronconUtils.PosInfo ps = new TronconUtils.PosInfo(positionableProperty().get());
                updateFromPosSRInfo(newSR, ps);
            } catch (Exception e) {
                //pas de geometrie
                updateWithDefaultValues();
            }

            buildGeometry();
        } finally {
            reseting = false;
        }
    }

    private void updateWithDefaultValues() {
        uiAvalStart.setSelected(true);
        uiAmontStart.setSelected(false);
        uiDistanceStart.getValueFactory().setValue(0.0);
        uiBorneStart.getSelectionModel().clearSelection();

        uiAvalEnd.setSelected(true);
        uiAmontEnd.setSelected(false);
        uiDistanceEnd.getValueFactory().setValue(0.0);
        uiBorneEnd.getSelectionModel().clearSelection();
    }

    void updateFromPosSRInfo(SystemeReperage newSR, TronconUtils.PosInfo ps) {
        final TronconUtils.PosSR rp = ps.getForSR(newSR);

        uiAvalStart.setSelected(!rp.startAval);
        uiAmontStart.setSelected(rp.startAval);
        uiDistanceStart.getValueFactory().setValue(rp.distanceStartBorne);
        uiBorneStart.getSelectionModel().select(rp.borneDigueStart);

        uiAvalEnd.setSelected(!rp.endAval);
        uiAmontEnd.setSelected(rp.endAval);
        uiDistanceEnd.getValueFactory().setValue(rp.distanceEndBorne);
        uiBorneEnd.getSelectionModel().select(rp.borneDigueEnd);
    }

    /**
     * Return the Linear geometry on which the input {@link SystemeReperage} is
     * based on.
     *
     * @param source The SR to get linear for. If null, we'll try to get tronçon
     * geometry of the currently edited {@link Positionable}.
     * @return The linear associated, or null if we cannot get it.
     */
    protected LinearReferencing.SegmentInfo[] getSourceLinear(final SystemeReperage source) {
        if (tronconSegments == null) {
            final Positionable positionable = posProperty.get();
            final TronconDigue t = ConvertPositionableCoordinates.getTronconFromPositionable(positionable);
            tronconSegments = LinearReferencingUtilities.getSourceLinear(t, source);
        }
        return tronconSegments;
    }

    private static class BorneComparator implements Comparator<BorneDigue> {

        @Override
        public int compare(BorneDigue o1, BorneDigue o2) {
            if (o1 == null) {
                return o2 == null ? 0 : 1;
            }

            if (o1.getLibelle() == null) {
                if (o2.getLibelle() == null) {
                    if (o1.getDesignation() == null) {
                        if (o2.getDesignation() == null) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else if (o2.getDesignation() == null) {
                        return -1;
                    } else {
                        return o1.getDesignation().compareTo(o2.getDesignation());
                    }
                } else {
                    return 1;
                }
            }

            return o1.getLibelle().compareTo(o2.getLibelle());
        }

    }
}
