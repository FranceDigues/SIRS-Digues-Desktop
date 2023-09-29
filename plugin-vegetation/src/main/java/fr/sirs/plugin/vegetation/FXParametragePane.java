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
package fr.sirs.plugin.vegetation;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.AbstractZoneVegetationRepository;
import fr.sirs.core.component.ParcelleVegetationRepository;
import fr.sirs.core.component.PlanVegetationRepository;
import fr.sirs.core.model.ParcelleVegetation;
import fr.sirs.core.model.PlanVegetation;
import fr.sirs.core.model.ZoneVegetation;
import fr.sirs.theme.ui.FXPlanVegetationPane;
import fr.sirs.ui.Growl;
import fr.sirs.util.SirsStringConverter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.elasticsearch.common.joda.time.LocalDate;


/**
 * Panneau de paramétrage des plans après sélection dans une liste des plans disponibles. Ce panneau est accessible par :
 * 
 * "Plan de gestion" > "paramétrage"
 * 
 * La sélection d'un {@link PlanVegetation} dans la liste provoque l'édition du plan dans un panneau {@link FXPlanVegetationPane}.
 *
 * @author Johann Sorel (Geomatys)
 * @author Samuel Andrés (Geomatys)
 * 
 * @see FXPlanVegetationPane
 */
public class FXParametragePane extends SplitPane {

    @FXML private ListView<PlanVegetation> uiPlanList;
    @FXML private Button uiAdd;
    @FXML private Button uiDuplicate;
    @FXML private Button uiDelete;

    private final Session session = Injector.getSession();
    private final PlanVegetationRepository planRepo = VegetationSession.INSTANCE.getPlanRepository();
    private final ParcelleVegetationRepository parcelleRepo = (ParcelleVegetationRepository) session.getRepositoryForClass(ParcelleVegetation.class);
    private final SirsStringConverter converter = new SirsStringConverter();
    

    public FXParametragePane() {
        SIRS.loadFXML(this);
        setDividerPositions(.25);
        initialize();
    }

    private void initialize() {
        final BorderPane pane = new BorderPane();
        this.getItems().add(pane);

        uiPlanList.setItems(FXCollections.observableList(planRepo.getAll()));
        
        uiPlanList.setCellFactory((ListView<PlanVegetation> param)-> new UpdatableListCell());
        
        uiPlanList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        uiPlanList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<PlanVegetation>() {
            @Override
            public void changed(ObservableValue<? extends PlanVegetation> observable, PlanVegetation oldValue, PlanVegetation newValue) {
                if(newValue!=null){
                    SIRS.fxRun(false, ()->pane.setCenter(new FXPlanVegetationPane(newValue)));
                }
            }
        });

        VegetationSession.INSTANCE.planProperty().addListener(new WeakChangeListener<>(new ChangeListener<PlanVegetation>() {

            @Override
            public void changed(ObservableValue<? extends PlanVegetation> observable, PlanVegetation oldValue, PlanVegetation newValue) {
                refreshPlanList();
            }
        }));
        
        uiAdd.setOnAction(this::planAdd);
        uiAdd.setGraphic(new ImageView(SIRS.ICON_ADD_WHITE));
        
        uiDuplicate.disableProperty().bind(uiPlanList.getSelectionModel().selectedItemProperty().isNull());
        uiDuplicate.setOnAction(this::planDuplicate);
        uiDuplicate.setGraphic(new ImageView(SIRS.ICON_COPY_WHITE));
        
