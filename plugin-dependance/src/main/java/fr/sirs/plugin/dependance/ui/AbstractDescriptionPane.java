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
package fr.sirs.plugin.dependance.ui;

import fr.sirs.SIRS;
import fr.sirs.core.model.TronconDigue;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import fr.sirs.theme.AbstractTheme;

/**
 *
 * @author Maxime Gavens (Geomatys)
 */
public class AbstractDescriptionPane extends BorderPane {

    @FXML
    private ComboBox<TronconDigue> tronconBox;
    
    @FXML
    private BorderPane uiCenter;
    
    public AbstractDescriptionPane() {
       this(null, null);
    }
     
    public AbstractDescriptionPane(final String tableName, final Class tableClass) {
        SIRS.loadFXML(this);

        if (tableClass != null) {
            final AbstractTheme.ThemeManager themeManager = AbstractTheme.generateThemeManager(tableName, tableClass);
            final FXDependanceThemePane tab = new FXDependanceThemePane(tronconBox, themeManager);
            uiCenter.setCenter(tab);
        }
     }
}
