
package fr.sirs.theme.ui;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.Injector;
import fr.sirs.PropertiesFileUtilities;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.*;
import fr.sirs.theme.ui.pojotable.PojoTableExternalAddable;
import fr.sirs.ui.Growl;
import fr.sirs.util.DesignationIncrementer;
import fr.sirs.util.FXFreeTab;
import fr.sirs.util.SirsStringConverter;
import fr.sirs.util.javafx.FloatSpinnerValueFactory;
import fr.sirs.util.javafx.SpinnerUtils;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.internal.GeotkFX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static fr.sirs.core.SirsCore.BUNDLE_KEY_CLASS_ABREGE;

/**
 *
 * @author Estelle Idée (Geomatys)
 */
public class FXPrestationsOnTronconsPane extends AbstractFXElementPane<Prestation> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    @FXML private Button uiSave;

    // Propriétés de Positionable
    @FXML private BorderPane uiPositionable;
    @FXML private FXValidityPeriodPane uiValidityPeriod;

    // Propriétés de Prestation
    @FXML private TextField uiDesignation;
    @FXML protected Label linearId;
    @FXML protected TextField ui_libelle;
    @FXML protected Spinner ui_coutMetre;
    @FXML protected Spinner ui_coutGlobal;
    @FXML protected CheckBox ui_realisationInterne;
    @FXML protected CheckBox ui_registreAttribution;
    @FXML protected ComboBox ui_coteId;
    @FXML protected Button ui_coteId_link;
    @FXML protected ComboBox ui_positionId;
    @FXML protected Button ui_positionId_link;
    @FXML protected ComboBox ui_sourceId;
    @FXML protected Button ui_sourceId_link;
    @FXML protected ComboBox ui_typePrestationId;
    @FXML protected Button ui_typePrestationId_link;
    @FXML protected ComboBox ui_marcheId;
    @FXML protected Button ui_marcheId_link;
    @FXML protected FXFreeTab ui_evenementHydrauliqueIds;
    protected ListeningPojoTable evenementHydrauliqueIdsTable;
    @FXML protected FXFreeTab ui_intervenantsIds;
    protected ListeningPojoTable intervenantsIdsTable;
    @FXML protected FXFreeTab ui_rapportEtudeIds;
    protected ListeningPojoTable rapportEtudeIdsTable;
    @FXML protected FXFreeTab ui_documentGrandeEchelleIds;
    protected ListeningPojoTable documentGrandeEchelleIdsTable;
    @FXML protected FXFreeTab ui_observations;
    protected PojoTable observationsTable;
    @FXML protected FXFreeTab ui_globalPrestationIds;
    protected ListeningPojoTable globalPrestationIdsTable;

    // Propriétés de AvecGeometrie

    // Propriétés de AvecSettableGeometrie

    // Propriétés de Objet
    @FXML protected TextArea ui_commentaire;
    @FXML protected ListView<Preview> uiListTroncon;

    // Propriétés de ObjetPhotographiable
