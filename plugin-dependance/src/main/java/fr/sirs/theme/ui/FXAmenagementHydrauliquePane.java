
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.component.TraitAmenagementHydrauliqueRepository;
import fr.sirs.core.model.*;
import fr.sirs.util.FXFreeTab;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.javafx.FloatSpinnerValueFactory;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *
 * @author Maxime Gavens (Geomatys)
 */
public class FXAmenagementHydrauliquePane extends AbstractFXElementPane<AmenagementHydraulique> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    @FXML private FXValidityPeriodPane uiValidityPeriod;

    // Propriétés de AmenagementHydraulique
    @FXML protected Spinner ui_superficie;
    @FXML protected Spinner ui_capaciteStockage;
    @FXML protected Spinner ui_profondeurMoyenne;
    @FXML protected ComboBox ui_organismeId;
    @FXML protected ComboBox ui_fonctionnementId;
    @FXML protected ComboBox ui_typeId;
    @FXML protected FXFreeTab ui_gestionnaireIds;
    protected ListeningPojoTable gestionnaireIdsTable;
    @FXML protected FXFreeTab ui_tronconIds;
    protected ListeningPojoTable tronconIdsTable;
    @FXML protected FXFreeTab ui_observations;
    protected PojoTable observationsTable;
    @FXML protected FXFreeTab ui_proprietaireIds;
    protected ListeningPojoTable proprietaireIdsTable;
    @FXML protected FXFreeTab ui_traits;
    protected PojoTable traitsTable;
//    @FXML protected FXFreeTab ui_desordreIds;
//    protected ListeningPojoTable desordreIdsTable;
//    @FXML protected FXFreeTab ui_prestationIds;
//    protected ListeningPojoTable prestationIdsTable;

    // Propriétés de AotCotAssociable

    // Propriétés de AvecGeometrie

    // Propriétés de AvecSettableGeometrie

    // Propriétés de AbstractDependance
    @FXML protected TextArea ui_commentaire;
    @FXML protected TextField ui_libelle;
    // We don't want to use these attibutes
