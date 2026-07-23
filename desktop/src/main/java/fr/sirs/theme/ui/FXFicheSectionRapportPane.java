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
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SQLQueries;
import fr.sirs.core.model.SQLQuery;
import fr.sirs.core.model.report.FicheSectionRapport;
import fr.sirs.core.model.report.ModeleElement;
import fr.sirs.query.FXSearchPane;
import java.util.Optional;

import fr.sirs.util.javafx.SpinnerUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.ektorp.DbAccessException;

/**
 * TODO : model edition
 * @author Alexis Manin (Geomatys)
 */
public class FXFicheSectionRapportPane extends AbstractFXElementPane<FicheSectionRapport> {

    private static enum PhotoChoice {
        NONE("Aucune", 0),
        LAST("La dernière", 1),
        CHOOSE("Les n dernières", -1),
        ALL("Toutes", Short.MAX_VALUE);

        public final String title;
        public final int number;
        PhotoChoice(final String title, final int number) {
            this.title = title;
            this.number = number;
        }
    }

    @FXML private TextField uiTitle;
    @FXML private Label uiQueryTitle;
    @FXML private ComboBox<Preview> uiModelChoice;
    @FXML private ChoiceBox<PhotoChoice> uiNbPhotoChoice;
    @FXML private Spinner<Integer> uiNbPhotoSpinner;

    private final ObjectProperty<SQLQuery> queryProperty = new SimpleObjectProperty<>();

    private final AbstractSIRSRepository<ModeleElement> modelRepo;

    public FXFicheSectionRapportPane() {
        super();
        SIRS.loadFXML(this);
        final Session session = Injector.getSession();

        disableProperty().bind(disableFieldsProperty());

        modelRepo = session.getRepositoryForClass(ModeleElement.class);
        SIRS.initCombo(uiModelChoice, SIRS.observableList(session.getPreviews().getByClass(ModeleElement.class)).sorted(), null);

        elementProperty.addListener(this::elementChanged);
        queryProperty.addListener(this::queryChanged);
        uiNbPhotoChoice.valueProperty().addListener((ObservableValue<? extends PhotoChoice> observable, PhotoChoice oldValue, PhotoChoice newValue) -> {
            uiNbPhotoSpinner.setVisible(newValue != null && newValue.number < 0);
        });
        uiNbPhotoChoice.setItems(FXCollections.observableArrayList(PhotoChoice.values()));
        uiNbPhotoChoice.setValue(PhotoChoice.NONE);
        uiNbPhotoChoice.setConverter(new StringConverter<PhotoChoice>() {

            @Override
            public String toString(PhotoChoice object) {
                return object.title;
            }

            @Override
            public PhotoChoice fromString(String string) {
                if (string == null)
                    return null;
                for (final PhotoChoice ph : PhotoChoice.values()) {
                    if (ph.title.equals(string))
                        return ph;
                }
                return null;
            }
        });
        uiNbPhotoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Short.MAX_VALUE, 0));
        uiNbPhotoSpinner.managedProperty().bind(uiNbPhotoSpinner.visibleProperty());
        SpinnerUtils.commitOnFocusLost(uiNbPhotoSpinner);
        // TODO : tooltips.
    }

    public FXFicheSectionRapportPane(final FicheSectionRapport rapport) {
        this();
        setElement(rapport);
    }

    /**
     * Called when element edited change. We must update all UI to manage the new one.
     * @param obs
     * @param oldValue
     * @param newValue
     */
    private void elementChanged(ObservableValue<? extends FicheSectionRapport> obs, FicheSectionRapport oldValue, FicheSectionRapport newValue) {
        // Start by clearing bindings from old element.
        if (oldValue != null) {
            uiTitle.textProperty().unbindBidirectional(oldValue.libelleProperty());
        }

        if (newValue != null) {
            uiTitle.textProperty().bindBidirectional(newValue.libelleProperty());
            if (newValue.getRequeteId() != null) {
                try {
                    queryProperty.set(Injector.getSession().getRepositoryForClass(SQLQuery.class).get(newValue.getRequeteId()));
                } catch (DbAccessException e) {
                    queryProperty.set(null);
                }
            }
            final String modelId = newValue.getModeleElementId();
            if (modelId!= null) {
                for (final Preview p : uiModelChoice.getItems()) {
                    if (modelId.equals(p.getElementId())) {
                        uiModelChoice.setValue(p);
                        break;
                    }
                }
            }

            // Analyze number of photos chosen, to pick adapted UI element (combo-box or spinner)
            final int nbPhotos = newValue.getNbPhotos();
            uiNbPhotoChoice.setValue(null);
            for (final PhotoChoice c : PhotoChoice.values()) {
                if (c.number == nbPhotos) {
                    uiNbPhotoChoice.setValue(c);
                }
            }

            if (uiNbPhotoChoice.getValue() == null) {
                if (nbPhotos > 0) {
                    uiNbPhotoChoice.setValue(PhotoChoice.CHOOSE);
                    uiNbPhotoSpinner.getValueFactory().setValue(nbPhotos);
                } else {
                    uiNbPhotoChoice.setValue(PhotoChoice.NONE);
                }
            }
        } else {
            queryProperty.set(null);
            uiModelChoice.setValue(null);
            uiNbPhotoChoice.setValue(PhotoChoice.NONE);
        }
    }

    /**
     * Update query label when user change its value.
     */
    private void queryChanged(ObservableValue<? extends SQLQuery> obs, SQLQuery oldValue, SQLQuery newValue) {
        if (newValue == null)
            uiQueryTitle.setText("N/A");
        else
            uiQueryTitle.setText(newValue.getLibelle());
    }

    @Override
    public void preSave() throws Exception {
        final FicheSectionRapport element = elementProperty.get();
        if (queryProperty.get() != null) {
            element.setRequeteId(queryProperty.get().getId());
        } else {
            element.setRequeteId(null);
        }

        final PhotoChoice photoChoice = uiNbPhotoChoice.getValue();
        if (photoChoice.number < 0) {
            final Integer spinnerValue = uiNbPhotoSpinner.getValue();
            if (spinnerValue == null ) {
                element.setNbPhotos(0);
            } else {
                element.setNbPhotos(Math.max(0, spinnerValue));
            }
        } else {
            element.setNbPhotos(photoChoice.number);
        }

        final String modelChoice = uiModelChoice.getValue() == null? null : uiModelChoice.getValue().getElementId();
        if (modelChoice != null) {
            if (!modelChoice.equals(element.getModeleElementId())) {
                element.setModeleElementId(modelChoice);
            }
        } else if (element.getModeleElementId() != null) {
            element.setModeleElementId(null);
        }
    }

    /**
     * Action to perform when user want to select a query.
     */
    @FXML
    private void chooseQuery(final ActionEvent e) {
        final Optional<SQLQuery> query = FXSearchPane.chooseSQLQuery(SQLQueries.dbQueries());
        if (query.isPresent())
            queryProperty.set(query.get());
    }

    /**
     * Triggered when user clean query value.
     */
    @FXML
    private void deleteQuery(final ActionEvent e) {
        queryProperty.set(null);
    }
}
