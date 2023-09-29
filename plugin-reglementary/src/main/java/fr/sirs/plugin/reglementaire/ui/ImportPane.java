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
import fr.sirs.util.DatePickerConverter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;

/**
 *
 * @author guilhem
 */
public class ImportPane extends GridPane {

    @FXML
    private Button chooseFileButton;

    @FXML
    protected TextField fileField;

    @FXML
    protected CheckBox isSyntheseTable;

    @FXML
    protected DatePicker horodatageDate;


    public ImportPane() {
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);

        DatePickerConverter.register(horodatageDate);
        isSyntheseTable.setSelected(true);
        // Block date in future
        horodatageDate.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }

    /**
     * Event when button chooseFileButton is clicked.
     * @param event
     */
    @FXML
    public void chooseFileButton(ActionEvent event) {
        final FileChooser fileChooser = new FileChooser();
        final File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            fileField.setText(file.getPath());
        }
    }
}