//    @FXML protected FXFreeTab ui_proprietes;
//    protected PojoTable proprietesTable;
//    @FXML protected FXFreeTab ui_gestions;
//    protected PojoTable gestionsTable;
    @FXML protected FXFreeTab ui_photos;
    protected PojoTable photosTable;

    @FXML FXPositionDependancePane uiPosition;

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXAmenagementHydrauliquePane() {
        SIRS.loadFXML(this, AmenagementHydraulique.class);
        final Session session = Injector.getBean(Session.class);
        previewRepository = session.getPreviews();
        elementProperty().addListener(this::initFields);

        uiValidityPeriod.disableFieldsProperty().bind(disableFieldsProperty());
        uiValidityPeriod.targetProperty().bind(elementProperty());

        /*
        * Disabling rules.
        */
        ui_superficie.disableProperty().bind(disableFieldsProperty());
        ui_superficie.setEditable(true);
        ui_superficie.setValueFactory(new FloatSpinnerValueFactory(0, Float.MAX_VALUE));
        ui_capaciteStockage.disableProperty().bind(disableFieldsProperty());
        ui_capaciteStockage.setEditable(true);
        ui_capaciteStockage.setValueFactory(new FloatSpinnerValueFactory(0, Float.MAX_VALUE));
        ui_profondeurMoyenne.disableProperty().bind(disableFieldsProperty());
        ui_profondeurMoyenne.setEditable(true);
        ui_profondeurMoyenne.setValueFactory(new FloatSpinnerValueFactory(0, Float.MAX_VALUE));
        ui_fonctionnementId.disableProperty().bind(disableFieldsProperty());
        ui_organismeId.disableProperty().bind(disableFieldsProperty());
        ui_typeId.disableProperty().bind(disableFieldsProperty());
        uiPosition.disableFieldsProperty().bind(disableFieldsProperty());
        uiPosition.dependanceProperty().bind(elementProperty);

        ui_gestionnaireIds.setContent(() -> {
            gestionnaireIdsTable = new ListeningPojoTable(Organisme.class, null, elementProperty());
            gestionnaireIdsTable.editableProperty().bind(disableFieldsProperty().not());
            gestionnaireIdsTable.createNewProperty().set(false);
            updateGestionnaireIdsTable(session, elementProperty.get());
            return gestionnaireIdsTable;
        });
        ui_gestionnaireIds.setClosable(false);

        ui_tronconIds.setContent(() -> {
            tronconIdsTable = new TronconTable( null, elementProperty());
            tronconIdsTable.editableProperty().bind(disableFieldsProperty().not());
            tronconIdsTable.fichableProperty().set(false);
            tronconIdsTable.createNewProperty().set(false);
            updateTronconIdsTable(session, elementProperty.get());
            return tronconIdsTable;
        });
        ui_tronconIds.setClosable(false);

        ui_observations.setContent(() -> {
            observationsTable = new PojoTable(ObservationDependance.class, null, elementProperty());
            observationsTable.editableProperty().bind(disableFieldsProperty().not());
            updateObservationsTable(session, elementProperty.get());
            return observationsTable;
        });
        ui_observations.setClosable(false);

        ui_proprietaireIds.setContent(() -> {
            proprietaireIdsTable = new ListeningPojoTable(Contact.class, null, elementProperty());
            proprietaireIdsTable.editableProperty().bind(disableFieldsProperty().not());
            proprietaireIdsTable.createNewProperty().set(false);
            updateProprietaireIdsTable(session, elementProperty.get());
            return proprietaireIdsTable;
        });
        ui_proprietaireIds.setClosable(false);
        ui_commentaire.setWrapText(true);
        ui_commentaire.editableProperty().bind(disableFieldsProperty().not());
        ui_libelle.disableProperty().bind(disableFieldsProperty());

//        ui_proprietes.setContent(() -> {
//            proprietesTable = new PojoTable(ProprieteObjet.class, null, elementProperty());
//            proprietesTable.editableProperty().bind(disableFieldsProperty().not());
//            updateProprietesTable(session, elementProperty.get());
//            return proprietesTable;
//        });
//        ui_proprietes.setClosable(false);

//        ui_gestions.setContent(() -> {
//            gestionsTable = new PojoTable(GestionObjet.class, null, elementProperty());
//            gestionsTable.editableProperty().bind(disableFieldsProperty().not());
//            updateGestionsTable(session, elementProperty.get());
//            return gestionsTable;
//        });
//        ui_gestions.setClosable(false);

        ui_photos.setContent(() -> {
            photosTable = new PojoTable(PhotoDependance.class, null, elementProperty());
            photosTable.editableProperty().bind(disableFieldsProperty().not());
            updatePhotosTable(session, elementProperty.get());
            return photosTable;
        });
        ui_photos.setClosable(false);

        ui_traits.setContent(() -> {
            traitsTable = new PojoTable(TraitAmenagementHydraulique.class, null, elementProperty());
            traitsTable.editableProperty().bind(disableFieldsProperty().not());
            updateTraitIdsTable(session, elementProperty.get());
            return traitsTable;
        });
        ui_traits.setClosable(false);

//        ui_desordreIds.setContent(() -> {
//            desordreIdsTable = new ListeningPojoTable(DesordreDependance.class, "Désordre affectant l'AH", elementProperty());
//            desordreIdsTable.editableProperty().bind(disableFieldsProperty().not());
//            desordreIdsTable.createNewProperty().set(false);
//            updateDesordreIdsTable(session, elementProperty.get());
//            return desordreIdsTable;
//        });
//        ui_desordreIds.setClosable(false);
//
//        ui_prestationIds.setContent(() -> {
//            prestationIdsTable = new ListeningPojoTable(PrestationAmenagementHydraulique.class, "Prestation affectant l'AH", elementProperty());
//            prestationIdsTable.editableProperty().bind(disableFieldsProperty().not());
//            prestationIdsTable.createNewProperty().set(false);
//            updatePrestationIdsTable(session, elementProperty.get());
//            return prestationIdsTable;
//        });
//        ui_prestationIds.setClosable(false);
    }

    public FXAmenagementHydrauliquePane(final AmenagementHydraulique amenagementHydraulique){
        this();
        this.elementProperty().set(amenagementHydraulique);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends AmenagementHydraulique > observableElement, AmenagementHydraulique oldElement, AmenagementHydraulique newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
            // Propriétés de AmenagementHydraulique
            ui_superficie.getValueFactory().valueProperty().unbindBidirectional(oldElement.superficieProperty());
            ui_superficie.getValueFactory().setValue(0);
            ui_capaciteStockage.getValueFactory().valueProperty().unbindBidirectional(oldElement.capaciteStockageProperty());
            ui_capaciteStockage.getValueFactory().setValue(0);
            ui_profondeurMoyenne.getValueFactory().valueProperty().unbindBidirectional(oldElement.profondeurMoyenneProperty());
            ui_profondeurMoyenne.getValueFactory().setValue(0);
            // Propriétés de AotCotAssociable
            // Propriétés de AvecGeometrie
            // Propriétés de AvecSettableGeometrie
            // Propriétés de AbstractDependance
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());
            ui_commentaire.setText(null);
            ui_libelle.textProperty().unbindBidirectional(oldElement.libelleProperty());
            ui_libelle.setText(null);
        }

        final Session session = Injector.getBean(Session.class);

        if (newElement == null) {

            ui_fonctionnementId.setItems(null);
            ui_typeId.setItems(null);
            ui_organismeId.setItems(null);
        } else {

            /*
            * Bind control properties to Element ones.
            */
            // Propriétés de AmenagementHydraulique
            // * superficie
            ui_superficie.getValueFactory().valueProperty().bindBidirectional(newElement.superficieProperty());
            // * capaciteStockage
            ui_capaciteStockage.getValueFactory().valueProperty().bindBidirectional(newElement.capaciteStockageProperty());
            // * profondeurMoyenne
            ui_profondeurMoyenne.getValueFactory().valueProperty().bindBidirectional(newElement.profondeurMoyenneProperty());
            final AbstractSIRSRepository<RefFonctionnementAH> fonctionnementRepo = session.getRepositoryForClass(RefFonctionnementAH.class);
            SIRS.initCombo(ui_fonctionnementId, SIRS.observableList(fonctionnementRepo.getAll()), newElement.getFonctionnementId() == null? null : fonctionnementRepo.get(newElement.getFonctionnementId()));
            final AbstractSIRSRepository<Organisme> organismeRepo = session.getRepositoryForClass(Organisme.class);
            SIRS.initCombo(ui_organismeId, SIRS.observableList(organismeRepo.getAll()), newElement.getOrganismeId() == null? null : organismeRepo.get(newElement.getOrganismeId()));
            final AbstractSIRSRepository<RefTypeAmenagementHydraulique> typeRepo = session.getRepositoryForClass(RefTypeAmenagementHydraulique.class);
            SIRS.initCombo(ui_typeId, SIRS.observableList(typeRepo.getAll()), newElement.getTypeId() == null? null : typeRepo.get(newElement.getTypeId()));
            // Propriétés de AotCotAssociable
            // Propriétés de AvecGeometrie
            // Propriétés de AvecSettableGeometrie
            // Propriétés de AbstractDependance
            // * commentaire
            ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());
            // * libelle
            ui_libelle.textProperty().bindBidirectional(newElement.libelleProperty());
        }

        updateGestionnaireIdsTable(session, newElement);
        updateTronconIdsTable(session, newElement);
        updateObservationsTable(session, newElement);
        updateProprietaireIdsTable(session, newElement);
