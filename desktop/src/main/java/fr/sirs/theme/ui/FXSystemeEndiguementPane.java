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
import fr.sirs.util.javafx.SpinnerUtils;
import javafx.beans.property.ObjectProperty;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @author Samuel Andrés (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 */
public class FXSystemeEndiguementPane extends AbstractFXElementPane<SystemeEndiguement> {

    private final Previews previewRepository;

    private final DiguePojoTable table;

    // Propriétés de SystemeEndiguement
    @FXML TextArea ui_commentaire;
    @FXML TextField ui_libelle;
    @FXML Spinner ui_populationProtegee;
    @FXML TextField ui_classement;
    @FXML TextField ui_niveauProtection;
    @FXML ComboBox ui_gestionnaireDecretId;
    @FXML Button ui_gestionnaireDecretId_link;
    @FXML ComboBox ui_gestionnaireTechniqueId;
    @FXML Button ui_gestionnaireTechniqueId_link;
    @FXML private TabPane tabs;
    @FXML private VBox centerContent;

    /**
     * Constructor. Initialize part of the UI which will not require update when element edited change.
     */
    private FXSystemeEndiguementPane() {
        SIRS.loadFXML(this, SystemeEndiguement.class);
        previewRepository = Injector.getBean(Session.class).getPreviews();
        elementProperty().addListener(this::initFields);


		/*
		 * Disabling rules.
		 */
        ui_commentaire.disableProperty().bind(disableFieldsProperty());
        ui_libelle.disableProperty().bind(disableFieldsProperty());
        ui_populationProtegee.disableProperty().bind(disableFieldsProperty());
        ui_populationProtegee.setEditable(true);
        ui_populationProtegee.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
        SpinnerUtils.commitOnFocusLost(ui_populationProtegee);
        ui_classement.disableProperty().bind(disableFieldsProperty());
        ui_niveauProtection.disableProperty().bind(disableFieldsProperty());
        ui_gestionnaireDecretId.disableProperty().bind(disableFieldsProperty());
        ui_gestionnaireDecretId_link.disableProperty().bind(ui_gestionnaireDecretId.getSelectionModel().selectedItemProperty().isNull());
        ui_gestionnaireDecretId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_gestionnaireDecretId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_gestionnaireDecretId.getSelectionModel().getSelectedItem()));
        ui_gestionnaireTechniqueId.disableProperty().bind(disableFieldsProperty());
        ui_gestionnaireTechniqueId_link.disableProperty().bind(ui_gestionnaireTechniqueId.getSelectionModel().selectedItemProperty().isNull());
        ui_gestionnaireTechniqueId_link.setGraphic(new ImageView(SIRS.ICON_LINK));
        ui_gestionnaireTechniqueId_link.setOnAction((ActionEvent e)->Injector.getSession().showEditionTab(ui_gestionnaireTechniqueId.getSelectionModel().getSelectedItem()));


        table = new DiguePojoTable(elementProperty());
    }

    public FXSystemeEndiguementPane(final SystemeEndiguement systemeEndiguement){
        this();
        this.elementProperty().set(systemeEndiguement);

        table.editableProperty().set(false);
        table.parentElementProperty().bind(elementProperty);
        tabs.getTabs().add(new Tab("Digues", table));
    }

    /**
     * Initialize fields at element setting.
     */
    private void initFields(ObservableValue<? extends SystemeEndiguement > observableElement, SystemeEndiguement oldElement, SystemeEndiguement newElement) {
        // Unbind fields bound to previous element.
        table.setTableItems(()->null);
        if (oldElement != null) {
        // Propriétés de SystemeEndiguement
            ui_libelle.textProperty().unbindBidirectional(oldElement.libelleProperty());
            ui_commentaire.textProperty().unbindBidirectional(oldElement.commentaireProperty());

            ui_populationProtegee.getValueFactory().valueProperty().unbindBidirectional(oldElement.populationProtegeeProperty());
            ui_classement.textProperty().unbindBidirectional(oldElement.classementProperty());

            ui_niveauProtection.textProperty().unbindBidirectional(oldElement.niveauProtectionProperty());
        }

        if (newElement == null) {
            ui_libelle.setText(null);
            ui_commentaire.setText(null);
            ui_classement.setText(null);
            ui_niveauProtection.setText(null);
            table.setTableItems(null);

        } else {
            final Session session = Injector.getBean(Session.class);

            /*
         * Bind control properties to Element ones.
             */
            // Propriétés de SystemeEndiguement
            // * commentaire
            ui_commentaire.textProperty().bindBidirectional(newElement.commentaireProperty());
            // * libelle
            ui_libelle.textProperty().bindBidirectional(newElement.libelleProperty());
            // * populationProtegee
            ui_populationProtegee.getValueFactory().valueProperty().bindBidirectional(newElement.populationProtegeeProperty());
            // * classement
            ui_classement.textProperty().bindBidirectional(newElement.classementProperty());
            // * niveauProtection
            ui_niveauProtection.textProperty().bindBidirectional(newElement.niveauProtectionProperty());

            table.setTableItems(() -> (ObservableList) SIRS.observableList(
                    ((DigueRepository) session.getRepositoryForClass(Digue.class)).getBySystemeEndiguement(newElement)));

            SIRS.initCombo(ui_gestionnaireDecretId, SIRS.observableList(
                    previewRepository.getByClass(Organisme.class)).sorted(),
                    newElement.getGestionnaireDecretId() == null ? null : previewRepository.get(newElement.getGestionnaireDecretId()));
            SIRS.initCombo(ui_gestionnaireTechniqueId, SIRS.observableList(
                    previewRepository.getByClass(Organisme.class)).sorted(),
                    newElement.getGestionnaireTechniqueId() == null ? null : previewRepository.get(newElement.getGestionnaireTechniqueId()));
        }
    }

    @Override
    public void preSave() {
        final SystemeEndiguement element = (SystemeEndiguement) elementProperty().get();

        Object cbValue;
        cbValue = ui_gestionnaireDecretId.getValue();
        if (cbValue instanceof Preview) {
            element.setGestionnaireDecretId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setGestionnaireDecretId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setGestionnaireDecretId(null);
        }
        cbValue = ui_gestionnaireTechniqueId.getValue();
        if (cbValue instanceof Preview) {
            element.setGestionnaireTechniqueId(((Preview)cbValue).getElementId());
        } else if (cbValue instanceof Element) {
            element.setGestionnaireTechniqueId(((Element)cbValue).getId());
        } else if (cbValue == null) {
            element.setGestionnaireTechniqueId(null);
        }
    }

    private class DiguePojoTable extends PojoTable {

        public DiguePojoTable(final ObjectProperty<? extends Element> container) {
            super(Digue.class, "Digues du système d'endiguement", container);
            createNewProperty.set(false);
            fichableProperty.set(false);
            uiAdd.setVisible(false);
            uiFicheMode.setVisible(false);
            uiDelete.setVisible(false);
            setDeletor(input -> {
                if (input instanceof Digue) {
                    ((Digue)input).setSystemeEndiguementId(null);
                    session.getRepositoryForClass((Class)input.getClass()).update(input);
                }
            });
        }

        @Override
        protected Digue createPojo() {
            throw new UnsupportedOperationException("Vous ne devez pas créer de nouvelle digues depuis cette table !");
        }
    }
}
