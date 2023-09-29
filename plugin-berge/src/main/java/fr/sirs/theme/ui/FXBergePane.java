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

import fr.sirs.Session;
import fr.sirs.SIRS;
import fr.sirs.Injector;
import fr.sirs.core.component.*;
import fr.sirs.core.model.*;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.image.ImageView;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

import javafx.beans.property.ObjectProperty;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXBergePane extends AbstractFXElementPane<Berge> {

    protected final Previews previewRepository;
    protected LabelMapper labelMapper;

    @FXML private FXValidityPeriodPane uiValidityPeriod;

    // Propriétés de Berge

    // Propriétés de AvecGeometrie

    // Propriétés de TronconDigue
    @FXML protected TextField ui_libelle;
    @FXML protected TextArea ui_commentaire;
    @FXML protected ComboBox ui_typeRiveId;
    @FXML protected ComboBox ui_typologieTronconId;
    @FXML protected Button ui_typeRiveId_link;
    @FXML protected ComboBox ui_systemeRepDefautId;
    @FXML protected Button ui_systemeRepDefautId_link;
    @FXML protected Tab ui_borneIds;
    protected final ListeningPojoTable borneIdsTable;
    @FXML protected Tab ui_gestions;
    protected final PojoTable gestionsTable;
    @FXML protected Tab ui_traits;
    protected final PojoTable traitsTable;

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXBergePane() {
        SIRS.loadFXML(this, Berge.class);
        previewRepository = Injector.getBean(Session.class).getPreviews();
        elementProperty().addListener(this::initFields);

        uiValidityPeriod.disableFieldsProperty().bind(disableFieldsProperty());
        uiValidityPeriod.targetProperty().bind(elementProperty());

        /*
         * Disabling rules.
         */
        ui_libelle.disableProperty().bind(disableFieldsProperty());
        ui_commentaire.disableProperty().bind(disableFieldsProperty());
        ui_typeRiveId.disableProperty().bind(disableFieldsProperty());
        ui_typeRiveId_link.setVisible(false);
        ui_typologieTronconId.disableProperty().bind(disableFieldsProperty());
        ui_systemeRepDefautId.disableProperty().bind(disableFieldsProperty());
        ui_systemeRepDefautId_link.disableProperty().bind(ui_systemeRepDefautId.getSelectionModel().selectedItemProperty().isNull());
        ui_systemeRepDefautId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_systemeRepDefautId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_systemeRepDefautId.getSelectionModel().getSelectedItem()));
        borneIdsTable = new ListeningPojoTable(BorneDigue.class, null, elementProperty());
        borneIdsTable.editableProperty().bind(disableFieldsProperty().not());
        borneIdsTable.createNewProperty().set(false);
        ui_borneIds.setContent(borneIdsTable);
        ui_borneIds.setClosable(false);
        gestionsTable = new PojoTable(GestionTroncon.class, null, elementProperty());
        gestionsTable.editableProperty().bind(disableFieldsProperty().not());
        ui_gestions.setContent(gestionsTable);
        ui_gestions.setClosable(false);
        traitsTable = new TraitTable(elementProperty());
        traitsTable.editableProperty().bind(disableFieldsProperty().not());
        ui_traits.setContent(traitsTable);
        ui_traits.setClosable(false);

    }

    public FXBergePane(final Berge berge){
        this();
        this.elementProperty().set(berge);
    }

    /**
     * Initialize fields at element setting.
     */
    protected void initFields(ObservableValue<? extends Berge > observableElement, Berge oldElement, Berge newElement) {
        // Unbind fields bound to previous element.
        if (oldElement != null) {
        // Propriétés de Berge
        // Propriétés de AvecGeometrie
        // Propriétés de TronconDigue
            ui_libelle.textProperty().unbindBidirectional(oldElement.libelleProperty());
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());
            ui_commentaire.setText(null);
        }

        final Session session = Injector.getBean(Session.class);

        if (newElement == null) {
                ui_typeRiveId.setItems(null);
                ui_systemeRepDefautId.setItems(null);
        } else {
            /*
             * Bind control properties to Element ones.
             */
            // Propriétés de Berge
            // Propriétés de AvecGeometrie
            // Propriétés de TronconDigue
            // * libelle
            ui_libelle.textProperty().bindBidirectional(newElement.libelleProperty());
            // * commentaire
            ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());

            SIRS.initCombo(ui_typeRiveId, FXCollections.observableList(
                    previewRepository.getByClass(RefRive.class)),
                    newElement.getTypeRiveId() == null ? null : previewRepository.get(newElement.getTypeRiveId()));
            SIRS.initCombo(ui_typologieTronconId, SIRS.observableList(
                            previewRepository.getByClass(RefTypologieTroncon.class)).sorted(),
                    newElement.getTypologieTronconId() == null ? null : previewRepository.get(newElement.getTypologieTronconId()));
            SIRS.initCombo(ui_systemeRepDefautId, FXCollections.observableList(
                    previewRepository.getByClass(SystemeReperage.class)),
                    newElement.getSystemeRepDefautId() == null ? null : previewRepository.get(newElement.getSystemeRepDefautId()));
            borneIdsTable.setParentElement(null);
            final AbstractSIRSRepository<BorneDigue> borneIdsRepo = session.getRepositoryForClass(BorneDigue.class);
            borneIdsTable.setTableItems(() -> SIRS.toElementList(newElement.getBorneIds(), borneIdsRepo));
            gestionsTable.setParentElement(newElement);
            gestionsTable.setTableItems(() -> (ObservableList) newElement.getGestions());
            traitsTable.setParentElement(newElement);
            final TraitBergeRepository traitrepo = (TraitBergeRepository) session.getRepositoryForClass(TraitBerge.class);
            traitsTable.setTableItems(() -> FXCollections.observableArrayList(traitrepo.getByBergeId(newElement.getId())));
            borneIdsTable.setObservableListToListen(newElement.getBorneIds());
        }
    }
    @Override
    public void preSave() {
        final Berge element = elementProperty().get();

        Object cbValue;
        cbValue = ui_typeRiveId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypeRiveId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypeRiveId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypeRiveId(null);
        }
        cbValue = ui_typologieTronconId.getValue();
        if (cbValue instanceof Preview) {
            element.setTypologieTronconId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setTypologieTronconId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setTypologieTronconId(null);
        } else {
            SIRS.LOGGER.log(Level.WARNING, "Value not supported for Berge ui_typologieTronconId : {0}", ui_typologieTronconId);
        }
        cbValue = ui_systemeRepDefautId.getValue();
        if (cbValue instanceof Preview) {
            element.setSystemeRepDefautId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setSystemeRepDefautId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setSystemeRepDefautId(null);
        }
        // Manage opposite references for BorneDigue...
        final List<String> currentBorneDigueIdsList = new ArrayList<>();
        for(final Element elt : borneIdsTable.getAllValues()){
            final BorneDigue borneDigue = (BorneDigue) elt;
            currentBorneDigueIdsList.add(borneDigue.getId());
        }
        element.setBorneIds(currentBorneDigueIdsList);

    }

    private static final class TraitTable extends PojoTable{

        public TraitTable(final ObjectProperty<? extends Element> container) {
            super(TraitBerge.class, null, container);
            createNewProperty.set(false);
            detaillableProperty.set(false);
            uiAdd.setVisible(false);
        }

    }

}
