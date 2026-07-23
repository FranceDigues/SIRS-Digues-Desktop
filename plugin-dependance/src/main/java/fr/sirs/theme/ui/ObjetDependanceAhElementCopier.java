/**
 * This file is part of SIRS-Digues 2.
 * <p>
 * Copyright (C) 2019, FRANCE-DIGUES,
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
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.*;
import fr.sirs.theme.ui.pojotable.AbstractElementCopier;
import fr.sirs.theme.ui.pojotable.ElementCopier;
import fr.sirs.ui.Growl;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Class to allow the copy of elements inside the plugin Dependance and AH.
 * @author Estelle Idée (Geomatys)
 */
public class ObjetDependanceAhElementCopier extends AbstractElementCopier {

    // Repository
    protected AbstractSIRSRepository currentPojoRepo;
    protected Boolean isDesordreDependance;


    public ObjetDependanceAhElementCopier(Class pojoClass, ObjectProperty<? extends Element> container, AbstractSIRSRepository pojoRepo) {
        super(pojoClass, container);

        if (!ObjetDependanceAh.class.isAssignableFrom(pojoClass)) {
            throw new IllegalStateException("pojoClass " + pojoClass.getSimpleName() + " is not an ObjectDependanceAh");
        }

        this.isDesordreDependance = DesordreDependance.class.isAssignableFrom(pojoClass);

        //Identification de la classe vers laquelle on permet la copie.
        currentPojoRepo = pojoRepo;

        if (isDesordreDependance) {
            targetClass = Optional.of(AbstractDependance.class);
        } else {
            targetClass = Optional.of(AmenagementHydraulique.class);
        }

        this.targetRepo = Injector.getSession().getRepositoryForClass(targetClass.get());
    }

    /**
     * Méthode permettant d'obtenir la liste des éléments vers qui la copie est possible.
     *
     * @return choices : élément ciblé.
     */
    @Override
    protected ObservableList<Preview> getChoices() {
        final ObservableList<Preview> choices;
        final Session session = Injector.getSession();
        // Les ObjetDependance peuvent être liés à un Aménagement Hydraulique.
        // Les DesordreDependance sont un cas particulier car ils peuvent également être liés à des Dépendances.
        if (isDesordreDependance) {
            choices = SIRS.observableList((session.getPreviews().getByClass(AbstractDependance.class)));
        } else {
            choices = SIRS.observableList((session.getPreviews().getByClass(AmenagementHydraulique.class)));
        }
        return choices;
    }

    /**
     *
     * Copie des éléments sélectionnés vers un AmenagementHydraulique ou une Dependance.
     *
     * @param targetedDependanceOrAh: élément auquel on veut ajouter les éléments
     * copiés. Cet élément sera le AmenagementHydrauliqueId ou le DependanceId des copies.
     * @param pojosToCopy éléments à copier.
     * @return the list of the copied elements.
     */
    @Override
    public List<? extends Element> copyPojosTo(final Element targetedDependanceOrAh, final Element... pojosToCopy) {

        final Session session = Injector.getSession();

        // Redmine 7974 - Allows externe users to copy elements. The copy must be created as "invalid"
        final boolean isExtern = Role.EXTERN.equals(session.roleBinding().get());
        if (isExtern || session.editionAuthorized(targetedDependanceOrAh)) {

            List<ObjetDependanceAh> copiedPojos = new ArrayList<>();
            boolean completSuccess = true;

            final String userId = session.getUtilisateur().getId();

            for (Element pojo : pojosToCopy) {

                try {

                    ObjetDependanceAh copiedPojo = (ObjetDependanceAh) pojo.copy();

                    // A Desordre Dependance can be linked to a Dependance or to an AH. But not both at the same time.
                    if (targetedDependanceOrAh instanceof AmenagementHydraulique) {
                        copiedPojo.setAmenagementHydrauliqueId(targetedDependanceOrAh.getId());
                        if (isDesordreDependance) {
                            ((DesordreDependance) copiedPojo).setDependanceId(null);
                        }
                    } else if (isDesordreDependance) {
                        ((DesordreDependance) copiedPojo).setDependanceId(targetedDependanceOrAh.getId());
                        copiedPojo.setAmenagementHydrauliqueId(null);
                    }

                    ElementCopier.setAttributes(isExtern, copiedPojo, userId, session);

                    copiedPojos.add(copiedPojo);

                } catch (ClassCastException e) {
                    completSuccess = false;
                    SIRS.LOGGER.log(Level.FINE, "Echec de la copie de l'élément :\n" + pojo, e);
                }

            }
            try {
                currentPojoRepo.executeBulk(copiedPojos);
            } catch (NullPointerException e) {
                SIRS.LOGGER.log(Level.FINE, "Repository introuvable", e);
            }

            if (!completSuccess) {
                new Growl(Growl.Type.WARNING, "Certains éléments n'ont pas pu être copiés.").showAndFade();
            }
            return copiedPojos;

        } else {
            new Growl(Growl.Type.WARNING, "Les éléments n'ont pas été copiés car vous n'avez pas les droits nécessaires.").showAndFade();
            return null;
        }

    }

    // Allow to refresh the pojoTable after the copy.
    @Override
    public boolean hasForeignParent() {
        return true;
    }

    @Override
    public boolean isRapportEtude() {
        return false;
    }
}
