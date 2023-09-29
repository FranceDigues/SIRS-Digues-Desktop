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
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.RefSousTraitementVegetation;
import fr.sirs.core.model.RefTraitementVegetation;
import fr.sirs.core.model.TraitementZoneVegetation;
import static fr.sirs.plugin.vegetation.PluginVegetation.sousTypeTraitementFromTypeTraitementId;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXTraitementZoneVegetationPane extends FXTraitementZoneVegetationPaneStub {

    boolean frequencyChanged = false;
    String firstFrequencyId = "";
    
    public FXTraitementZoneVegetationPane(final TraitementZoneVegetation traitementZoneVegetation){
        super(traitementZoneVegetation);

        ui_typeTraitementId.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(newValue instanceof RefTraitementVegetation){
                    final List<RefSousTraitementVegetation> sousTraitements = sousTypeTraitementFromTypeTraitementId(((RefTraitementVegetation) newValue).getId());
                    SIRS.initCombo(ui_sousTypeTraitementId, FXCollections.observableList(sousTraitements), null);
                } else {
                    SIRS.initCombo(ui_sousTypeTraitementId, FXCollections.emptyObservableList(), null);
                }
            }
        });

        ui_typeTraitementPonctuelId.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if(newValue instanceof RefTraitementVegetation){
                    final List<RefSousTraitementVegetation> sousTraitements = sousTypeTraitementFromTypeTraitementId(((RefTraitementVegetation) newValue).getId());
                    SIRS.initCombo(ui_sousTypeTraitementPonctuelId, FXCollections.observableList(sousTraitements), null);
                } else {
                    SIRS.initCombo(ui_sousTypeTraitementPonctuelId, FXCollections.emptyObservableList(), null);
                }
            }
        });

        ui_frequenceId.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            if(!frequencyChanged && oldValue instanceof Preview){
                firstFrequencyId = ((Preview) oldValue).getElementId();
                frequencyChanged=true;
            }
                });
    }


    @Override
    protected void initFields(ObservableValue<? extends TraitementZoneVegetation > observableElement, TraitementZoneVegetation oldElement, TraitementZoneVegetation newElement) {
        super.initFields(observableElement, oldElement, newElement);

        final AbstractSIRSRepository<RefSousTraitementVegetation> repoSousTraitements = Injector.getSession().getRepositoryForClass(RefSousTraitementVegetation.class);
        final List<RefSousTraitementVegetation> sousTraitements = sousTypeTraitementFromTypeTraitementId(newElement.getTypeTraitementId());
        final List<RefSousTraitementVegetation> sousTraitementPonctuels = sousTypeTraitementFromTypeTraitementId(newElement.getTypeTraitementPonctuelId());
        final RefSousTraitementVegetation currentSousTraitement = newElement.getSousTypeTraitementId() == null ? null : repoSousTraitements.get(newElement.getSousTypeTraitementId());
        final RefSousTraitementVegetation currentSousTraitementPonctuel = newElement.getSousTypeTraitementPonctuelId() == null ? null : repoSousTraitements.get(newElement.getSousTypeTraitementPonctuelId());
        SIRS.initCombo(ui_sousTypeTraitementId, FXCollections.observableList(sousTraitements), currentSousTraitement);
        SIRS.initCombo(ui_sousTypeTraitementPonctuelId, FXCollections.observableList(sousTraitementPonctuels), currentSousTraitementPonctuel);
    }

    /*
    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    IMPORTANT
    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    La modification d'une donnée dans un traitemetn de zone de végétation est
    susceptible d'avoir un effet sur la planification d'une parcelle en mode
    auto.

    MAIS !

    Il est à noter que la planification, tout en étant en mode auto, peut avoir
    été personnalisée !

    exemple : si la planification est de 5 ans en 5 ans à partir de 2015
    (prochain traitement auto-calculé en 2020), on peut avoir forcé la planif à
    partir de 2016 (ce qui implique un prochain traitement auto-calculé en 2021)

    Ainsi, même en mode automatique, on ne peut pas être au courant des souhaits
    de l'utilisateur à vouloir ou pas garder sa planification courante.

    IL FAUT DONC LAISSER L'UTILISATEUR GÉRER SA PLANIFICATION COMME IL L'ENTEND.

    LES MODIFICATIONS RELATIVES AU TRAITEMENT DE LA ZONE DE VÉGÉTATION QUI
    AURAIENT ÉTÉ APPORTÉES VIA CE PANNEAU SERONT PRISES EN COMPTE LORS DES
    MODIFICATIONS POSTÉRIEURES DE LA PLANIFICATION DES PARCELLES.
    
     */

//    @Override
//    public void preSave(){
//        super.preSave();
//
//        final TraitementZoneVegetation traitement = elementProperty.get();
//        if(traitement!=null){
//            final Element parent = traitement.getParent();
//            if(parent instanceof ZoneVegetation && !(parent instanceof InvasiveVegetation)){
//                final Alert alert = new Alert(Alert.AlertType.INFORMATION,
//                        "La planification d'une parcelle doit être mise à jour.",
//                        ButtonType.OK);
//                alert.setResizable(true);
//                final Optional<ButtonType> result = alert.showAndWait();
//                if(result.isPresent() && result.get()==ButtonType.OK){
//                    updateParcelleAutoPlanif((ZoneVegetation) parent);
//                }
//            }
//        }
//
//    }


//    @Override
//    public void preSave(){
//        super.preSave();
//
//        if(ui_frequenceId.getSelectionModel().getSelectedItem() instanceof Preview){
//            final String choosenFrequencyId = ((Preview) ui_frequenceId.getSelectionModel().getSelectedItem()).getElementId();
//            if(frequencyChanged && !Objects.equals(firstFrequencyId, choosenFrequencyId)){
//                final TraitementZoneVegetation traitement = elementProperty.get();
//                if(traitement!=null){
//                    final Element parent = traitement.getParent();
//                    if(parent instanceof ZoneVegetation && !(parent instanceof InvasiveVegetation)){
//                        final Alert alert = new Alert(Alert.AlertType.INFORMATION,
//                                "Il semblerait que la fréquence ait été modifiée.\n"
//                                        + "Voulez-vous mettre à jour la planification de la parcelle à partir de l'année courante ?",
//                                ButtonType.YES, ButtonType.NO);
//                        alert.setResizable(true);
//                        final Optional<ButtonType> result = alert.showAndWait();
//                        if(result.isPresent() && result.get()==ButtonType.YES){
//                            updateParcelleAutoPlanif((ZoneVegetation) parent);
//                        }
//                    }
//                }
//            }
//        }
//    }
}
