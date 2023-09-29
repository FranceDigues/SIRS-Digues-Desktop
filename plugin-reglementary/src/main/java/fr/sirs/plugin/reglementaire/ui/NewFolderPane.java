/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2016, FRANCE-DIGUES,
 * <p>
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.reglementaire.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 *
 * @author Estelle Idee (Geomatys)
 */
public class NewFolderPane extends GridPane {

    public static final String IN_CURRENT_FOLDER = " Dans le dossier sélectionné";
    public static final String IN_SE_FOLDER      = " Uniquement dans les systèmes d'endiguement";

    @FXML
    public TextField folderNameField;

    @FXML
    public ComboBox<String> locCombo;

    public NewFolderPane() {
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);

        final ObservableList<String> prop = FXCollections.observableArrayList();
        prop.add(IN_CURRENT_FOLDER);
        prop.add(IN_SE_FOLDER);
        locCombo.setItems(prop);
        locCombo.getSelectionModel().selectFirst();
    }
}