//        updateProprietesTable(session, newElement);
//        updateGestionsTable(session, newElement);
        updatePhotosTable(session, newElement);
        updateTraitIdsTable(session, newElement);
//        updateDesordreIdsTable(session, newElement);
//        updatePrestationIdsTable(session, newElement);
    }

//    protected void updateDesordreIdsTable(final Session session, final AmenagementHydraulique newElement) {
//        if (desordreIdsTable == null)
//            return;
//
//        if (newElement == null) {
//            desordreIdsTable.setTableItems(null);
//        } else {
//            desordreIdsTable.setParentElement(null);
//            final AbstractSIRSRepository<DesordreDependance> desordreIdsRepo = session.getRepositoryForClass(DesordreDependance.class);
//            desordreIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getDesordreIds(), desordreIdsRepo));
//            desordreIdsTable.setObservableListToListen(newElement.getDesordreIds());
//        }
//    }
//    protected void updatePrestationIdsTable(final Session session, final AmenagementHydraulique newElement) {
//        if (prestationIdsTable == null)
//            return;
//
//        if (newElement == null) {
//            prestationIdsTable.setTableItems(null);
//        } else {
//            prestationIdsTable.setParentElement(null);
//            final AbstractSIRSRepository<PrestationAmenagementHydraulique> prestationIdsRepo = session.getRepositoryForClass(PrestationAmenagementHydraulique.class);
//            prestationIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getPrestationIds(), prestationIdsRepo));
//            prestationIdsTable.setObservableListToListen(newElement.getPrestationIds());
//        }
//    }

    protected void updateGestionnaireIdsTable(final Session session, final AmenagementHydraulique newElement) {
        if (gestionnaireIdsTable == null)
            return;

        if (newElement == null) {
            gestionnaireIdsTable.setTableItems(null);
        } else {
            gestionnaireIdsTable.setParentElement(null);
            final AbstractSIRSRepository<Organisme> gestionnaireIdsRepo = session.getRepositoryForClass(Organisme.class);
            gestionnaireIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getGestionnaireIds(), gestionnaireIdsRepo));
            gestionnaireIdsTable.setObservableListToListen(newElement.getGestionnaireIds());
        }
    }

    protected void updateTronconIdsTable(final Session session, final AmenagementHydraulique newElement) {
        if (tronconIdsTable == null)
            return;

        if (newElement == null) {
            tronconIdsTable.setTableItems(null);
        } else {
            tronconIdsTable.setParentElement(null);
            final AbstractSIRSRepository<TronconDigue> tronconIdsRepo = session.getRepositoryForClass(TronconDigue.class);
            tronconIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getTronconIds(), tronconIdsRepo));
            tronconIdsTable.setObservableListToListen(newElement.getTronconIds());
        }
    }

    protected void updateObservationsTable(final Session session, final AmenagementHydraulique newElement) {
        if (observationsTable == null)
            return;
        // HACK_REDMINE_7544 - to be removed if better fix - hide colomns 'nombre de désordres' and 'niveau d'urgence' for the AH's ObservationDependances
        observationsTable.getTable().getColumns().removeIf(this::shouldBeRemoved);

        if (newElement == null) {
            observationsTable.setTableItems(null);
        } else {
            observationsTable.setParentElement(newElement);
            observationsTable.setTableItems(()-> (ObservableList) newElement.getObservations());
        }
    }

    // HACK_REDMINE_7544 - to be removed if better fix - hide colomns 'nombre de désordres' and 'niveau d'urgence' for the AH's ObservationDependances
    private boolean shouldBeRemoved(TableColumn<?,?> c) {
        return "urgenceId".equals(c.getId()) || "nombreDesordres".equals(c.getId());
    }

    protected void updateTraitIdsTable(final Session session, final AmenagementHydraulique newElement) {
        if (traitsTable == null)
            return;

        if (newElement == null) {
            traitsTable.setTableItems(null);
        } else {
            traitsTable.setParentElement(newElement);
            final TraitAmenagementHydrauliqueRepository traitRepo = (TraitAmenagementHydrauliqueRepository) session.getRepositoryForClass(TraitAmenagementHydraulique.class);
            traitsTable.setTableItems(()-> FXCollections.observableArrayList(traitRepo.getByAmenagementHydrauliqueId(newElement.getId())));
        }
    }

    protected void updateProprietaireIdsTable(final Session session, final AmenagementHydraulique newElement) {
        if (proprietaireIdsTable == null)
            return;

        if (newElement == null) {
            proprietaireIdsTable.setTableItems(null);
        } else {
            proprietaireIdsTable.setParentElement(null);
            final AbstractSIRSRepository<Contact> proprietaireIdsRepo = session.getRepositoryForClass(Contact.class);
            proprietaireIdsTable.setTableItems(()-> SIRS.toElementList(newElement.getProprietaireIds(), proprietaireIdsRepo));
            proprietaireIdsTable.setObservableListToListen(newElement.getProprietaireIds());
        }
    }

