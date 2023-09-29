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
package fr.sirs.ui;

import static fr.sirs.core.SirsCore.fxRunAndWait;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import org.apache.sis.util.ArgumentChecks;

/**
 * Popup loaded when the application is started for the first time, to enter the path to the configuration folder .sirs
 * 
 * @author Maxime Gavens - Geomatys
 */
public class ConfFolderPane extends GridPane {

    @FXML
    public TextField rootFolderField;
    
    @FXML
    private Button chooRootButton;
    
    public ConfFolderPane() {
        loadFXML();
        final String userPath = System.getProperty("user.home");
        rootFolderField.setText(userPath);
        rootFolderField.setDisable(true);
    }

    @FXML
    public void chooseRootFile(ActionEvent event) {
        final DirectoryChooser fileChooser = new DirectoryChooser();
        final File file = fileChooser.showDialog(null);
        if (!(file.canExecute() && file.canRead() && file.canWrite())) {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Le répertoire selectionné ne possède pas les permissions suffisantes.");
            alert.setResizable(true);
            alert.showAndWait();
            return;
        }
        if (file != null) {
            rootFolderField.setText(file.getPath());
        }
    }
    
    private void loadFXML() {
        ArgumentChecks.ensureNonNull("JavaFX controller object", this);
        final String fxmlpath = "/"+this.getClass().getName().replace('.', '/')+".fxml";
        final URL resource = this.getClass().getResource(fxmlpath);
        if (resource == null) {
            throw new RuntimeException("No FXMl document can be found for path : "+fxmlpath);
        }
        final FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(this);
        loader.setRoot(this);
        loader.setClassLoader(this.getClass().getClassLoader());

        fxRunAndWait(() -> {
            try {
                loader.load();
            } catch (IOException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        });
    }
    
}

