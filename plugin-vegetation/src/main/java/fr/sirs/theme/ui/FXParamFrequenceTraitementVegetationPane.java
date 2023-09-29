package fr.sirs.theme.ui;

import fr.sirs.Session;
import fr.sirs.SIRS;
import fr.sirs.Injector;
import fr.sirs.core.component.*;
import fr.sirs.core.model.*;
import fr.sirs.plugin.vegetation.PluginVegetation;
import static fr.sirs.plugin.vegetation.PluginVegetation.sousTypeTraitementFromTypeTraitementId;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javafx.beans.value.ChangeListener;

/**
 *
 * @author Maxime Gavens (Geomatys)
 */
public class FXParamFrequenceTraitementVegetationPane extends AbstractFXElementPane<ParamFrequenceTraitementVegetation> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    // Document parent
    @FXML protected Label ui_parent_label;
    @FXML protected HBox ui_parent_box;
    @FXML protected ComboBox ui_parent_choice;
    @FXML protected Button ui_parent_link;

    /** A reference to the parent of the edited element. We keep it to be able to update old parent if a new one is set. */
    private Element originalParent;

    // Propriétés de ParamFrequenceTraitementVegetation
    @FXML protected ComboBox ui_typeVegetationId;
    @FXML protected Button ui_typeVegetationId_link;
    @FXML protected ComboBox ui_typeTraitementId;
    @FXML protected Button ui_typeTraitementId_link;
    @FXML protected ComboBox ui_sousTypeTraitementId;
    @FXML protected Button ui_sousTypeTraitementId_link;
    @FXML protected ComboBox ui_frequenceId;
    @FXML protected Button ui_frequenceId_link;

    @FXML ComboBox<Class<? extends ZoneVegetation>> ui_type;

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXParamFrequenceTraitementVegetationPane() {
        SIRS.loadFXML(this, ParamFrequenceTraitementVegetation.class);
        final Session session = Injector.getBean(Session.class);
        previewRepository = session.getPreviews();
        elementProperty().addListener(this::initFields);

        ui_parent_box.managedProperty().bind(ui_parent_box.visibleProperty());
        ui_parent_link.disableProperty().bind(ui_parent_choice.getSelectionModel().selectedItemProperty().isNull());
        ui_parent_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_parent_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_parent_choice.getSelectionModel().getSelectedItem()));
        ui_parent_choice.disableProperty().bind(disableFieldsProperty());

        /*
         * Disabling rules.
         */
        ui_typeVegetationId.disableProperty().bind(disableFieldsProperty());
        ui_typeVegetationId_link.setVisible(false);
        ui_typeTraitementId.disableProperty().bind(disableFieldsProperty());
        ui_typeTraitementId_link.setVisible(false);
        ui_sousTypeTraitementId.disableProperty().bind(disableFieldsProperty());
        ui_sousTypeTraitementId_link.setVisible(false);
        ui_frequenceId.disableProperty().bind(disableFieldsProperty());
        ui_frequenceId_link.setVisible(false);

        ui_type.disableProperty().bind(disableFieldsProperty());

        ui_type.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Class<? extends ZoneVegetation>>() {

            @Override
            public void changed(ObservableValue<? extends Class<? extends ZoneVegetation>> observable, Class<? extends ZoneVegetation> oldValue, Class<? extends ZoneVegetation> newValue) {
                initTypeVegetation(newValue);
            }
        });

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
    }

    public FXParamFrequenceTraitementVegetationPane(final ParamFrequenceTraitementVegetation paramFrequenceTraitementVegetation){
        this();
        this.elementProperty().set(paramFrequenceTraitementVegetation);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends ParamFrequenceTraitementVegetation > observableElement, ParamFrequenceTraitementVegetation oldElement, ParamFrequenceTraitementVegetation newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
        // Propriétés de ParamFrequenceTraitementVegetation
        }

        final Session session = Injector.getBean(Session.class);

        if (newElement == null) {
                ui_parent_choice.setItems(null);
                ui_parent_choice.setDisable(true);

                ui_typeVegetationId.setItems(null);
                ui_typeTraitementId.setItems(null);
                ui_sousTypeTraitementId.setItems(null);
                ui_frequenceId.setItems(null);
        } else {

        originalParent = newElement.getParent();
        if (originalParent == null && newElement.getDocumentId()!=null) {
            final Preview preview = previewRepository.get(newElement.getDocumentId());
            final AbstractSIRSRepository<Element> repository = session.getRepositoryForType(preview.getElementClass());
            originalParent = repository.get(newElement.getDocumentId());
        }

        /* Try to build a list of possible parents. If parent element is not the CouchDb document (depth level > 1),
         * we will just display current parent, and won't allow user to change it, because it's too unpredictible.
         */
        final String documentId = newElement.getDocumentId();
        if (originalParent != null && !originalParent.getId().equals(documentId)) {
            ui_parent_choice.disableProperty().unbind();
            ui_parent_choice.setDisable(true);
            SIRS.initCombo(ui_parent_choice, FXCollections.singletonObservableList(originalParent), originalParent);

        } else if (documentId != null) {
            final Preview parentLabel = previewRepository.get(documentId);
            SIRS.initCombo(ui_parent_choice, SIRS.observableList(previewRepository.getByClass(parentLabel.getElementClass())).sorted(), parentLabel);
        } else if (Objet.class.isAssignableFrom(newElement.getClass())
                    || PositionDocument.class.isAssignableFrom(newElement.getClass())) {
            SIRS.initCombo(ui_parent_choice, SIRS.observableList(previewRepository.getByClass(TronconDigue.class)).sorted(), null);
        }
        // Prepare parent edition. If we've got a list of potential parents, we display combobox, and notify edited element on selection change.
        ObservableList items = ui_parent_choice.getItems();
        if (items == null || items.isEmpty()) {
            ui_parent_box.setVisible(false);
        } else {
            if (newElement.parentProperty() != null) {
                ui_parent_choice.getSelectionModel().selectedItemProperty().addListener(
                        (ObservableValue observable, Object oldValue, Object newValue) -> {
                            if (newValue == null) {
                                newElement.setParent(null);
                            } else if (newValue instanceof Element) {
                                newElement.setParent((Element) newValue);
                            } else if (newValue instanceof Preview) {
                                final Preview tmpLabel = (Preview) newValue;
                                newElement.setParent((Element) session.getRepositoryForType(tmpLabel.getElementClass())
                                        .get(tmpLabel.getElementId()));
                            }
                        });
            }
            ui_parent_box.setVisible(true);

            // We've got a non-null parent list, we'll try display a more precise label.
            Object tmpParent = ui_parent_choice.getSelectionModel().getSelectedItem();
            if (tmpParent == null) {
                tmpParent = items.get(0);
            }
            final String parentType = Injector.getSession().getElementType(tmpParent);
            if (parentType != null) {
                try {
                    ui_parent_label.setText(LabelMapper.get(Class.forName(parentType)).mapClassName());
                } catch (ClassNotFoundException e) {
                    SIRS.LOGGER.log(Level.WARNING, "No class for input type " + parentType, e);
                }
            }
        }

        /*
         * Bind control properties to Element ones.
         */
        SIRS.initCombo(ui_typeVegetationId, FXCollections.observableList(previewRepository.getByClass(TypeZoneVegetation.class)), newElement.getTypeVegetationId() == null ? null : previewRepository.get(newElement.getTypeVegetationId()));
        final AbstractSIRSRepository<RefTraitementVegetation> typeTraitementIdRepo = session.getRepositoryForClass(RefTraitementVegetation.class);
        SIRS.initCombo(ui_typeTraitementId, SIRS.observableList(typeTraitementIdRepo.getAll()), newElement.getTypeTraitementId() == null? null : typeTraitementIdRepo.get(newElement.getTypeTraitementId()));
        final AbstractSIRSRepository<RefFrequenceTraitementVegetation> frequenceIdRepo = session.getRepositoryForClass(RefFrequenceTraitementVegetation.class);
        SIRS.initCombo(ui_frequenceId, SIRS.observableList(frequenceIdRepo.getAll()), newElement.getFrequenceId() == null? null : frequenceIdRepo.get(newElement.getFrequenceId()));
        }

        SIRS.initCombo(ui_type, PluginVegetation.zoneVegetationClasses(), newElement.getType() == null ? null : newElement.getType());

        final AbstractSIRSRepository<RefSousTraitementVegetation> repoSousTraitements = Injector.getSession().getRepositoryForClass(RefSousTraitementVegetation.class);
        final List<RefSousTraitementVegetation> sousTraitements = sousTypeTraitementFromTypeTraitementId(newElement.getTypeTraitementId());
        final RefSousTraitementVegetation currentSousTraitement = newElement.getSousTypeTraitementId() == null ? null : repoSousTraitements.get(newElement.getSousTypeTraitementId());
        SIRS.initCombo(ui_sousTypeTraitementId, FXCollections.observableList(sousTraitements), currentSousTraitement);
    }
    @Override
    public void preSave() {
        final Session session = Injector.getBean(Session.class);
        final ParamFrequenceTraitementVegetation element = (ParamFrequenceTraitementVegetation) elementProperty().get();

        final Object selectedParent = ui_parent_choice.getSelectionModel().selectedItemProperty().get();
        if (selectedParent == null) {
            throw new IllegalStateException("L'élément ne peut être enregistré sans parent valide.");
        }

        final Element newParent;
        if (selectedParent instanceof Preview) {
            final Preview tmpLabel = (Preview) selectedParent;
            newParent = (Element) session.getRepositoryForType(tmpLabel.getElementClass()).get(tmpLabel.getElementId());
        } else if (selectedParent instanceof Element) {
            newParent = (Element) selectedParent;
        } else {
            throw new IllegalStateException("L'élément parent est de type inconnu. Sauvegarde impossible.");
        }

        // If parent has changed, we have to dereference our object from the old one, and
        if (originalParent == null || !originalParent.getId().equals(newParent.getId())) {
            newParent.addChild(element);
            if (originalParent != null) {
                originalParent.removeChild(element);
            }
            originalParent = newParent;
        }

        Object cbValue;
        cbValue = ui_typeVegetationId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypeVegetationId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypeVegetationId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypeVegetationId(null);
        }
        cbValue = ui_typeTraitementId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypeTraitementId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypeTraitementId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypeTraitementId(null);
        }
        cbValue = ui_sousTypeTraitementId.getValue();
        if (cbValue instanceof Preview) {
            element.setSousTypeTraitementId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setSousTypeTraitementId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setSousTypeTraitementId(null);
        }
        cbValue = ui_frequenceId.getValue();
        if (cbValue instanceof Preview) {
            element.setFrequenceId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setFrequenceId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setFrequenceId(null);
        }

        element.setType(ui_type.getSelectionModel().getSelectedItem());
    }

    private void initTypeVegetation(final Class zoneClass){
        final ParamFrequenceTraitementVegetation param = elementProperty().get();
        if(param!=null){
            if(PeuplementVegetation.class.isAssignableFrom(zoneClass)){
                SIRS.initCombo(ui_typeVegetationId,
                        FXCollections.observableList(previewRepository.getByClass(RefTypePeuplementVegetation.class)),
                        param.getTypeVegetationId() == null ? null : previewRepository.get(param.getTypeVegetationId()));
            } else if(InvasiveVegetation.class.isAssignableFrom(zoneClass)){
                SIRS.initCombo(ui_typeVegetationId,
                        FXCollections.observableList(previewRepository.getByClass(RefTypeInvasiveVegetation.class)),
                        param.getTypeVegetationId() == null ? null : previewRepository.get(param.getTypeVegetationId()));
            } else{
                SIRS.initCombo(ui_typeVegetationId, FXCollections.emptyObservableList(),null);
            }
        }
    }
}