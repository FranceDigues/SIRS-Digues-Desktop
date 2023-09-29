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
package fr.sirs.util;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.*;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.*;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.referencing.LinearReferencing;

import java.util.function.Predicate;
import java.util.logging.Level;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class ConvertPositionableCoordinates {

    /**
     * Implémentation d'une interface fonctionnelle visant à calculer les
     * coordonnées manquantes de positionables.
     * Renvoie un boolean indiquant si des coordonnées ont été mises à jours.
     */
    final public static Predicate<Positionable> COMPUTE_MISSING_COORD = ConvertPositionableCoordinates::ensureCoordinates;

    private static boolean ensureCoordinates(final Positionable positionable) {
        try {
            if (positionable == null) {
                throw new NullPointerException("Null input positionable.");
            }

            boolean withLinearCoord = checkLinearCoordAndAffectMissing(positionable);
            boolean withGeoCoord = checkGeomCoordAndAffectMissing(positionable);

            //Si aucun type de coordonnées n'est présent on renvoie une exception
            if ((!withLinearCoord) && (!withGeoCoord)) {
                SirsCore.LOGGER.log(Level.FINE, "Missing coordinates computation failed : the positionable input must provide at least one kind of coordinates 'Linear or geo' but both of them are empty.");
                return false;
            }

            // Si les coordonnées sont déjà présentes, aucune modification n'est apportée.
            if ((withLinearCoord) && (withGeoCoord)) {

                if (positionable.getGeometry() == null) {
                    if (positionable.getEditedGeoCoordinate()) {
                        withLinearCoord = false;
                    } else {
                        withGeoCoord = false;
                    }
                } else {
                    if(isZeroPRs(positionable)) {
                        TronconUtils.computePRs(positionable, InjectorCore.getBean(SessionCore.class));
                        return !isZeroPRs(positionable); //Les PR sont toujours à 0.f et n'ont pas changé
                    }

                    // REDMINE-4559
                    // if linear is from aval to amont, the PRs, coord and linear infos are set from amont to aval,
                    // the geometry is recomputed
                    // and the positionable must be saved
                    return LinearReferencingUtilities.ensureAvalToAmont(positionable);
                }
            }

            //Si seules les coordonnées Linéaires sont présentes on essaie de calculer les coordonnées géo
            if (withLinearCoord) {
                return computePositionableGeometryAndCoord(positionable);

            //Sinon, on essaie de calculer les coordonnées linéaires à partir des coordonnées géo
            } else { //withGeoCoord
                return computePositionableLinearCoordinate(positionable);
            }

        } catch (RuntimeException e) {
            SirsCore.LOGGER.log(Level.WARNING, "Echec du calcul de coordonnées pour l'élément positionable.", e);
        }
        return false;
    }


    /**
     * moved from {@link AbstractSIRSRepository}
     * @param target : positionable to update
     */
    public static void updateGeometryAndPRs(final Positionable target) {
        try {
            final TronconUtils.PosInfo posInfo = new TronconUtils.PosInfo(target);
            if (posInfo.getTroncon() != null) {
                if (posInfo.getGeometry() != null) {
                    // Try computing PRs on default SR
                    TronconUtils.computePRs(posInfo, InjectorCore.getBean(SessionCore.class));
                }
            }
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.WARNING, "Cannot update geometry for newly loaded positionable object.", e);
        }
    }

    /**
     * Tests si les PRs de début et de fin d'un {@link Positionable} NON NULL
     * sont tous 2 égaux à 0.f.
     *
     * @param positionable NON NULL; La nullité ne sera pas testée ici.
     */
    private static boolean isZeroPRs(final Positionable positionable) {
        return (positionable.getPrDebut() == 0.f) && (positionable.getPrFin() == 0.f);
    }

    /**
     * Check if the input Positionable has got linear Coordinates ;
     * If is has only the starting (respectively ending) point  coordinates,
     * the methods affects the own coordinates to the missing ending (resp starting)
     * point.
     *
     * @return true if the input positionable has linear coordinates.
     */
    private static boolean checkLinearCoordAndAffectMissing(Positionable positionable){
        if(((positionable.getBorneDebutId() == null))&&((positionable.getBorneFinId() == null))){
            return false;
        }

        if (positionable.getBorneDebutId() == null) { //if missing, compute the starting coordinates from the ending ones
            positionable.setBorneDebutId(positionable.getBorneFinId());
            positionable.setBorne_debut_aval(positionable.getBorne_fin_aval());
            positionable.setBorne_debut_distance(positionable.getBorne_fin_distance());
        } else if (positionable.getBorneFinId() == null) { //if missing, compute the ending coordinates from the starting ones
            positionable.setBorneFinId(positionable.getBorneDebutId());
            positionable.setBorne_fin_aval(positionable.getBorne_debut_aval());
            positionable.setBorne_fin_distance(positionable.getBorne_debut_distance());
        }
        return true;
    }

    /**
     * Check if the input Positionable has got Geographical Coordinates ;
     * If is has only the starting (respectively ending) point coordinates,
     * the methods affects the own coordinates to the missing ending (resp starting)
     * point.
     *
     */
    private static boolean checkGeomCoordAndAffectMissing(Positionable positionable){
        if(((positionable.getPositionDebut() == null)
                    && (positionable.getPositionFin() == null))){
            return false;
        }

        if (positionable.getPositionDebut() == null) {//if missing, compute the starting coordinates from the ending ones
            positionable.setPositionDebut(positionable.getPositionFin()); //Peut il y avoir une incohérence avec la géométrie?
        } else if (positionable.getPositionFin() == null) { //if missing, compute the starting coordinates from the ending ones
            positionable.setPositionFin(positionable.getPositionDebut()); //idem?
        }
        return true;
    }

    /**
     * Méthode permettant de recalculer les coordonnées linéaires (ou
     * Géographiques) lorsqu'une propriété associée aux coordonnées
     * géographiques (respectivement Linéaires) a été modifiée.
     *
     * @param positionableToUpdate l'élément Positionable qui a (déjà!!) été
     * modifié
     * @param modifiedPropretieName le nom de la propriété modifiée : doit
     * correspondre à une (chaîne de caractère) constante de la classe
     * SirsCore.java (validée par le test unitaire
     * SirsCoreTest.test_Nom_Methodes_Positionable_Valides() )
     */
    public static void computeForModifiedPropertie(Positionable positionableToUpdate, String modifiedPropretieName) {
        ArgumentChecks.ensureNonNull("Positionable positionable", positionableToUpdate);
        ArgumentChecks.ensureNonNull("Propertie name modifiedPropertirName", modifiedPropretieName);

        //Si le PR a été modifié on ne permet pas le calcul des coordonnées. Pourra évoluer.
        if ((modifiedPropretieName.equals(SirsCore.PR_DEBUT_FIELD)) || (modifiedPropretieName.equals(SirsCore.PR_FIN_FIELD))) {
            throw new RuntimeException("Impossible de recalculer des coordonnées de position uniquement à partir des PR");
        }

        //Si c'est une coordonnées Géo qui a été modifiée on recalcule les coordonnées linéaires :
        if ((modifiedPropretieName.equals(SirsCore.POSITION_DEBUT_FIELD)) || (modifiedPropretieName.equals(SirsCore.POSITION_FIN_FIELD))) {

            computePositionableLinearCoordinate(positionableToUpdate);

        //Si c'est une coordonnées Linéaire qui a été modifiée on recalcule les coordonnées géo :
        } else if ((modifiedPropretieName.equals(SirsCore.BORNE_DEBUT_AVAL)) || (modifiedPropretieName.equals(SirsCore.BORNE_FIN_AVAL))
                || (modifiedPropretieName.equals(SirsCore.BORNE_DEBUT_DISTANCE)) || (modifiedPropretieName.equals(SirsCore.BORNE_FIN_DISTANCE))
                || (modifiedPropretieName.equals(SirsCore.BORNE_DEBUT_ID)) || (modifiedPropretieName.equals(SirsCore.BORNE_FIN_ID))) {

            computePositionableGeometryAndCoord(positionableToUpdate);

        }
    }