//    @FXML protected FXFreeTab ui_photos;
//    protected PojoTable photosTable;

    // Propriétés de AvecObservations

    private final Class<? extends TronconDigue> tronconClass;

    private static final Map<String, String> linearIdLabel = new HashMap<>();
    static {
        linearIdLabel.put("TronconDigue", "Tronçons");
        linearIdLabel.put("Berge", "Berges");
        linearIdLabel.put("TronconLit", "Lits");
    }


    protected FXPrestationsOnTronconsPane() {
        this(TronconDigue.class);
    }
    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXPrestationsOnTronconsPane(final Class<? extends TronconDigue> tronconClass) {
        ArgumentChecks.ensureNonNull("tronconClass", tronconClass);
        SIRS.loadFXML(this, Prestation.class);
        this.tronconClass = tronconClass;

        final String tronconLabel = linearIdLabel.get(tronconClass.getSimpleName());
        linearId.setText(tronconLabel != null ? tronconLabel : "Tronçons");

        final Session session = Injector.getBean(Session.class);
        previewRepository = session.getPreviews();
        elementProperty().addListener(this::initFields);

        uiValidityPeriod.disableFieldsProperty().bind(disableFieldsProperty());
        uiValidityPeriod.targetProperty().bind(elementProperty());

        /*
         * Disabling rules.
         */
        uiDesignation.disableProperty().bind(disableFieldsProperty());
        ui_libelle.disableProperty().bind(disableFieldsProperty());
        ui_coutMetre.disableProperty().bind(disableFieldsProperty());
        ui_coutMetre.setEditable(true);
        ui_coutMetre.setValueFactory(new FloatSpinnerValueFactory(0, Float.MAX_VALUE));
        SpinnerUtils.commitOnFocusLost(ui_coutMetre);
        ui_coutGlobal.disableProperty().bind(disableFieldsProperty());
        ui_coutGlobal.setEditable(true);
        ui_coutGlobal.setValueFactory(new FloatSpinnerValueFactory(0, Float.MAX_VALUE));
        SpinnerUtils.commitOnFocusLost(ui_coutGlobal);
        ui_realisationInterne.disableProperty().bind(disableFieldsProperty());
        ui_registreAttribution.disableProperty().bind(disableFieldsProperty());
        ui_coteId.disableProperty().bind(disableFieldsProperty());
        ui_coteId_link.setVisible(false);
        ui_positionId.disableProperty().bind(disableFieldsProperty());
        ui_positionId_link.setVisible(false);
        ui_sourceId.disableProperty().bind(disableFieldsProperty());
        ui_sourceId_link.setVisible(false);
        ui_typePrestationId.disableProperty().bind(disableFieldsProperty());
        ui_typePrestationId_link.setVisible(false);
        ui_marcheId.disableProperty().bind(disableFieldsProperty());
        ui_marcheId_link.disableProperty().bind(ui_marcheId.getSelectionModel().selectedItemProperty().isNull());
        ui_marcheId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_marcheId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_marcheId.getSelectionModel().getSelectedItem()));

        ui_evenementHydrauliqueIds.setContent(() -> {
            evenementHydrauliqueIdsTable = new ListeningPojoTable(EvenementHydraulique.class, null, elementProperty());
            evenementHydrauliqueIdsTable.editableProperty().bind(disableFieldsProperty().not());
            evenementHydrauliqueIdsTable.createNewProperty().set(false);
            updateEvenementHydrauliqueIdsTable(session, elementProperty.get());
            return evenementHydrauliqueIdsTable;
        });
        ui_evenementHydrauliqueIds.setClosable(false);

        ui_intervenantsIds.setContent(() -> {
            intervenantsIdsTable = new ListeningPojoTable(Contact.class, null, elementProperty());
            intervenantsIdsTable.editableProperty().bind(disableFieldsProperty().not());
            intervenantsIdsTable.createNewProperty().set(false);
            updateIntervenantsIdsTable(session, elementProperty.get());
            return intervenantsIdsTable;
        });
        ui_intervenantsIds.setClosable(false);

        ui_rapportEtudeIds.setContent(() -> {
            rapportEtudeIdsTable = new ListeningPojoTable(RapportEtude.class, null, elementProperty());
            rapportEtudeIdsTable.editableProperty().bind(disableFieldsProperty().not());
            rapportEtudeIdsTable.createNewProperty().set(false);
            updateRapportEtudeIdsTable(session, elementProperty.get());
            return rapportEtudeIdsTable;
        });
        ui_rapportEtudeIds.setClosable(false);

        ui_documentGrandeEchelleIds.setContent(() -> {
            documentGrandeEchelleIdsTable = new ListeningPojoTable(DocumentGrandeEchelle.class, null, elementProperty());
            documentGrandeEchelleIdsTable.editableProperty().bind(disableFieldsProperty().not());
            documentGrandeEchelleIdsTable.createNewProperty().set(false);
            updateDocumentGrandeEchelleIdsTable(session, elementProperty.get());
            return documentGrandeEchelleIdsTable;
        });
        ui_documentGrandeEchelleIds.setClosable(false);

        ui_observations.setContent(() -> {
            observationsTable = new PojoTableExternalAddable(ObservationPrestation.class, elementProperty());
            observationsTable.editableProperty().bind(disableFieldsProperty().not());
            updateObservationsTable(elementProperty.get());
            return observationsTable;
        });
        ui_observations.setClosable(false);

        ui_globalPrestationIds.setContent(() -> {
            globalPrestationIdsTable = new ListeningPojoTable(GlobalPrestation.class, null, elementProperty());
            globalPrestationIdsTable.editableProperty().bind(disableFieldsProperty().not());
            globalPrestationIdsTable.createNewProperty().set(false);
            updateGlobalPrestationIdsTable(session, elementProperty.get());
            return globalPrestationIdsTable;
        });
        ui_globalPrestationIds.setClosable(false);

        ui_commentaire.setWrapText(true);
        ui_commentaire.editableProperty().bind(disableFieldsProperty().not());

