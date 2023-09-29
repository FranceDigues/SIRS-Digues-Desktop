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

package fr.sirs.theme;

import fr.sirs.Injector;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.GlobalPrestation;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.SimpleFXEditMode;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Parent;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class GlobalPrestationTheme extends Theme {

    private static final String GLOBAL_PRESTATION_THEME_TITLE = "Prestation Globale";

    public GlobalPrestationTheme() {
        super(GLOBAL_PRESTATION_THEME_TITLE, Type.UNLOCALIZED);
    }

    @Override
    public Parent createPane() {

        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        PojoTable pojoTable = new PojoTable(Injector.getSession().getRepositoryForClass(GlobalPrestation.class), getName(), (ObjectProperty<? extends Element>) null);
        pojoTable.editableProperty().bind(editMode.editionState());
        return new BorderPane(pojoTable, topPane, null, null, null);

    }

}
