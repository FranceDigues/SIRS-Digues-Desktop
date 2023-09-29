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
import fr.sirs.plugin.reglementaire.PluginReglementary;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.prefs.Preferences;

import static fr.sirs.plugin.reglementaire.ui.RegistreDocumentsPane.ROOT_FOLDER;

/**
 *
 * @author guilhem
 */
public class MainFolderPane extends GridPane {

    @FXML
    public TextField rootFolderField;

    @FXML
    private Button chooRootButton;

    @FXML
    public void chooseRootFile(ActionEvent event) {
        final DirectoryChooser fileChooser = new DirectoryChooser();
        final File file = fileChooser.showDialog(null);
        if (file != null) {
            rootFolderField.setText(file.getPath());
        }
    }

    public MainFolderPane() {
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);
        final Preferences prefs = Preferences.userRoot().node(PluginReglementary.NODE_PREFERENCE_NAME);
        if(prefs != null) {
            final String initial = prefs.get(ROOT_FOLDER, null);
            if (initial != null) {
                rootFolderField.setText(initial);
            }
        }
    }

}