//        ui_photos.setContent(() -> {
//            photosTable = new PojoTable(Photo.class, null, elementProperty());
//            photosTable.editableProperty().bind(disableFieldsProperty().not());
//            updatePhotosTable(session, elementProperty.get());
//            return photosTable;
//        });
//        ui_photos.setClosable(false);

        // The Prestation shall automatically be added to the SE register depending on the type of Prestation.
        ui_typePrestationId.valueProperty().addListener(autoSelectRegistreListener());

        final AbstractSIRSRepository<Prestation> repo = Injector.getSession().getRepositoryForClass(Prestation.class);
        this.elementProperty().set(repo.create());
    }

    /**
     * Change listener on typePrestationId to auto select/deselect the registreAttribution checkbox.
     * @return the @{@link ChangeListener}
     */
    private ChangeListener<RefPrestation> autoSelectRegistreListener() {
        return (obs, oldValue, newValue) -> {
            if (newValue != null) {
                final Prestation prestation = elementProperty().get();
                if (prestation != null) {
                    prestation.setRegistreAttribution(FXPrestationPane.isAutoSelectedRegistre(newValue.getId()));
                }
            }
        };
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends Prestation > observableElement, Prestation oldElement, Prestation newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
        // Propriétés de Prestation
            uiDesignation.textProperty().unbindBidirectional(oldElement.designationProperty());
            ui_libelle.textProperty().unbindBidirectional(oldElement.libelleProperty());
            ui_libelle.setText(null);
            ui_coutMetre.getValueFactory().valueProperty().unbindBidirectional(oldElement.coutMetreProperty());
            ui_coutMetre.getValueFactory().setValue(0);
            ui_coutGlobal.getValueFactory().valueProperty().unbindBidirectional(oldElement.coutGlobalProperty());
            ui_coutGlobal.getValueFactory().setValue(0);
            ui_realisationInterne.selectedProperty().unbindBidirectional(oldElement.realisationInterneProperty());
            ui_realisationInterne.setSelected(false);
            ui_registreAttribution.selectedProperty().unbindBidirectional(oldElement.registreAttributionProperty());
            ui_registreAttribution.setSelected(false);
        // Propriétés de Objet
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());
            ui_commentaire.setText(null);
        }

        final Session session = Injector.getBean(Session.class);

        if (newElement == null) {
            uiDesignation.textProperty().set(null);
            ui_coteId.setItems(null);
            ui_positionId.setItems(null);
            ui_sourceId.setItems(null);
            ui_typePrestationId.setItems(null);
            ui_marcheId.setItems(null);
        } else {

            /*
             * Bind control properties to Element ones.
             */
            // Propriétés de Prestation
            // * libelle
            uiDesignation.textProperty().bindBidirectional(newElement.designationProperty());
            ui_libelle.textProperty().bindBidirectional(newElement.libelleProperty());
            // * coutMetre
            ui_coutMetre.getValueFactory().valueProperty().bindBidirectional(newElement.coutMetreProperty());
            // * coutGlobal
            ui_coutGlobal.getValueFactory().valueProperty().bindBidirectional(newElement.coutGlobalProperty());
            // * realisationInterne
            ui_realisationInterne.selectedProperty().bindBidirectional(newElement.realisationInterneProperty());
            // * registreAttribution
            ui_registreAttribution.selectedProperty().bindBidirectional(newElement.registreAttributionProperty());

            final AbstractSIRSRepository<RefCote> coteIdRepo = session.getRepositoryForClass(RefCote.class);
            SIRS.initCombo(ui_coteId, SIRS.observableList(coteIdRepo.getAll()), (newElement.getCoteId() == null || newElement.getCoteId().trim().isEmpty()) ? null : coteIdRepo.get(newElement.getCoteId()));
            final AbstractSIRSRepository<RefPosition> positionIdRepo = session.getRepositoryForClass(RefPosition.class);
            SIRS.initCombo(ui_positionId, SIRS.observableList(positionIdRepo.getAll()), (newElement.getPositionId() == null || newElement.getPositionId().trim().isEmpty()) ? null : positionIdRepo.get(newElement.getPositionId()));
            final AbstractSIRSRepository<RefSource> sourceIdRepo = session.getRepositoryForClass(RefSource.class);
            SIRS.initCombo(ui_sourceId, SIRS.observableList(sourceIdRepo.getAll()), (newElement.getSourceId() == null || newElement.getSourceId().trim().isEmpty()) ? null : sourceIdRepo.get(newElement.getSourceId()));
            final AbstractSIRSRepository<RefPrestation> typePrestationIdRepo = session.getRepositoryForClass(RefPrestation.class);
            SIRS.initCombo(ui_typePrestationId, SIRS.observableList(typePrestationIdRepo.getAll()), (newElement.getTypePrestationId() == null || newElement.getTypePrestationId().trim().isEmpty()) ? null : typePrestationIdRepo.get(newElement.getTypePrestationId()));

            final boolean hideArchivedProperty = SirsPreferences.getHideArchivedProperty();
            {
                final Preview linearPreview = newElement.getMarcheId() == null ? null : previewRepository.get(newElement.getMarcheId());
                // HACK-REDMINE-4408 : hide archived troncons from selection lists
                SIRS.initCombo(ui_marcheId, SIRS.observableList(
                    previewRepository.getByClass(linearPreview == null ? Marche.class : linearPreview.getJavaClassOr(Marche.class))).sorted(),
                    linearPreview, hideArchivedProperty, true);
            }

            // * commentaire
            ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());

            // Set the available troncons list.
            {
                // TODO find a way to get the type of Troncon : TronconDigue, Berge or Lit
                List<Preview> tronconsPreviews = previewRepository.getByClass(tronconClass);

                // Filter archived troncons depending on user's preferences.
                if (hideArchivedProperty && !tronconsPreviews.isEmpty()) {
                    tronconsPreviews = tronconsPreviews.stream().filter(new AvecFinTemporelle.IsNotArchivedPredicate()).collect(Collectors.toList());
                }

                uiListTroncon.setItems(SIRS.observableList(tronconsPreviews).sorted(Comparator.comparing(new SirsStringConverter()::toString)));
                uiListTroncon.setCellFactory(TextFieldListCell.forListView(new SirsStringConverter()));
                uiListTroncon.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            }
        }

        updateEvenementHydrauliqueIdsTable(session, newElement);
        updateIntervenantsIdsTable(session, newElement);
        updateRapportEtudeIdsTable(session, newElement);
        updateDocumentGrandeEchelleIdsTable(session, newElement);
        updateObservationsTable(newElement);
        updateGlobalPrestationIdsTable(session, newElement);
