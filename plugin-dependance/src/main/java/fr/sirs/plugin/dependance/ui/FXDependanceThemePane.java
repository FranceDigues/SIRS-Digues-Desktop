/*
 * This file is part of SIRS-Digues 2.
 *
 *  Copyright (C) 2021, FRANCE-DIGUES,
 *
 *  SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any
 *  later version.
 *
 *  SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.dependance.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.AbstractAmenagementHydraulique;
import fr.sirs.core.model.AmenagementHydraulique;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Preview;
import fr.sirs.theme.AbstractTheme;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.SimpleFXEditMode;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.logging.Level;

/**
 * Thémes regroupant les panneaux des dépendances et de l'aménagement hydraulique
 *
 * @author Maxime Gavens (Geomatys)
 */
public class FXDependanceThemePane extends BorderPane {

    public static final Preview EMPTY_PREVIEW = new Preview();

    static {
        EMPTY_PREVIEW.setElementClass(AmenagementHydraulique.class.getCanonicalName());
        EMPTY_PREVIEW.setLibelle("   Objets orphelins   ");
    }

    protected final StringProperty ahIdProperty = new SimpleStringProperty();
    protected final Session session = Injector.getBean(Session.class);

    public StringProperty ahIdProperty(){return ahIdProperty;}

    public FXDependanceThemePane(AbstractTheme.ThemeManager theme) {
        setCenter(createContent(theme));
    }

    public FXDependanceThemePane(ComboBox<Preview> uiAhChoice, AbstractTheme.ThemeManager theme) {
        setCenter(createContent(theme));
        uiAhChoice.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Preview> observable, Preview oldValue, Preview newValue) -> {
            if (newValue != null) {
                ahIdProperty.set(newValue.getElementId());
            } else {
                ahIdProperty.set(null);
            }
        });

        final List<Preview> rawAhPreviews = session.getPreviews().getByClass(AmenagementHydraulique.class);
        if(!rawAhPreviews.contains(EMPTY_PREVIEW)){
            rawAhPreviews.add(EMPTY_PREVIEW);
        }
        final ObservableList<Preview> ahPreviews = SIRS.observableList(rawAhPreviews).sorted();
        // HACK-REDMINE-4408 : hide archived AH from selection lists
        SIRS.initCombo(uiAhChoice, ahPreviews, null, SirsPreferences.getHideArchivedProperty(), false);
    }

    protected Parent createContent(AbstractTheme.ThemeManager manager) {
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        final DependanceThemePojoTable table = new DependanceThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
        table.setDeletor(manager.getDeletor());
        table.getEditableProperty().bind(editMode.editionState());
        table.ahIdProperty().bindBidirectional(ahIdProperty);

        return new BorderPane(table, topPane, null, null, null);
    }

    protected class DependanceThemePojoTable<T extends AbstractAmenagementHydraulique> extends PojoTable{

        protected final StringProperty ahIdProperty = new SimpleStringProperty();
        private final AbstractTheme.ThemeManager<T> group;

        public void setAhIdProperty(final String ahId) {
            ahIdProperty.set(ahId);
        }

        public String getAhIdProperty() {
            return ahIdProperty.get();
        }

        public StringProperty ahIdProperty() {
            return ahIdProperty;
        }

        public DependanceThemePojoTable(AbstractTheme.ThemeManager<T> group, final ObjectProperty<? extends Element> container) {
            super(group.getDataClass(), group.getTableTitle(), container);
            ahIdProperty.addListener(this::updateTable);
            this.group = group;
            if (ahIdProperty.get() == null) {
                updateTable(null, null, null);
            }
        }

        private void updateTable(ObservableValue<? extends String> observable, String oldValue, String newValue){
            if(group==null) {
                setTableItems(FXCollections::emptyObservableList);
            } else {
                setTableItems(() -> (ObservableList) group.getExtractor().apply(newValue));
            }
        }

        public BooleanProperty getEditableProperty() {
            return editableProperty;
        }

        @Override
        protected T createPojo() {
            if (getAhIdProperty() == null) {
                throw new RuntimeException("L'élément ne peut être enregistré sans Aménagement hydraulique de rattachement.");
            }
            T created = null;
            if (repo != null) {
                created = (T) repo.create();
            } else if (pojoClass != null) {
                try {
                    created = (T) session.getElementCreator().createElement(pojoClass);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (created != null) {
                created.setAmenagementHydrauliqueId(getAhIdProperty());

                try {
                    repo.add(created);
                } catch (NullPointerException e) {
                    SIRS.LOGGER.log(Level.WARNING, "Repository introuvable", e);
                }
                getAllValues().add(created);
            }
            return created;
        }
    }
}