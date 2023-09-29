/**
 *
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

package fr.sirs.migration.upgrade.v2and23;

import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import static fr.sirs.core.SirsCore.LOGGER;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Positionable;
import fr.sirs.util.ConvertPositionableCoordinates;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.DbAccessException;
import org.geotoolkit.internal.GeotkFX;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public final class UpgradeLink1NtoNN extends Task {

    private final DatabaseRegistry dbRegistry;
    private final String dbName;
    private final Upgrades1NtoNNSupported upgrade;

    /**
     * Attribut représentant l'ensemble des échecs durant la mise à jour.
     * Consiste en une Map. Les clefs sont les éléments portant initialement la
     * relation mise à jour en échec. Les valeurs sont les identifiants (String)
     * des éléments associé à la clef qui n'ont pas pu être mis à jour.
     *
     */
    private final UpgradeFailureReport failureReport = new UpgradeFailureReport();

    public UpgradeLink1NtoNN(final DatabaseRegistry dbRegistry, final String dbName, final Upgrades1NtoNNSupported upgrade) {
        ArgumentChecks.ensureNonNull("dbRegistry", dbRegistry);
        ArgumentChecks.ensureNonNull("dbName", dbName);

        SirsCore.LOGGER.info(String.format("Initialisation de la mise à jours des relations 1-N en relation N-N pour la version : %d.%d ", upgrade.upgradeMajorVersion, upgrade.upgradeMinorVersion));
        this.upgrade = upgrade;
        this.dbRegistry = dbRegistry;
        this.dbName = dbName;

    }

    @Override
    protected Object call() throws Exception {

        updateTitle(String.format("Mise à jours des relations 1-N en relation N-N pour la version : %d.%d ", upgrade.upgradeMajorVersion, upgrade.upgradeMinorVersion));

        updateMessage("Connection à la base de données");
        try (final ConfigurableApplicationContext upgradeContext = this.dbRegistry.connectToSirsDatabase(dbName, false, false, false)) {

            Session session = upgradeContext.getBean(Session.class);

            SirsCore.LOGGER.info("Récupération des répositories nécessaire à la mise à jour");

            //Récupération des répositories nécessaire à la mise à jour :
            //-----------------------------------------------------------
            final AbstractSIRSRepository<Element> repoSide1 = session.getRepositoryForClass(upgrade.linkSide1);

            final Map< Class, AbstractSIRSRepository<Element>> reposSidesN = upgrade.linkSidesN.stream()
                    .map(u -> u.clazz)
                    .collect(Collectors.toMap(c -> c,
                            c -> session.getRepositoryForClass(c)
                    ));

            updateMessage("Parcours des éléments portant initialement les liens et mise à jour");

            /* Parcours des éléments portant initialement les liens
            * (exemple : si migration [Desordre 1..* Ouvrage] en [Desordre *..* Ouvrage]
            * => parcours de chacun des désordres). */
            repoSide1.getAllStreaming()
                    .forEach(d -> upgrade(upgrade, d, repoSide1, reposSidesN));

            return true;

        } catch (DbAccessException ex) {
            LOGGER.log(Level.WARNING, "Problème d'accès au CouchDB, utilisateur n'ayant pas les droits administrateur.", ex);
            Platform.runLater(() -> GeotkFX.newExceptionDialog("L'utilisateur de la base CouchDB n'a pas les bons droits. "
                    + "Réinstaller CouchDB ou supprimer cet utilisateur \"geouser\" des administrateurs de CouchDB, "
                    + "puis relancer l'application.", ex).showAndWait());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            Platform.runLater(() -> GeotkFX.newExceptionDialog("Une erreur est survenue pendant l'import de la base.", ex).showAndWait());

        } finally {
            updateMessage("Fin de la mise à jour; Lecture du rapport d'erreurs");
            final String message
                    = "\n\n====================================================="
                    + "Fin de la mise à jour; Lecture du rapport d'erreurs :"
                    + "=====================================================\n";
            LOGGER.log(Level.WARNING, message);
            LOGGER.log(Level.WARNING, failureReport.getStringReport());

        }

        return false;
    }

    private void upgrade(final Upgrades1NtoNNSupported upgrade, final Element element,final AbstractSIRSRepository<Element> repoSide1, final Map< Class, AbstractSIRSRepository<Element>> reposSidesN) {
        ArgumentChecks.ensureNonNull("Upgrade Task", upgrade);
        ArgumentChecks.ensureNonNull("Upgrade Task", upgrade.linkSide1);
        ArgumentChecks.ensureNonNull("Upgrade Task", upgrade.linkSidesN);
        ArgumentChecks.ensureNonNull("Repositories' map", reposSidesN);

        if (element != null) {
            final String elementInfo = String.format("Element parcouru : %s   -> %s ", element.getClass().getCanonicalName(), element.getDesignation());
            updateMessage(elementInfo);

            //Ensure both coordinates kinds
            tryUpgradeCoordinates(element, elementInfo);

            if (!(upgrade.linkSide1.isInstance(element))) {
                throw new IllegalArgumentException(" Element input must be an instance of the upgrade input");
            }

            try {
                for (ClassAndItsGetter classeAndGetter : upgrade.linkSidesN) {
                    final List<String> extractedIds = (List<String>) classeAndGetter.getter.invoke(element);
                    final AbstractSIRSRepository<Element> repo = reposSidesN.get(classeAndGetter.clazz);
                    if (repo != null) {
                        final List<Element> updated = extractedIds.stream()
                                .map(
                                        id -> addChildToElementFromId(element, id, repo) //return null if an exception occured when trying to add child
                                )
                                .filter(e -> e != null)
                                .peek(e -> tryUpgradeCoordinates(e, e.getDesignation()))
                                .collect(Collectors.toList());
                        repo.executeBulk(updated);
                    }

                }
                repoSide1.update(element);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Impossible de mettre \u00e0 jour l''\u00e9l\u00e9ment suivant :%s id :%s", elementInfo, element.getId()), e);
//                throw new BackingStoreException(e.getCause());
            }
        } else {
            LOGGER.log(Level.WARNING, "Try tu upgrade a null element.");
        }
    }

    /**
     *
     * @param element Non null!
     */
    static boolean tryUpgradeCoordinates(final Element element , final String elementInfo) {

        try {
            if (element instanceof Positionable) {
                return ConvertPositionableCoordinates.COMPUTE_MISSING_COORD.test((Positionable) element);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to upgrade coordinates for {0}", elementInfo);
        }
        return false;
    }

    /**
     *
     * @param element to add as child
     * @param id id of the {@link Element} we want to add a child
     * @param repo repository allowing to retrive the {@link Element} associated
     * with the id-input
     * @return the {@link Element} associated with the given input after adding
     * the input {@linkplain Element element} Or null if an exception occured.
     * The null return aim to allow to filter the updated elements.
     */
    private Element addChildToElementFromId(final Element element, final String id, final AbstractSIRSRepository<Element> repo) {
        try {
            final Element elt = repo.get(id);
            elt.addChild(element);
            return elt;
        } catch (Exception e) {
            final String message = failureReport.addFailure(element, id);
            LOGGER.log(Level.WARNING, message, e);
            return null;
        }
    }

}