//        updatePhotosTable(session, newElement);
    }

    protected void updateEvenementHydrauliqueIdsTable(final Session session, final Prestation newElement) {
        if (evenementHydrauliqueIdsTable == null)
            return;

        if (newElement == null) {
            evenementHydrauliqueIdsTable.setTableItems(null);
        } else {
            evenementHydrauliqueIdsTable.setParentElement(null);
            final AbstractSIRSRepository<EvenementHydraulique> evenementHydrauliqueIdsRepo = session.getRepositoryForClass(EvenementHydraulique.class);
            final ObservableList<String> observableList = newElement.getEvenementHydrauliqueIds();
            evenementHydrauliqueIdsTable.setTableItems(()-> SIRS.toElementList(observableList, evenementHydrauliqueIdsRepo));
            evenementHydrauliqueIdsTable.setObservableListToListen(observableList);
        }
    }


    protected void updateIntervenantsIdsTable(final Session session, final Prestation newElement) {
        if (intervenantsIdsTable == null)
            return;

        if (newElement == null) {
            intervenantsIdsTable.setTableItems(null);
        } else {
            intervenantsIdsTable.setParentElement(null);
            final AbstractSIRSRepository<Contact> intervenantsIdsRepo = session.getRepositoryForClass(Contact.class);
            final ObservableList<String> observableList = newElement.getIntervenantsIds();
            intervenantsIdsTable.setTableItems(()-> SIRS.toElementList(observableList, intervenantsIdsRepo));
            intervenantsIdsTable.setObservableListToListen(observableList);
        }
    }


    protected void updateRapportEtudeIdsTable(final Session session, final Prestation newElement) {
        if (rapportEtudeIdsTable == null)
            return;

        if (newElement == null) {
            rapportEtudeIdsTable.setTableItems(null);
        } else {
            rapportEtudeIdsTable.setParentElement(null);
            final AbstractSIRSRepository<RapportEtude> rapportEtudeIdsRepo = session.getRepositoryForClass(RapportEtude.class);
            final ObservableList<String> observableList = newElement.getRapportEtudeIds();
            rapportEtudeIdsTable.setTableItems(()-> SIRS.toElementList(observableList, rapportEtudeIdsRepo));
            rapportEtudeIdsTable.setObservableListToListen(observableList);
        }
    }


    protected void updateDocumentGrandeEchelleIdsTable(final Session session, final Prestation newElement) {
        if (documentGrandeEchelleIdsTable == null)
            return;

        if (newElement == null) {
            documentGrandeEchelleIdsTable.setTableItems(null);
        } else {
            documentGrandeEchelleIdsTable.setParentElement(null);
            final AbstractSIRSRepository<DocumentGrandeEchelle> documentGrandeEchelleIdsRepo = session.getRepositoryForClass(DocumentGrandeEchelle.class);
            final ObservableList<String> observableList = newElement.getDocumentGrandeEchelleIds();
            documentGrandeEchelleIdsTable.setTableItems(()-> SIRS.toElementList(observableList, documentGrandeEchelleIdsRepo));
            documentGrandeEchelleIdsTable.setObservableListToListen(observableList);
        }
    }


    protected void updateObservationsTable(final Prestation newElement) {
        if (observationsTable == null)
            return;

        if (newElement == null) {
            observationsTable.setTableItems(null);
        } else {
            observationsTable.setParentElement(newElement);
            observationsTable.setTableItems(()-> (ObservableList) newElement.getObservations());
        }
    }

    protected void updateGlobalPrestationIdsTable(final Session session, final Prestation newElement) {
        if (globalPrestationIdsTable == null)
            return;

        if (newElement == null) {
            globalPrestationIdsTable.setTableItems(null);
        } else {
            globalPrestationIdsTable.setParentElement(null);
            final AbstractSIRSRepository<GlobalPrestation> globalPrestationIdsRepo = session.getRepositoryForClass(GlobalPrestation.class);
            final ObservableList<String> observableList = newElement.getGlobalPrestationIds();
            globalPrestationIdsTable.setTableItems(()-> SIRS.toElementList(observableList, globalPrestationIdsRepo));
            globalPrestationIdsTable.setObservableListToListen(observableList);
        }
    }


