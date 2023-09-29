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

import fr.sirs.Injector;
import fr.sirs.SIRS;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

/**
 *
 * @author guilhem
 */
public class DatabaseVersionPane extends GridPane{

    @FXML
    private Label SFUuid;
    
    @FXML
    private Label SFProj;

    @FXML
    private Label SFVersion;
    
    @FXML
    private Label SFDistantUrl;

    @FXML
    private Label BDUuid;

    @FXML
    private Label BDProj;

    @FXML
    private Label BDVersion;
    
    @FXML
    private Label BDDistantUrl;

    
    public DatabaseVersionPane(final String existingKey, final String dbKey) {
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);
        String[] sfKey = existingKey.split("\\|");
        
        SFUuid.setText(cleanNull(sfKey[0]));
        SFProj.setText(cleanNull(sfKey[1]));
        SFVersion.setText(cleanNull(sfKey[2]));
        SFDistantUrl.setText(cleanNull(sfKey[3]));
        
        String[] bdKey = dbKey.split("\\|");
        BDUuid.setText(cleanNull(bdKey[0]));
        BDProj.setText(cleanNull(bdKey[1]));
        BDVersion.setText(cleanNull(bdKey[2]));
        BDDistantUrl.setText(cleanNull(bdKey[3]));
    }
 
    private String cleanNull(final String value) {
        if (value.equals("null")) {
            return "non disponible";
        }
        return value;
    }
}
