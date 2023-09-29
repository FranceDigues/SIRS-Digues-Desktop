
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.*;
import fr.sirs.util.FXFreeTab;
import fr.sirs.util.javafx.FloatSpinnerValueFactory;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Maxime Gavens (Geomatys)
 */
public class FXOrganeProtectionCollectivePane extends AbstractFXElementPane<OrganeProtectionCollective> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    @FXML private FXValidityPeriodPane uiValidityPeriod;

    // Propriétés de OrganeProtectionCollective
    @FXML protected TextField ui_libelle;
    @FXML protected Spinner ui_cote;
    @FXML protected ComboBox ui_typeId;
    @FXML protected ComboBox ui_etatId;
    @FXML protected TextArea ui_commentaire;
    @FXML protected FXFreeTab ui_observations;
    protected PojoTable observationsTable;
    @FXML protected FXFreeTab ui_photos;
    protected PojoTable photosTable;

    // Propriétés de AvecGeometrie

    // Propriétés de AvecSettableGeometrie

    // Propriétés de AbstractAmenagementHydraulique
    @FXML protected ComboBox ui_amenagementHydrauliqueId;
    @FXML protected Button ui_amenagementHydrauliqueId_link;

    @FXML FXPositionDependancePane uiPosition;

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXOrganeProtectionCollectivePane() {
        SIRS.loadFXML(this, OrganeProtectionCollective.class);
        final Session session = Injector.getBean(Session.class);
        previewRepository = session.getPreviews();
        elementProperty().addListener(this::initFields);

        uiValidityPeriod.disableFieldsProperty().bind(disableFieldsProperty());
        uiValidityPeriod.targetProperty().bind(elementProperty());

        /*
        * Disabling rules.
        */
        ui_libelle.disableProperty().bind(disableFieldsProperty());
        ui_cote.disableProperty().bind(disableFieldsProperty());
        ui_cote.setEditable(true);
        ui_cote.setValueFactory(new FloatSpinnerValueFactory(0, Float.MAX_VALUE));
        ui_typeId.disableProperty().bind(disableFieldsProperty());
        ui_etatId.disableProperty().bind(disableFieldsProperty());
        uiPosition.disableFieldsProperty().bind(disableFieldsProperty());
        ui_commentaire.disableProperty().bind(disableFieldsProperty());
        ui_commentaire.setWrapText(true);

        uiPosition.dependanceProperty().bind(elementProperty);

        ui_observations.setContent(() -> {
            observationsTable = new PojoTable(ObservationDependance.class, null, elementProperty());
            observationsTable.editableProperty().bind(disableFieldsProperty().not());
            updateObservationsTable(session, elementProperty.get());
            return observationsTable;
        });
        ui_observations.setClosable(false);

        ui_photos.setContent(() -> {
            photosTable = new PojoTable(PhotoDependance.class, null, elementProperty());
            photosTable.editableProperty().bind(disableFieldsProperty().not());
            updatePhotosTable(session, elementProperty.get());
            return photosTable;
        });
        ui_photos.setClosable(false);
        ui_amenagementHydrauliqueId.disableProperty().bind(disableFieldsProperty());
        ui_amenagementHydrauliqueId_link.disableProperty().bind(ui_amenagementHydrauliqueId.getSelectionModel().selectedItemProperty().isNull());
        ui_amenagementHydrauliqueId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_amenagementHydrauliqueId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_amenagementHydrauliqueId.getSelectionModel().getSelectedItem()));
    }

    public FXOrganeProtectionCollectivePane(final OrganeProtectionCollective organeProtectionCollective){
        this();
        this.elementProperty().set(organeProtectionCollective);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends OrganeProtectionCollective > observableElement, OrganeProtectionCollective oldElement, OrganeProtectionCollective newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
            // Propriétés de OrganeProtectionCollective
            ui_libelle.textProperty().unbindBidirectional(oldElement.libelleProperty());
            ui_libelle.setText(null);
            ui_cote.getValueFactory().valueProperty().unbindBidirectional(oldElement.coteProperty());
            ui_cote.getValueFactory().setValue(0);
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());
            ui_commentaire.setText(null);
            // Propriétés de AvecGeometrie
            // Propriétés de AvecSettableGeometrie
            // Propriétés de AbstractAmenagementHydraulique
        }

        final Session session = Injector.getBean(Session.class);

        if (newElement == null) {

            ui_typeId.setItems(null);
            ui_etatId.setItems(null);
            ui_amenagementHydrauliqueId.setItems(null);
            ui_commentaire.setText(null);
        } else {


            /*
            * Bind control properties to Element ones.
            */
            // Propriétés de OrganeProtectionCollective
            // * libelle
            ui_libelle.textProperty().bindBidirectional(newElement.libelleProperty());
            // * cote
            ui_cote.getValueFactory().valueProperty().bindBidirectional(newElement.coteProperty());
            // * commentaire
            ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());
            final AbstractSIRSRepository<RefTypeOrganeProtectionCollective> typeRepo = session.getRepositoryForClass(RefTypeOrganeProtectionCollective.class);
            SIRS.initCombo(ui_typeId, SIRS.observableList(typeRepo.getAll()), newElement.getTypeId() == null? null : typeRepo.get(newElement.getTypeId()));
            final AbstractSIRSRepository<RefEtat> etatRepo = session.getRepositoryForClass(RefEtat.class);
            SIRS.initCombo(ui_etatId, SIRS.observableList(etatRepo.getAll()), newElement.getEtatId() == null? null : etatRepo.get(newElement.getEtatId()));
            // Propriétés de AvecGeometrie
            // Propriétés de AvecSettableGeometrie
            // Propriétés de AbstractAmenagementHydraulique
            Preview linearPreview = newElement.getAmenagementHydrauliqueId() == null ? null : previewRepository.get(newElement.getAmenagementHydrauliqueId());
            final List<Preview> byClass = previewRepository.getByClass(linearPreview == null ? AmenagementHydraulique.class : linearPreview.getJavaClassOr(AmenagementHydraulique.class));
            final List<Preview> withoutEmptyPreview = byClass.stream().filter(p -> p.getElementId() != null).collect(Collectors.toList());
            final ObservableList<Preview> sorted = SIRS.observableList(withoutEmptyPreview).sorted();

            if (linearPreview == null && sorted.size() >= 1) {
                linearPreview = sorted.get(0);
            }
            // HACK-REDMINE-4408 : hide archived AH from selection lists
            SIRS.initCombo(ui_amenagementHydrauliqueId, sorted, linearPreview, SirsPreferences.getHideArchivedProperty(), true);
        }

        updateObservationsTable(session, newElement);
        updatePhotosTable(session, newElement);
    }


    protected void updateObservationsTable(final Session session, final OrganeProtectionCollective newElement) {
        if (observationsTable == null)
            return;

        if (newElement == null) {
            observationsTable.setTableItems(null);
        } else {
            observationsTable.setParentElement(newElement);
            observationsTable.setTableItems(()-> (ObservableList) newElement.getObservations());
        }
    }


    protected void updatePhotosTable(final Session session, final OrganeProtectionCollective newElement) {
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
        final OrganeProtectionCollective element = (OrganeProtectionCollective) elementProperty().get();




        Object cbValue;
        cbValue = ui_typeId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypeId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypeId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypeId(null);
        }
        cbValue = ui_etatId.getValue();
        if (cbValue instanceof Preview) {
            element.setEtatId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setEtatId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setEtatId(null);
        }
        cbValue = ui_amenagementHydrauliqueId.getValue();
        if (cbValue instanceof Preview) {
            element.setAmenagementHydrauliqueId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setAmenagementHydrauliqueId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setAmenagementHydrauliqueId(null);
        }
    }
}