        uiDelete.setOnAction(this::planDelete);
        uiDelete.setGraphic(new ImageView(SIRS.ICON_TRASH_WHITE));
    }

    /**
     * Updates the planification list.
     */
    private void refreshPlanList() {
        ObservableList<PlanVegetation> items = uiPlanList.getItems();
        uiPlanList.setItems(FXCollections.emptyObservableList()); // Pour obliger la liste à rafraîchir même les éléments qui semblent n'avoir pas "bougé" (de manière à forcer la vérification du plan actif).
        uiPlanList.setItems(FXCollections.observableList(items));
    }

    /**
     * Creates a new planification.
     * 
     * @param event
     */
    @FXML
    void planAdd(ActionEvent event) {
        try{
            final PlanVegetation newPlan = planRepo.create();

            // Par défaut, on crée le plan commençant à l'année courante pour une durée de dix ans.
            newPlan.setAnneeDebut(LocalDate.now().getYear());
            newPlan.setAnneeFin(LocalDate.now().getYear()+10);

            planRepo.add(newPlan);
            uiPlanList.getItems().add(newPlan);
            refreshPlanList();

            final Growl growlInfo = new Growl(Growl.Type.INFO, "Le plan a été créé.");
            growlInfo.showAndFade();
        }
        catch(Exception e){
            SIRS.LOGGER.log(Level.WARNING, e.getMessage());
            final Growl growlInfo = new Growl(Growl.Type.ERROR, "Une erreur est survenue lors de la création du plan.");
            growlInfo.showAndFade();
        }
    }

    /**
     * Duplicates a planification.
     *
     * @param event
     */
    void planDuplicate(ActionEvent event) {
        final PlanVegetation toDuplicate = uiPlanList.getSelectionModel().getSelectedItem();
        if(toDuplicate!=null){

            try{
                // Vérification des intentions de l'utilisateur.
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment dupliquer le plan "+converter.toString(toDuplicate)+" ?\n"
                        + "Cette opération dupliquera les parcelles de ce plan, leurs zones de végétation, ainsi que tous les paramétrages liés aux plans et aux parcelles.", ButtonType.YES, ButtonType.NO);
                alert.setResizable(true);

                final Optional<ButtonType> result = alert.showAndWait();
                if(result.isPresent() && result.get()==ButtonType.YES){

                    // Duplication du plan.
                    final PlanVegetation newPlan = toDuplicate.copy();
                    planRepo.add(newPlan);
                    uiPlanList.getItems().add(newPlan);


                    // Récupération des parcelles de l'ancien plan.
                    final List<ParcelleVegetation> oldParcelles = parcelleRepo.getByPlanId(toDuplicate.getId());

                    final List<String> oldParcellesIds = new ArrayList<>();
                    for(final ParcelleVegetation oldParcelle : oldParcelles) oldParcellesIds.add(oldParcelle.getId());

                    // Duplication des parcelles.
                    final Map<String, ParcelleVegetation> newParcelles = new HashMap<>();
                    for(final ParcelleVegetation oldParcelle : oldParcelles){
                        final ParcelleVegetation newParcelle = oldParcelle.copy();
                        // Réinitialisation des planifications
                        for(int i=0; i<newParcelle.getPlanifications().size(); i++){
                            newParcelle.getPlanifications().set(i, Boolean.FALSE);
                        }
                        // Réajustement de la taille (normalement inutile, mais par précaution…
                        PluginVegetation.ajustPlanifSize(newParcelle, newPlan.getAnneeFin()-newPlan.getAnneeDebut());

                        newParcelle.setPlanId(newPlan.getId());
                        newParcelles.put(oldParcelle.getId(), newParcelle);
                    }

                    // Enregistrement des parcelles (nécessaire pour les doter d'identifiants)
                    parcelleRepo.executeBulk(newParcelles.values());

                    // Récupération des zones de végétation des parcelles de l'ancien plan, pour chaque sorte de zone.
                    final Collection<AbstractSIRSRepository> zonesVegetationRepos = session.getRepositoriesForClass(ZoneVegetation.class);
                    final List<ZoneVegetation> oldZonesVegetation = new ArrayList<>();
                    for(final AbstractSIRSRepository zoneVegetationRepo : zonesVegetationRepos){
                        if(zoneVegetationRepo instanceof AbstractZoneVegetationRepository){
                            final AbstractZoneVegetationRepository zoneRepo = (AbstractZoneVegetationRepository) zoneVegetationRepo;
                            final List retrievedZones = zoneRepo.getByParcelleIds(oldParcellesIds);
                            if(retrievedZones!=null && !retrievedZones.isEmpty()) oldZonesVegetation.addAll(retrievedZones);

                        }
                    }


                    final Map<Class<? extends ZoneVegetation>, List<ZoneVegetation>> zonesByClass = new HashMap<>();

                    // Duplication des zones et affectation aux nouvelles parcelles (on a besoin de leur identifiant.
                    for(final ZoneVegetation oldZone : oldZonesVegetation){
                        final ZoneVegetation newZone = oldZone.copy();

                        final ParcelleVegetation newParcelle = newParcelles.get(oldZone.getParcelleId());
                        if(newParcelle!=null) newZone.setParcelleId(newParcelle.getId());

                        if(zonesByClass.get(newZone.getClass())==null) zonesByClass.put(newZone.getClass(), new ArrayList<>());

                        zonesByClass.get(newZone.getClass()).add(newZone);
                    }

                    // Enregistrement des zones de végétation
                    for(final Class clazz : zonesByClass.keySet()){
                        session.getRepositoryForClass(clazz).executeBulk(zonesByClass.get(clazz));
                    }

                    refreshPlanList();
                    uiPlanList.getSelectionModel().select(newPlan);

                    final Growl growlInfo = new Growl(Growl.Type.INFO, "Le plan a été dupliqué.");
                    growlInfo.showAndFade();
                }
            }
            catch(Exception e){
                SIRS.LOGGER.log(Level.WARNING, e.getMessage());
                final Growl growlInfo = new Growl(Growl.Type.ERROR, "Une erreur est survenue lors de la duplication du plan.");
                growlInfo.showAndFade();
            }
        }
        else {
            final Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un plan à dupliquer.", ButtonType.CLOSE);
            alert.setResizable(true);
            alert.showAndWait();
        }
    }

    /**
     * Deletes a planification.
     *
     * @param event
     */
    @FXML
    void planDelete(ActionEvent event) {
        final PlanVegetation toDelete = uiPlanList.getSelectionModel().getSelectedItem();
        if(toDelete!=null){

            try{
                // Vérification des intentions de l'utilisateur
                final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer le plan "+converter.toString(toDelete)+" ?\n"
                        + "Cette opération supprimera les parcelles de ce plan, leurs zones de végétation, ainsi que tous les paramétrages liés aux plans et aux parcelles.", ButtonType.YES, ButtonType.NO);
                alert.setResizable(true);
                final Optional<ButtonType> result = alert.showAndWait();

                if(result.isPresent() && result.get()==ButtonType.YES){

                    // Récupération des parcelles à supprimer
                    final List<ParcelleVegetation> parcellesToDelete = parcelleRepo.getByPlan(toDelete);

                    if(parcellesToDelete!=null && !parcellesToDelete.isEmpty()){

                        final List<String> parcellesIdsToDelete = new ArrayList();
                        for(final ParcelleVegetation parcelle : parcellesToDelete) parcellesIdsToDelete.add(parcelle.getId());

                        // Récupération des zones à supprimer et suppression
                        final Collection<AbstractSIRSRepository> zoneVegetationRepos = session.getRepositoriesForClass(ZoneVegetation.class);

                        for(final AbstractSIRSRepository zoneVegetationRepo : zoneVegetationRepos){
                            if(zoneVegetationRepo instanceof AbstractZoneVegetationRepository){
                                final AbstractZoneVegetationRepository repo = (AbstractZoneVegetationRepository) zoneVegetationRepo;
                                final List<ZoneVegetation> zones = repo.getByParcelleIds(parcellesIdsToDelete);
                                repo.executeBulkDelete(zones);
                            }
                        }

                        // Suppression des parcelles
                        parcelleRepo.executeBulkDelete(parcellesToDelete);
                    }

                    // Suppression du plan
                    planRepo.remove(toDelete);
                    uiPlanList.getItems().remove(toDelete);
                    refreshPlanList();

                    final Growl growlInfo = new Growl(Growl.Type.INFO, "Le plan a été supprimé.");
                    growlInfo.showAndFade();
                }
            }
            catch(Exception e){
                SIRS.LOGGER.log(Level.WARNING, e.getMessage());
                final Growl growlInfo = new Growl(Growl.Type.ERROR, "Une erreur est survenue lors de la suppression du plan.");
                growlInfo.showAndFade();
            }
        }
    }

    /**
     * A specific ListCell bound to the libelle and designation fields of the planification it represents.
     */
    private class UpdatableListCell extends ListCell<PlanVegetation> {

        @Override
        protected void updateItem(final PlanVegetation item, boolean empty) {
            super.updateItem(item, empty);

            graphicProperty().unbind();
            textProperty().unbind();
            if(item!=null){
                
                graphicProperty().bind(new ObjectBinding<Node>(){
                    {
                        bind(VegetationSession.INSTANCE.planProperty());
                    }
                    @Override
                    protected Node computeValue() {
                        if(item.equals(VegetationSession.INSTANCE.planProperty().get())){
                             return new ImageView(SIRS.ICON_CHECK);
                        }
                        else return null;
                    }
                });

                textProperty().bind(new ObjectBinding<String>() {
                    {
                        bind(item.libelleProperty(), item.designationProperty());
                    }
                    @Override
                    protected String computeValue() {
                        return converter.toString(item);
                    }
                });
            } else {
                setText("");
                setGraphic(null);
            }
        }
    }
}
