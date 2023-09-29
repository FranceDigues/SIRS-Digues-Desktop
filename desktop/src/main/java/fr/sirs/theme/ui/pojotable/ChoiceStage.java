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
package fr.sirs.theme.ui.pojotable;

import fr.sirs.SIRS;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Preview;
import fr.sirs.theme.ui.PojoTableComboBoxChoiceStage;
import fr.sirs.util.property.SirsPreferences;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;

/**
 *
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class ChoiceStage extends PojoTableComboBoxChoiceStage<Element, Preview> {

    /**
     * Constructeur de ChoiceStage.
     *
     * @param repo : Répositorie de l'élément sélectionné.
     * @param items : Elements sélectionnable.
     * @param defaultSelection : Sélection par defaut
     * @param windowTitle : Titre de la fenêtre d'interface ouverte.
     * @param okButtonLabel : Label du bouton de validation.
     */
    public ChoiceStage(final AbstractSIRSRepository repo,final ObservableList<Preview> items, final Object defaultSelection, final String windowTitle, final String okButtonLabel){
        super(okButtonLabel);
        setTitle(windowTitle);

        SIRS.initCombo(comboBox, items, defaultSelection, SirsPreferences.getHideArchivedProperty(), true);
        retrievedElement.bind(new ObjectBinding<Element>() {

            {
                bind(comboBox.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected Element computeValue() {
                if(comboBox.valueProperty()!=null){
                    final Preview preview = comboBox.valueProperty().get();
                    if((preview!=null)&&(repo!=null)){
                        return (Element) repo.get(preview.getDocId());
                    }
                }
                return null;
            }
        });
    }
}