//    protected void updateProprietesTable(final Session session, final AmenagementHydraulique newElement) {
//        if (proprietesTable == null)
//            return;
//
//        if (newElement == null) {
//            proprietesTable.setTableItems(null);
//        } else {
//            proprietesTable.setParentElement(newElement);
//            proprietesTable.setTableItems(()-> (ObservableList) newElement.getProprietes());
//        }
//    }

//    protected void updateGestionsTable(final Session session, final AmenagementHydraulique newElement) {
//        if (gestionsTable == null)
//            return;
//
//        if (newElement == null) {
//            gestionsTable.setTableItems(null);
//        } else {
//            gestionsTable.setParentElement(newElement);
//            gestionsTable.setTableItems(()-> (ObservableList) newElement.getGestions());
//        }
//    }

    protected void updatePhotosTable(final Session session, final AmenagementHydraulique newElement) {
        if (photosTable == null)
            return;

        if (newElement == null) {
            photosTable.setTableItems(null);
        } else {
            photosTable.setParentElement(newElement);
            photosTable.setTableItems(()-> (ObservableList) newElement.getPhotos());
        }
    }

    @Override
    public void preSave() {
        final Session session = Injector.getBean(Session.class);
        final AmenagementHydraulique element = (AmenagementHydraulique) elementProperty().get();

        element.setCommentaire(ui_commentaire.getText());

        Object cbValue;
        cbValue = ui_fonctionnementId.getValue();
        if (cbValue instanceof Preview) {
            element.setFonctionnementId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setFonctionnementId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setFonctionnementId(null);
        }
        cbValue = ui_typeId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypeId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypeId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypeId(null);
        }
        cbValue = ui_organismeId.getValue();
        if (cbValue instanceof Preview) {
            element.setOrganismeId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setOrganismeId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setOrganismeId(null);
        }
