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
package fr.sirs.plugin.vegetation.map;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.ZoneVegetation;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.util.logging.Level;

import static fr.sirs.core.SirsCore.LOGGER;

final class EditVegetationUtils {

    static final String LABEL_DESIGNATION = "Désignation :";
    static final String LABEL_CONTACT_EAU = "Contact eau :";
    static final String LABEL_POSITION_ID = "Position :";
    static final String LABEL_COTE_ID = "Côté :";
    static final String LABEL_TYPE_VEGETATION = "Type de végétation :";
    static final String LABEL_TYPE_ARBRE = "Type :";
    static final String LABEL_DENSITE = "Densité :";
    static final String LABEL_HAUTEUR ="Hauteur :";
    static final String LABEL_DIAMETRE ="Diamètre :";
    static final String LABEL_ETAT_SANITAIRE_ID="Etat sanitaire :";
    static final String LABEL_ESPECE_ID="Espèce :";
    static final String LABEL_COMMENTAIRE="Commentaire :";


    static Label generateHeaderLabel(final String labelText) {
        final Label label = new Label(labelText);
        label.getStyleClass().add("label-header");
        return label;
    }

    static void initRefPreviewComboBox(final ComboBox<Preview> toInit, final Class clazz, final String defaultId) {
        final Previews previewRepository = Injector.getSession().getPreviews();
        SIRS.initCombo(toInit, FXCollections.observableList(
                        previewRepository.getByClass(clazz)),
                (defaultId == null || defaultId.trim().isEmpty()) ? null : previewRepository.get(defaultId));
    }

    /**
     * set to an inpur {@link ZoneVegetation} the  ZoneVegetation#typeCoteId and the  ZoneVegetation#contactEau
     * from input comboBox and checkBox respectively
     */
    static void setEditedAttributes(final ZoneVegetation zone, final CheckBox checkContactEau, final ComboBox<Preview>ui_typePositionId, final ComboBox<Preview>ui_typeCoteId ) {
        zone.setContactEau(checkContactEau.isSelected());
        zone.setTypePositionId(getElementIdOrnull(ui_typePositionId));
        zone.setTypeCoteId(getElementIdOrnull(ui_typeCoteId));
    }


    static String getElementIdOrnull(final ComboBox<Preview> comboBox) {
        final Preview preview = comboBox.getSelectionModel().getSelectedItem();
        if (preview != null) {
            return preview.getElementId();
        } else {
            LOGGER.log(Level.INFO, "Selected preview is null or isn't a Preview; set null to created vegetation element.");
            return null;
        }
    }

}
