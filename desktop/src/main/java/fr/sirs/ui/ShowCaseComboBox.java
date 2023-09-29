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
package fr.sirs.ui;

import fr.sirs.SIRS;
import fr.sirs.util.property.ShowCasePossibility;
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
 * ComboBox utilisée pour choisir et enregistrer les préférences de l'utilisateur
 * quant à l'affichage utilisé pour désigné les objets de l'application :
 * -abrégé seulement
 * -nom complet seulement
 * -abrégé : nom complet (par défaut)
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
@DefaultProperty("stringValue")
public class ShowCaseComboBox extends BorderPane {

    protected final StringProperty stringValue = new SimpleStringProperty(this, "stringValue");
    protected final ComboBox comboBox;

    public ShowCaseComboBox() {
        ArgumentChecks.ensureNonNull(" Instance de SirsPreferences", SirsPreferences.INSTANCE);
        comboBox = new ComboBox();

        ObservableList<ShowCasePossibility> values = FXCollections.observableArrayList(Arrays.asList(ShowCasePossibility.values()));
        final StringConverter showCaseConverter = ShowCasePossibility.getConverter();

        //Chargement de la valeur sauvegardée ou par défaut.
        final ShowCasePossibility saved = ShowCasePossibility.getFromName(SirsPreferences.INSTANCE.getPropertySafeOrDefault(SirsPreferences.PROPERTIES.ABSTRACT_SHOWCASE));
        SIRS.initCombo(comboBox, values, saved, showCaseConverter);

        setCenter(comboBox);

        Bindings.bindBidirectional(stringValue, comboBox.valueProperty(), showCaseConverter);

        //Ecouteur sur la valeur sélectionnée.
        comboBox.valueProperty().addListener(change -> {
            try {
                SirsPreferences.INSTANCE.setShowCase(((ShowCasePossibility) ((SimpleObjectProperty) change).get()));
            } catch (ClassCastException | NullPointerException ex) {
                SirsPreferences.INSTANCE.setShowCase(ShowCasePossibility.BOTH);
            }
        });

    }

    /**
     * Used by {@link FXSirsPreferenceEditor#getNodeProperty}
     */
    public StringProperty stringValueProperty() {
        return stringValue;
    }

}
