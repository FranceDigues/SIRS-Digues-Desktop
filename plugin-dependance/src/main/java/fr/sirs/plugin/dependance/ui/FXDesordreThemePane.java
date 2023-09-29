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

import fr.sirs.SIRS;
import fr.sirs.core.model.AbstractDependance;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Preview;
import fr.sirs.theme.AbstractTheme;
import fr.sirs.util.SimpleFXEditMode;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
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
 *
 * @author maximegavens
 */
public class FXDesordreThemePane extends FXDependanceThemePane {

    public static final Preview ORPHELIN_PREVIEW = new Preview();

    static {
        ORPHELIN_PREVIEW.setLibelle("   Objets orphelins   ");
    }

    public FXDesordreThemePane(ComboBox<Preview> uiDependanceAhChoice, AbstractTheme.ThemeManager theme) {
        super(theme);
        uiDependanceAhChoice.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Preview> observable, Preview oldValue, Preview newValue) -> {
            if (newValue != null) {
                ahIdProperty.set(newValue.getElementId());
            } else {
                ahIdProperty.set(null);
            }
        });

        final List<Preview> previews = session.getPreviews().getByClass(AbstractDependance.class);
       if(!previews.contains(ORPHELIN_PREVIEW)){
            previews.add(ORPHELIN_PREVIEW);
        }
        final ObservableList<Preview> previewsWithEmpty = SIRS.observableList(previews).sorted();
       // HACK-REDMINE-4408 : hide archived Dépendances and AHs from selection lists
        SIRS.initCombo(uiDependanceAhChoice, previewsWithEmpty, null, SirsPreferences.getHideArchivedProperty(), false);
    }

    @Override
    protected Parent createContent(AbstractTheme.ThemeManager manager) {
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        final DesordreThemePojotable table = new DesordreThemePojotable(manager, (ObjectProperty<? extends Element>) null);
        table.setDeletor(manager.getDeletor());
        table.getEditableProperty().bind(editMode.editionState());
        table.ahIdProperty().bindBidirectional(ahIdProperty);

        return new BorderPane(table, topPane, null, null, null);
    }

    final private class DesordreThemePojotable extends DependanceThemePojoTable {

        public DesordreThemePojotable(AbstractTheme.ThemeManager<DesordreDependance> group, ObjectProperty container) {
            super(group, container);
        }

        @Override
        protected DesordreDependance createPojo() {
            if (getAhIdProperty() == null) {
                throw new RuntimeException("L'élément ne peut être enregistré sans aménagement hydraulique ou dépendance de rattachement.");
            }
            DesordreDependance created = null;
            if (repo != null) {
                created = (DesordreDependance) repo.create();
            } else if (pojoClass != null) {
                try {
                    created = (DesordreDependance) session.getElementCreator().createElement(DesordreDependance.class);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (created != null) {
                final Preview get = session.getPreviews().get(getAhIdProperty());
                if (get != null) {
                    if ("fr.sirs.core.model.AmenagementHydraulique".equals(get.getElementClass())) {
                        created.setAmenagementHydrauliqueId(getAhIdProperty());
                    } else {
                        created.setDependanceId(getAhIdProperty());
                    }
                }

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