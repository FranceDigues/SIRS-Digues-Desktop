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
package fr.sirs.util;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AvecForeignParent;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import java.util.function.Function;
import java.util.logging.Level;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.referencing.LinearReferencing;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class ConvertPositionableCoordinates {

    /**
     * Implémentation d'une interface fonctionnelle visant à calculer les
     * coordonnées manquantes de positionables.
     */
    final public static Function<Positionable, Positionable> COMPUTE_MISSING_COORD = positionable -> {

        try {

            if (positionable == null) {
                throw new NullPointerException("Null input positionable.");
            }

            final boolean withLinearCoord = ((positionable.getBorneDebutId() != null))||((positionable.getBorneDebutId() != null));
               
            final boolean withGeoCoord = ((positionable.getPositionDebut() != null)
                    || (positionable.getPositionFin() != null));

            //Si aucun type de coordonnées n'est présent on renvoie une exception
            if ((!withLinearCoord) && (!withGeoCoord)) {
                throw new IllegalArgumentException("The positionable input must provide at least one kind of coordinates 'Linear or geo' but both of them are empty.");
            }
            
            if (withLinearCoord) {
                if (positionable.getBorneDebutId() == null) { //if missing, compute the starting coordinates from the ending ones
                    positionable.setBorneDebutId(positionable.getBorneFinId());
                    positionable.setBorne_debut_aval(positionable.getBorne_fin_aval());
                    positionable.setBorne_debut_distance(positionable.getBorne_fin_distance());
                } else if (positionable.getBorneFinId() == null) { //if missing, compute the ending coordinates from the starting ones
                    positionable.setBorneFinId(positionable.getBorneDebutId());
                    positionable.setBorne_fin_aval(positionable.getBorne_debut_aval());
                    positionable.setBorne_fin_distance(positionable.getBorne_debut_distance());
                }
            }
            if (withGeoCoord) {
                if (positionable.getPositionDebut() == null) {//if missing, compute the starting coordinates from the ending ones
                    positionable.setPositionDebut(positionable.getPositionFin()); //Peut il y avoir une incohérence avec la géométrie?
                } else if (positionable.getPositionFin() == null) { //if missing, compute the starting coordinates from the ending ones
                    positionable.setPositionFin(positionable.getPositionDebut()); //idem?
                }
            }
                // Si les coordonnées sont déjà présentes, aucune modification n'est apportée.
            if ((withLinearCoord) && (withGeoCoord)) {
                
                if (positionable.getPositionFin() == null) {
                    positionable.setPositionFin(positionable.getPositionDebut());

                } else if (positionable.getPositionDebut() == null) {
                    positionable.setPositionDebut(positionable.getPositionFin());
                }
                
                return positionable;

                //Si seules les coordonnées Linéaires sont présentes on essaie de calculer les coordonnées géo    
            } else if (withLinearCoord) {
                computePositionableGeometryAndCoord(positionable);

                //Sinon, on essaie de calculer les coordonnées linéaires à partir des coordonnées géo    
            } else { //withGeoCoord
                computePositionableLinearCoordinate(positionable);
            }

        } catch (RuntimeException e) {
            SirsCore.LOGGER.log(Level.WARNING, "Echec du calcul de coordonnées pour l'élément positionable.", e);
        }

        return positionable;
    };

    /**
     * Méthode permettant de recalculer les coordonnées linéaires (ou
     * Géographiques) lorsqu'une propriété associée aux coordonnées
     * géographiques (respectivement Linéaires) a été modifiée.
     *
     * @param PositionableToUpdate l'élément Positionable qui a (déjà!!) été
     * modifié
     * @param modifiedPropretieName le nom de la propriété modifiée : doit
     * correspondre à une (chaîne de caractère) constante de la classe
     * SirsCore.java (validée par le test unitaire
     * SirsCoreTest.test_Nom_Methodes_Positionable_Valides() )
     */
    public static void computeForModifiedPropertie(Positionable PositionableToUpdate, String modifiedPropretieName) {
        ArgumentChecks.ensureNonNull("Positionable positionable", PositionableToUpdate);
        ArgumentChecks.ensureNonNull("Propertie name modifiedPropertirName", modifiedPropretieName);

        //Si le PR a été modifié on ne permet pas le calcul des coordonnées. Pourra évoluer.
        if ((modifiedPropretieName.equals(SirsCore.PR_DEBUT_FIELD)) || (modifiedPropretieName.equals(SirsCore.PR_FIN_FIELD))) {
            throw new RuntimeException("Impossible de recalculer des coordonnées de position uniquement à partir des PR");
        }

        //Si c'est une coordonnées Géo qui a été modifiée on recalcule les coordonnées linéaires :
        if ((modifiedPropretieName.equals(SirsCore.POSITION_DEBUT_FIELD)) || (modifiedPropretieName.equals(SirsCore.POSITION_FIN_FIELD))) {

            computePositionableLinearCoordinate((Positionable) PositionableToUpdate);

            //Si c'est une coordonnées Linéaire qui a été modifiée on recalcule les coordonnées géo :
        } else if ((modifiedPropretieName.equals(SirsCore.BORNE_DEBUT_AVAL)) || (modifiedPropretieName.equals(SirsCore.BORNE_FIN_AVAL))
                || (modifiedPropretieName.equals(SirsCore.BORNE_DEBUT_DISTANCE)) || (modifiedPropretieName.equals(SirsCore.BORNE_FIN_DISTANCE))
                || (modifiedPropretieName.equals(SirsCore.BORNE_DEBUT_ID)) || (modifiedPropretieName.equals(SirsCore.BORNE_FIN_ID))) {

            computePositionableGeometryAndCoord((Positionable) PositionableToUpdate);

        }
    }

//    //===============================
//    //  Compute Geo from Linear.
//    //===============================
    /**
     * Compute current positionable point using linear referencing information
     * defined in the form. Returned point is expressed with Database CRS.
     *
     * Méthode extraite de FXPositionableAbstractLinearMode.java
     *
     * @param distance
     * @param borneProperty
     * @param amont
     * @param positionable
     * @return The point computed from starting borne. If we cannot, we return
     * null.
     */
    public static Point computeGeoFromLinear(final Number distance,
            final BorneDigue borneProperty, final boolean amont, final Positionable positionable) {

//        final Positionable positionable = posProperty.get();
        final TronconDigue t = getTronconFromPositionable(positionable);

        if (distance != null && borneProperty != null && t != null) {
            //calcul à partir des bornes
            final Point bornePoint = borneProperty.getGeometry();
            double dist = distance.doubleValue();
            if (amont) {
                dist *= -1;
            }
            return LinearReferencingUtilities.computeCoordinate(t.getGeometry(), bornePoint, dist, 0);
        } else {
            return null;
        }
    }

    /**
     * Calcule de la géométrie et des coordonnées d'un positionable à partir de
     * ses coordonnées linéaires.
     *
     * @param positionableWithLinearCoord
     */
    public static void computePositionableGeometryAndCoord(Positionable positionableWithLinearCoord) {
        ArgumentChecks.ensureNonNull("Borne de début du Positionable", positionableWithLinearCoord.getBorneDebutId());
//        ArgumentChecks.ensureNonNull("Borne de fin du Positionable", positionableWithLinearCoord.getBorneFinId());
        ArgumentChecks.ensureNonNull("Distance borne début du Positionable", positionableWithLinearCoord.getBorne_debut_distance());
//        ArgumentChecks.ensureNonNull("Distance borne fin du Positionable", positionableWithLinearCoord.getBorne_fin_distance());

        try {

            final TronconDigue troncon = getTronconFromPositionable(positionableWithLinearCoord);
            final AbstractSIRSRepository<BorneDigue> borneRepo = InjectorCore.getBean(SessionCore.class).getRepositoryForClass(BorneDigue.class);
            final LineString geometry = LinearReferencingUtilities.buildGeometryFromBorne(troncon.getGeometry(), positionableWithLinearCoord, borneRepo);

            //sauvegarde de la geometrie
            positionableWithLinearCoord.geometryProperty().set(geometry);

            // Affectation des coordonnées calculées.
            positionableWithLinearCoord.setPositionDebut(geometry.getStartPoint());
            positionableWithLinearCoord.setPositionFin(geometry.getEndPoint());

            // On indique que les coordonnées Géographique du Positionable n'ont pas été éditées.
            positionableWithLinearCoord.setEditedGeoCoordinate(Boolean.FALSE);

        } catch (RuntimeException re) {
            SirsCore.LOGGER.log(Level.WARNING, "Echec du calcul de géométrie depuis les coordonnées linéaires du positionable :\n"
                    + positionableWithLinearCoord.getDesignation(), re);

        }

    }

    //===============================
    //  Compute Linear from Geo
    //===============================
    //--------------------------------------------------------------------------
    /**
     * Calcul des coordonnées linéaires et mise à jour d'un Positionable à
     * partir de ses coordonnées Géométriques.
     *
     * @param sr : Systeme de repérage utilisé comme référence pour les
     * coordonnées linéaires.
     * @param positionableWithGeo : le Positionable pour lequel les coordonnées
     * linéaires seront mises à jour. Les attributs startPoint et endPoint de ce
     * Positionable doivent être renseignés.
     */
    public static void computePositionableLinearCoordinate(final SystemeReperage sr, final Positionable positionableWithGeo) {
        ArgumentChecks.ensureNonNull("Système de repérage", sr);
        ArgumentChecks.ensureNonNull("Positionable", positionableWithGeo);

        try {

            //Vérification que le Positionable dispose bien d'une géométrie
            final Point startPoint = positionableWithGeo.getPositionDebut();
            final Point endPoint = positionableWithGeo.getPositionFin();

            if (startPoint == null || endPoint == null) {
                throw new IllegalArgumentException("The attributes 'positionDebut' and 'positionFin' of the method's Positionable input must be provided to compute the linear coordinates");
            }

            //Initialisation
            final AbstractSIRSRepository<BorneDigue> borneRepo = InjectorCore.getBean(SessionCore.class).getRepositoryForClass(BorneDigue.class);
            final TronconDigue tronconFromPositionable = getTronconFromPositionable(positionableWithGeo);
            final LinearReferencing.SegmentInfo[] segments = getSourceLinear(sr, positionableWithGeo);
            final TronconUtils.PosInfo posInfo = new TronconUtils.PosInfo(positionableWithGeo, tronconFromPositionable, segments);

            if ((!startPoint.equals(posInfo.getGeoPointStart())) || (!endPoint.equals(posInfo.getGeoPointEnd()))) {
                throw new AssertionError("The same starting and ending points must be found for the Positionable positionableWithGeo.");
            }

            //Calcule des coordonnées linéaires
            final TronconUtils.PosSR posSR = posInfo.getForSR(sr);
            //Mise à jour du positionable avec ces coordonnées et l'identifiant du Système de représentation
            posInfo.setPosSRToPositionable(posSR);

            //Calcule des PR (Position Relative) dans le Système de représentation :
            float computedPR = TronconUtils.computePR(segments, sr, startPoint, borneRepo);
            positionableWithGeo.setPrDebut(computedPR);
            // Calcul duPr du point de fin du Positionable si différent du points de départ.
            if (!startPoint.equals(endPoint)) {
                computedPR = TronconUtils.computePR(getSourceLinear(sr, positionableWithGeo), sr, endPoint, borneRepo);
            }
            positionableWithGeo.setPrFin(computedPR);

            // On indique que les coordonnées Géographique du Positionable ont été éditées.
            if ((positionableWithGeo.getEditedGeoCoordinate() == null) || (!positionableWithGeo.getEditedGeoCoordinate())) {
                positionableWithGeo.setEditedGeoCoordinate(Boolean.TRUE);
            }
        } catch (RuntimeException re) {
            SirsCore.LOGGER.log(Level.WARNING, "Echec du calcul de coordonnées linéaires pour le positionable :\n "
                    + positionableWithGeo.getDesignation() + "\n Dans le Système de représentation :\n" + sr.getLibelle(), re);

        }

    }

    /**
     * /**
     * Calcul des coordonnées linéaires et mise à jour d'un Positionable à
     * partir de ses coordonnées Géométriques MAIS en cherchant un Système de
     * représentation par défaut pour le positionable donné.
     *
     * @param positionableWithGeo
     */
    public static void computePositionableLinearCoordinate(final Positionable positionableWithGeo) {
        ArgumentChecks.ensureNonNull("Positionable", positionableWithGeo);

        final SystemeReperage sr = ConvertPositionableCoordinates.getDefaultSRforPositionable(positionableWithGeo);

        if (sr == null) {
            throw new NullPointerException("Impossible d'identifier un Système de représentation par défaut pour le positionable : " + positionableWithGeo.getDesignation());
        }

        computePositionableLinearCoordinate(sr, positionableWithGeo);
    }

    /**
     * Méthode permettant de chercher un Système de représentation (SR) pour un
     * élément 'Positionable' donné.
     *
     *
     *
     * @param positionable
     * @return SystemeReperage sr : Système de repérage associé à l'attribut
     * SystemeRepId du positionable ou s'il n'est pas donné, le SR par défaut du
     * tronçon sur lequel est placé le positionable.
     *
     * Throws RuntimeException si aucun SR n'a été trouvé.
     */
    public static SystemeReperage getDefaultSRforPositionable(final Positionable positionable) {
        ArgumentChecks.ensureNonNull("Positionable positionable", positionable);

        final SystemeReperage sr;

        //On cherche le Système de repérage dans lequel calculer les coordonnées.
        if (positionable.getSystemeRepId() != null) {
            sr = InjectorCore.getBean(SessionCore.class).getRepositoryForClass(SystemeReperage.class).get(positionable.getSystemeRepId());
        } else {
            //Si le positionable n'a pas de SR renseigné, on prend celui par défaut du tronçon.
            final TronconDigue troncon = getTronconFromPositionable(positionable);
            sr = InjectorCore.getBean(SessionCore.class).getRepositoryForClass(SystemeReperage.class).get(troncon.getSystemeRepDefautId());
        }

        if (sr == null) {
            // On signale par une RuntimeException que le Système de représentation n'a pas été trouvé.
            throw new RuntimeException("Système de repérage non trouvé pour le positionable : " + positionable.getDesignation());
        }

        return sr;

    }

    public static LinearReferencing.SegmentInfo[] getSourceLinear(final SystemeReperage source, final Positionable positionable) {
        final TronconDigue t = getTronconFromPositionable(positionable);
        return LinearReferencingUtilities.getSourceLinear(t, source);
    }
    
//==============================================================================
    /**
     * Méthodes permettant de retrouver le tronçon sur lequel se trouve un
     * élément (input).
     *
     * Peut retourner un null si la recherche échoue.
     * 
     * Initialement dans le module desktop : fr.sirs.theme.ui.FXPositionableMode.java
     * cette méthode a été ramenée dans le module core.
     * 
     * Cette méthode est comparable à la méthode getTroncon() de la classe PosInfo dans
     * fr.sirs.core.TronconUtils.java ; Il faudrait à terme les fusionner, par 
     * exemple en permettant à la méthode getTroncon() de prendre en compte les 
     * Berge ou autre dans la recherche des éléments parent :
     * getRepositoryForClass(TronconDigue.class) -> getRepositories... + stream
     *
     * @param element
     * @return
     */
    /**
     * Search recursively the troncon of the positionable.
     *
     * @param pos Positionable object to find parent linear.
     * @return Found linear object, or null if we cannot deduce it from input.
     */
    public static TronconDigue getTronconFromPositionable(final Positionable pos) {
        final Element currentElement = getTronconFromElement(pos);
        if (currentElement instanceof TronconDigue) {
            return (TronconDigue) currentElement;
        } else {
            return null;
        }
    }
    
    
    public static Element getTronconFromElement(final Element element) {
        Element candidate = null;

        // Si on arrive sur un Troncon, on renvoie le troncon.
        if (element instanceof TronconDigue) {
            candidate = element;
        } // Sinon on cherche un troncon dans les parents
        else {
            // On privilégie le chemin AvecForeignParent
            if (element instanceof AvecForeignParent) {
                String id = ((AvecForeignParent) element).getForeignParentId();
                final SessionCore session = InjectorCore.getBean(SessionCore.class);
                final Preview preview = session.getPreviews().get(id);
                if (preview == null) {
                    return null;
                }

                final AbstractSIRSRepository repo = InjectorCore.getBean(SessionCore.class).getRepositoryForType(preview.getElementClass());
                candidate = getTronconFromElement((Element) repo.get(id));
            }
            // Si on n'a pas (ou pas trouvé) de troncon via la référence ForeignParent on cherche via le conteneur
            if (candidate == null && element.getParent() != null) {
                candidate = getTronconFromElement(element.getParent());
            }
        }
        return candidate;
    }

    //=====================================================================
    //Méthodes initiales : substituée par le recours à la classe TronçonUtils
    //===================================================================== 
//    public static void computePositionableLinearCoordinate(final SystemeReperage sr, final Positionable positionableWithGeo) {
//        ArgumentChecks.ensureNonNull("Système de repérage", sr);
//        ArgumentChecks.ensureNonNull("Positionable", positionableWithGeo);
//
//        final Point startPoint = positionableWithGeo.getPositionDebut();
//        final Point endPoint = positionableWithGeo.getPositionFin();
//
//        if (startPoint == null || endPoint == null) {
//            throw new IllegalArgumentException("The attributes 'positionDebut' and 'positionFin' of the method's Positionable input must be provided to compute the linear coordinates");
//        }
//
//        final AbstractSIRSRepository<BorneDigue> borneRepo = Injector.getSession().getRepositoryForClass(BorneDigue.class);
//
//        // Calcul des coordonnées linéaires du début du positionnable
//        final LinearReferencing.SegmentInfo[] segments = getSourceLinear(sr, positionableWithGeo);
//        Map.Entry<BorneDigue, Double> computedLinear = computeLinearFromGeo(segments, sr, startPoint);
//        boolean aval = true;
//        double distanceBorne = computedLinear.getValue();
//        if (distanceBorne < 0) {
//            distanceBorne = -distanceBorne;
//            aval = false;
//        }
//        float computedPR = TronconUtils.computePR(segments, sr, startPoint, borneRepo);
//
//        // Affectation des coordonnées linéaires calculées au Positionable :
//        positionableWithGeo.setBorneDebutId(computedLinear.getKey().getId());
//        positionableWithGeo.setBorne_debut_distance(distanceBorne);
//        positionableWithGeo.setBorne_debut_aval(!aval);  // '!' si on parle de la borne.
//        positionableWithGeo.setPrDebut(computedPR);
//
//        // Calcul des coordonnées linéaires du point de fin du positionnable si différent du points de départ.
//        if (!startPoint.equals(endPoint)) {
//            computedLinear = computeLinearFromGeo(segments, sr, endPoint);
//            aval = true;
//            distanceBorne = computedLinear.getValue();
//            if (distanceBorne < 0) {
//                distanceBorne = -distanceBorne;
//                aval = false;
//            }
//            computedPR = TronconUtils.computePR(getSourceLinear(sr, positionableWithGeo), sr, endPoint, borneRepo);
//        }
//
//        // Affectation des coordonnées linéaires calculées au Positionable :
//        positionableWithGeo.setBorneFinId(computedLinear.getKey().getId());
//        positionableWithGeo.setBorne_fin_distance(distanceBorne);
//        positionableWithGeo.setBorne_fin_aval(!aval);  // '!' si on parle de la borne.
//        positionableWithGeo.setPrFin(computedPR);
//
//        // Mise à jour de l'identifiant du Système de représentation;
//        positionableWithGeo.setSystemeRepId(sr.getId());
//
//        // On indique que les coordonnées Géographique du Positionable ont été éditées.
//        if ((positionableWithGeo.getEditedGeoCoordinate() == null) || (!positionableWithGeo.getEditedGeoCoordinate())) {
//            positionableWithGeo.setEditedGeoCoordinate(Boolean.TRUE);
//        }
//
//    }
//    
//    /**
//     * Compute a linear position for the edited {@link Positionable} using
//     * defined geographic position.
//     *
//     * Méthode extraite de FXPositionableMode.java
//     *
//     * Semble redondant avec final TronconUtils.PosInfo ps = new
//     * TronconUtils.PosInfo(pos, t); final TronconUtils.PosSR rp =
//     * ps.getForSR(defaultSR);
//     *
//     * @param segments
//     * @param sr The SR to use to generate linear position.
//     * @param geoPoint
//     * @return The borne to use as start point, and the distance from the borne
//     * until the input geographic position. It's negative if we go from downhill
//     * to uphill.
//     *
//     * @throws RuntimeException If the computing fails.
//     */
//    public static Map.Entry<BorneDigue, Double> computeLinearFromGeo(
//            final LinearReferencing.SegmentInfo[] segments, final SystemeReperage sr, final Point geoPoint) {
//        ArgumentChecks.ensureNonNull("Geographic point", geoPoint);
//
//        if (segments == null) {
//            throw new IllegalStateException("No computing can be done without a source linear object.");
//        }
//
//        // Get list of bornes which can be possibly used.
//        final HashMap<Point, BorneDigue> availableBornes = getAvailableBornes(sr);
//        final Point[] arrayGeom = availableBornes.keySet().toArray(new Point[0]);
//
//        // Get nearest borne from our start geographic point.
//        final Map.Entry<Integer, Double> computedRelative = LinearReferencingUtilities.computeRelative(segments, arrayGeom, geoPoint);
//        final int borneIndex = computedRelative.getKey();
//        if (borneIndex < 0 || borneIndex >= availableBornes.size()) {
//            throw new RuntimeException("Computing failed : no valid borne found.");
//        }
//        final double foundDistance = computedRelative.getValue();
//        if (Double.isNaN(foundDistance) || Double.isInfinite(foundDistance)) {
//            throw new RuntimeException("Computing failed : no valid distance found.");
//        }
//        return new AbstractMap.SimpleEntry<>(availableBornes.get(arrayGeom[borneIndex]), foundDistance);
//    }
//
//    /**
//     * Return valid bornes defined by the input {@link SystemeReperage} PRs
//     * ({@link SystemeReperageBorne}). Only bornes containing a geometry are
//     * returned.
//     *
//     * Méthode extraite de FXPositionableMode.java
//     *
//     * @param source The SR to extract bornes from.
//     * @return A map, whose values are found bornes, and keys are their
//     * associated geometry. Never null, but can be empty.
//     */
//    public static HashMap<Point, BorneDigue> getAvailableBornes(final SystemeReperage source) {
//        ArgumentChecks.ensureNonNull("Système de repérage source", source);
//        final AbstractSIRSRepository<BorneDigue> borneRepo = Injector.getSession().getRepositoryForClass(BorneDigue.class);
//        final HashMap<Point, BorneDigue> availableBornes = new HashMap<>(source.systemeReperageBornes.size());
//        for (final SystemeReperageBorne pr : source.systemeReperageBornes) {
//            if (pr.getBorneId() != null) {
//                final BorneDigue borne = borneRepo.get(pr.getBorneId());
//                if (borne != null && borne.getGeometry() != null) {
//                    availableBornes.put(borne.getGeometry(), borne);
//                }
//            }
//        }
//        return availableBornes;
//    }
}
