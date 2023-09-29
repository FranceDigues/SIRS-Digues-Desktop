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
package fr.sirs.plugin.document.ui;

import fr.sirs.plugin.document.FileAndUnsupportedFiles;
import fr.sirs.ui.LoadingPane;
import javafx.concurrent.Task;

import java.util.List;

/**
 * A simple panel whose aim is to display advancement of a given task.
 *
 * @author Alexis Manin (Geomatys)
 */
public class DocumentLoadingPane extends LoadingPane {
    public DocumentLoadingPane() {
        super();
    }

    @Override
    protected void checkTask(Task newValue) {
        final Object taskValue = newValue.getValue();
        if (taskValue instanceof FileAndUnsupportedFiles) {
            final FileAndUnsupportedFiles taskResult = (FileAndUnsupportedFiles) taskValue;
            final List<String> unsupportedFiles = taskResult.getUnsupportedFiles();
            if (unsupportedFiles != null && !unsupportedFiles.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("Information : \nLes fichiers suivants n'ont pas été ajoutés au dossier de synthèse \ncar leurs formats ne sont pas pris en charge:\n");
                for (String fileName : unsupportedFiles) {
                    errorMsg.append("\n" + fileName);
                }
                errorMsg.append(" \n\nFormats pris en charge : ODF, PDF, Image et text\n");
                uiErrorLabel.setText(errorMsg.toString());
                uiErrorLabel.setVisible(true);
                this.getScene().getWindow().sizeToScene();
            }
        }
    }
}