//    //===============================
//    //  Compute Geo from Linear.
//    //===============================

    /**
     * Calcule de la géométrie et des coordonnées d'un positionable à partir de
     * ses coordonnées linéaires.
     *
     * @return boolean indiquant si des coordonnées ont été mises à jours
     */
    public static boolean computePositionableGeometryAndCoord(final Positionable positionableWithLinearCoord) {
        if(!checkLinearCoordAndAffectMissing(positionableWithLinearCoord)){
            throw new IllegalStateException("Try to compute Geometry and Coordinate from positionable without linear coordinates");
        }

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

            TronconUtils.computePRs(positionableWithLinearCoord,  InjectorCore.getBean(SessionCore.class));

//            pr pas mis à jours
            return true;

        } catch (RuntimeException re) {
            SirsCore.LOGGER.log(Level.WARNING, "Echec du calcul de géométrie depuis les coordonnées linéaires du positionable :\n"
                    + ((positionableWithLinearCoord==null)?"positionableWithLinearCoord null":positionableWithLinearCoord.getDesignation()), re);

        }
        return false;

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
     * @param forceRecomputeGeometry hack to avoid recomputing geometry for polygons of ZoneVegetation
     * @return boolean indiquant si des coordonnées ont été mises à jours
     */
    public static boolean computePositionableLinearCoordinate(final SystemeReperage sr, final Positionable positionableWithGeo, final boolean forceRecomputeGeometry) {
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
            final TronconDigue tronconFromPositionable = getTronconFromPositionable(positionableWithGeo);
            final LinearReferencing.SegmentInfo[] segments = getSourceLinear(sr, positionableWithGeo);
            if (forceRecomputeGeometry && tronconFromPositionable != null) {
                positionableWithGeo.setGeometry(null); // to recompute it
            }
            final TronconUtils.PosInfo posInfo = new TronconUtils.PosInfo(positionableWithGeo, tronconFromPositionable, segments);

            if ((!startPoint.equals(posInfo.getGeoPointStart())) || (!endPoint.equals(posInfo.getGeoPointEnd()))) {
                throw new AssertionError("The same starting and ending points must be found for the Positionable positionableWithGeo.");
            }

            //Calcule des coordonnées linéaires
            final TronconUtils.PosSR posSR = posInfo.getForSR(sr);
            //Mise à jour du positionable avec ces coordonnées et l'identifiant du Système de représentation
            posInfo.setPosSRToPositionable(posSR);

            //Update geometry and PR
            if (posInfo.getTroncon() != null) {
                if (posInfo.getGeometry() != null) { //getGeometry try to recompute the geometry if missing.
                    //Calcule des PR (Position Relative) dans le Système de représentation :
                    TronconUtils.computePRs(posInfo, InjectorCore.getBean(SessionCore.class));
                }
            }

            // On indique que les coordonnées Géographique du Positionable ont été éditées.
            if ((positionableWithGeo.getEditedGeoCoordinate() == null) || (!positionableWithGeo.getEditedGeoCoordinate())) {
                positionableWithGeo.setEditedGeoCoordinate(Boolean.TRUE);
            }
            return true;

        } catch (RuntimeException re) {
            SirsCore.LOGGER.log(Level.WARNING, "Echec du calcul de coordonnées linéaires pour le positionable :\n "
                    + positionableWithGeo.getDesignation() + "\n Dans le Système de représentation :\n" + sr.getLibelle(), re);

        }
        return false;

    }

    /**
     * Calcul des coordonnées linéaires et mise à jour d'un Positionable à
     * partir de ses coordonnées Géométriques MAIS en cherchant un Système de
     * représentation par défaut pour le positionable donné.
     *
     * @param positionableWithGeo
     * @param forceRecomputeGeometry hack to avoid recomputing geometry for polygons of ZoneVegetation
     * @return boolean indiquant si des coordonnées ont été mises à jours
     */
    public static boolean computePositionableLinearCoordinate(final Positionable positionableWithGeo, final boolean forceRecomputeGeometry) {
        ArgumentChecks.ensureNonNull("Positionable", positionableWithGeo);

        final SystemeReperage sr = ConvertPositionableCoordinates.getDefaultSRforPositionable(positionableWithGeo);

        if (sr == null) {
            throw new NullPointerException("Impossible d'identifier un Système de représentation par défaut pour le positionable : " + positionableWithGeo.getDesignation());
        }

        return computePositionableLinearCoordinate(sr, positionableWithGeo, forceRecomputeGeometry);
    }

    /**
     * Calcul des coordonnées linéaires et mise à jour d'un Positionable à
     * partir de ses coordonnées Géométriques MAIS en cherchant un Système de
     * représentation par défaut pour le positionable donné.
     *
     * @param positionableWithGeo
     * @return boolean indiquant si des coordonnées ont été mises à jours
     */
    public static boolean computePositionableLinearCoordinate(final Positionable positionableWithGeo) {
        return computePositionableLinearCoordinate(positionableWithGeo, true);
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
     *
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
        ArgumentChecks.ensureNonNull("element", element);
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
                final Preview preview;
                try {
                    preview = session.getPreviews().get(id);
                }catch (Exception e) {
                    SirsCore.LOGGER.log(Level.WARNING, "Pas de tron\u00e7on ou \u00e9chec de l''identification du tron\u00e7on pour l''''\u00e9l\u00e9ment{0} : {1}", new Object[]{element.getId(), e.getMessage()});
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
}
