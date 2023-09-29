/**
 *
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
import fr.sirs.util.DatePickerConverter;
import fr.sirs.util.FXFreeTab;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.logging.Level;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @author Estelle Idee (Geomatys) : HACK_REDMINE_7544 class added and not auto-generated in order to hide attributs for AH's ObservationDependance
 */
public class FXObservationDependancePane extends AbstractFXElementPane<ObservationDependance> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    // Document parent
    @FXML protected Label ui_parent_label;
    @FXML protected HBox ui_parent_box;
    @FXML protected ComboBox ui_parent_choice;
    @FXML protected Button ui_parent_link;

    /** A reference to the parent of the edited element. We keep it to be able to update old parent if a new one is set. */
    private Element originalParent;

    // Propriétés de ObservationDependance
    @FXML protected TextArea ui_evolution;
    @FXML protected DatePicker ui_date;
    @FXML protected TextArea ui_suite;
    // HACK_REDMINE_7544 - to be removed if better fix - add fx:id on the label to hide the 'nombre de désordres' for the AH's observation dependance
    @FXML protected Label ui_nombreDesordres_label;
    @FXML protected Spinner ui_nombreDesordres;
    // HACK_REDMINE_7544 - to be removed if better fix - add fx:id on the label hide the 'Niveau d'urgence' for the AH's observation dependance
    @FXML protected Label  ui_urgenceId_label;
    @FXML protected ComboBox ui_urgenceId;
    @FXML protected Button ui_urgenceId_link;
    @FXML protected ComboBox ui_suiteApporterId;
    @FXML protected Button ui_suiteApporterId_link;
    @FXML protected FXFreeTab ui_photos;
    protected PojoTable photosTable;

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXObservationDependancePane() {
        SIRS.loadFXML(this, ObservationDependance.class);
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
        ui_evolution.setWrapText(true);
        ui_evolution.editableProperty().bind(disableFieldsProperty().not());
        ui_date.disableProperty().bind(disableFieldsProperty());
        DatePickerConverter.register(ui_date);
        ui_suite.setWrapText(true);
        ui_suite.editableProperty().bind(disableFieldsProperty().not());
        ui_nombreDesordres.disableProperty().bind(disableFieldsProperty());
        ui_nombreDesordres.setEditable(true);
        ui_nombreDesordres.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
        ui_urgenceId.disableProperty().bind(disableFieldsProperty());

        ui_urgenceId_link.setVisible(false);
        ui_suiteApporterId.disableProperty().bind(disableFieldsProperty());
        ui_suiteApporterId_link.setVisible(false);

        ui_photos.setContent(() -> {
        photosTable = new PojoTable(PhotoDependance.class, null, elementProperty());
        photosTable.editableProperty().bind(disableFieldsProperty().not());
        updatePhotosTable(session, elementProperty.get());
        return photosTable;
        });
        ui_photos.setClosable(false);
    }

    public FXObservationDependancePane(final ObservationDependance observationDependance){
        this();
        this.elementProperty().set(observationDependance);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends ObservationDependance > observableElement, ObservationDependance oldElement, ObservationDependance newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
        // Propriétés de ObservationDependance
            ui_evolution.textProperty().unbindBidirectional(oldElement.evolutionProperty());
            ui_evolution.setText(null);
            ui_date.valueProperty().unbindBidirectional(oldElement.dateProperty());
            ui_date.setValue(null);
            ui_suite.textProperty().unbindBidirectional(oldElement.suiteProperty());
            ui_suite.setText(null);
            ui_nombreDesordres.getValueFactory().valueProperty().unbindBidirectional(oldElement.nombreDesordresProperty());
            ui_nombreDesordres.getValueFactory().setValue(0);
        }

        final Session session = Injector.getBean(Session.class);

        if (newElement == null) {
                ui_parent_choice.setItems(null);
                ui_parent_choice.setDisable(true);

                ui_urgenceId.setItems(null);
                ui_suiteApporterId.setItems(null);
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
                    // HACK_REDMINE_7544 - to be removed if better fix - hide the 'nombre de désordres' and 'Niveau d'urgence' for the AH's observation dependance
                    // best would be to create an AbstractObservationDependance and a specific ObservationDependance for the Ahs
                    if(Class.forName(parentType).equals(AmenagementHydraulique.class)){
                        ui_nombreDesordres_label.setVisible(false);
                        ui_nombreDesordres.setVisible(false);
                        ui_urgenceId_label.setVisible(false);
                        ui_urgenceId.setVisible(false);
                    }
                } catch (ClassNotFoundException e) {
                    SIRS.LOGGER.log(Level.WARNING, "No class for input type " + parentType, e);
                }
            }
        }

        /*
         * Bind control properties to Element ones.
         */
        // Propriétés de ObservationDependance
        // * evolution
        ui_evolution.textProperty().bindBidirectional(newElement.evolutionProperty());
        // * date
        ui_date.valueProperty().bindBidirectional(newElement.dateProperty());
        // * suite
        ui_suite.textProperty().bindBidirectional(newElement.suiteProperty());
        // * nombreDesordres
        // HACK_REDMINE_7544 - to be removed if better fix - hide the 'nombre de désordres' and 'Niveau d'urgence' for the AH's observation dependance
        if (ui_nombreDesordres.isVisible()) ui_nombreDesordres.getValueFactory().valueProperty().bindBidirectional(newElement.nombreDesordresProperty());

        // * urgenceId
        // HACK_REDMINE_7544 - to be removed if better fix - hide the 'nombre de désordres' and 'Niveau d'urgence' for the AH's observation dependance
        if (ui_nombreDesordres.isVisible()) {
            final AbstractSIRSRepository<RefUrgence> urgenceIdRepo = session.getRepositoryForClass(RefUrgence.class);
            SIRS.initCombo(ui_urgenceId, SIRS.observableList(urgenceIdRepo.getAll()), newElement.getUrgenceId() == null? null : urgenceIdRepo.get(newElement.getUrgenceId()));
        }
        // * suiteApporterId
        final AbstractSIRSRepository<RefSuiteApporter> suiteApporterIdRepo = session.getRepositoryForClass(RefSuiteApporter.class);
        SIRS.initCombo(ui_suiteApporterId, SIRS.observableList(suiteApporterIdRepo.getAll()), newElement.getSuiteApporterId() == null? null : suiteApporterIdRepo.get(newElement.getSuiteApporterId()));
        }

        updatePhotosTable(session, newElement);
    }


    protected void updatePhotosTable(final Session session, final ObservationDependance newElement) {
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
        final ObservationDependance element = (ObservationDependance) elementProperty().get();

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
        cbValue = ui_urgenceId.getValue();
        if (cbValue instanceof Preview) {
            element.setUrgenceId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setUrgenceId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setUrgenceId(null);
        }
        cbValue = ui_suiteApporterId.getValue();
        if (cbValue instanceof Preview) {
            element.setSuiteApporterId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setSuiteApporterId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setSuiteApporterId(null);
        }
    }
}
