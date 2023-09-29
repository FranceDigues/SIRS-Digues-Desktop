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

import fr.sirs.core.component.DesordreDependanceRepository;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.PrestationAmenagementHydraulique;
import fr.sirs.theme.ui.AbstractPrestationDesordresPojoTable;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonType;

import java.util.List;

/**
 *
 * @author Estelle Idée (Geomatys)
 */
public class PrestationDesordresDependancePojoTable extends AbstractPrestationDesordresPojoTable {

    /**
     * Creation of a @ListeningPojoTable including a button to link the container to the desordres of its parent
     * @param container
     */
    public PrestationDesordresDependancePojoTable(final ObjectProperty<PrestationAmenagementHydraulique> container) {
        super(DesordreDependance.class, null, container, false);
    }

    @Override
    protected EventHandler linkParentDesordres(ObjectProperty<? extends Element> container, boolean isOnTronconLit) {
        return (EventHandler<ActionEvent>) event -> {

            if (!createNewProperty.get()) {

                final Element cont = container.get();
                if (DesordreDependance.class.isAssignableFrom(pojoClass) && cont instanceof PrestationAmenagementHydraulique) {

                    // Collects all the Elements from the pojoClass repository
                    final List<DesordreDependance> entities = ((DesordreDependanceRepository) session.getRepositoryForClass(pojoClass)).getDesordreOpenByLinearId(((PrestationAmenagementHydraulique) cont).getAmenagementHydrauliqueId());

                    if (entities.isEmpty()) {
                        showWarningOrInformationAlert("Aucun désordre dépendance ouvert disponible sur l'AH de la prestation.", false);
                        return;
                    }

                    // Removes the Desordres already present in the pojoTable
                    entities.removeIf(d -> getAllValues().contains(d));

                    if (entities.isEmpty()) {
                        showWarningOrInformationAlert("Tous les désordres dépendances ouverts du parent sont déjà liés à la prestation.", false);
                        return;
                    }

                    final ButtonType res = showConfirmationAlertAndGetResult("Confirmer le rattachement de " + entities.size() + " désordres dépendances ouverts à la prestation.");
                    if (res == ButtonType.NO) return;
                    entities.forEach(e -> getAllValues().add(e));
                } else {
                    showWarningOrInformationAlert("Le bouton d'ajout multiple ne devrait pas être présent pour ce type d'élément", true);
                }
            } else {
                showWarningOrInformationAlert("Le bouton de création ne devrait pas être présent pour ce type d'élément", true);
            }
        };
    }
}
