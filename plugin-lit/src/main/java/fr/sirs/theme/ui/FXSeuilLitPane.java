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
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.*;
import fr.sirs.util.StreamingIterable;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.geotoolkit.util.collection.CloseableIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXSeuilLitPane  extends AbstractFXElementPane<SeuilLit> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    // Propriétés de Positionable
    @FXML private FXPositionablePane uiPositionable;
    @FXML private FXValidityPeriodPane uiValidityPeriod;

    // Propriétés de SeuilLit
    @FXML protected TextField ui_libelle;
    @FXML protected TextField ui_commune;
    @FXML protected Spinner ui_anneeConstruction;
    @FXML protected Spinner ui_penteRampant;
    @FXML protected Spinner ui_longueurTotale;
    @FXML protected Spinner ui_longueurCoursier;
    @FXML protected Spinner ui_largeurEnCrete;
    @FXML protected Spinner ui_hauteurChute;
    @FXML protected CheckBox ui_passeSportEauVive;
    @FXML protected CheckBox ui_passePoisson;
    @FXML protected Spinner ui_surfaceRempantEntretien;
    @FXML protected ComboBox ui_fonctionSeuilId;
    @FXML protected Button ui_fonctionSeuilId_link;
    @FXML protected ComboBox ui_materiauPrincipalA;
    @FXML protected Button ui_materiauPrincipalA_link;
    @FXML protected ComboBox ui_materiauPrincipalB;
    @FXML protected Button ui_materiauPrincipalB_link;
    @FXML protected ComboBox ui_positionSeuilId;
    @FXML protected Button ui_positionSeuilId_link;
    @FXML protected ComboBox ui_geometrieCreteId;
    @FXML protected Button ui_geometrieCreteId_link;
    @FXML protected ComboBox ui_profilCoursierId;
    @FXML protected Button ui_profilCoursierId_link;
    @FXML protected Tab ui_plans;
    protected final PojoTable plansTable;
    @FXML protected Tab ui_voieAccesIds;
    protected final ListeningPojoTable voieAccesIdsTable;
    @FXML protected Tab ui_ouvrageFranchissementIds;
    protected final ListeningPojoTable ouvrageFranchissementIdsTable;
    @FXML protected Tab ui_voieDigueIds;
    protected final ListeningPojoTable voieDigueIdsTable;
    @FXML protected Tab ui_ouvrageVoirieIds;
    protected final ListeningPojoTable ouvrageVoirieIdsTable;
    @FXML protected Tab ui_stationPompageIds;
    protected final ListeningPojoTable stationPompageIdsTable;
    @FXML protected Tab ui_reseauHydrauliqueFermeIds;
    protected final ListeningPojoTable reseauHydrauliqueFermeIdsTable;
    @FXML protected Tab ui_reseauHydrauliqueCielOuvertIds;
    protected final ListeningPojoTable reseauHydrauliqueCielOuvertIdsTable;
    @FXML protected Tab ui_ouvrageHydrauliqueAssocieIds;
    protected final ListeningPojoTable ouvrageHydrauliqueAssocieIdsTable;
    @FXML protected Tab ui_ouvrageTelecomEnergieIds;
    protected final ListeningPojoTable ouvrageTelecomEnergieIdsTable;
    @FXML protected Tab ui_reseauTelecomEnergieIds;
    protected final ListeningPojoTable reseauTelecomEnergieIdsTable;
    @FXML protected Tab ui_ouvrageParticulierIds;
    protected final ListeningPojoTable ouvrageParticulierIdsTable;
    @FXML protected Tab ui_echelleLimnimetriqueIds;
    protected final ListeningPojoTable echelleLimnimetriqueIdsTable;
    @FXML protected Tab ui_desordreIds;
    protected final ListeningPojoTable desordreIdsTable;
    @FXML protected Tab ui_bergeIds;
    protected final ListeningPojoTable bergeIdsTable;
    @FXML protected Tab ui_digueIds;
    protected final ListeningPojoTable digueIdsTable;
    @FXML protected Tab ui_inspections;
    protected final PojoTable inspectionsTable;
    @FXML protected Tab ui_couches;
    protected final PojoTable couchesTable;

    // Propriétés de AvecGeometrie

    // Propriétés de Objet
    @FXML protected TextArea ui_commentaire;
    @FXML protected ComboBox ui_linearId;
    @FXML protected Button ui_linearId_link;

    // Propriétés de ObjetPhotographiable
    @FXML protected Tab ui_photos;
    protected final PojoTable photosTable;

    // Propriétés de ObjetLit
    @FXML protected Tab ui_proprietes;
    protected final PojoTable proprietesTable;
    @FXML protected Tab ui_gestions;
    protected final PojoTable gestionsTable;

    private Class bergeClass;

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXSeuilLitPane() {
        SIRS.loadFXML(this, SeuilLit.class);
        previewRepository = Injector.getBean(Session.class).getPreviews();
        elementProperty().addListener(this::initFields);

        uiPositionable.disableFieldsProperty().bind(disableFieldsProperty());
        uiPositionable.positionableProperty().bind(elementProperty());
        uiValidityPeriod.disableFieldsProperty().bind(disableFieldsProperty());
        uiValidityPeriod.targetProperty().bind(elementProperty());

        /*
         * Disabling rules.
         */
        ui_libelle.disableProperty().bind(disableFieldsProperty());
        ui_commune.disableProperty().bind(disableFieldsProperty());
        ui_anneeConstruction.disableProperty().bind(disableFieldsProperty());
        ui_anneeConstruction.setEditable(true);
        ui_anneeConstruction.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 2100));
        ui_penteRampant.disableProperty().bind(disableFieldsProperty());
        ui_penteRampant.setEditable(true);
        ui_penteRampant.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        ui_longueurTotale.disableProperty().bind(disableFieldsProperty());
        ui_longueurTotale.setEditable(true);
        ui_longueurTotale.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        ui_longueurCoursier.disableProperty().bind(disableFieldsProperty());
        ui_longueurCoursier.setEditable(true);
        ui_longueurCoursier.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        ui_largeurEnCrete.disableProperty().bind(disableFieldsProperty());
        ui_largeurEnCrete.setEditable(true);
        ui_largeurEnCrete.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        ui_hauteurChute.disableProperty().bind(disableFieldsProperty());
        ui_hauteurChute.setEditable(true);
        ui_hauteurChute.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        ui_passeSportEauVive.disableProperty().bind(disableFieldsProperty());
        ui_passePoisson.disableProperty().bind(disableFieldsProperty());
        ui_surfaceRempantEntretien.disableProperty().bind(disableFieldsProperty());
        ui_surfaceRempantEntretien.setEditable(true);
        ui_surfaceRempantEntretien.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Double.MAX_VALUE));
        ui_fonctionSeuilId.disableProperty().bind(disableFieldsProperty());
        ui_fonctionSeuilId_link.setVisible(false);
        ui_materiauPrincipalA.disableProperty().bind(disableFieldsProperty());
        ui_materiauPrincipalA_link.setVisible(false);
        ui_materiauPrincipalB.disableProperty().bind(disableFieldsProperty());
        ui_materiauPrincipalB_link.setVisible(false);
        ui_positionSeuilId.disableProperty().bind(disableFieldsProperty());
        ui_positionSeuilId_link.setVisible(false);
        ui_geometrieCreteId.disableProperty().bind(disableFieldsProperty());
        ui_geometrieCreteId_link.setVisible(false);
        ui_profilCoursierId.disableProperty().bind(disableFieldsProperty());
        ui_profilCoursierId_link.setVisible(false);
        plansTable = new PojoTable(PlanSeuilLit.class, null, elementProperty());
        plansTable.editableProperty().bind(disableFieldsProperty().not());
        ui_plans.setContent(plansTable);
        ui_plans.setClosable(false);
        voieAccesIdsTable = new ListeningPojoTable(VoieAcces.class, null, elementProperty());
        voieAccesIdsTable.editableProperty().bind(disableFieldsProperty().not());
        voieAccesIdsTable.createNewProperty().set(false);
        ui_voieAccesIds.setContent(voieAccesIdsTable);
        ui_voieAccesIds.setClosable(false);
        ouvrageFranchissementIdsTable = new ListeningPojoTable(OuvrageFranchissement.class, null, elementProperty());
        ouvrageFranchissementIdsTable.editableProperty().bind(disableFieldsProperty().not());
        ouvrageFranchissementIdsTable.createNewProperty().set(false);
        ui_ouvrageFranchissementIds.setContent(ouvrageFranchissementIdsTable);
        ui_ouvrageFranchissementIds.setClosable(false);
        voieDigueIdsTable = new ListeningPojoTable(VoieDigue.class, null, elementProperty());
        voieDigueIdsTable.editableProperty().bind(disableFieldsProperty().not());
        voieDigueIdsTable.createNewProperty().set(false);
        ui_voieDigueIds.setContent(voieDigueIdsTable);
        ui_voieDigueIds.setClosable(false);
        ouvrageVoirieIdsTable = new ListeningPojoTable(OuvrageVoirie.class, null, elementProperty());
        ouvrageVoirieIdsTable.editableProperty().bind(disableFieldsProperty().not());
        ouvrageVoirieIdsTable.createNewProperty().set(false);
        ui_ouvrageVoirieIds.setContent(ouvrageVoirieIdsTable);
        ui_ouvrageVoirieIds.setClosable(false);
        stationPompageIdsTable = new ListeningPojoTable(StationPompage.class, null, elementProperty());
        stationPompageIdsTable.editableProperty().bind(disableFieldsProperty().not());
        stationPompageIdsTable.createNewProperty().set(false);
        ui_stationPompageIds.setContent(stationPompageIdsTable);
        ui_stationPompageIds.setClosable(false);
        reseauHydrauliqueFermeIdsTable = new ListeningPojoTable(ReseauHydrauliqueFerme.class, null, elementProperty());
        reseauHydrauliqueFermeIdsTable.editableProperty().bind(disableFieldsProperty().not());
        reseauHydrauliqueFermeIdsTable.createNewProperty().set(false);
        ui_reseauHydrauliqueFermeIds.setContent(reseauHydrauliqueFermeIdsTable);
        ui_reseauHydrauliqueFermeIds.setClosable(false);
        reseauHydrauliqueCielOuvertIdsTable = new ListeningPojoTable(ReseauHydrauliqueCielOuvert.class, null, elementProperty());
        reseauHydrauliqueCielOuvertIdsTable.editableProperty().bind(disableFieldsProperty().not());
        reseauHydrauliqueCielOuvertIdsTable.createNewProperty().set(false);
        ui_reseauHydrauliqueCielOuvertIds.setContent(reseauHydrauliqueCielOuvertIdsTable);
        ui_reseauHydrauliqueCielOuvertIds.setClosable(false);
        ouvrageHydrauliqueAssocieIdsTable = new ListeningPojoTable(OuvrageHydrauliqueAssocie.class, null, elementProperty());
        ouvrageHydrauliqueAssocieIdsTable.editableProperty().bind(disableFieldsProperty().not());
        ouvrageHydrauliqueAssocieIdsTable.createNewProperty().set(false);
        ui_ouvrageHydrauliqueAssocieIds.setContent(ouvrageHydrauliqueAssocieIdsTable);
        ui_ouvrageHydrauliqueAssocieIds.setClosable(false);
        ouvrageTelecomEnergieIdsTable = new ListeningPojoTable(OuvrageTelecomEnergie.class, null, elementProperty());
        ouvrageTelecomEnergieIdsTable.editableProperty().bind(disableFieldsProperty().not());
        ouvrageTelecomEnergieIdsTable.createNewProperty().set(false);
        ui_ouvrageTelecomEnergieIds.setContent(ouvrageTelecomEnergieIdsTable);
        ui_ouvrageTelecomEnergieIds.setClosable(false);
        reseauTelecomEnergieIdsTable = new ListeningPojoTable(ReseauTelecomEnergie.class, null, elementProperty());
        reseauTelecomEnergieIdsTable.editableProperty().bind(disableFieldsProperty().not());
        reseauTelecomEnergieIdsTable.createNewProperty().set(false);
        ui_reseauTelecomEnergieIds.setContent(reseauTelecomEnergieIdsTable);
        ui_reseauTelecomEnergieIds.setClosable(false);
        ouvrageParticulierIdsTable = new ListeningPojoTable(OuvrageParticulier.class, null, elementProperty());
        ouvrageParticulierIdsTable.editableProperty().bind(disableFieldsProperty().not());
        ouvrageParticulierIdsTable.createNewProperty().set(false);
        ui_ouvrageParticulierIds.setContent(ouvrageParticulierIdsTable);
        ui_ouvrageParticulierIds.setClosable(false);
        echelleLimnimetriqueIdsTable = new ListeningPojoTable(EchelleLimnimetrique.class, null, elementProperty());
        echelleLimnimetriqueIdsTable.editableProperty().bind(disableFieldsProperty().not());
        echelleLimnimetriqueIdsTable.createNewProperty().set(false);
        ui_echelleLimnimetriqueIds.setContent(echelleLimnimetriqueIdsTable);
        ui_echelleLimnimetriqueIds.setClosable(false);
        desordreIdsTable = new ListeningPojoTable(DesordreLit.class, null, elementProperty());
        desordreIdsTable.editableProperty().bind(disableFieldsProperty().not());
        desordreIdsTable.createNewProperty().set(false);
        ui_desordreIds.setContent(desordreIdsTable);
        ui_desordreIds.setClosable(false);
        digueIdsTable = new ListeningPojoTable(Digue.class, null, elementProperty());
        digueIdsTable.editableProperty().bind(disableFieldsProperty().not());
        digueIdsTable.createNewProperty().set(false);
        ui_digueIds.setContent(digueIdsTable);
        ui_digueIds.setClosable(false);
        inspectionsTable = new PojoTable(InspectionSeuilLit.class, null, elementProperty());
        inspectionsTable.editableProperty().bind(disableFieldsProperty().not());
        ui_inspections.setContent(inspectionsTable);
        ui_inspections.setClosable(false);
        couchesTable = new PojoTable(CoucheSeuilLit.class, null, elementProperty());
        couchesTable.editableProperty().bind(disableFieldsProperty().not());
        ui_couches.setContent(couchesTable);
        ui_couches.setClosable(false);
        ui_commentaire.disableProperty().bind(disableFieldsProperty());
        ui_linearId.disableProperty().bind(disableFieldsProperty());
        ui_linearId_link.disableProperty().bind(ui_linearId.getSelectionModel().selectedItemProperty().isNull());
        ui_linearId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_linearId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_linearId.getSelectionModel().getSelectedItem()));
        photosTable = new PojoTable(Photo.class, null, elementProperty());
        photosTable.editableProperty().bind(disableFieldsProperty().not());
        ui_photos.setContent(photosTable);
        ui_photos.setClosable(false);
        proprietesTable = new PojoTable(ProprieteObjet.class, null, elementProperty());
        proprietesTable.editableProperty().bind(disableFieldsProperty().not());
        ui_proprietes.setContent(proprietesTable);
        ui_proprietes.setClosable(false);
        gestionsTable = new PojoTable(GestionObjet.class, null, elementProperty());
        gestionsTable.editableProperty().bind(disableFieldsProperty().not());
        ui_gestions.setContent(gestionsTable);
        ui_gestions.setClosable(false);

        try {
            bergeClass = Class.forName("fr.sirs.core.model.Berge");
        } catch (ClassNotFoundException ex) {
            SIRS.LOGGER.log(Level.WARNING, "Le module berge semble n'être pas chargé.", ex);
            bergeClass = null;
        }

        if(bergeClass!=null){
            bergeIdsTable = new ListeningPojoTable(bergeClass, null, elementProperty());
            bergeIdsTable.editableProperty().bind(disableFieldsProperty().not());
            bergeIdsTable.createNewProperty().set(false);
            ui_bergeIds.setContent(bergeIdsTable);
            ui_bergeIds.setClosable(false);
        }
        else{
            bergeIdsTable = null;
            ui_bergeIds.getTabPane().getTabs().remove(ui_bergeIds);
        }
    }

    public FXSeuilLitPane(final SeuilLit seuilLit){
        this();
        this.elementProperty().set(seuilLit);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends SeuilLit > observableElement, SeuilLit oldElement, SeuilLit newElement) {   // Unbind fields bound to previous element.
        if (oldElement != null) {
            ui_libelle.textProperty().unbindBidirectional(oldElement.libelleProperty());
            ui_commune.textProperty().unbindBidirectional(oldElement.communeProperty());
            ui_anneeConstruction.getValueFactory().valueProperty().unbindBidirectional(oldElement.anneeConstructionProperty());
            ui_penteRampant.getValueFactory().valueProperty().unbindBidirectional(oldElement.penteRampantProperty());
            ui_longueurTotale.getValueFactory().valueProperty().unbindBidirectional(oldElement.longueurTotaleProperty());
            ui_longueurCoursier.getValueFactory().valueProperty().unbindBidirectional(oldElement.longueurCoursierProperty());
            ui_largeurEnCrete.getValueFactory().valueProperty().unbindBidirectional(oldElement.largeurEnCreteProperty());
            ui_hauteurChute.getValueFactory().valueProperty().unbindBidirectional(oldElement.hauteurChuteProperty());
            ui_passeSportEauVive.selectedProperty().unbindBidirectional(oldElement.passeSportEauViveProperty());
            ui_passePoisson.selectedProperty().unbindBidirectional(oldElement.passePoissonProperty());
            ui_surfaceRempantEntretien.getValueFactory().valueProperty().unbindBidirectional(oldElement.surfaceRempantEntretienProperty());
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());
        }

        final Session session = Injector.getBean(Session.class);

        /*
         * Bind control properties to Element ones.
         */
        // Propriétés de SeuilLit
        // * libelle
        ui_libelle.textProperty().bindBidirectional(newElement.libelleProperty());
        // * commune
        ui_commune.textProperty().bindBidirectional(newElement.communeProperty());
        // * anneeConstruction
        ui_anneeConstruction.getValueFactory().valueProperty().bindBidirectional(newElement.anneeConstructionProperty());
        // * penteRampant
        ui_penteRampant.getValueFactory().valueProperty().bindBidirectional(newElement.penteRampantProperty());
        // * longueurTotale
        ui_longueurTotale.getValueFactory().valueProperty().bindBidirectional(newElement.longueurTotaleProperty());
        // * longueurCoursier
        ui_longueurCoursier.getValueFactory().valueProperty().bindBidirectional(newElement.longueurCoursierProperty());
        // * largeurEnCrete
        ui_largeurEnCrete.getValueFactory().valueProperty().bindBidirectional(newElement.largeurEnCreteProperty());
        // * hauteurChute
        ui_hauteurChute.getValueFactory().valueProperty().bindBidirectional(newElement.hauteurChuteProperty());
        // * passeSportEauVive
        ui_passeSportEauVive.selectedProperty().bindBidirectional(newElement.passeSportEauViveProperty());
        // * passePoisson
        ui_passePoisson.selectedProperty().bindBidirectional(newElement.passePoissonProperty());
        // * surfaceRempantEntretien
        ui_surfaceRempantEntretien.getValueFactory().valueProperty().bindBidirectional(newElement.surfaceRempantEntretienProperty());
            final AbstractSIRSRepository<RefFonctionSeuilLit> fonctionSeuilIdRepo = session.getRepositoryForClass(RefFonctionSeuilLit.class);
            SIRS.initCombo(ui_fonctionSeuilId, SIRS.observableList(fonctionSeuilIdRepo.getAll()), newElement.getFonctionSeuilId() == null? null : fonctionSeuilIdRepo.get(newElement.getFonctionSeuilId()));
            final AbstractSIRSRepository<RefMateriau> materiauPrincipalARepo = session.getRepositoryForClass(RefMateriau.class);
            SIRS.initCombo(ui_materiauPrincipalA, SIRS.observableList(materiauPrincipalARepo.getAll()), newElement.getMateriauPrincipalA() == null? null : materiauPrincipalARepo.get(newElement.getMateriauPrincipalA()));
            final AbstractSIRSRepository<RefMateriau> materiauPrincipalBRepo = session.getRepositoryForClass(RefMateriau.class);
            SIRS.initCombo(ui_materiauPrincipalB, SIRS.observableList(materiauPrincipalBRepo.getAll()), newElement.getMateriauPrincipalB() == null? null : materiauPrincipalBRepo.get(newElement.getMateriauPrincipalB()));
            final AbstractSIRSRepository<RefPositionAxeSeuilLit> positionSeuilIdRepo = session.getRepositoryForClass(RefPositionAxeSeuilLit.class);
            SIRS.initCombo(ui_positionSeuilId, SIRS.observableList(positionSeuilIdRepo.getAll()), newElement.getPositionSeuilId() == null? null : positionSeuilIdRepo.get(newElement.getPositionSeuilId()));
            final AbstractSIRSRepository<RefGeometrieCreteSeuilLit> geometrieCreteIdRepo = session.getRepositoryForClass(RefGeometrieCreteSeuilLit.class);
            SIRS.initCombo(ui_geometrieCreteId, SIRS.observableList(geometrieCreteIdRepo.getAll()), newElement.getGeometrieCreteId() == null? null : geometrieCreteIdRepo.get(newElement.getGeometrieCreteId()));
            final AbstractSIRSRepository<RefProfilCoursierSeuilLit> profilCoursierIdRepo = session.getRepositoryForClass(RefProfilCoursierSeuilLit.class);
            SIRS.initCombo(ui_profilCoursierId, SIRS.observableList(profilCoursierIdRepo.getAll()), newElement.getProfilCoursierId() == null? null : profilCoursierIdRepo.get(newElement.getProfilCoursierId()));
        plansTable.setParentElement(newElement);
        plansTable.setTableItems(()-> (ObservableList) newElement.getPlans());
        voieAccesIdsTable.setParentElement(null);
        final AbstractSIRSRepository<VoieAcces> voieAccesIdsRepo = session.getRepositoryForClass(VoieAcces.class);
        voieAccesIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getVoieAccesIds(), voieAccesIdsRepo));
        ouvrageFranchissementIdsTable.setParentElement(null);
        final AbstractSIRSRepository<OuvrageFranchissement> ouvrageFranchissementIdsRepo = session.getRepositoryForClass(OuvrageFranchissement.class);
        ouvrageFranchissementIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getOuvrageFranchissementIds(), ouvrageFranchissementIdsRepo));
        voieDigueIdsTable.setParentElement(null);
        final AbstractSIRSRepository<VoieDigue> voieDigueIdsRepo = session.getRepositoryForClass(VoieDigue.class);
        voieDigueIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getVoieDigueIds(), voieDigueIdsRepo));
        ouvrageVoirieIdsTable.setParentElement(null);
        final AbstractSIRSRepository<OuvrageVoirie> ouvrageVoirieIdsRepo = session.getRepositoryForClass(OuvrageVoirie.class);
        ouvrageVoirieIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getOuvrageVoirieIds(), ouvrageVoirieIdsRepo));
        stationPompageIdsTable.setParentElement(null);
        final AbstractSIRSRepository<StationPompage> stationPompageIdsRepo = session.getRepositoryForClass(StationPompage.class);
        stationPompageIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getStationPompageIds(), stationPompageIdsRepo));
        reseauHydrauliqueFermeIdsTable.setParentElement(null);
        final AbstractSIRSRepository<ReseauHydrauliqueFerme> reseauHydrauliqueFermeIdsRepo = session.getRepositoryForClass(ReseauHydrauliqueFerme.class);
        reseauHydrauliqueFermeIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getReseauHydrauliqueFermeIds(), reseauHydrauliqueFermeIdsRepo));
        reseauHydrauliqueCielOuvertIdsTable.setParentElement(null);
        final AbstractSIRSRepository<ReseauHydrauliqueCielOuvert> reseauHydrauliqueCielOuvertIdsRepo = session.getRepositoryForClass(ReseauHydrauliqueCielOuvert.class);
        reseauHydrauliqueCielOuvertIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getReseauHydrauliqueCielOuvertIds(), reseauHydrauliqueCielOuvertIdsRepo));
        ouvrageHydrauliqueAssocieIdsTable.setParentElement(null);
        final AbstractSIRSRepository<OuvrageHydrauliqueAssocie> ouvrageHydrauliqueAssocieIdsRepo = session.getRepositoryForClass(OuvrageHydrauliqueAssocie.class);
        ouvrageHydrauliqueAssocieIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getOuvrageHydrauliqueAssocieIds(), ouvrageHydrauliqueAssocieIdsRepo));
        ouvrageTelecomEnergieIdsTable.setParentElement(null);
        final AbstractSIRSRepository<OuvrageTelecomEnergie> ouvrageTelecomEnergieIdsRepo = session.getRepositoryForClass(OuvrageTelecomEnergie.class);
        ouvrageTelecomEnergieIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getOuvrageTelecomEnergieIds(), ouvrageTelecomEnergieIdsRepo));
        reseauTelecomEnergieIdsTable.setParentElement(null);
        final AbstractSIRSRepository<ReseauTelecomEnergie> reseauTelecomEnergieIdsRepo = session.getRepositoryForClass(ReseauTelecomEnergie.class);
        reseauTelecomEnergieIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getReseauTelecomEnergieIds(), reseauTelecomEnergieIdsRepo));
        ouvrageParticulierIdsTable.setParentElement(null);
        final AbstractSIRSRepository<OuvrageParticulier> ouvrageParticulierIdsRepo = session.getRepositoryForClass(OuvrageParticulier.class);
        ouvrageParticulierIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getOuvrageParticulierIds(), ouvrageParticulierIdsRepo));
        echelleLimnimetriqueIdsTable.setParentElement(null);
        final AbstractSIRSRepository<EchelleLimnimetrique> echelleLimnimetriqueIdsRepo = session.getRepositoryForClass(EchelleLimnimetrique.class);
        echelleLimnimetriqueIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getEchelleLimnimetriqueIds(), echelleLimnimetriqueIdsRepo));
        desordreIdsTable.setParentElement(null);
        final AbstractSIRSRepository<DesordreLit> desordreIdsRepo = session.getRepositoryForClass(DesordreLit.class);
        desordreIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getDesordreIds(), desordreIdsRepo));
        if(bergeClass!=null){
            bergeIdsTable.setParentElement(null);
            final AbstractSIRSRepository<TronconDigue> bergeIdsRepo = Injector.getSession().getRepositoryForClass(bergeClass);
            bergeIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getBergeIds(), bergeIdsRepo));
        }
        digueIdsTable.setParentElement(null);
        final AbstractSIRSRepository<Digue> digueIdsRepo = session.getRepositoryForClass(Digue.class);
        digueIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getDigueIds(), digueIdsRepo));
        inspectionsTable.setParentElement(newElement);
        inspectionsTable.setTableItems(()-> (ObservableList) newElement.getInspections());
        couchesTable.setParentElement(newElement);
        couchesTable.setTableItems(()-> (ObservableList) newElement.getCouches());
        // * commentaire
        ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());

        final Preview linearPreview = newElement.getLinearId() == null ? null : previewRepository.get(newElement.getLinearId());
        // HACK-REDMINE-4408 : hide archived linear from selection lists
        SIRS.initCombo(ui_linearId, SIRS.observableList(
            previewRepository.getByClass(linearPreview == null ? TronconDigue.class : linearPreview.getJavaClassOr(TronconDigue.class))).sorted(),
            linearPreview, SirsPreferences.getHideArchivedProperty(), true);
