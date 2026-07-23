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
package fr.sirs.theme.ui.pojotable;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.*;
import fr.sirs.theme.ui.FXPrestationPane;
import fr.sirs.ui.Growl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import org.apache.sis.util.ArgumentChecks;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class ElementCopier extends AbstractElementCopier{

    // Repository
    protected AbstractSIRSRepository currentPojoRepo;

    final protected Boolean avecForeignParent;
    final protected Boolean isAbstractObservation;
    final protected Boolean isRapportEtude;

    public ElementCopier(Class pojoClass, ObjectProperty<? extends Element> container, AbstractSIRSRepository pojoRepo) {
        super(pojoClass, container);

        this.avecForeignParent = AvecForeignParent.class.isAssignableFrom(pojoClass);
        this.isAbstractObservation = AbstractObservation.class.isAssignableFrom(pojoClass);
        this.isRapportEtude = RapportEtude.class.isAssignableFrom(pojoClass);

        //Identification de la classe vers laquelle on permet la copie.
        if (avecForeignParent) {

            //On a besoin du repositorie de la classe courante uniquement pour
            // les classes AvecForeignParent.
            currentPojoRepo = pojoRepo;

            try {
                this.targetClass = Optional.of(AvecForeignParent.getForeignParentClass(pojoClass));
            } catch (Exception e) {
                this.targetClass = Optional.empty();
            }
        } else if (isAbstractObservation) { //On priorise la classe du ForeignParent pour la target classe.
            try {
                this.targetClass = Optional.of(container.getValue().getClass());
            } catch (NullPointerException e) {
                this.targetClass = Optional.empty();
            }
        } else if (isRapportEtude) {
            currentPojoRepo = pojoRepo;
            this.targetClass = Optional.of(SystemeEndiguement.class);
        } else {
            this.targetClass = Optional.empty();
        }

        //Si on a trouvé une classe cible, on récupère son repositorie.
        targetClass.ifPresent(aClass -> this.targetRepo = Injector.getSession().getRepositoryForClass(aClass));
    }

    public ElementCopier(Class pojoClass, ObjectProperty<? extends Element> container, AbstractSIRSRepository pojoRepo, Class targetClass) {
        this(pojoClass, container, pojoRepo);

        ArgumentChecks.ensureNonNull("Target class", targetClass);

        this.targetClass = Optional.of(targetClass);
        this.targetRepo = Injector.getSession().getRepositoryForClass(targetClass);
    }

    /**
     * Méthode permettant d'obtenir la liste des éléments vers qui la copie est possible.
     *
     * @return choices : élément ciblé.
     * @throws CopyElementException
     */
    @Override
    protected ObservableList<Preview> getChoices() throws CopyElementException {
        final ObservableList<Preview> choices;

        //-------------------------Test si ces cas particuliers se produisent----------------------
        if (avecForeignParent && isAbstractObservation) {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cas particulier pour la copie (ForeignParent && Observation).");
            alert.setResizable(true);
            alert.showAndWait();
        }

        if ((avecForeignParent && (container != null))) {//Se produit : exemple des voies sur Digues depuis désordre. Container -> désordre ; ForeignParent-> tronçon;
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cas particulier pour la copie (ForeignParent && container).");
            alert.setResizable(true);
            alert.showAndWait();
        }
        //------------------------------------------------------------------------------------------

        //L'identification de la targetClass est faite dans le constructeur et priorise
        //le ForeignParent sur le container.
        if (avecForeignParent || isAbstractObservation || isRapportEtude) {

            if (targetClass.isPresent()) {
                // récupération de tous les éléments de la classe identifiée
                choices = SIRS.observableList(new ArrayList<>(Injector.getSession().getPreviews().getByClass(targetClass.get())));
            } else {
                throw new CopyElementException("Copie impossible, aucune cible identifiée pour la copie.");
            }
        } else {
            throw new CopyElementException("Copie impossible pour ce type d'élément.");
        }

        return choices;
    }

    /**
     * Copie d'un ensemble d'éléments sélectionnés vers un élément cible.
     * <p>
     * Cette méthode vise à être redéfinie dans les PojoTables spécifiques
     * (Classe extends PojoTable) en fonction du comportement souhaité de la
     * 'copie'. Par défaut, cette méthode informe l'utilisateur que la copie est
     * impossible.
     *
     * @param targetedElement : Elément auquel on veut ajouter les éléments
     * copiés.
     * @param pojosToCopy : éléments à copier.
     * @return
     */
    @Override
    public List<? extends Element> copyPojosTo(final Element targetedElement, final Element... pojosToCopy) {

        if (this.avecForeignParent) {
            return copyPojosToForeignParent(targetedElement, pojosToCopy);

        } else if (this.isAbstractObservation) {

            final Alert alert = new Alert(Alert.AlertType.WARNING, "Attention les éléments copiés sont susceptibles de ne pas respecter les bornes temporelles de l'élément de destination! Vous devrez les mettre à jour manuellement.");
            alert.setResizable(true);
            alert.showAndWait();

            return copyPojosToContainer(targetedElement, pojosToCopy);
        } else if (this.isRapportEtude) {
            return copyPojosToSystemeEndiguement(targetedElement, pojosToCopy);

        }

        new Growl(Growl.Type.WARNING, "La copie n'est pas définie pour ce type d'élément ou depuis cette fenêtre.").showAndFade();
        return null;
    }

    //======================================
    //Différentes méthodes de copie gérées :
    //======================================

    /**
     *
     * Copie des éléments sélectionnés vers un Foreign Parent.
     *
     * @param targetedForeignParent: élément auquel on veut ajouter les éléments
     * copiés. Cette élément sera le ForeignParent des copies.
     * @param pojosToCopy éléments à copier.
     * @return
     */
    public List<AvecForeignParent> copyPojosToForeignParent(final Element targetedForeignParent, final Element... pojosToCopy) {
        if (currentPojoRepo == null) {
            throw new IllegalStateException("CurrentPojoRepo must not be null when with foreignParent");
        }
        final Session session = Injector.getSession();

        // Redmine 7974 - Allows externe users to copy elements. The copy must be created as "invalid"
        final boolean isExtern = Role.EXTERN.equals(session.roleBinding().get());
        if (isExtern || session.editionAuthorized(targetedForeignParent)) {

            List<AvecForeignParent> copiedPojos = new ArrayList<>();
            boolean completSuccess = true;

            final String userId = session.getUtilisateur().getId();

            for (Element pojo : pojosToCopy) {

                try {

                    AvecForeignParent copiedPojo = (AvecForeignParent) pojo.copy();
                    if (Prestation.class.isAssignableFrom(copiedPojo.getClass())) {
                        Prestation prestation = (Prestation) copiedPojo;
                        prestation.setHorodatageStatusId(HorodatageReference.getRefNonTimeStampedStatus());
                        prestation.setHorodatageStartDate(null);
                        prestation.setHorodatageEndDate(null);
                        prestation.setHorodatageDateDebutStart(null);
                        prestation.setHorodatageDateDebutEnd(null);
                        prestation.setHorodatageDateFinEnd(null);
                        prestation.setRegistreAttribution(FXPrestationPane.isAutoSelectedRegistre(prestation.getTypePrestationId()));
                        prestation.setSyntheseTablePathStart(null);
                        prestation.setSyntheseTablePathEnd(null);
                    }

                    setAttributes(isExtern, copiedPojo, userId, session);
                    copiedPojo.setForeignParentId(targetedForeignParent.getId());

                    //CHECK BORNES TEMPORELLES Parent ET BORNES TEMPORELLLES copiedPojo
                    copiedPojos.add(copiedPojo);

                } catch (ClassCastException e) {

                    completSuccess = false;
                    SIRS.LOGGER.log(Level.INFO, "Echec de la copie de l'élément :\n" + pojo, e);
                }

            }

            currentPojoRepo.executeBulk(copiedPojos);

            if (!completSuccess) {
                new Growl(Growl.Type.WARNING, "Certains éléments n'ont pas pu être copiés.").showAndFade();
            }
            return copiedPojos;

        } else {
            new Growl(Growl.Type.WARNING, "Les éléments n'ont pas été copiés car vous n'avez pas les droits nécessaires.").showAndFade();
            return null;
        }

    }

    /**
     * Copie des éléments sélectionnés vers un container ciblé.
     * <p>
     * Cette méthode est utilisée pour copier des éléments héritant de la classe
     * AbstractObservation. Note : elle peut servir de base si la demande de
     * l'utilisateur évolue pour permettre la copie d'autres type d'éléments
     * vers leur container. ((avecForeignParent && (container != null))
     *
     * @param targetedContainer : container (implémentant Element) auquel on
     * veut ajouter les éléments copiés.
     * @param pojosToCopy : éléments à copier
     * @return
     */
    public List<? extends Element> copyPojosToContainer(Element targetedContainer, Element... pojosToCopy) {

        final Session session = Injector.getSession();

        // Redmine 7974 - Allows externe users to copy elements. The copy must be created as "invalid"
        final boolean isExtern = Role.EXTERN.equals(session.roleBinding().get());
        if (isExtern || session.editionAuthorized(targetedContainer)) {
            boolean completSuccess = true;

            List<Element> copiedPojos = new ArrayList<>();

            final String userId = session.getUtilisateur().getId();

            for (Element pojo : pojosToCopy) {

                try {
                    AbstractObservation copiedPojo = ((AbstractObservation) pojo).copy();
                    setAttributes(isExtern, copiedPojo, userId, session);
                    copiedPojos.add(copiedPojo);
                    targetedContainer.addChild(copiedPojo);

                    //TODO : CHECK BORNES TEMPORELLES Parent ET BORNES TEMPORELLLES copiedPojo
                } catch (ClassCastException e) {

                    completSuccess = false;
                    SIRS.LOGGER.log(Level.FINE, "Echec de la copie de l'élément car il n'implémente pas la classe AbstractObservation :\n" + pojo, e);
                }

            }

            try {
                // Le container porte les référence vers les "enfants" sauf pour les
                // AbstractObservation qui ne sont pas des documents couchDB.
                // C'est donc le container cible de la copie, qu'il faut mettre à
                // jour en base.
                this.getTargetRepo().update(targetedContainer);
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

    /**
     *
     * Copie des rapports d'étude sélectionnés vers un Systeme d'Endiguement.
     *
     * @param targetedSystemeEndiguement: élément auquel on veut ajouter les éléments
     * copiés. Cet élément sera le SystemeEndiguement des copies.
     * @param pojosToCopy éléments à copier.
     * @return
     */
    public List<RapportEtude> copyPojosToSystemeEndiguement(final Element targetedSystemeEndiguement, final Element... pojosToCopy) {

        final Session session = Injector.getSession();

        // Redmine 7974 - Allows externe users to copy elements. The copy must be created as "invalid"
        final boolean isExtern = Role.EXTERN.equals(session.roleBinding().get());
        if (isExtern || session.editionAuthorized(targetedSystemeEndiguement)) {

            List<RapportEtude> copiedPojos = new ArrayList<>();
            boolean completSuccess = true;

            final String userId = session.getUtilisateur().getId();

            RapportEtude copiedPojo;
            for (Element pojo : pojosToCopy) {
                if (!(pojo instanceof RapportEtude))
                    SIRS.LOGGER.log(Level.WARNING, "Echec de la copie de l'élément :\n" + pojo.toString());

                copiedPojo = (RapportEtude) pojo.copy();

                setAttributes(isExtern, copiedPojo, userId, session);

                copiedPojo.setSystemeEndiguementId(targetedSystemeEndiguement.getId());

                //CHECK BORNES TEMPORELLES Parent ET BORNES TEMPORELLLES copiedPojo
                copiedPojos.add(copiedPojo);
            }
            if (currentPojoRepo == null)
                SIRS.LOGGER.log(Level.WARNING, "Repository introuvable");

            currentPojoRepo.executeBulk(copiedPojos);
            if (!completSuccess) {
                new Growl(Growl.Type.WARNING, "Certains éléments n'ont pas pu être copiés.").showAndFade();
            }
            return copiedPojos;

        } else {
            new Growl(Growl.Type.WARNING, "Les éléments n'ont pas été copiés car vous n'avez pas les droits nécessaires.").showAndFade();
            return null;
        }

    }

    /**
     * Method to set the global attributes to the copy.
     * If the connected user has an Extern role, then the copy must be invalid.
     *
     * @param isExtern whether the connected user has Extern role.
     * @param copiedPojo the copy of the selected pojo.
     * @param userId the id of the connected user.
     * @param session the session :D
     */
    public static void setAttributes(boolean isExtern, Element copiedPojo, String userId, Session session) {
        if (isExtern) {
            // Redmine 7974
            copiedPojo.setValid(false);
        }
        copiedPojo.setDesignation(null);

        // Otherwise the copied object has the same creator as the original object and thus
        // the info of the user making the copy is lost.
        copiedPojo.setAuthor(userId);
        copiedPojo.setLastUpdateAuthor(null);

        session.getElementCreator().tryAutoIncrementDesignation(copiedPojo);
    }

    //Getters

    @Override
    public boolean hasForeignParent() {
        return avecForeignParent;
    }

    @Override
    public boolean isRapportEtude() {
        return isRapportEtude;
    }
}
