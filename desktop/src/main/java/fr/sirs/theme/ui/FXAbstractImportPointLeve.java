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

import fr.sirs.core.model.PointZ;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import org.geotoolkit.feature.Feature;
import org.opengis.feature.PropertyType;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
public abstract class FXAbstractImportPointLeve<T extends PointZ> extends FXAbstractImportCoordinate {

    protected static final String ATT_Z_KEY = "attZ";
    protected static final String ATT_DESIGNATION_KEY = "attDesignation";

    protected final ObservableList<Feature> selectionProperty;
    protected final PojoTable pojoTable;
    @FXML protected ComboBox<PropertyType> uiAttDesignation;
    @FXML protected ComboBox<PropertyType> uiAttZ;

    FXAbstractImportPointLeve(final PojoTable pojoTable) {
        super();
        this.pojoTable = pojoTable;
        uiAttDesignation.setConverter(stringConverter);
        uiAttZ.setConverter(stringConverter);
        selectionProperty = FXCollections.observableArrayList();
    }

    @FXML
    void importSelection(ActionEvent event) {
        saveFieldValue();
        final ObservableList<T> pt = getSelectionPoint();
        if(pt==null || pt.isEmpty()) return;
        pojoTable.getAllValues().addAll(pt);
    }

    @Override
    protected void saveFieldValue() {
        super.saveFieldValue();
        savePreference(ATT_Z_KEY, stringConverter.toString(uiAttZ.getSelectionModel().getSelectedItem()));
        savePreference(ATT_DESIGNATION_KEY, stringConverter.toString(uiAttDesignation.getSelectionModel().getSelectedItem()));
    }

    protected abstract ObservableList<T> getSelectionPoint();
}