//        SIRS.initCombo(ui_linearId, SIRS.observableList(
//            previewRepository.getByClass(TronconDigue.class)).sorted(),
//            newElement.getLinearId() == null? null : previewRepository.get(newElement.getLinearId()));

        // Propriétés de ObjetPhotographiable
        photosTable.setParentElement(newElement);
        photosTable.setTableItems(()-> (ObservableList) newElement.getPhotos());
        // Propriétés de ObjetLit
        proprietesTable.setParentElement(newElement);
        proprietesTable.setTableItems(()-> (ObservableList) newElement.getProprietes());
        gestionsTable.setParentElement(newElement);
        gestionsTable.setTableItems(()-> (ObservableList) newElement.getGestions());
        voieAccesIdsTable.setObservableListToListen(newElement.getVoieAccesIds());
        ouvrageFranchissementIdsTable.setObservableListToListen(newElement.getOuvrageFranchissementIds());
        voieDigueIdsTable.setObservableListToListen(newElement.getVoieDigueIds());
        ouvrageVoirieIdsTable.setObservableListToListen(newElement.getOuvrageVoirieIds());
        stationPompageIdsTable.setObservableListToListen(newElement.getStationPompageIds());
        reseauHydrauliqueFermeIdsTable.setObservableListToListen(newElement.getReseauHydrauliqueFermeIds());
        reseauHydrauliqueCielOuvertIdsTable.setObservableListToListen(newElement.getReseauHydrauliqueCielOuvertIds());
        ouvrageHydrauliqueAssocieIdsTable.setObservableListToListen(newElement.getOuvrageHydrauliqueAssocieIds());
        ouvrageTelecomEnergieIdsTable.setObservableListToListen(newElement.getOuvrageTelecomEnergieIds());
        reseauTelecomEnergieIdsTable.setObservableListToListen(newElement.getReseauTelecomEnergieIds());
        ouvrageParticulierIdsTable.setObservableListToListen(newElement.getOuvrageParticulierIds());
        echelleLimnimetriqueIdsTable.setObservableListToListen(newElement.getEchelleLimnimetriqueIds());
        desordreIdsTable.setObservableListToListen(newElement.getDesordreIds());
        if(bergeClass!=null){
            bergeIdsTable.setObservableListToListen(newElement.getBergeIds());
        }
        digueIdsTable.setObservableListToListen(newElement.getDigueIds());

    }

    @Override
    public void preSave() {
        final Session session = Injector.getBean(Session.class);
        final SeuilLit element = (SeuilLit) elementProperty().get();

        uiPositionable.preSave();

        Object cbValue;
        cbValue = ui_fonctionSeuilId.getValue();
        if (cbValue instanceof Preview) {
            element.setFonctionSeuilId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setFonctionSeuilId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setFonctionSeuilId(null);
        }
        cbValue = ui_materiauPrincipalA.getValue();
        if (cbValue instanceof Preview) {
            element.setMateriauPrincipalA(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setMateriauPrincipalA(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setMateriauPrincipalA(null);
        }
        cbValue = ui_materiauPrincipalB.getValue();
        if (cbValue instanceof Preview) {
            element.setMateriauPrincipalB(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setMateriauPrincipalB(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setMateriauPrincipalB(null);
        }
        cbValue = ui_positionSeuilId.getValue();
        if (cbValue instanceof Preview) {
            element.setPositionSeuilId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setPositionSeuilId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setPositionSeuilId(null);
        }
        cbValue = ui_geometrieCreteId.getValue();
        if (cbValue instanceof Preview) {
            element.setGeometrieCreteId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setGeometrieCreteId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setGeometrieCreteId(null);
        }
        cbValue = ui_profilCoursierId.getValue();
        if (cbValue instanceof Preview) {
            element.setProfilCoursierId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setProfilCoursierId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setProfilCoursierId(null);
        }
        // Manage opposite references for VoieAcces...
        final List<String> currentVoieAccesIdsList = new ArrayList<>();
        for(final Element elt : voieAccesIdsTable.getAllValues()){
            final VoieAcces voieAcces = (VoieAcces) elt;
            currentVoieAccesIdsList.add(voieAcces.getId());
        }
        element.setVoieAccesIds(currentVoieAccesIdsList);

        // Manage opposite references for OuvrageFranchissement...
        final List<String> currentOuvrageFranchissementIdsList = new ArrayList<>();
        for(final Element elt : ouvrageFranchissementIdsTable.getAllValues()){
            final OuvrageFranchissement ouvrageFranchissement = (OuvrageFranchissement) elt;
            currentOuvrageFranchissementIdsList.add(ouvrageFranchissement.getId());
        }
        element.setOuvrageFranchissementIds(currentOuvrageFranchissementIdsList);

        // Manage opposite references for VoieDigue...
        final List<String> currentVoieDigueIdsList = new ArrayList<>();
        for(final Element elt : voieDigueIdsTable.getAllValues()){
            final VoieDigue voieDigue = (VoieDigue) elt;
            currentVoieDigueIdsList.add(voieDigue.getId());
        }
        element.setVoieDigueIds(currentVoieDigueIdsList);

        // Manage opposite references for OuvrageVoirie...
        final List<String> currentOuvrageVoirieIdsList = new ArrayList<>();
        for(final Element elt : ouvrageVoirieIdsTable.getAllValues()){
            final OuvrageVoirie ouvrageVoirie = (OuvrageVoirie) elt;
            currentOuvrageVoirieIdsList.add(ouvrageVoirie.getId());
        }
        element.setOuvrageVoirieIds(currentOuvrageVoirieIdsList);

        // Manage opposite references for StationPompage...
        final List<String> currentStationPompageIdsList = new ArrayList<>();
        for(final Element elt : stationPompageIdsTable.getAllValues()){
            final StationPompage stationPompage = (StationPompage) elt;
            currentStationPompageIdsList.add(stationPompage.getId());
        }
        element.setStationPompageIds(currentStationPompageIdsList);

        // Manage opposite references for ReseauHydrauliqueFerme...
        final List<String> currentReseauHydrauliqueFermeIdsList = new ArrayList<>();
        for(final Element elt : reseauHydrauliqueFermeIdsTable.getAllValues()){
            final ReseauHydrauliqueFerme reseauHydrauliqueFerme = (ReseauHydrauliqueFerme) elt;
            currentReseauHydrauliqueFermeIdsList.add(reseauHydrauliqueFerme.getId());
        }
        element.setReseauHydrauliqueFermeIds(currentReseauHydrauliqueFermeIdsList);

        // Manage opposite references for ReseauHydrauliqueCielOuvert...
        final List<String> currentReseauHydrauliqueCielOuvertIdsList = new ArrayList<>();
        for(final Element elt : reseauHydrauliqueCielOuvertIdsTable.getAllValues()){
            final ReseauHydrauliqueCielOuvert reseauHydrauliqueCielOuvert = (ReseauHydrauliqueCielOuvert) elt;
            currentReseauHydrauliqueCielOuvertIdsList.add(reseauHydrauliqueCielOuvert.getId());
        }
        element.setReseauHydrauliqueCielOuvertIds(currentReseauHydrauliqueCielOuvertIdsList);

        // Manage opposite references for OuvrageHydrauliqueAssocie...
        final List<String> currentOuvrageHydrauliqueAssocieIdsList = new ArrayList<>();
        for(final Element elt : ouvrageHydrauliqueAssocieIdsTable.getAllValues()){
            final OuvrageHydrauliqueAssocie ouvrageHydrauliqueAssocie = (OuvrageHydrauliqueAssocie) elt;
            currentOuvrageHydrauliqueAssocieIdsList.add(ouvrageHydrauliqueAssocie.getId());
        }
        element.setOuvrageHydrauliqueAssocieIds(currentOuvrageHydrauliqueAssocieIdsList);

        // Manage opposite references for OuvrageTelecomEnergie...
        final List<String> currentOuvrageTelecomEnergieIdsList = new ArrayList<>();
        for(final Element elt : ouvrageTelecomEnergieIdsTable.getAllValues()){
            final OuvrageTelecomEnergie ouvrageTelecomEnergie = (OuvrageTelecomEnergie) elt;
            currentOuvrageTelecomEnergieIdsList.add(ouvrageTelecomEnergie.getId());
        }
        element.setOuvrageTelecomEnergieIds(currentOuvrageTelecomEnergieIdsList);

        // Manage opposite references for ReseauTelecomEnergie...
        final List<String> currentReseauTelecomEnergieIdsList = new ArrayList<>();
        for(final Element elt : reseauTelecomEnergieIdsTable.getAllValues()){
            final ReseauTelecomEnergie reseauTelecomEnergie = (ReseauTelecomEnergie) elt;
            currentReseauTelecomEnergieIdsList.add(reseauTelecomEnergie.getId());
        }
        element.setReseauTelecomEnergieIds(currentReseauTelecomEnergieIdsList);

        // Manage opposite references for OuvrageParticulier...
        final List<String> currentOuvrageParticulierIdsList = new ArrayList<>();
        for(final Element elt : ouvrageParticulierIdsTable.getAllValues()){
            final OuvrageParticulier ouvrageParticulier = (OuvrageParticulier) elt;
            currentOuvrageParticulierIdsList.add(ouvrageParticulier.getId());
        }
        element.setOuvrageParticulierIds(currentOuvrageParticulierIdsList);

        // Manage opposite references for EchelleLimnimetrique...
        final List<String> currentEchelleLimnimetriqueIdsList = new ArrayList<>();
        for(final Element elt : echelleLimnimetriqueIdsTable.getAllValues()){
            final EchelleLimnimetrique echelleLimnimetrique = (EchelleLimnimetrique) elt;
            currentEchelleLimnimetriqueIdsList.add(echelleLimnimetrique.getId());
        }
        element.setEchelleLimnimetriqueIds(currentEchelleLimnimetriqueIdsList);

        /*
        * En cas de reference opposee on se prepare a stocker les objets
        * "opposes" pour les mettre � jour.
        */
        final List<DesordreLit> currentDesordreLitList = new ArrayList<>();
        // Manage opposite references for DesordreLit...
        /*
        * Si on est sur une reference principale, on a besoin du depot pour
        * supprimer reellement les elements que l'on va retirer du tableau.
        * Si on a une reference opposee, on a besoin du depot pour mettre a jour
        * les objets qui referencent l'objet courant en sens contraire.
        */
        final AbstractSIRSRepository<DesordreLit> desordreLitRepository = session.getRepositoryForClass(DesordreLit.class);
        final List<String> currentDesordreLitIdsList = new ArrayList<>();
        for(final Element elt : desordreIdsTable.getAllValues()){
            final DesordreLit desordreLit = (DesordreLit) elt;
            currentDesordreLitIdsList.add(desordreLit.getId());
            currentDesordreLitList.add(desordreLit);

            // Addition
            if(!desordreLit.getSeuilIds().contains(element.getId())){
                desordreLit.getSeuilIds().add(element.getId());
            }
        }
        desordreLitRepository.executeBulk(currentDesordreLitList);
        element.setDesordreIds(currentDesordreLitIdsList);

        // Deletion
        final StreamingIterable<DesordreLit> listDesordreLit = desordreLitRepository.getAllStreaming();
        try (final CloseableIterator<DesordreLit> it = listDesordreLit.iterator()) {
            while (it.hasNext()) {
                final DesordreLit i = it.next();
                if(i.getSeuilIds().contains(element.getId())
                    || element.getDesordreIds().contains(i.getId())){
                    if(!desordreIdsTable.getAllValues().contains(i)){
                        element.getDesordreIds().remove(i.getId()); //Normalement inutile du fait du  clear avant les op�rations d'ajout
                        i.getSeuilIds().remove(element.getId());
                        desordreLitRepository.update(i);
                    }
                }
            }
        }
        // Manage opposite references for TronconDigue...
        if (bergeIdsTable != null) {
            final List<String> currentTronconDigueIdsList = new ArrayList<>();
            for (final Element elt : bergeIdsTable.getAllValues()) {
                final TronconDigue tronconDigue = (TronconDigue) elt;
                currentTronconDigueIdsList.add(tronconDigue.getId());
            }
            element.setBergeIds(currentTronconDigueIdsList);
        }

        // Manage opposite references for Digue...
        final List<String> currentDigueIdsList = new ArrayList<>();
        for(final Element elt : digueIdsTable.getAllValues()){
            final Digue digue = (Digue) elt;
            currentDigueIdsList.add(digue.getId());
        }
        element.setDigueIds(currentDigueIdsList);

        cbValue = ui_linearId.getValue();
        if (cbValue instanceof Preview) {
            element.setLinearId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setLinearId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setLinearId(null);
        }
    }
}