//        if (desordreIdsTable != null) {
//            // Manage opposite references for Desordre...
//            final List<String> currentDesordreIdsList = new ArrayList<>();
//            for(final Element elt : desordreIdsTable.getAllValues()){
//                final DesordreDependance desordre = (DesordreDependance) elt;
//                currentDesordreIdsList.add(desordre.getId());
//            }
//            element.setDesordreIds(currentDesordreIdsList);
//
//        }
//        if (prestationIdsTable != null) {
//            // Manage opposite references for Prestation...
//            final List<String> currentPrestationIdsList = new ArrayList<>();
//            for(final Element elt : prestationIdsTable.getAllValues()){
//                final PrestationAmenagementHydraulique prestation = (PrestationAmenagementHydraulique) elt;
//                currentPrestationIdsList.add(prestation.getId());
//            }
//            element.setPrestationIds(currentPrestationIdsList);
//        }
        if (gestionnaireIdsTable != null) {
            // Manage opposite references for Organisme...
            final List<String> currentOrganismeIdsList = new ArrayList<>();
            for(final Element elt : gestionnaireIdsTable.getAllValues()){
                final Organisme organisme = (Organisme) elt;
                currentOrganismeIdsList.add(organisme.getId());
            }
            element.setGestionnaireIds(currentOrganismeIdsList);

        }
        if (tronconIdsTable != null) {
            /*
            * En cas de reference opposee on se prepare a stocker les objets
            * "opposes" pour les mettre � jour.
            */
            final List<TronconDigue> currentTronconDigueList = new ArrayList<>();
            /*
            * En cas d'écrasement de référence dans le troncon opposé, on
            * s'assure de détruire le lien coté AH.
            */
            final List<AmenagementHydraulique> currentOppositeAhList = new ArrayList<>();
            // Manage opposite references for TronconDigue...
            /*
            * Si on est sur une reference principale, on a besoin du depot pour
            * supprimer reellement les elements que l'on va retirer du tableau.
            * Si on a une reference opposee, on a besoin du depot pour mettre a jour
            * les objets qui referencent l'objet courant en sens contraire.
            */
            final AbstractSIRSRepository<TronconDigue> tronconDigueRepository = session.getRepositoryForClass(TronconDigue.class);
            final AbstractSIRSRepository<AmenagementHydraulique> ahRepository = Injector.getSession().getRepositoryForClass(AmenagementHydraulique.class);
            final List<String> currentTronconDigueIdsList = new ArrayList<>();
            for(final Element elt : tronconIdsTable.getAllValues()){
                final TronconDigue tronconDigue = (TronconDigue) elt;
                final String tronconId = tronconDigue.getId();
                final String previousAhId = tronconDigue.getAmenagementHydrauliqueId();

                currentTronconDigueIdsList.add(tronconId);
                tronconDigue.setAmenagementHydrauliqueId(element.getId());
                currentTronconDigueList.add(tronconDigue);

                //clean the troncon list of the opposite AHs
                if (previousAhId != null) {
                    AmenagementHydraulique ah = ahRepository.get(previousAhId);
                    ObservableList<String> tronconIds = ah.getTronconIds();
                    tronconIds.removeIf(s -> s.equals(tronconId));
                    ah.setTronconIds(tronconIds);
                    currentOppositeAhList.add(ah);
                }
            }
            tronconDigueRepository.executeBulk(currentTronconDigueList);
            ahRepository.executeBulk(currentOppositeAhList);

            // Manage opposite references for the tronconDigue. Remove the ah
            // reference when it's no more linked with
            List<String> toRemoveIds = element.getTronconIds().stream().filter(id -> !currentTronconDigueIdsList.contains(id)).collect(Collectors.toList());
            List<TronconDigue> toRemoveTroncons = tronconDigueRepository.get(toRemoveIds);
            toRemoveTroncons.stream().forEach(tr -> tr.setAmenagementHydrauliqueId(null));
            tronconDigueRepository.executeBulk(toRemoveTroncons);
            element.setTronconIds(currentTronconDigueIdsList);
        }
        if (proprietaireIdsTable != null) {
            // Manage opposite references for Contact...
            final List<String> currentContactIdsList = new ArrayList<>();
            for(final Element elt : proprietaireIdsTable.getAllValues()){
                final Contact contact = (Contact) elt;
                currentContactIdsList.add(contact.getId());
            }
            element.setProprietaireIds(currentContactIdsList);
        }
    }

    private static final class TronconTable extends ListeningPojoTable<TronconDigue> {
        public TronconTable(String title, final ObjectProperty<AmenagementHydraulique> container) {
            super(TronconDigue.class, title, container);

            final SirsStringConverter converter = new SirsStringConverter();
            final AbstractSIRSRepository<AmenagementHydraulique> repo = Injector.getSession().getRepositoryForClass(AmenagementHydraulique.class);

            // On supprime la colonne des aménagements hydrauliques courantes
            ObservableList<TableColumn<Element, ?>> columns = getColumns();
            TableColumn ahCol = null;
            for (TableColumn col : columns) {
                if ("amenagementHydrauliqueId".equals(col.getId())) {
                    ahCol = col;
                    break;
                }
            }
            columns.remove(ahCol);

            // On ajoute la nouvelle colonne.
            TableColumn newColumn = new TableColumn<>("Aménagement hydraulique");
            newColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TronconDigue, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<TronconDigue, String> param) {
                    if (param!=null && param.getValue()!=null) {
                        final TronconDigue troncon = param.getValue();
                        String amenagementHydrauliqueId = troncon.getAmenagementHydrauliqueId();
                        if (amenagementHydrauliqueId != null) {
                            final AmenagementHydraulique amenagement = repo.get(amenagementHydrauliqueId);
                            return amenagement == null ? new SimpleStringProperty() : new SimpleStringProperty(amenagement.getLibelle());
                        }
                        return new SimpleStringProperty();
                    }
                    return null;
                }
            });
            columns.add(newColumn);
        }

        @Override
        public void setTableItems(Supplier<ObservableList<Element>> producer) {
            // HACK-REDMINE-4408 : hide archived troncons from pojo table
            if (SirsPreferences.getHideArchivedProperty() && repo != null && producer != null) {
                final List<Element> notArchivedElement = new ArrayList<>();
                producer.get().forEach(p -> {
                    TronconDigue t = (TronconDigue) p;
                    if(t.getDate_fin() == null || t.getDate_fin().isAfter(LocalDate.now()))
                        notArchivedElement.add(p);
                });
                super.setTableItems(() -> SIRS.observableList(notArchivedElement));
                return;
            }
            super.setTableItems(producer);
        }
    }
}
