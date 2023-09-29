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
package fr.sirs.theme.ui;

import fr.sirs.core.model.AireStockageDependance;
import javafx.fxml.FXML;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXAireStockageDependancePane extends FXAireStockageDependancePaneStub {

    @FXML FXPositionDependancePane uiPosition;

    /**
     * Constructor. Initialize part of the UI which will not require update when element edited change.
     */
    private FXAireStockageDependancePane() {
        super();
        uiPosition.disableFieldsProperty().bind(disableFieldsProperty());
        uiPosition.dependanceProperty().bind(elementProperty);
    }

    public FXAireStockageDependancePane(final AireStockageDependance aireStockageDependance){
        this();
        this.elementProperty().set(aireStockageDependance);
    }
}