//    protected void updatePhotosTable(final Session session, final Prestation newElement) {
//        if (photosTable == null)
//            return;
//
//        if (newElement == null) {
//            photosTable.setTableItems(null);
//        } else {
//            photosTable.setParentElement(newElement);
//            photosTable.setTableItems(()-> (ObservableList) newElement.getPhotos());
//        }
//    }


    @Override
    public void preSave() {
        final Prestation element = elementProperty.get();

        element.setCommentaire(ui_commentaire.getText());

        Object cbValue;
        cbValue = ui_coteId.getValue();
        if (cbValue instanceof Preview) {
            element.setCoteId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setCoteId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setCoteId(null);
        }
        cbValue = ui_positionId.getValue();
        if (cbValue instanceof Preview) {
            element.setPositionId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setPositionId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setPositionId(null);
        }
        cbValue = ui_sourceId.getValue();
        if (cbValue instanceof Preview) {
            element.setSourceId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setSourceId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setSourceId(null);
        }
        cbValue = ui_typePrestationId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypePrestationId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypePrestationId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypePrestationId(null);
        }
        cbValue = ui_marcheId.getValue();
        if (cbValue instanceof Preview) {
            element.setMarcheId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setMarcheId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setMarcheId(null);
        }
        if (evenementHydrauliqueIdsTable != null) {

            // Manage opposite references for EvenementHydraulique...
            final List<String> currentEvenementHydrauliqueIdsList = new ArrayList<>();
            for (final Element elt : evenementHydrauliqueIdsTable.getAllValues()) {
                final EvenementHydraulique evenementHydraulique = (EvenementHydraulique) elt;

                currentEvenementHydrauliqueIdsList.add(evenementHydraulique.getId());
            }
            element.setEvenementHydrauliqueIds(currentEvenementHydrauliqueIdsList);

        }
        if (intervenantsIdsTable != null) {

            // Manage opposite references for Contact...
            final List<String> currentContactIdsList = new ArrayList<>();
            for (final Element elt : intervenantsIdsTable.getAllValues()) {
                final Contact contact = (Contact) elt;

                currentContactIdsList.add(contact.getId());
            }
            element.setIntervenantsIds(currentContactIdsList);

        }
        if (rapportEtudeIdsTable != null) {
            // Manage opposite references for RapportEtude...
            /*
             * Add the RapportEtudes' ids to the prestation.
             * The opposite relation will be done in the save() method.
             */
            final ObservableList<Element> rapportEtudes = rapportEtudeIdsTable.getAllValues();
            if (!rapportEtudes.isEmpty()) {
                element.setRapportEtudeIds(rapportEtudes.stream().map(Identifiable::getId).collect(Collectors.toList()));
            }
        }
        if (documentGrandeEchelleIdsTable != null) {

            // Manage opposite references for DocumentGrandeEchelle...
            final List<String> currentDocumentGrandeEchelleIdsList = new ArrayList<>();
            for (final Element elt : documentGrandeEchelleIdsTable.getAllValues()) {
                final DocumentGrandeEchelle documentGrandeEchelle = (DocumentGrandeEchelle) elt;

                currentDocumentGrandeEchelleIdsList.add(documentGrandeEchelle.getId());
            }
            element.setDocumentGrandeEchelleIds(currentDocumentGrandeEchelleIdsList);

        }
        if (globalPrestationIdsTable != null) {
            // Manage opposite references for GlobalPrestation...
            /*
            * Add the GlobalPrestations' ids to the prestation.
            * The opposite relation will be done in the save() method.
            */
            final ObservableList<Element> globalPrestations = globalPrestationIdsTable.getAllValues();
            if (!globalPrestations.isEmpty()) {
                element.setGlobalPrestationIds(globalPrestations.stream().map(Identifiable::getId).collect(Collectors.toList()));
            }
        }
    }

    public void save() {
        final ObservableList<Preview> selectedtroncons = uiListTroncon.getSelectionModel().getSelectedItems();
        if (selectedtroncons.isEmpty()) {
            PropertiesFileUtilities.showErrorDialog("L'élément ne peut être enregistré sans élément parent. \nVeuillez sélectionner au moins un tronçon de la liste.", "Erreur lors de l'enregistrement", 0, 150);
            return;
        }

        final LabelMapper labelMapper = LabelMapper.get(tronconClass);
        final String classAbrege = (labelMapper == null) ? "" : labelMapper.mapPropertyName(BUNDLE_KEY_CLASS_ABREGE) + " - ";

        final StringBuilder tronconsList = new StringBuilder();
        final Map<Preview, String> tronconsText = new HashMap<>();
        String designation;
        String s;
        for (Preview t : selectedtroncons) {
            designation = t.getDesignation();
            designation = designation == null ? "" : designation;
            s = "\n    - " + classAbrege + designation + " : " + t.getLibelle();
            tronconsList.append(s);
            tronconsText.put(t, s);
        }

        final Optional dialog = PropertiesFileUtilities.showConfirmationDialog("Souhaitez-vous créer cette prestation sur les tronçons suivants : \n" + tronconsList, "Confirmation de créaion multiple", 500, 500, true);

        if (dialog.isPresent()) {
            final Object result = dialog.get();
            if (!ButtonType.YES.equals(result)) {
                return;
            }
        }

        final Session session = Injector.getSession();
        final AbstractSIRSRepository<Prestation> repo = session.getRepositoryForClass(Prestation.class);
        final List<Prestation> createdPrestations = new ArrayList<>();
        final StringBuilder tronconsOk = new StringBuilder();
        final Map<String, Exception> tronconsNotOk = new HashMap<>();
        final StringBuilder errorForTroncon = new StringBuilder();
        final List<Element> globalPrestationList = globalPrestationIdsTable != null ? globalPrestationIdsTable.getAllValues() : null;
        final List<Element> rapportEtudeList = rapportEtudeIdsTable != null ? rapportEtudeIdsTable.getAllValues() : null;

        this.preSave();

        final AbstractSIRSRepository<? extends TronconDigue> tronconRepo = session.getRepositoryForClass(tronconClass);
        final AbstractSIRSRepository<BorneDigue> borneRepo = session.getRepositoryForClass(BorneDigue.class);
        final CoordinateReferenceSystem crs = session.getProjection();

        final Prestation prestation = elementProperty.get();

        Prestation copy = null;
        boolean tryAutoIncrement = Boolean.parseBoolean(SirsPreferences.INSTANCE.getPropertySafe(SirsPreferences.PROPERTIES.DESIGNATION_AUTO_INCREMENT)) && DesignationIncrementer.designationAsInt(prestation) != 0; //do not increment if the user has manually set a designation that is not a number
        boolean notFirst = false;


        final ElementCreator elementCreator;
        if (tryAutoIncrement) {
            elementCreator = Injector.getSession().getElementCreator();
        } else {
            elementCreator = null;
        }

        for (Preview tronconP : selectedtroncons) {
            try {
                copy = prestation.copy();
                copy.setLinearId(tronconP.getElementId());
                final TronconDigue troncon = tronconRepo.get(tronconP.getElementId());

                if (tryAutoIncrement && notFirst) {

                    // Determine an auto-incremented value for designation
                    elementCreator.tryAutoIncrementDesignation(copy);
                } else {
                    notFirst = true;
                }
                copy.setSystemeRepId(troncon.getSystemeRepDefautId());
                final ObservableList<String> borneIds = troncon.getBorneIds();
                final List<BorneDigue> borneDigues = borneRepo.get(borneIds);

                // The prestations must be on the whole TronconDigue.
                // So we get the bornes corresponding to the Troncon's start and end bornes.
                // PRs are automatically set to 0 so no need to update the values.
                String borneStartId = null;
                String borneEndId = null;
                for (BorneDigue borneDigue : borneDigues) {
                    final String libelle = borneDigue.getLibelle();
                    if (SirsCore.SR_ELEMENTAIRE_START_BORNE.equals(libelle)) {
                        borneStartId = borneDigue.getId();
                    } else if (SirsCore.SR_ELEMENTAIRE_END_BORNE.equals(libelle)) {
                        borneEndId = borneDigue.getId();
                    }
                    if (borneStartId != null && borneEndId != null) break;
                }

                if (borneStartId != null && borneEndId != null) {
                    prestation.setGeometryMode(FXPositionableLinearMode.MODE);
                    prestation.setEditedGeoCoordinate(false);
                    copy.setBorneDebutId(borneStartId);
                    copy.setBorneFinId(borneEndId);

                } else {
                    final Geometry geometry = troncon.getGeometry();
                    // If we can't save the prestation on its troncon geometry, the prestation is still created but with no location.
                    // A warning will pop at the end of the process to inform the user.
                    if (!(geometry instanceof LineString)) {
                        errorForTroncon.append(tronconsText.get(tronconP));
                    } else {
                        final Coordinate[] coordinates = geometry.getCoordinates();

                        final Point positionDebut = EditionHelper.createPoint(coordinates[0]);
                        JTS.setCRS(positionDebut, crs);

                        final Point positionFin = EditionHelper.createPoint(coordinates[coordinates.length - 1]);
                        JTS.setCRS(positionFin, crs);

                        copy.setPositionDebut(positionDebut);
                        copy.setPositionFin(positionFin);
                        prestation.setGeometryMode(FXPositionableCoordMode.MODE);
                        prestation.setEditedGeoCoordinate(true);
                    }

                }


                repo.add(copy);

                createdPrestations.add(copy);
                tronconsOk.append(tronconsText.get(tronconP));

            } catch (Exception e) {
                tronconsNotOk.put(tronconsText.get(tronconP), e);
                if (copy != null && copy.getId() != null) {
                    repo.remove(copy);
                }
            }
        }

        // Update n-n relations
        /* GlobalPrestation */
        if (globalPrestationList != null) {
            final AbstractSIRSRepository<GlobalPrestation> globalPrestationRepository = session.getRepositoryForClass(GlobalPrestation.class);
            final List<GlobalPrestation> gbToUpdate = new ArrayList<>();
            for (final Element elt : globalPrestationList) {
                final GlobalPrestation globalPrestation = (GlobalPrestation) elt;

                for (Prestation presta : createdPrestations) {
                    // Addition
                    final ObservableList<String> prestationIds = globalPrestation.getPrestationIds();
                    if (!prestationIds.contains(presta.getId())) {
                        prestationIds.add(presta.getId());
                    }
                    gbToUpdate.add(globalPrestation);
                }

            }
            globalPrestationRepository.executeBulk(gbToUpdate);
        }

        /* RapportEtude */
        if (rapportEtudeList != null) {
            final AbstractSIRSRepository<RapportEtude> rapportEtudeRepository = session.getRepositoryForClass(RapportEtude.class);
            final List<RapportEtude> reToUpdate = new ArrayList<>();
            for (final Element elt : rapportEtudeList) {
                final RapportEtude rapportEtude = (RapportEtude) elt;

                for (Prestation presta : createdPrestations) {
                    // Addition
                    final ObservableList<String> prestationIds = rapportEtude.getPrestationIds();
                    if (!prestationIds.contains(presta.getId())) {
                        prestationIds.add(presta.getId());
                    }
                    reToUpdate.add(rapportEtude);
                }

            }
            rapportEtudeRepository.executeBulk(reToUpdate);
        }

        if (tronconsOk.length() != 0) {
            PropertiesFileUtilities.showInformationDialog(
                    "Les prestations ont été créées sur les tronçons suivants : \n" + tronconsOk,
                    "Succès", 500, 300);
        }
        if (errorForTroncon.length() != 0) {
            errorForTroncon.insert(0, "Prestations créées mais incomplètes. La géométrie n'a pas pu être renseignée pour les tronçons suivants : \n");
            errorForTroncon.append("Raison : La géométrie du tronçon n'est pas un lineString.");

            final Growl growlError = new Growl(Growl.Type.ERROR, "Erreurs survenues pendant l'enregistrement.");
            growlError.showAndFade();

            PropertiesFileUtilities.showWarningDialog(
                    errorForTroncon.toString(),
                    "Erreur lors de l'enregistrement", 500, 300);
        }
        if (!tronconsNotOk.isEmpty()) {
            tronconsNotOk.forEach((t, e) -> {
                GeotkFX.newExceptionDialog("La prestation sur le tronçon "  + t + " \nn'a pas pu être créée.", e).show();
                SIRS.LOGGER.log(Level.WARNING, e.getMessage(), e);
            });
        }

    }

    /**
     * Method called when closing the FXFreeTab containing this Pane.
     * <p>
     * This method is necessary as when reopening the element's pane,
     * a new tab is created and the old one remains existing with its listeners,
     * leading to as many calls to the listeners as the tab was closed/reopened
     * <p>
     * Method to be overriden in case more listeners are added in the FXPane of the element.
     */
    @Override
    public void removeListenersBeforeClosingTab() {
            uiValidityPeriod.removeListeners();
    }
}
