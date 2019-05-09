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

import fr.sirs.SIRS;
import fr.sirs.util.property.ShowCase_Possibility;
import fr.sirs.util.property.SirsPreferences;
import java.util.Arrays;
import javafx.beans.DefaultProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;
import org.apache.sis.util.ArgumentChecks;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
@DefaultProperty("stringValue")
public class ShowCaseComboBox extends BorderPane {

    protected final StringProperty stringValue = new SimpleStringProperty(this, "stringValue");
    protected final ComboBox comboBox;

    public ShowCaseComboBox() {
        comboBox = new ComboBox();

        ObservableList<ShowCase_Possibility> values = FXCollections.observableArrayList(Arrays.asList(ShowCase_Possibility.values()));
        final StringConverter showCaseConverter  = ShowCase_Possibility.getConverter();
        
        SIRS.initCombo(comboBox, values, ShowCase_Possibility.BOTH, showCaseConverter);

        setCenter(comboBox);

        Bindings.bindBidirectional(stringValue, comboBox.valueProperty(), showCaseConverter);

        comboBox.valueProperty().setValue(
                ShowCase_Possibility.getFromName(SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.ABSTRACT_SHOWCASE))
        );
        
        comboBox.valueProperty().addListener(change ->{
            ArgumentChecks.ensureNonNull(" Instance de SirsPreferences", SirsPreferences.INSTANCE);
            try{
                SirsPreferences.INSTANCE.setShowCase(((ShowCase_Possibility) ((SimpleObjectProperty) change).get()).booleanValue);
            }catch(ClassCastException | NullPointerException ex){
                SirsPreferences.INSTANCE.setShowCase(null);
            }
        });

    }
    
    

    public StringProperty stringValueProperty() {
        return stringValue;
    }

}
