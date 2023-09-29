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

import fr.sirs.core.model.Positionable;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface FXPositionableMode {

    String COORDS_TYPE_UNKNOWN = "Coord. d'origine inconnues.";
    String COORDS_TYPE_COMPUTED = "Coordonnées calculées";
    String COORDS_TYPE_ENTERED = "Coordonnées saisies";
    String COORDS_TYPE_CARTO = "Géométrie créée via la carte";

    /**
     * ID du mode d'édition. Cette valeur est stoquée dans le champ geometryMode
     * du positionable.
     *
     * @return identifiant unique
     */
    String getID();

    /**
     * Nom du mode d'édition.
     *
     * @return
     */
    String getTitle();

    Node getFXNode();

    ObjectProperty<Positionable> positionableProperty();

    BooleanProperty disablingProperty();

    default List<Node> getExtraButton() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Méthode de mise à jour des champs.
     *
     * Cette méthode doit permettre aux champs de rester à jour lorsque l'
     * information géométrique du positionable est modifiée.
     *
     * Mais elle n'est pas chargée elle-même de modifier l'information
     * géométrique.
     */
    void updateFields();

    /**
     * Méthode de mise à jour de la géométrie.
     *
     * Cette méthode doit permettre à la géométrie de rester à jour avec les
     * champs. Elle doit pour cela veiller que la modification des champs ait
     * bien eu lieu depuis le mode courant car sinon des modifications en boucle
     * risquent de se produire, par exemple :
     *
     * Modification d'un champ dans le mode A
     * <ul>
     * <li> mise à jour de la géométrie </li>
     * <li> modification d'un champ dans le mode B pour préserver la cohérence
     * de l'affichage avec la géométrie modifiée depuis le mode A.</li>
     * <li> mise à jour de la géométrie (si on n'a pas vérifié qu'on n'est pas
     * en mode B)</li>
     * <li> modification d'un champ dans le mode A pour présernver la cohérence
     * de l'affichage avec la géométrie modifiée depuis le mode B.</li>
     * <li> etc.</li>
     * </ul>
     */
    void buildGeometry();


    /**
     *
     * @param spinnerNumber
     * @return
     */
    public static double fxNumberValue(final ObjectProperty<Double> spinnerNumber) {
        if (spinnerNumber.get() == null) {
            return 0;
        }
        return spinnerNumber.get();
    }
}
