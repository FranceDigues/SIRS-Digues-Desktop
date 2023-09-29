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
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.TraitAmenagementHydraulique;
import fr.sirs.util.SirsStringConverter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;

/**
 * Copy of the class FXTraitBerge.
 *
 * @author Maxime Gavens (Geomatys)
 */
public class FXTraitAmenagementHydraulique extends GridPane{

    private static final String MESSAGE_AH = "Sélectionner un aménagement hydraulique sur la carte.";
    private static final String MESSAGE_TRAIT = "Sélectionner un trait d'aménagement hydraulique sur la carte ou cliquer sur nouveau.";
    private static final String MESSAGE_TRAIT_IMPORT = "Sélectionner une géometrie à convertir en trait d'aménagement hydraulique sur la carte.";
    private static final String MESSAGE_TRAIT_CREATE = "Cliquer sur la carte pour créer la géométrie, double-click pour terminer la creation.";

    @FXML private Label uiLblAmenagementHydraulique;
    @FXML private Label uiLblTrait;
    @FXML private DatePicker uiDateDebut;
    @FXML private DatePicker uiDateFin;
    @FXML private Button uiBtnDelete;
    @FXML private Button uiBtnSave;
    @FXML private ToggleButton uiBtnNew;

    private final BooleanProperty importProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<AmenagementHydraulique> amenagementHydrauliqueProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<TraitAmenagementHydraulique> traitProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<FXMap> mapProperty = new SimpleObjectProperty<>();

    public FXTraitAmenagementHydraulique(){
        SIRS.loadFXML(this);

        getStylesheets().add(SIRS.CSS_PATH);
        getStyleClass().add("blue-light");

        uiLblAmenagementHydraulique.setText(MESSAGE_AH);
        amenagementHydrauliqueProperty.addListener(new ChangeListener<AmenagementHydraulique>() {
            @Override
            public void changed(ObservableValue<? extends AmenagementHydraulique> observable, AmenagementHydraulique oldValue, AmenagementHydraulique newValue) {
                if(newValue!=null){
                    uiLblAmenagementHydraulique.setText(new SirsStringConverter().toString(newValue));
                    uiLblTrait.setText(importProperty.get() ? MESSAGE_TRAIT_IMPORT :MESSAGE_TRAIT );
                }else{
                    uiLblAmenagementHydraulique.setText(MESSAGE_AH);
                    uiLblTrait.setText("");
                }
            }
        });

        uiLblTrait.setText("");
        traitProperty.addListener(new ChangeListener<TraitAmenagementHydraulique>() {
            @Override
            public void changed(ObservableValue<? extends TraitAmenagementHydraulique> observable, TraitAmenagementHydraulique oldValue, TraitAmenagementHydraulique newValue) {
                if(newValue!=null){
                    uiLblTrait.setText(new SirsStringConverter().toString(newValue));
                    uiDateDebut.setValue(newValue.getDate_debut());
                    uiDateFin.setValue(newValue.getDate_fin());
                }else{
                    uiLblTrait.setText(importProperty.get() ? MESSAGE_TRAIT_IMPORT :MESSAGE_TRAIT );
                }
            }
        });

        uiBtnNew.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(newValue){
                    uiLblTrait.setText(MESSAGE_TRAIT_CREATE);
                }else{
                    uiLblTrait.setText(MESSAGE_TRAIT);
                }
            }
        });

        uiBtnNew.disableProperty().bind(amenagementHydrauliqueProperty.isNull().or(traitProperty.isNotNull()).or(uiBtnNew.selectedProperty()));
        uiBtnNew.visibleProperty().bind(importProperty.not());

        uiDateDebut.disableProperty().bind(traitProperty.isNull());
        uiDateFin.disableProperty().bind(traitProperty.isNull());
        uiBtnSave.disableProperty().bind(traitProperty.isNull());
        uiBtnDelete.disableProperty().bind(traitProperty.isNull());

    }

    public BooleanProperty importProperty(){
        return importProperty;
    }

    public ObjectProperty<AmenagementHydraulique> amenagementHydrauliqueProperty(){
        return amenagementHydrauliqueProperty;
    }

    public ObjectProperty<TraitAmenagementHydraulique> traitProperty(){
        return traitProperty;
    }

    public ObjectProperty<FXMap> mapProperty(){
        return mapProperty;
    }

    public BooleanProperty newProperty(){
        return uiBtnNew.selectedProperty();
    }

    @FXML
    void delete(ActionEvent event) {
        final TraitAmenagementHydraulique traitAmenagementHydraulique = traitProperty.get();
        final AbstractSIRSRepository<TraitAmenagementHydraulique> repo = Injector.getSession().getRepositoryForClass(TraitAmenagementHydraulique.class);
        if(!traitAmenagementHydraulique.isNew()){
            repo.remove(traitAmenagementHydraulique);
        }
        endEdition();
    }

    @FXML
    void save(ActionEvent event) {
        final TraitAmenagementHydraulique traitAmenagementHydraulique = traitProperty.get();
        traitAmenagementHydraulique.setDate_debut(uiDateDebut.getValue());
        traitAmenagementHydraulique.setDate_fin(uiDateFin.getValue());
        traitAmenagementHydraulique.setAmenagementHydrauliqueId(amenagementHydrauliqueProperty.get() == null ? null : amenagementHydrauliqueProperty.get().getId());
        final AbstractSIRSRepository<TraitAmenagementHydraulique> repo = Injector.getSession().getRepositoryForClass(TraitAmenagementHydraulique.class);
        if (traitAmenagementHydraulique.isNew()){
            repo.add(traitAmenagementHydraulique);
        } else {
            repo.update(traitAmenagementHydraulique);
        }

        endEdition();
    }

    private void endEdition(){
        final FXMap map = mapProperty.get();
        if(map != null){
            map.setHandler(new FXPanHandler(true));
        }
    }
}
