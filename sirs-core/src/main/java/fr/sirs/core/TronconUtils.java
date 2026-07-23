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
package fr.sirs.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import static fr.sirs.core.LinearReferencingUtilities.asLineString;
import static fr.sirs.core.LinearReferencingUtilities.buildGeometry;
import static fr.sirs.core.SirsCore.SR_ELEMENTAIRE;
import static fr.sirs.core.SirsCore.SR_ELEMENTAIRE_END_BORNE;
import static fr.sirs.core.SirsCore.SR_ELEMENTAIRE_START_BORNE;
import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.BorneDigueRepository;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.AbstractPositionDocument;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.AvecForeignParent;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.GardeTroncon;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.ProprieteTroncon;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.StreamingIterable;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentOperationResult;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.referencing.LinearReferencing;
import org.geotoolkit.referencing.LinearReferencing.ProjectedPoint;
import org.geotoolkit.referencing.LinearReferencing.SegmentInfo;
import static org.geotoolkit.referencing.LinearReferencing.buildSegments;
import static org.geotoolkit.referencing.LinearReferencing.computeRelative;
import static org.geotoolkit.referencing.LinearReferencing.projectReference;
import org.geotoolkit.util.collection.CloseableIterator;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A set of utility methods for manipulation of geometries of
 * {@link Positionable} or {@link TronconDigue} objects.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class TronconUtils {

    private static final GeometryFactory GF = GO2Utilities.JTS_FACTORY;

    //==================================================================================================================
    // UTILITAIRES DU CONTRÔLE DE L'ARCHIVAGE DES TRONÇONS ET DES OBJETS QUI LES RÉFÉRENCENT.
    //==================================================================================================================
    /**
     * Description des scénarios d'évolution de l'archivage d'un tronçon.
     */
    public enum ArchiveMode {
        /**
         * L'état d'archivage reste inchangé. Le tronçon archivé reste archivé à
         * date constante. Le tronçon non archivé reste non archivé.
         */
        UNCHANGED,
        /**
         * Un tronçon non archivé est archivé.
         */
        ARCHIVE,
        /**
         * Un tronçon archivé est désarchivé.
         */
        UNARCHIVE,
        /**
         * La date d'archivage d'un tronçon est modifiée.
         */
        UPDATE_ARCHIVE
    }

    /**
     * Copy a {@link TronconDigue} and all bound data, i.e linear referencing
     * systems and objects positioned on it.
     *
     * @param original Linear to copy
     * @param session Current session, to perform database updates.
     * @return A copy of input data, up to date in database.
     */
    public static TronconDigue copyTronconAndRelative(final TronconDigue original, final SessionCore session) {
        final TronconDigue tdCopy = original.copy();

        final AbstractSIRSRepository<TronconDigue> tronconRepo = (AbstractSIRSRepository<TronconDigue>) session.getRepositoryForClass(original.getClass());
        tronconRepo.add(tdCopy);

        final SystemeReperageRepository srRepo = InjectorCore.getBean(SystemeReperageRepository.class);
        SystemeReperage srCopy;
        final Map<String, String> srMapping = new HashMap<>();
        for (final SystemeReperage sr : srRepo.getByLinearStreaming(original)) {
            srCopy = sr.copy();
            srCopy.setLinearId(tdCopy.getId());
            srRepo.add(srCopy);
            srMapping.put(sr.getId(), srCopy.getId());
        }

        if (tdCopy.getSystemeRepDefautId() != null) {
            tdCopy.setSystemeRepDefautId(srMapping.get(tdCopy.getSystemeRepDefautId()));
            tronconRepo.update(tdCopy);
        }

        // On ajoute les structures du tronçon paramètre.
        copyElementsOnNewTroncon(tdCopy, original, session, srMapping);

        return tdCopy;
    }

    /**
     *
     * @param section The linear object to archive.
     * @param session The session to use for database connection.
     * @param archiveDate Archive date to set (as {@link AvecBornesTemporelles#setDate_fin(java.time.LocalDate)
     * }. If null, the linear and its related data will be unarchived.
     * @param filter Update only objects matching this predicate (for which {@link Predicate#test(java.lang.Object)
     * } is true.
     * @return The list of update failures. Not that if the linear itself cannot
     * be updated, an exception will be thrown.
     * @throws DbAccessException If the given linear object cannot be updated.
     */
    public static List<DocumentOperationResult> archiveSectionWithTemporalObjects(final TronconDigue section,
            final SessionCore session, final LocalDate archiveDate, final Predicate<AvecBornesTemporelles> filter) {

        // First we update linear object. If it fails, it's no use to continue.
        section.setDate_fin(archiveDate);
        final AbstractSIRSRepository tdRepo = session.getRepositoryForClass(section.getClass());
        if (section.getId() == null) {
            tdRepo.add(section);
        } else {
            tdRepo.update(section);
        }

        /* To avoid memory nor processing overload, we manually process and update
         * objects by type. Another approach would be to use getPositionableList
         * to get all objects to update, but it put all of them in memory, and the
         * session bulk is forced to sort them before updating them in database.
         */
        return session.getRepositoriesForClass(AvecBornesTemporelles.class).stream()
                .filter(AbstractPositionableRepository.class::isInstance)
                .map(AbstractPositionableRepository.class::cast)
                .map(repo -> {
                    final List linears = repo.getByLinearId(section.getId());
                    linears.removeIf(filter.negate());
                    linears.forEach(dated -> ((AvecBornesTemporelles) dated).setDate_fin(archiveDate));
                    return repo.executeBulk(linears);
                })
                .flatMap(List<DocumentOperationResult>::stream)
                .collect(Collectors.toList());
    }

    public static List<DocumentOperationResult> archiveBornes(final Collection<String> borneIds,
            final SessionCore session, final LocalDate archiveDate, final Predicate<AvecBornesTemporelles> updateCondition) {

        final List<TronconDigue> allTroncons = session.getRepositoryForClass(TronconDigue.class).getAll();

        final Consumer<AvecBornesTemporelles> setArchiveDate = dated -> dated.setDate_fin(archiveDate);

        /*
        Predicate over milestones must always return true when unarchiving, 
        because the rule is to archive the milestone if and only if all the 
        sections which it is referenced by are themselves archived.
        An archive date 'null' means a section is currently being unarchived and 
        so, the referenced milestones MUST all be unarchived.
         */
        final Predicate<BorneDigue> unarchive = (BorneDigue t) -> archiveDate == null;

        // [SYM-1692] requires explicitly the milestones only linked to an archived section to be archived too.
        // Prédicate over milestones to filter referenced ones by unarchived sections.
        final Predicate<BorneDigue> referencedByUnarchivedSection = new Predicate<BorneDigue>() {
            @Override
            public boolean test(BorneDigue borne) {
                /*
                A milestone must be archived if and only if there is 
                no more unarchived section referencing it.
                
                This predicate checks the current milestone is referenced by an 
                unarchived section.
                 */
                for (final TronconDigue section : allTroncons) {
                    // We search an unarchived section referencing the current milestone
                    if (section.getDate_fin() == null || section.getDate_fin().isAfter(archiveDate)) {
                        for (final String borneId : section.getBorneIds()) {
                            if (borneId.equals(borne.getId())) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        };

        final AbstractSIRSRepository<BorneDigue> borneRepo = session.getRepositoryForClass(BorneDigue.class);

        final List<BorneDigue> bornes = borneRepo.get(borneIds).stream()
                .filter(updateCondition)
                .filter(unarchive.or(referencedByUnarchivedSection.negate()))
                .peek(setArchiveDate)
                .collect(Collectors.toList());

        return borneRepo.executeBulk(bornes);
    }

    //==================================================================================================================
    // UTILITAIRES DE DÉCOUPAGE DE TRONÇON
    //==================================================================================================================
    /**
     * Crée une copie du tronçon en entrée, dont la géométrie se limite à la
     * polyligne donnée. Les objets du tronçon source présents sur cette
     * nouvelle polyligne sont copiés, et les systèmes de repérages copiés et
     * adaptés à la nouvelle géométrie.
     *
     * @param troncon troncon a decouper
     * @param cutLinear partie du troncon a garder
     * @param newName Le nom à affecter au troncon généré.
     * @param session La session applicative courante.
     * @return nouveau troncon découpé
     */
    public static TronconDigue cutTroncon(TronconDigue troncon, LineString cutLinear, String newName, SessionCore session) {
        ArgumentChecks.ensureNonNull("Troncon to cut", troncon);
        ArgumentChecks.ensureNonNull("Line string to extract", cutLinear);
        ArgumentChecks.ensureNonNull("Database session", session);

        /* First, we get index (as distance along the linear) of the bounds of new
         * tronçon segments. It will allow us to retrieve objects which are
         * projected in those bounds and must be effectively copied.
         */
        final LengthIndexedLine index = new LengthIndexedLine(troncon.getGeometry());
        final double startDistance = index.project(cutLinear.getStartPoint().getCoordinate());
        final double endDistance = index.project(cutLinear.getEndPoint().getCoordinate());

        final SystemeReperageRepository srRepo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);
        final AbstractSIRSRepository<BorneDigue> bdRepo = session.getRepositoryForClass(BorneDigue.class);
        final AbstractSIRSRepository<TronconDigue> tdRepo = session.getRepositoryForClass(TronconDigue.class);

        //======================================================================
        // MODIFICATIONS DE BASE SUR LE TRONÇON
        //======================================================================
        final TronconDigue tronconCp = troncon.copy();
        tronconCp.setGeometry(cutLinear);
        tronconCp.setLibelle(newName);
        // On enlève toute réference vers un SR appartenant au tronçon copié
        tronconCp.setSystemeRepDefautId(null);

        // On sauvegarde les modifications
        tdRepo.add(tronconCp);

        //======================================================================
        // RÉFÉRENCES DE BORNES À CONSERVER
        //======================================================================
        /* On évince toutes les bornes qui ne sont pas sur le nouveau tronçon. On
         * garde un index des ids de borne conservés, cela accélerera le tri sur
         * les SR.
         */
        final LineString sourceLine = asLineString(troncon.getGeometry());
        final SegmentInfo[] sourceTronconSegments = buildSegments(sourceLine);
        final ListIterator<BorneDigue> borneIt = bdRepo.get(tronconCp.getBorneIds()).listIterator();
        final Set<String> keptBornes = new HashSet<>();
        while (borneIt.hasNext()) {
            final BorneDigue borne = borneIt.next();
            final ProjectedPoint proj = projectReference(sourceTronconSegments, borne.getGeometry());
            if (proj.distanceAlongLinear >= startDistance && proj.distanceAlongLinear <= endDistance) {
                keptBornes.add(borne.getId());
            }
            tronconCp.getBorneIds().setAll(keptBornes);
        }

        //======================================================================
        // COPIE DES SRs / RETRAIT DES BORNES HS / AFFECTATION D'UN SR PAR DEFAUT / MISE À JOUR DU SR ÉLÉMENTAIRE
        //======================================================================
        /* On copie les SR du tronçon original. Pour chaque, on regarde si il contient
         * des bornes référencées sur le nouveau tronçon. Si c'est le cas, on le
         * garde pour enregistrement à la fin de l'opération. On garde aussi une
         * réference vers le SR original, pour pouvoir mettre à jour la position
         * des structures.
         */
        final Map<String, SystemeReperage> newSRs = new HashMap<>();
        final StreamingIterable<SystemeReperage> srs = srRepo.getByLinearStreaming(troncon);
        try (final CloseableIterator<SystemeReperage> srIt = srs.iterator()) {
            while (srIt.hasNext()) {
                final SystemeReperage sr = srIt.next();
                final SystemeReperage srCp = sr.copy();
                final ListIterator<SystemeReperageBorne> srBorneIt = srCp.getSystemeReperageBornes().listIterator();
                while (srBorneIt.hasNext()) {
                    if (!keptBornes.contains(srBorneIt.next().getBorneId())) {
                        srBorneIt.remove();
                    }
                }
                if (!srCp.systemeReperageBornes.isEmpty()) {
                    newSRs.put(sr.getId(), srCp);
                }
            }
        }
        // On essaye de trouver un SR par défaut pour notre nouveau tronçon et on enregistre les SR.
        // On l'enleve de la map pour le remettre après insertion de tous les autres SRs, car il doit
        // être traité différemment.
        final SystemeReperage newDefaultSR = newSRs.remove(troncon.getSystemeRepDefautId());
        if (newDefaultSR != null) {
            newDefaultSR.setLinearId(tronconCp.getDocumentId());
            srRepo.add(newDefaultSR, tronconCp, true);
        }
        for (final SystemeReperage newSR : newSRs.values()) {
            newSR.setLinearId(tronconCp.getDocumentId());
            srRepo.add(newSR, tronconCp, false);
        }
        if (troncon.getSystemeRepDefautId() != null) {
            newSRs.put(troncon.getSystemeRepDefautId(), newDefaultSR);
        }

        // Mise à jour particulière pour le SR élémentaire qui doit avoir une borne de début et de fin.
        updateSRElementaire(tronconCp, session);

        //======================================================================
        // DÉCOUPAGE DES POSITIONABLES POSITIONÉS SUR LE MORCEAU DE TRONÇON
        //======================================================================
        /* On parcourt la liste des objets positionnés sur le tronçon originel.
         * Pour tout objet contenu dans le morceau découpé (tronçon de sortie)
         * on met simplement à jour ses positions linéaires pour rester cohérent
         * avec sa géométrie. Les objets qui intersectent le nouveau tronçon sont
         * quand à eux découpés.
         * Note : On fait une copie des objets à affecter au nouveau tronçon
         */
        final ListIterator<Positionable> posIt = getPositionableList(troncon).listIterator();
        final List<Positionable> newPositions = new ArrayList<>();
        SegmentInfo[] cutTronconSegments = null;
        final Geometry intersectionBuffer = cutLinear.buffer(0.00001);
        Positionable original;
        Geometry originalGeometry;
        Positionable copied;

        // Map to store the link between original objets ids and created ones ids.
        final Map<String, String> mapOldandNewIds= new HashMap<>();
        AbstractSIRSRepository<Positionable> repo;
        while (posIt.hasNext()) {
            original = posIt.next();

            // Redmine 8196 : We keep archived objects
            final boolean isDocument = original.getParent() == null;

            //on vérifie que cet objet intersecte le segment
            originalGeometry = original.getGeometry();
            if (originalGeometry == null) {
                //on la calcule
                originalGeometry = buildGeometry(sourceLine, sourceTronconSegments, original, bdRepo);
                if (originalGeometry == null && isDocument) {
                    throw new IllegalStateException("Impossible de déterminer la géométrie de l'objet suivant :\n" + original);
                } else if (originalGeometry == null) {
                    continue;
                }
                original.setGeometry(originalGeometry);
            }

            /*
            Les opérations de vividsolutions "intersects", "contains" et "intersection"
            ne donnent pas les mêmes résultats selon qu'un point est représenté par
            une géométrie de type "POINT(x y)" ou par une géométrie de type "LINESTRING(x y, x y)"
            c'est-à-dire une ligne formée de deux points de mêmes coordonnées.

            Dans les lignes suivantes la géométrie interpObjGeom est destinée
            à représenter temporairement sous forme de géométrie "POINT" les
            géométries "LINESTRING" qui, de fait représentent des points, de manière
            à obtenir les bons résultats d'opérations topologiques.
             */
            if (originalGeometry instanceof LineString) {
                final LineString line = (LineString) originalGeometry;

                // Si l'objet est ponctuel il faut transformer sa géométrie en point pour détecter intersection
                if (line.getNumPoints() == 2 && line.getPointN(0).equals(line.getPointN(1))) {
                    originalGeometry = line.getPointN(0);
                }
            }

            /*
             * JTS intersections does not work well with colinear polylines. We
             * must use a buffer to perform them.
             */
            if (!intersectionBuffer.intersects(originalGeometry)) {
                continue;
            }

            copied = original.copy();
            if (copied instanceof AvecForeignParent) {
                ((AvecForeignParent) copied).setForeignParentId(tronconCp.getId());
            }
            // TODO understand why this line. If it is not a document, then it is not added to the database, so why keep going ?
            if (isDocument) {
                newPositions.add(copied);
            }

            // Mise à jour des infos géographiques
            if (!(originalGeometry instanceof Point || intersectionBuffer.covers(originalGeometry))) {
                final Geometry newGeom = intersectionBuffer.intersection(originalGeometry);
                copied.setGeometry(newGeom);
            }

            if ((originalGeometry instanceof Point || intersectionBuffer.covers(originalGeometry)) && (copied.getPositionDebut() != null || copied.getPositionFin() != null)) {
                // Do nothing. Positions are already correct. If we set them based on geometry, we will lose unprojected information.
            } else {
                copied.setPositionDebut(GF.createPoint(copied.getGeometry().getCoordinates()[0]));
                copied.setPositionFin(GF.createPoint(copied.getGeometry().getCoordinates()[copied.getGeometry().getNumPoints() - 1]));
            }

            // Mise à jour du réferencement linéaire
            final SystemeReperage sr = newSRs.get(copied.getSystemeRepId());
            if (sr == null) {
                copied.setSystemeRepId(null);
                copied.setBorneDebutId(null);
                copied.setBorneFinId(null);
                copied.setBorne_debut_distance(0);
                copied.setBorne_fin_distance(0);
            } else {
                final PosInfo info;
                if (cutTronconSegments == null) {
                    info = new PosInfo(copied, tronconCp);
                    cutTronconSegments = info.getTronconSegments(true);
                } else {
                    info = new PosInfo(copied, tronconCp, cutTronconSegments);
                }
                final PosSR posSr = info.getForSR(sr);

                copied.setSystemeRepId(posSr.srid);
                copied.setBorneDebutId(posSr.borneStartId);
                copied.setBorne_debut_distance((float) posSr.distanceStartBorne);
                copied.setBorne_debut_aval(posSr.startAval);
                copied.setBorneFinId(posSr.borneEndId);
                copied.setBorne_fin_distance((float) posSr.distanceEndBorne);
                copied.setBorne_fin_aval(posSr.endAval);
            }

            repo = (AbstractSIRSRepository<Positionable>) session.getRepositoryForClass(copied.getClass());
            // Add to repo to get an id for the newly created positionable.
            if (repo == null) {
                // Photo is a Positionable and is located based on the parent's SR. But it is not an Objet and thus has no linearId.
                // It will be copied directly through its parent : the Observation.
                SirsCore.LOGGER.log(Level.WARNING, "Repository null for class : " + copied.getClass());
            } else {
                repo.add(copied);
            }
            // Store the old and new Ids to use later on to update the links between positionables.
            mapOldandNewIds.put(original.getId(), copied.getId());
        }

        // Update links between positionables.
        updatePositionablesRefs(newPositions, mapOldandNewIds);

        // Et on termine par la sérialisation des objets positionés sur le nouveau tronçon.
        final List<DocumentOperationResult> bulkErrors = session.executeBulk((Collection) newPositions);
        for (final DocumentOperationResult failure : bulkErrors) {
            SirsCore.LOGGER.log(Level.WARNING, "Update failed : ".concat(failure.getError()));
        }

        return tronconCp;
    }

    //==================================================================================================================
    // UTILITAIRES DE RÉCUPÉRATION D'ENTITÉS POSITIONNÉES SUR LE TRONÇON
    //==================================================================================================================
    /**
     * Retrieve the list of owners of a linear whose id is given as parameter.
     *
     * @param linearId Id of the {@link TronconDigue} to get bound
     * {@link ProprieteTroncon} for.
     * @return List of {@link ProprieteTroncon} bound to the given
     * {@link TronconDigue}.
     */
    public static List<ProprieteTroncon> getProprieteList(final String linearId) {
        return InjectorCore.getBean(SessionCore.class).getProprietesByTronconId(linearId);
    }

    /**
     * Retrieve the list of owners of a linear.
     *
     * @param linear {@link TronconDigue} to get bound {@link ProprieteTroncon}
     * for.
     * @return List of {@link ProprieteTroncon} bound to the given
     * {@link TronconDigue}.
     */
    public static List<ProprieteTroncon> getProprieteList(final TronconDigue linear) {
        return getProprieteList(linear.getId());
    }

    /**
     * Retrieve the list of guards linked to a linear whose id is given as a
     * parameter.
     *
     * @param linearId Id of the {@link TronconDigue} to get bound
     * {@link GardeTroncon} for.
     * @return List of {@link GardeTroncon} bound to the given
     * {@link TronconDigue}.
     */
    public static List<GardeTroncon> getGardeList(final String linearId) {
        return InjectorCore.getBean(SessionCore.class).getGardesByTronconId(linearId);
    }

    /**
     * Retrieve the list of guards of a linear.
     *
     * @param linear {@link TronconDigue} to get bound {@link GardeTroncon} for.
     * @return List of {@link GardeTroncon} bound to the given
     * {@link TronconDigue}.
     */
    public static List<GardeTroncon> getGardeList(final TronconDigue linear) {
        return getGardeList(linear.getId());
    }

    /**
     * Retrieve the list of objects linked to a linear whose id is given as a
     * parameter.
     *
     * @param linearId Id of the {@link TronconDigue} to get bound {@link Objet}
     * for.
     * @return List of {@link Objet} bound to the given {@link TronconDigue}.
     */
    public static List<Objet> getObjetList(final String linearId) {
        return InjectorCore.getBean(SessionCore.class).getObjetsByTronconId(linearId);
    }

    /**
     * Retrieve the list of objects linked to a linear.
     *
     * @param linear {@link TronconDigue} to get bound {@link Objet} for.
     * @return List of {@link Objet} bound to the given {@link TronconDigue}.
     */
    public static List<Objet> getObjetList(final TronconDigue linear) {
        return getObjetList(linear.getId());
    }

    /**
     * Retrieve the list of document positions linked to a linear whose id is
     * given as a parameter.
     *
     * @param linearId Id of the {@link TronconDigue} to get bound
     * {@link AbstractPositionDocument} for.
     * @return List of {@link AbstractPositionDocument} bound to the given
     * {@link TronconDigue}.
     */
    public static List<AbstractPositionDocument> getPositionDocumentList(final String linearId) {
        return InjectorCore.getBean(SessionCore.class).getPositionDocumentsByTronconId(linearId);
    }

    /**
     * Retrieve the list of document positions linked to a linear given as a
     * parameter.
     *
     * @param linear {@link TronconDigue} to get bound
     * {@link AbstractPositionDocument} for.
     * @return List of {@link AbstractPositionDocument} bound to the given
     * {@link TronconDigue}.
     */
    public static List<AbstractPositionDocument> getPositionDocumentList(final TronconDigue linear) {
        return getPositionDocumentList(linear.getId());
    }

    /**
     * Return the positionable included, linked or included into linked elements
     * for the linar given as a parameter.
     *
     * @param linear {@link TronconDigue} to get bound {@link Positionable} for.
     * @return a list containing the objets, positions de documents, proprietes,
     * gardes and photos related to the linear.
     */
    public static List<Positionable> getPositionableList(final TronconDigue linear) {
        return getPositionableList(linear.getId());
    }

    /**
     * Return the positionable included, linked or included into linked elements
     * for the linar given as a parameter.
     *
     * @param linearId Id of the linear to filter on.
     * @return a list containing the objets, positions de documents, proprietes,
     * gardes and photos related to the linear.
     */
    public static List<Positionable> getPositionableList(final String linearId) {
        return InjectorCore.getBean(SessionCore.class).getPositionableByLinearId(linearId);
    }

    //==================================================================================================================
    // UTILITAIRES DE FUSION DE TRONÇON
    //==================================================================================================================
    /**
     * On ajoute / copie les propriétés du second tronçon (incluant les
     * structures) dans le premier.
     *
     * TODO : check SR par défaut dans le troncon final.
     *
     * @param mergeResult Le tronçon qui va servir de base à la fusion, qui va
     * être mis à jour.
     * @param mergeParam Le tronçon dont on va prendre les propriétés pour les
     * copier dans le second.
     * @param session La session applicative permettant de mettre à jour les
     * SRs.
     * @return le premier tronçon (mergeResult).
     */
    public static TronconDigue mergeTroncon(TronconDigue mergeResult, TronconDigue mergeParam, SessionCore session) {

        final SystemeReperageRepository srRepo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);

        // on ajoute les bornes. Pas de copie / modification ici, car les bornes
        // sont indépendantes des tronçons.
        final Set<String> borneIds = new HashSet<>();
        borneIds.addAll(mergeResult.getBorneIds());
        borneIds.addAll(mergeParam.getBorneIds());
        mergeResult.setBorneIds(new ArrayList<>(borneIds));

        //on combine les geometries
        final Geometry line1 = mergeResult.getGeometry();
        final Geometry line2 = mergeParam.getGeometry();

        final List<Coordinate> coords = new ArrayList<>();
        coords.addAll(Arrays.asList(line1.getCoordinates()));
        coords.addAll(Arrays.asList(line2.getCoordinates()));

        final LineString serie = GF.createLineString(coords.toArray(new Coordinate[0]));
        serie.setSRID(line1.getSRID());
        serie.setUserData(line1.getUserData());
        mergeResult.setGeometry(serie);

        ((AbstractSIRSRepository<TronconDigue>) session.getRepositoryForClass(mergeResult.getClass())).update(mergeResult);

        /* On fusionne les SR. On cherche les systèmes portant le même nom dans
         * les deux tronçons originaux, puis en fait un seul comportant les bornes
         * des deux. Pour le reste, on fait une simple copie des SR.
         */
        final Map<String, String> modifiedSRs = new HashMap<>();

        final StreamingIterable<SystemeReperage> srs = srRepo.getByLinearStreaming(mergeParam);
        try (final CloseableIterator<SystemeReperage> srIt = srs.iterator()) {
            while (srIt.hasNext()) {
                SystemeReperage sr2 = srIt.next();

                //on cherche le SR du meme nom
                SystemeReperage sibling = null;
                for (SystemeReperage sr1 : srRepo.getByLinearId(mergeResult.getId())) {
                    if (sr1.getLibelle().equals(sr2.getLibelle())) {
                        sibling = sr1;
                        break;
                    }
                }

                if (sibling == null) {
                    //on copie le SR
                    final SystemeReperage srCp = sr2.copy();
                    srCp.setLinearId(mergeResult.getDocumentId());
                    //sauvegarde du sr
                    srRepo.add(srCp, mergeResult);
                    modifiedSRs.put(sr2.getId(), srCp.getId());
                } else {
                    //on merge les bornes
                    final List<SystemeReperageBorne> srbs1 = sibling.getSystemeReperageBornes();
                    final List<SystemeReperageBorne> srbs2 = sr2.getSystemeReperageBornes();

                    loop:
                    for (SystemeReperageBorne srb2 : srbs2) {
                        for (SystemeReperageBorne srb1 : srbs1) {
                            if (srb1.getBorneId().equals(srb2.getBorneId())) {
                                continue loop;
                            }
                        }
                        //cette borne n'existe pas dans l'autre SR, on la copie
                        srbs1.add(srb2.copy());
                    }
                    //maj du sr
                    srRepo.update(sibling, mergeResult);
                    modifiedSRs.put(sr2.getId(), sibling.getId());
                }
            }
        }

        // On ajoute les structures du tronçon paramètre.
        copyElementsOnNewTroncon(mergeResult, mergeParam, session, modifiedSRs);

        return mergeResult;
    }

    /**
     * Copy Positionables linked to the mergeParam troncon onto the mergeResult troncon.
     * Update links between positionable objects with new Ids.
     *
     * @param mergeResult   The troncon used as a base for the fusion. The troncon that is being updated.
     * @param mergeParam    The troncon containing the properties to be copied onto mergeResult.
     * @param session       The applicative session to create the copies of the mergeParam properties.
     * @param modifiedSRs   The SRs to be used for the copies.
     */
    private static void copyElementsOnNewTroncon(TronconDigue mergeResult, TronconDigue mergeParam, SessionCore session, Map<String, String> modifiedSRs) {
        final List<Positionable> toUpdate = new ArrayList<>();
        // Map to store the link between original objets ids and created ones ids.
        final Map<String, String> mapOldandNewIds= new HashMap<>();
        AbstractSIRSRepository<Positionable> repo;
        for (final Positionable objet : getPositionableList(mergeParam)) {
            // Si on a un objet imbriqué dans un document, on le passe. Il sera
            // mis à jour via son parent.
            if (!objet.getId().equals(objet.getDocumentId())) {
                continue;
            }

            final Positionable positionable = objet.copy();
            if (positionable instanceof AvecForeignParent) {
                ((AvecForeignParent) positionable).setForeignParentId(mergeResult.getId());
            }
            if (positionable.getSystemeRepId() != null) {
                positionable.setSystemeRepId(modifiedSRs.get(positionable.getSystemeRepId()));
            }
            // Add to repo to get an id for the newly created positionable.
            repo = (AbstractSIRSRepository<Positionable>) session.getRepositoryForClass(objet.getClass());
            repo.add(positionable);
            // Store the old and new Ids to use later on to update the links between positionables.
            mapOldandNewIds.put(objet.getId(), positionable.getId());
            toUpdate.add(positionable);
        }

        // Update link between positionables.
        updatePositionablesRefs(toUpdate, mapOldandNewIds);

        // We save the changes.
        final List<DocumentOperationResult> failures = session.executeBulk((Collection) toUpdate);
        for (final DocumentOperationResult failure : failures) {
            SirsCore.LOGGER.log(Level.WARNING, "Update failed : ".concat(failure.getError()));
        }
    }

    private static void updatePositionablesRefs(List<Positionable> toUpdate, Map<String, String> mapOldandNewIds) {
        final Map<String, String> updatePosIdsFailures = new HashMap<>();
        toUpdate.forEach(pos -> updatePosIdsFailures.putAll(pos.updatePositionableRefs(mapOldandNewIds)));
        for (final Map.Entry<String, String> idsNotfound : updatePosIdsFailures.entrySet()) {
            SirsCore.LOGGER.log(Level.WARNING, "Positionable id update failed for : ".concat(idsNotfound.getValue()).concat(" with id ").concat(idsNotfound.getKey()));
        }
    }

    /**
     * Creation ou mise a jour du systeme de reperage elementaire .
     *
     * @param troncon Tronçon à analyser.
     * @param session La session applicative en cours.
     */
    public static void updateSRElementaireIfExists(TronconDigue troncon, SessionCore session) {

        final SystemeReperageRepository srRepo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);
        final AbstractSIRSRepository<BorneDigue> bdRepo = session.getRepositoryForClass(BorneDigue.class);

        SystemeReperage sr = null;
        final StreamingIterable<SystemeReperage> srs = srRepo.getByLinearStreaming(troncon);
        try (final CloseableIterator<SystemeReperage> srIt = srs.iterator()) {
            while (srIt.hasNext()) {
                final SystemeReperage csr = srIt.next();
                if (SR_ELEMENTAIRE.equalsIgnoreCase(csr.getLibelle())) {
                    sr = csr;
                    break;
                }
            }
        }

        if (sr != null) {
            updateSRElementaire(troncon, session);
        }
    }

    /**
     * Creation ou mise a jour du systeme de reperage elementaire .
     *
     * @param troncon Tronçon à analyser.
     * @param session La session applicative en cours.
     */
    public static void updateSRElementaire(TronconDigue troncon, SessionCore session) {

        final SystemeReperageRepository srRepo = (SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class);
        final AbstractSIRSRepository<BorneDigue> bdRepo = session.getRepositoryForClass(BorneDigue.class);

        SystemeReperage sr = null;
        final StreamingIterable<SystemeReperage> srs = srRepo.getByLinearStreaming(troncon);
        try (final CloseableIterator<SystemeReperage> srIt = srs.iterator()) {
            while (srIt.hasNext()) {
                final SystemeReperage csr = srIt.next();
                if (SR_ELEMENTAIRE.equalsIgnoreCase(csr.getLibelle())) {
                    sr = csr;
                    break;
                }
            }
        }

        //on le crée s'il n'existe pas
        if (sr == null) {
            sr = srRepo.create();
            sr.setLibelle(SR_ELEMENTAIRE);
            sr.setLinearId(troncon.getDocumentId());
            srRepo.add(sr, troncon);
        }

        // On cherche les bornes de début et de fin du SR élémentaire : on se base pour cela sur les libellés de bornes.
        SystemeReperageBorne srbStart = null;
        SystemeReperageBorne srbEnd = null;

        // On parcours les bornes du SR élémentaire…
        final BorneDigueRepository bdr = (BorneDigueRepository) session.getRepositoryForClass(BorneDigue.class);
        final List<BorneDigue> tronconBornes = bdr.get(troncon.getBorneIds());
        for (final SystemeReperageBorne srb : sr.getSystemeReperageBornes()) {

            // On cherche pour chaque borne du SR élémentaire s'il s'agit de la borne de début ou de la borne de fin.
            for (final BorneDigue bd : tronconBornes) {
                if (bd.getId().equals(srb.getBorneId())) {
                    if (SR_ELEMENTAIRE_START_BORNE.equals(bd.getLibelle())) {
                        srbStart = srb;
                    } else if (SR_ELEMENTAIRE_END_BORNE.equals(bd.getLibelle())) {
                        srbEnd = srb;
                    }
                }
            }
        }

        final BorneDigue bdStart;
        final BorneDigue bdEnd;
        if (srbStart == null) {
            //creation de la borne de début
            bdStart = bdRepo.create();
            bdStart.setLibelle(SR_ELEMENTAIRE_START_BORNE);
            bdRepo.add(bdStart);

            srbStart = session.getElementCreator().createElement(SystemeReperageBorne.class);
            srbStart.setBorneId(bdStart.getDocumentId());

            // On initialise le PR de la borne de départ à 0. lors de sa création uniquement car sa valeur, quand elle
            // est non nulle, sert d'offset aux autres valeurs de PR calculées dans le SR élémentaire.
            srbStart.setValeurPR(0);
            sr.systemeReperageBornes.add(srbStart);
        } else {
            bdStart = bdRepo.get(srbStart.getBorneId());
        }

        if (srbEnd == null) {
            //creation de la borne de fin
            bdEnd = bdRepo.create();
            bdEnd.setLibelle(SR_ELEMENTAIRE_END_BORNE);
            bdRepo.add(bdEnd);

            srbEnd = session.getElementCreator().createElement(SystemeReperageBorne.class);
            srbEnd.setBorneId(bdEnd.getDocumentId());
            sr.systemeReperageBornes.add(srbEnd);
        } else {
            bdEnd = bdRepo.get(srbEnd.getBorneId());
        }

        // Dans tous les cas, on force le PR de la borne de fin à s'aligner sur la valeur du PR de la borne de début
        // augmentée de la longueur du tronçon.
        final float prStart = srbStart.getValeurPR();
        srbEnd.setValeurPR((float) troncon.getGeometry().getLength() + prStart);

        // On réajuste la postion des bornes de début et de fin aux extrémités du tronçon.
        final Coordinate[] coords = troncon.getGeometry().getCoordinates();
        bdStart.setGeometry(GO2Utilities.JTS_FACTORY.createPoint(coords[0]));
        bdEnd.setGeometry(GO2Utilities.JTS_FACTORY.createPoint(coords[coords.length - 1]));

        bdRepo.executeBulk(bdStart, bdEnd);

        // Mise à jour des PRs des autres bornes du SR élémentaire au cas où la géométrie du tronçon aurait changé.
        final SegmentInfo[] buildSegments = buildSegments(asLineString(troncon.getGeometry()));
        for (final SystemeReperageBorne currentSrb : sr.getSystemeReperageBornes()) {
            // Le PR de la borne de début ne doit pas être calculé et celui de la borne de fin a déjà été calculé.
            if (currentSrb != srbStart && currentSrb != srbEnd) {
                // On a besoin de la borne correspondante
                for (final BorneDigue bd : tronconBornes) {
                    if (bd.getId().equals(currentSrb.getBorneId())) {
                        final ProjectedPoint proj = projectReference(buildSegments, bd.getGeometry());
                        currentSrb.setValeurPR((float) proj.distanceAlongLinear + prStart);
                        break;
                    }
                }
            }
        }

        srRepo.update(sr, troncon);
    }

    /**
     * Méthode de recherche du PR de la borne de début du SR élémentaire d'un
     * tronçon.
     *
     * Cette méthode s'appuie sur l'étiquette définie par
     * {@link SirsCore#SR_ELEMENTAIRE_START_BORNE}.
     *
     * @param troncon Le tronçon utilisé pour la recherche des bornes.
     * @param sr Le SR dont on recherche la borne de début, qui doit être un SR
     * élémentaire et relatif au tronçon donné en premier argument.
     * @param session Session utilisée pour la connexion à la base.
     * @return Le PR de la borne de début du SR élémentaire.
     */
    public static float getPRStart(final TronconDigue troncon, final SystemeReperage sr, final SessionCore session) {

        final List<BorneDigue> tronconBornes = session.getRepositoryForClass(BorneDigue.class).get(troncon.getBorneIds());
        for (final SystemeReperageBorne currentSrb : sr.getSystemeReperageBornes()) {
            for (final BorneDigue currentBorne : tronconBornes) {
                if (currentBorne.getId().equals(currentSrb.getBorneId()) && SirsCore.SR_ELEMENTAIRE_START_BORNE.equals(currentBorne.getLibelle())) {
                    return currentSrb.getValeurPR();
                }
            }
        }
        throw new IllegalStateException("Le système de repérage " + sr.getLibelle() + " n'a pas de borne \"" + SirsCore.SR_ELEMENTAIRE_START_BORNE + "\".");
    }

    /**
     * Recalcule des geometries des differents positionnables apres que la
     * géometrie ou que les SR du troncon aient changés.
     *
     * @param troncon {@link TronconDigue} contenant les objets à mettre à jour.
     * @param session La session applicative active.
     */
    public static void updatePositionableGeometry(TronconDigue troncon, SessionCore session) {
        for (Objet obj : getObjetList(troncon)) {
            final LineString structGeom = buildGeometry(
                    troncon.getGeometry(), obj, session.getRepositoryForClass(BorneDigue.class));
            obj.setGeometry(structGeom);
        }
        session.getRepositoryForClass(TronconDigue.class).update(troncon);
    }

    /**
     * A function to switch SR of a given PR. Gives the new PR value in the
     * target SR.
     *
     * @param refLinear An array containing the segments of the reference linear
     * along wich one the distances are computed.
     * @param initialPR The initial PR (expressed in the initial SR).
     * @param initialSR The SR the initial PR is expressed in.
     * @param targetSR The SR the result PR is required expressed in.
     * @param borneRepo The borne repository.
     * @return PR value in chosen {@link SystemeReperage}.
     */
    public static float switchSRForPR(
            final SegmentInfo[] refLinear,
            final double initialPR,
            final SystemeReperage initialSR,
            final SystemeReperage targetSR,
            final AbstractSIRSRepository<BorneDigue> borneRepo) {
        ArgumentChecks.ensureNonNull("Reference linear", refLinear);
        ArgumentChecks.ensureNonNull("Initial SR", initialSR);
        ArgumentChecks.ensureNonNull("Target SR", targetSR);
        ArgumentChecks.ensureNonNull("Database connection", borneRepo);

        // Map des bornes du SR de saisie des PR/Z : la clef contient le PR des bornes dans le SR de saisi. La valeur contient l'id de la borne.
        final Map.Entry<Float, String>[] orderedInitialSRBornes
                = initialSR.systemeReperageBornes.stream().map((SystemeReperageBorne srBorne) -> {
                    return new HashMap.SimpleEntry<>(srBorne.getValeurPR(), srBorne.getBorneId());
                }).sorted((Map.Entry<Float, String> first, Map.Entry<Float, String> second) -> {
                    return Float.compare(first.getKey(), second.getKey());// On trie suivant la valeurs des PR qui est en clef.
                }).toArray((int size) -> {
                    return new Map.Entry[size];
                });

        // On recherche les bornes entre lesquelles se situe le point à convertir, en se basant sur les PRs dans le SR de départ.
        Map.Entry<Float, String> nearestInitialSRBorne = orderedInitialSRBornes[0];
        Map.Entry<Float, String> followingInitialSRBorne = orderedInitialSRBornes[1];
        int borneCnt = 1;
        while (++borneCnt < orderedInitialSRBornes.length && initialPR > orderedInitialSRBornes[borneCnt].getKey()) {
            nearestInitialSRBorne = orderedInitialSRBornes[borneCnt - 1];
            followingInitialSRBorne = orderedInitialSRBornes[borneCnt];
        }

        // Calcul du ratio de distance du point à convertir, entre les deux bornes trouvées.
        final double initialRatio = (initialPR - nearestInitialSRBorne.getKey()) / (followingInitialSRBorne.getKey() - nearestInitialSRBorne.getKey());

        // Récupération des deux bornes.
        final BorneDigue nearestInitialSRBorneDigue = borneRepo.get(nearestInitialSRBorne.getValue());
        final BorneDigue followingInitialSRBorneDigue = borneRepo.get(followingInitialSRBorne.getValue());

        //Distance du point dont le PR est initialPr sur le troncon ?
        //=> distance de la nearestborne sur le troncon :
        final ProjectedPoint nearestInitialSRBorneProj = projectReference(refLinear, nearestInitialSRBorneDigue.getGeometry());
        //=> distance de la secondBorneDigue sur le troncon :
        final ProjectedPoint followingInitialSRBorneProj = projectReference(refLinear, followingInitialSRBorneDigue.getGeometry());

        //=> distance sur le troncon du point dont le PR est initialPR :
        final double distanceOrigineTroncon = nearestInitialSRBorneProj.distanceAlongLinear + (followingInitialSRBorneProj.distanceAlongLinear - nearestInitialSRBorneProj.distanceAlongLinear) * initialRatio;

        // On parcourt les segments pour rechercher celui sur lequel se situe le point et à quelle distance sur ce segment.
        SegmentInfo bonSegment = null;
        double distanceSurLeBonSegment = distanceOrigineTroncon;
        for (final SegmentInfo segmentInfo : refLinear) {
            if (segmentInfo.endDistance > distanceOrigineTroncon) {
                bonSegment = segmentInfo;
                break;
            } else {
                distanceSurLeBonSegment -= segmentInfo.length;
            }
        }

        if (bonSegment != null) {
            // Calcul des coordonnées du point à convertir.
            final Point initialPointPR = GO2Utilities.JTS_FACTORY.createPoint(bonSegment.getPoint(distanceSurLeBonSegment, 0));

            // Conversion des coordonnées du point à convertir vers le SR demandé.
            return computePR(refLinear, targetSR, initialPointPR, borneRepo);
        } else {
            throw new SirsCoreRuntimeException("Unable to compute segment for the given PR and SRs.");
        }
    }

    /**
     * Compute PR value for the point referenced by input linear parameter.
     *
     * @param refLinear Reference linear for bornes positions and relative
     * distances.
     * @param targetSR The system to express output PR into.
     * @param toGetPRFor the point we want to compute a PR for.
     * @param borneRepo Database connection to read {@link BorneDigue} objects
     * referenced in target {@link SystemeReperage}.
     * @return Value of the computed PR, or {@link Float#NaN} if we cannot
     * compute any.
     */
    public static float computePR(final SegmentInfo[] refLinear, final SystemeReperage targetSR, final Point toGetPRFor, final AbstractSIRSRepository<BorneDigue> borneRepo) {
        ArgumentChecks.ensureNonNull("Reference linear", refLinear);
        ArgumentChecks.ensureNonNull("Target SR", targetSR);
        ArgumentChecks.ensureNonNull("Point to compute PR for", toGetPRFor);
        ArgumentChecks.ensureNonNull("Database connection", borneRepo);

        if (targetSR.getSystemeReperageBornes().isEmpty()) {
            return Float.NaN;
        }

        final ProjectedPoint prjPt = projectReference(refLinear, toGetPRFor);

        final TreeMap<Double, SystemeReperageBorne> bornes = new TreeMap<>();
        for (SystemeReperageBorne srb : targetSR.systemeReperageBornes) {
            final BorneDigue borne = borneRepo.get(srb.getBorneId());
            final ProjectedPoint projBorne = projectReference(refLinear, borne.getGeometry());
            bornes.put(projBorne.distanceAlongLinear, srb);
        }

        final Map.Entry<Double, SystemeReperageBorne> under = bornes.floorEntry(prjPt.distanceAlongLinear);
        final Map.Entry<Double, SystemeReperageBorne> above = bornes.ceilingEntry(prjPt.distanceAlongLinear);
        if (under == null && above == null) {
            // Should never occur since it should has already returned NaN because it would mean that the SR has no bornes.
            return Float.NaN;
        }

        if (under == null || above == null) {
            //on doit interpoler avec d'autres bornes plus loin sur le tronçon puisqu'il manque une borne avant ou après le point
            final double delta = 1E-4;
            if (under == null) {
                // pas de bornes avant, on prend les 2 prochaines bornes
                final Map.Entry<Double, SystemeReperageBorne> justAfterAbove = bornes.ceilingEntry(above.getKey() + delta);
                if (justAfterAbove == null) {
                    // Only one borne on this troncon, we can't interpolate there...
                    return Float.NaN;
                }

                final SystemeReperageBorne aboveBorne = above.getValue();
                final SystemeReperageBorne justAfterAboveBorne = justAfterAbove.getValue();
                final double diffPr = justAfterAboveBorne.getValeurPR() - aboveBorne.getValeurPR();
                final double diffDist = justAfterAbove.getKey() - above.getKey();
                final double ratio = diffPr / diffDist;
                final double pr = aboveBorne.getValeurPR() - ratio * (above.getKey() - prjPt.distanceAlongLinear);
                return (float) pr;
            } else {
                // pas de bornes après, on prend les 2 précédentes bornes
                final Map.Entry<Double, SystemeReperageBorne> justBeforeUnder = bornes.floorEntry(under.getKey() - delta);
                if (justBeforeUnder == null) {
                    // Only one borne on this troncon, we can't interpolate there...
                    return Float.NaN;
                }

                final SystemeReperageBorne underBorne = under.getValue();
                final SystemeReperageBorne justBeforeUnderBorne = justBeforeUnder.getValue();
                final double diffPr = underBorne.getValeurPR() - justBeforeUnderBorne.getValeurPR();
                final double diffDist = under.getKey() - justBeforeUnder.getKey();
                final double ratio = diffPr / diffDist;
                final double pr = underBorne.getValeurPR() + ratio * (prjPt.distanceAlongLinear - under.getKey());
                return (float) pr;
            }
        } else {
            if (under.equals(above)) {
                //exactement sur le point.
                return under.getValue().getValeurPR();
            } else {
                //on interpole entre les deux bornes.
                final double distance = prjPt.distanceAlongLinear;
                final SystemeReperageBorne underBorne = under.getValue();
                final SystemeReperageBorne aboveBorne = above.getValue();
                final double diffPr = aboveBorne.getValeurPR() - underBorne.getValeurPR();
                final double diffDist = above.getKey() - under.getKey();
                final double ratio = (distance - under.getKey()) / diffDist;
                final double pr = underBorne.getValeurPR() + ratio * diffPr;
                return (float) pr;
            }
        }
    }

    /**
     * Recherche des bornes amont et aval les plus proches.
     *
     * @param refLinear Segments constituant la polyligne sur laquelle effectuer
     * la recherche.
     * @param targetSR Systeme de repérage cible.
     * @param toGetPRFor Le point pour lequel on veut trouver les bornes amont /
     * aval les plus proches.
     * @param borneRepo Accesseur permettant de récupérer les bornes depuis la
     * base de données.
     * @return [0] distance à la borne en amont comme clé, Borne en amont comme
     * valeur. Peut être nul. [1] distance à la borne en aval comme clé, Borne
     * en aval comme valeur. Peut être nul.
     */
    public static Map.Entry<Double, SystemeReperageBorne>[] findNearest(final SegmentInfo[] refLinear, final SystemeReperage targetSR, final Point toGetPRFor, final AbstractSIRSRepository<BorneDigue> borneRepo) {
        ArgumentChecks.ensureNonNull("Reference linear", refLinear);
        ArgumentChecks.ensureNonNull("Target SR", targetSR);
        ArgumentChecks.ensureNonNull("Point to compute PR for", toGetPRFor);
        ArgumentChecks.ensureNonNull("Database connection", borneRepo);

        final ProjectedPoint prjPt = projectReference(refLinear, toGetPRFor);

        final TreeMap<Double, SystemeReperageBorne> bornes = new TreeMap<>();
        for (SystemeReperageBorne srb : targetSR.systemeReperageBornes) {
            final BorneDigue borne = borneRepo.get(srb.getBorneId());
            final ProjectedPoint projBorne = projectReference(refLinear, borne.getGeometry());
            bornes.put(projBorne.distanceAlongLinear, srb);
        }

        Map.Entry<Double, SystemeReperageBorne> under = bornes.floorEntry(prjPt.distanceAlongLinear);
        Map.Entry<Double, SystemeReperageBorne> above = bornes.ceilingEntry(prjPt.distanceAlongLinear);

        if (under != null) {
            under = new AbstractMap.SimpleImmutableEntry(prjPt.distanceAlongLinear - under.getKey(), under.getValue());
        }
        if (above != null) {
            above = new AbstractMap.SimpleImmutableEntry(above.getKey() - prjPt.distanceAlongLinear, above.getValue());
        }

        return new Map.Entry[]{under, above};
    }

    /**
     * Compute PR values (start and end point) for input {@link Positionable}.
     *
     * @param targetPos The Positionable object to compute PR for.
     * @param session Connection to database, to retrieve SR and bornes.
     */
    public static void computePRs(final Positionable targetPos, final SessionCore session) {
        ArgumentChecks.ensureNonNull("Input position to compute PR for.", targetPos);
        computePRs(new PosInfo(targetPos), session);
    }

    public static void computePRs(final PosInfo targetPos, final SessionCore session) {
        ArgumentChecks.ensureNonNull("Input position to compute PR for.", targetPos);
        ArgumentChecks.ensureNonNull("Database connection.", session);

        /* To be able to compute a PR, we need at least a valid linear position,
         * which implies at least one valid borne and a distance to this borne.
         * The borne must be contained in the current positionable target.
         * We also need the linear on which the object is projected.
         */
        LinearReferencing.SegmentInfo[] linearSegments = targetPos.getTronconSegments(false);
        ArgumentChecks.ensureNonNull("Linear for position projection.", linearSegments);

        final String srid = targetPos.getTroncon().getSystemeRepDefautId();
        ArgumentChecks.ensureNonEmpty("SRID ", srid);

        BorneDigueRepository borneRepo = (BorneDigueRepository) session.getRepositoryForClass(BorneDigue.class);
        SystemeReperage currentSR = session.getRepositoryForClass(SystemeReperage.class).get(srid);

        targetPos.pos.setPrDebut(computePR(linearSegments, currentSR, targetPos.getGeoPointStart(), borneRepo));
        targetPos.pos.setPrFin(computePR(linearSegments, currentSR, targetPos.getGeoPointEnd(), borneRepo));
    }

    /**
     * Calcul de la position avec un systeme de reperage et un PR.
     *
     * @param sr Système de repérage à utiliser pour le calcul.
     * @param pr Valeur de PR pour lequel on veut trouver un point.
     * @return Le point calculé pour le PR donné.
     */
    public static Point computeCoordinate(SystemeReperage sr, double pr) {
        final TronconDigueRepository tronconRepo = InjectorCore.getBean(TronconDigueRepository.class);
        final BorneDigueRepository borneRepo = InjectorCore.getBean(BorneDigueRepository.class);
        final TronconDigue troncon = tronconRepo.get(sr.getLinearId());

        final List<SystemeReperageBorne> srbs = sr.getSystemeReperageBornes();

        //on cherche les bornes les plus proche
        SystemeReperageBorne borneBasse = null;
        SystemeReperageBorne borneHaute = null;
        for (SystemeReperageBorne srb : srbs) {
            if ((borneBasse == null || borneBasse.getValeurPR() < srb.getValeurPR()) && srb.getValeurPR() <= pr) {
                borneBasse = srb;
            }
            if ((borneHaute == null || borneHaute.getValeurPR() > srb.getValeurPR()) && srb.getValeurPR() >= pr) {
                borneHaute = srb;
            }
        }

        if (borneBasse == null) {
            borneBasse = borneHaute;
        }
        if (borneHaute == null) {
            borneHaute = borneBasse;
        }
        if (borneBasse == null) {
            return null;
        }

        final Geometry linear = troncon.getGeometry();
        Point pt;
        if (borneBasse == borneHaute) {
            //une seule borne, on ne peut pas caluler la valeur réel des PR.
            final BorneDigue borne = borneRepo.get(borneBasse.getBorneId());
            pt = LinearReferencingUtilities.computeCoordinate(linear, borne.getGeometry(), 0.0, 0.0);
        } else {
            final BorneDigue borne0 = borneRepo.get(borneBasse.getBorneId());
            final BorneDigue borne1 = borneRepo.get(borneHaute.getBorneId());

            final SegmentInfo[] segments = LinearReferencingUtilities.buildSegments(LinearReferencing.asLineString(linear));

            final LinearReferencing.ProjectedPoint rel0 = LinearReferencingUtilities.projectReference(segments, borne0.getGeometry());
            final LinearReferencing.ProjectedPoint rel1 = LinearReferencingUtilities.projectReference(segments, borne1.getGeometry());

            //on converti le PR en distance le long du lineaire
            final double diffPr = borneHaute.getValeurPR() - borneBasse.getValeurPR();
            final double diffDist = rel1.distanceAlongLinear - rel0.distanceAlongLinear;
            final double ratio = (pr - borneBasse.getValeurPR()) / diffPr;
            final double distance = ratio * diffDist;

            pt = LinearReferencingUtilities.computeCoordinate(linear, borne0.getGeometry(), distance, 0.0);
        }

        pt.setSRID(linear.getSRID());
        pt.setUserData(linear.getUserData());
        return pt;

    }

    /**
     * Calcul de la position avec un systeme de reperage, une borne et une
     * distance.
     *
     * @param sr Système de repérage à utiliser pour le calcul.
     * @param srBorne borne de référence pour le point à calculer.
     * @param distance Distance du point à la borne (négatif si le point est en
     * amont).
     * @return Le point calculé pour les paramètres linéaires donnés.
     */
    public static Point computeCoordinate(SystemeReperage sr, SystemeReperageBorne srBorne, double distance) {
        final TronconDigueRepository tronconRepo = InjectorCore.getBean(TronconDigueRepository.class);
        final BorneDigueRepository borneRepo = InjectorCore.getBean(BorneDigueRepository.class);

        final TronconDigue troncon = tronconRepo.get(sr.getLinearId());

        //une seule borne, on ne peut pas caluler la valeur réel des PR.
        final BorneDigue borne = borneRepo.get(srBorne.getBorneId());
        final Geometry linear = troncon.getGeometry();
        final Point pt = LinearReferencingUtilities.computeCoordinate(linear, borne.getGeometry(), distance, 0.0);

        pt.setSRID(linear.getSRID());
        pt.setUserData(linear.getUserData());
        return pt;
    }

    /**
     * Calcule un point à partir de l'information sur la distance à une borne.
     *
     * @param borneId Identifiant de la borne de référence.
     * @param distance Distance entre la borne et le point à calculer.
     * @param amontAval Vrai si la borne est en aval du point, faux si le point
     * à calculer est en aval de la borne.
     * @param tronconSegments Segments composant le tronçon cible.
     * @param borneRepo Permet de récupérer les bornes depuis la base de
     * données.
     * @return Le point calculé pour les paramètres linéaires donnés.
     */
    public static Point getPointFromBorne(final String borneId, final double distance, final boolean amontAval, final SegmentInfo[] tronconSegments, final AbstractSIRSRepository<BorneDigue> borneRepo) {
        final Point bornePoint = borneRepo.get(borneId).getGeometry();
        double dist = distance;
        if (amontAval) {
            dist *= -1;
        }
        return LinearReferencingUtilities.computeCoordinate(tronconSegments, bornePoint, dist, 0);
    }

    /**
     * Calcule un point à partir d'une géométrie.
     *
     * Si la géométrie n'est pas de type LineString, on commence par la projeter
     * sur linearSegments de manière à obtenir une géométrie de type LineString.
     *
     * Puis on décide du point à retourner : le premier point du premier segment
     * (si on veut le début) ou le second point du dernier segment (si on veut
     * la fin).
     *
     * @param geometry La géométrie pour laquelle on veut extraire un point.
     * @param linearSegments Les segments constituant la polyligne de référence.
     * @param crs La projection à utiliser pour la géométrie de sortie.
     * @param endPoint Vrai si le dernier point de la géométrie doit être
     * retourné, faux si c'est le premier point.
     * @return Le premier ou dernier point de la géométrie en entrée.
     */
    public static Point getPointFromGeometry(Geometry geometry, final SegmentInfo[] linearSegments, final CoordinateReferenceSystem crs, final boolean endPoint) {
        if (!(geometry instanceof LineString) && linearSegments != null) {
            geometry = LinearReferencing.project(linearSegments, geometry);
        }
        final Coordinate[] coords = geometry.getCoordinates();
        // Which point : first or last ?
        final int index = endPoint ? coords.length - 1 : 0;
        final Point point = GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(coords[index]));
        if (crs != null) {
            JTS.setCRS(point, crs);
        }
        return point;
    }

    /**
     * Utility object for manipulation of spatial information of a
     * {@link Positionable} object.
     */
    public static final class PosInfo {

        private final Positionable pos;
        private final SessionCore session;
        private TronconDigue troncon;
        private LineString linear;
        private SegmentInfo[] linearSegments;

        public PosInfo(Positionable pos) {
            this(pos, null);
        }

        public PosInfo(Positionable pos, TronconDigue troncon) {
            this(pos, troncon, null);
        }

        public PosInfo(Positionable pos, Geometry linearGeom, SegmentInfo[] segments) {
            this(pos, null, linearGeom, segments);
        }

        public PosInfo(Positionable pos, TronconDigue troncon, SegmentInfo[] linear) {
            this(pos, troncon, null, linear);
        }

        public PosInfo(Positionable pos, TronconDigue troncon, Geometry geom, SegmentInfo[] segments) {
            ArgumentChecks.ensureNonNull("Input positionable object", pos);
            this.pos = pos;
            this.troncon = troncon;
            this.linear = geom != null ? asLineString(geom) : null;
            this.linearSegments = segments;
            this.session = InjectorCore.getBean(SessionCore.class);
        }

        /**
         *
         * @return Identifier of the {@link TronconDigue} on which this object
         * is placed.
         */
        public String getTronconId() {
            String tdId = null;
            if (troncon == null) {
                if (pos instanceof AvecForeignParent) {
                    tdId = ((AvecForeignParent) pos).getForeignParentId();
                } else {
                    Element tmp = pos.getParent();
                    while (tmp != null) {
                        if (tmp instanceof TronconDigue) {
                            troncon = (TronconDigue) tmp;
                            break;
                        } else if (tmp instanceof AvecForeignParent) {
                            tdId = ((AvecForeignParent) tmp).getForeignParentId();
                            break;
                        }
                        tmp = tmp.getParent();
                    }
                }

                // Last chance, we must try to get it from SR
                if (troncon == null && tdId == null && pos.getSystemeRepId() != null) {
                    SystemeReperage sr = session.getRepositoryForClass(SystemeReperage.class).get(pos.getSystemeRepId());
                    tdId = sr.getLinearId();
                }
            }

            if (troncon != null) {
                return troncon.getId();
            } else {
                return tdId;
            }
        }

        /**
         * Try to retrieve {@link TronconDigue} on which thee current
         * Positionable is defined.
         *
         * @return Troncon of the object, or null if we cannot retrieve it (no
         * valid SR).
         */
        public TronconDigue getTroncon() {
            if (troncon != null) {
                return troncon;
            }

            final String tronconId = getTronconId();
            if (troncon == null && tronconId != null) {
                try {
                    troncon = session.getRepositoryForClass(TronconDigue.class).get(tronconId);
                } catch (Exception e) {
                    troncon = null;
                }
            }

            return troncon;
        }

        /**
         * Return the geometry object associated to the {@link TronconDigue}
         * bound to the positionable.
         *
         * @return geometry of this object {@link TronconDigue}.
         */
        public LineString getTronconLinear() {
            if (linear == null) {
                if (getTroncon() != null) {
                    linear = asLineString(getTroncon().getGeometry());
                }
            }
            return linear;
        }

        public void setTronconLinear(final LineString newLinear) {
            linear = newLinear;
        }

        /**
         * Succession of segments which compose the geometry of the tronçon.
         *
         * @param forceRefresh True if we want to reload tronçon geometry from
         * database, false if we want to get it from cache.
         * @return An ordered list of the segments of current tronçon.
         */
        public SegmentInfo[] getTronconSegments(final boolean forceRefresh) {
            if (linearSegments == null || forceRefresh) {
                final LineString tmpLinear = asLineString(getTronconLinear());
                if (tmpLinear != null) {
                    linearSegments = buildSegments(tmpLinear);
                }
            }
            return linearSegments;
        }

        public void setTronconSegments(final SegmentInfo[] newSegments) {
            linearSegments = newSegments;
        }

        /**
         * Get input Positionable start point in native CRS. If it does not
         * exist, it's computed from linear position of the Positionable.
         *
         * @return A point, never null.
         * @throws IllegalStateException If we cannot get nor compute any point.
         */
        public Point getGeoPointStart() {
            Point point = pos.getPositionDebut();
            //calcul de la position geographique
            if (point == null) {
                if (pos.getBorneDebutId() != null) {
                    //calcule a partir des bornes
                    point = getPointFromBorne(pos.getBorneDebutId(), pos.getBorne_debut_distance(), pos.getBorne_debut_aval());
                } else if (pos.getPositionFin() != null) {
                    point = pos.getPositionFin();
                } else if (pos.getBorneFinId() != null) {
                    point = getPointFromBorne(pos.getBorneFinId(), pos.getBorne_fin_distance(), pos.getBorne_fin_aval());
                } else {
                    //we extract point from the geometry
                    point = getPointFromGeometry(false);
                }
            }
            return point;
        }

        /**
         * Get input Positionable start point, reprojected in given CRS. If it
         * does not exist, it's computed from linear position of the
         * Positionable.
         *
         * @param crs projection to use for output point. If null, database
         * projection is used.
         * @return A point, never null.
         * @throws org.opengis.util.FactoryException If we cannot access
         * Referencing module.
         * @throws IllegalStateException If we cannot get nor compute any point.
         * @throws org.opengis.referencing.operation.TransformException if an
         * error happens during reprojecction.
         */
        public Point getGeoPointStart(CoordinateReferenceSystem crs) throws
                FactoryException, MismatchedDimensionException, TransformException {
            Point point = getGeoPointStart();
            final CoordinateReferenceSystem geomCrs = JTS.findCoordinateReferenceSystem(point);
            if (crs != null && !Utilities.equalsIgnoreMetadata(geomCrs, crs)) {
                final CoordinateOperation trs = CRS.findOperation(geomCrs, crs, null);
                point = (Point) JTS.transform(point, trs.getMathTransform());
            }
            return point;
        }

        /**
         * Calcule un point à partir de l'information sur la distance à une
         * borne.
         *
         * @param borneId identifiant de la borne de réference.
         * @param distance Distance entre la borne et le point à calculer.
         * @param amontAval - Vrai si la borne est en aval du point, faux si le
         * point à calculer est en aval de la borne.
         * @return Le point calculé pour les paramètres linéaires donnés.
         */
        private Point getPointFromBorne(final String borneId, final double distance, final boolean amontAval) {
            return TronconUtils.getPointFromBorne(borneId, distance, amontAval, getTronconSegments(false), session.getRepositoryForClass(BorneDigue.class));
        }

        /**
         * @param endPoint True if we want end point of the geometry, false if
         * we need its start position.
         * @return End point or start point of this positionable geometry.
         */
        private Point getPointFromGeometry(final boolean endPoint) {
            return TronconUtils.getPointFromGeometry(pos.getGeometry(), getTronconSegments(false), session.getProjection(), endPoint);
        }

        /**
         * Get input Positionable end point in native CRS. If it does not exist,
         * it's computed from linear position of the Positionable.
         *
         * @return A point, never null.
         * @throws IllegalStateException If we cannot get nor compute any point.
         */
        public Point getGeoPointEnd() {
            Point point = pos.getPositionFin();
            //calcul de la position geographique
            if (point == null) {
                if (pos.getBorneFinId() != null) {
                    //calcule a partir des bornes
                    point = getPointFromBorne(pos.getBorneFinId(), pos.getBorne_fin_distance(), pos.getBorne_fin_aval());
                } else if (pos.getPositionDebut() != null) {
                    point = pos.getPositionDebut();
                } else if (pos.getBorneDebutId() != null) {
                    point = getPointFromBorne(pos.getBorneDebutId(), pos.getBorne_debut_distance(), pos.getBorne_debut_aval());
                } else {
                    point = getPointFromGeometry(true);
                }
            }
            return point;
        }

        /**
         * Get input Positionable end point, reprojected in given CRS. If it
         * does not exist, it's computed from linear position of the
         * Positionable.
         *
         * @param crs projection to use for output point. If null, database
         * projection is used.
         * @return A point, never null.
         * @throws org.opengis.util.FactoryException If we cannot access
         * Referencing module.
         * @throws IllegalStateException If we cannot get nor compute any point.
         * @throws org.opengis.referencing.operation.TransformException if an
         * error happens during reprojecction.
         */
        public Point getGeoPointEnd(CoordinateReferenceSystem crs) throws
                FactoryException, TransformException {
            Point point = getGeoPointEnd();
            final CoordinateReferenceSystem geomCrs = JTS.findCoordinateReferenceSystem(point);
            if (crs != null && !Utilities.equalsIgnoreMetadata(geomCrs, crs)) {
                final CoordinateOperation trs = CRS.findOperation(geomCrs, crs, null);
                point = (Point) JTS.transform(point, trs.getMathTransform());
            }
            return point;
        }

        public PosSR getForSR() {
            String srid = pos.getSystemeRepId();
            if (srid == null) {
                final TronconDigue troncon = getTroncon();
                //On utilise le SR du troncon
                if (troncon != null) {
                    srid = troncon.getSystemeRepDefautId();
                }
                if (srid == null) {
                    return new PosSR();
                }
                final SystemeReperage sr = session.getRepositoryForClass(SystemeReperage.class).get(srid);
                if (sr == null) {
                    return new PosSR();
                }
                return getForSR(sr);
            } else {
                //valeur deja présente
                final PosSR possr = new PosSR();
                possr.srid = srid;
                possr.borneStartId = pos.getBorneDebutId();
                possr.distanceStartBorne = pos.getBorne_debut_distance();
                possr.startAval = pos.getBorne_debut_aval();

                possr.borneEndId = pos.getBorneFinId();
                possr.distanceEndBorne = pos.getBorne_fin_distance();
                possr.endAval = pos.getBorne_fin_aval();
                return possr;
            }
        }

        public PosSR getForSR(SystemeReperage sr) {
            final Point startPoint = getGeoPointStart();
            final Point endPoint = getGeoPointEnd();

            final PosSR possr = new PosSR();
            possr.srid = sr.getDocumentId();

            // If given SR is empty, we return an empty position.
            if (sr.getSystemeReperageBornes().isEmpty()) {
                return possr;
            }

            final List<BorneDigue> bornes = new ArrayList<>();
            final List<Point> references = new ArrayList<>();
            for (SystemeReperageBorne srb : sr.systemeReperageBornes) {
                final String bid = srb.getBorneId();
                final BorneDigue bd = session.getRepositoryForClass(BorneDigue.class).get(bid);
                if (bd != null) {
                    bornes.add(bd);
                    references.add(bd.getGeometry());
                }
            }

            final Map.Entry<Integer, Double> startRef = computeRelative(getTronconSegments(false), references.toArray(new Point[0]), startPoint);
            final BorneDigue startBorne = bornes.get(startRef.getKey());
            possr.borneDigueStart = startBorne;
            possr.borneStartId = startBorne.getDocumentId();
            possr.startAval = startRef.getValue() < 0;
            possr.distanceStartBorne = Math.abs(startRef.getValue());

            final Map.Entry<Integer, Double> endRef = computeRelative(getTronconSegments(false), references.toArray(new Point[0]), endPoint);
            final BorneDigue endBorne = bornes.get(endRef.getKey());
            possr.borneDigueEnd = endBorne;
            possr.borneEndId = endBorne.getDocumentId();
            possr.endAval = endRef.getValue() < 0;
            possr.distanceEndBorne = Math.abs(endRef.getValue());

            return possr;
        }

        /**
         * @return geometry of the input positionable. If it has no geometry,
         * it's computed and affected to the positionable object. If we cannot
         * deduce a geometry (no linear associated, etc.), a null value is
         * returned.
         */
        public Geometry getGeometry() {
            Geometry geometry = pos.getGeometry();
            if (geometry == null) {
                final LineString tmpLinear = getTronconLinear();
                if (tmpLinear != null) {
                    final SegmentInfo[] segments = getTronconSegments(false);
                    if (segments != null) {
                        geometry = buildGeometry(tmpLinear, segments, pos, session.getRepositoryForClass(BorneDigue.class));
                        pos.setGeometry(geometry);
                    }
                }
            }
            return geometry;
        }

        /**
         * Mise à jour du positionable en attribut à partir de coordonnées
         * linéaires calculées pour un SR donné.
         *
         *
         * @param posSR : instance de la classe PosSR portant les coordonnées
         * linéaires calculées.
         */
        public void setPosSRToPositionable(PosSR posSR) {
            ArgumentChecks.ensureNonNull("Positionable pos", pos);
            ArgumentChecks.ensureNonNull("PosSR posSR", posSR);
            ArgumentChecks.ensureNonEmpty("Système de Représentation posSR.srid", posSR.srid);

            // Affectation des coordonnées linéaires calculées au Positionable :
            
            //Système de représentation :
            if (!posSR.srid.equals(pos.getSystemeRepId())) {
                pos.setSystemeRepId(posSR.srid);
            }

            //point de départ
            pos.setBorneDebutId(posSR.borneStartId);
            pos.setBorne_debut_distance(posSR.distanceStartBorne);
            pos.setBorne_debut_aval(posSR.startAval);

            //point de fin
            pos.setBorneFinId(posSR.borneEndId);
            pos.setBorne_fin_distance(posSR.distanceEndBorne);
            pos.setBorne_fin_aval(posSR.endAval);

        }
    }

    public static final class PosSR {

        public String srid = "";

        public BorneDigue borneDigueStart;
        public String borneStartId = "";
        public double distanceStartBorne;
        public boolean startAval;

        public BorneDigue borneDigueEnd;
        public String borneEndId = "";
        public double distanceEndBorne;
        public boolean endAval;
    }
}
