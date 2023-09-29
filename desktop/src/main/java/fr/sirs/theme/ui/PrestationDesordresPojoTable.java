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

import fr.sirs.core.component.AbstractDesordreRepository;
import fr.sirs.core.component.DesordreRepository;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Prestation;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonType;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author Estelle Idée (Geomatys)
 */
public class PrestationDesordresPojoTable extends AbstractPrestationDesordresPojoTable {

    /**
     * Creation of a @ListeningPojoTable including a button to link the container to the desordres of its parent
     * If the prestation's parent is a TronconLit, the button action gets the DesordreLit
     * @param container
     * @param isOnTronconLit
     */
    public PrestationDesordresPojoTable(final ObjectProperty<Prestation> container, final boolean isOnTronconLit) {
        super(Desordre.class, null, container, isOnTronconLit);
    }

    protected EventHandler linkParentDesordres(final ObjectProperty<? extends Element> container, final boolean isOnTronconLit){
        return (EventHandler<ActionEvent>) event -> {
            if (!createNewProperty.get()) {
                final Element cont = container.get();
                final List<Desordre> entities;
                if (Desordre.class.isAssignableFrom(pojoClass) && cont instanceof Prestation) {

                    if (isOnTronconLit) {
                        // Collects the repo for DesordreLit specific to TronconLit
                        Optional<AbstractDesordreRepository> repoOp = session.getRepositoriesForClass(Desordre.class).stream().map(r -> (AbstractDesordreRepository) r).filter(r -> r.getClass().getSimpleName().equals("DesordreLitRepository")).findFirst();
                        if (!repoOp.isPresent())
                            throw new IllegalStateException("No repository found to read elements of type : DesordreLit");

                        // Collects all DesordreLit
                        entities = repoOp.get().getDesordreOpenByLinearId(((Prestation) cont).getLinearId());

                    } else {
                        // Collects all the Elements from the pojoClass repository
                        entities = ((DesordreRepository) session.getRepositoryForClass(pojoClass)).getDesordreOpenByLinearId(((Prestation) cont).getLinearId());
                    }

                    if (entities.isEmpty()) {
                        showWarningOrInformationAlert("Aucun désordre ouvert disponible sur le parent de la prestation.", false);
                        return;
                    }

                    // Removes the Desordres already present in the pojoTable
                    entities.removeIf(d -> getAllValues().contains(d));

                    if (entities.isEmpty()) {
                        showWarningOrInformationAlert("Tous les désordres ouverts du parent sont déjà liés à la prestation.", false);
                        return;
                    }

                    final ButtonType res = showConfirmationAlertAndGetResult( "Confirmer le rattachement de " + entities.size() + " désordres ouverts à la prestation.");
                    if (res == ButtonType.NO) return;

                    // adds the Desordres to the pojoTable
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
