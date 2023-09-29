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
package fr.sirs.core;

import com.vividsolutions.jts.geom.*;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.BorneDigueRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.ConvertPositionableCoordinates;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.primitive.jts.JTSLineIterator;
import org.geotoolkit.display2d.style.j2d.DoublePathWalker;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.math.XMath;
import org.geotoolkit.referencing.LinearReferencing;

import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Methodes de calculs utilitaires pour le référencement linéaire.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class LinearReferencingUtilities extends LinearReferencing {

    /**
     * Overload method from LinearReferencing to for a LineString and forbid LinearRing.
     *
     * @param candidate The geometry to convert. If null, a null value is returned.
     * @return The resulting linear, or null.
     */
    public static LineString asLineString(Geometry candidate) {
        LineString linear = null;
        if (candidate instanceof LineString) {
            linear = (LineString) candidate;
        } else if (candidate instanceof Polygon) {
            linear = ((Polygon)candidate).getExteriorRing();
        } else if (candidate instanceof Point) {
            Coordinate coordinate = candidate.getCoordinate();
            return GO2Utilities.JTS_FACTORY.createLineString(new Coordinate[]{coordinate, coordinate});
        } else if (candidate instanceof MultiPoint) {
            return GO2Utilities.JTS_FACTORY.createLineString(((candidate).getCoordinates()));
        } else if (candidate instanceof GeometryCollection) {
            final GeometryCollection gc = (GeometryCollection) candidate;
            final int nb = gc.getNumGeometries();
            if(nb>0){
                linear = asLineString(gc.getGeometryN(0));
            }
        }

        if(linear instanceof LinearRing){
            linear = GO2Utilities.JTS_FACTORY.createLineString(linear.getCoordinates());
        }

        return linear;
    }

    /**
     * Return the Linear geometry on which the input {@link SystemeReperage} is based on.
     *
     * @param t {@link TronconDigue} to extract line string from.
     * @param source The SR to get linear for. If null, we'll try to get tronçon
     * geometry of the currently edited {@link Positionable}.
     *
     * @return The linear associated, or null if we cannot get it.
     */
    public static LinearReferencing.SegmentInfo[] getSourceLinear(TronconDigue t, final SystemeReperage source) {
        LinearReferencing.SegmentInfo[] tronconSegments = null;
        Geometry linearSource = (t == null) ? null : t.getGeometry();
        if (linearSource == null) {
            if (source != null && source.getLinearId() != null) {
                final TronconDigue tmpTroncon = InjectorCore.getBean(SessionCore.class).getRepositoryForClass(TronconDigue.class).get(source.getLinearId());
                if (tmpTroncon != null) {
                    linearSource = tmpTroncon.getGeometry();
                }
            }
        }
        if (linearSource != null) {
            tronconSegments = LinearReferencingUtilities.buildSegments(LinearReferencing.asLineString(linearSource));
        }
        return tronconSegments;
    }

    /**
     * Builds a geometry for a target positionable that has prDebut, prFin and an SRId.
     *
     * The result geometry is built using the geometry of a reference positionable.
     *
     * @param refPositionable The reference positionable (must have a geometry).
     * @param target The positionable to build a geometry for (must have a
     * prDebut, prFin and SRId (to make sense to PRs values). Note the PRs have
     * to be included in the reference positionable geometry !
     * @param repo A repositoru to query needed {@link BorneDigue} from database.
     * @param srRepo A repositoru to query needed {@link SystemeReperage} from database.
     * @return A line string for target positionable object.
     */
    public static LineString buildSubGeometry(final Positionable refPositionable,
            final Positionable target,
            final AbstractSIRSRepository<BorneDigue> repo,
            final AbstractSIRSRepository<SystemeReperage> srRepo) {

        /*
         Situation exemple.

         À partir d'une géométrie de référence (issue du positionable de
         référence), on souhaite construire une nouvelle géométrie délimitée par
         deux PRs.

        *
        *    segment 1   |          segment 2        |  3   | segment 4 |segment 5
        * ===============|===========================|======|===========|=========
        *
        *                  prDebut                               prFin
        *                     x----------------------|------|------x
        *
        */
        final LineString refPositionableLineString = asLineString(refPositionable.getGeometry());
        final float prDebut = target.getPrDebut();
        final float prFin = target.getPrFin();
        final String srId = target.getSystemeRepId();
        final SystemeReperage targetSR = srRepo.get(srId);

        final LinearReferencing.SegmentInfo[] segments = buildSegments(refPositionableLineString);

        // Le segment de début est le segment de géométrie sur lequel se trouve le point de début
        LinearReferencing.SegmentInfo segmentDebut = null, segmentFin = null;
        double prDebutSegmentDebut = 0.;
        double prFinSegmentDebut = 0.;
        double prDebutSegmentFin = 0.;
        double prFinSegmentFin = 0.;
        double distanceAlongLinear0 = 0.;
        double distanceAlongLinear1 = 0.;

        for (final LinearReferencing.SegmentInfo segment : segments) {

            final Point ptDebutSegment = GO2Utilities.JTS_FACTORY.createPoint(segment.segmentCoords[0]);
            final double prDebutSegment = TronconUtils.computePR(segments, targetSR, ptDebutSegment, repo);
            final Point ptFinSegment = GO2Utilities.JTS_FACTORY.createPoint(segment.segmentCoords[1]);
            final double prFinSegment = TronconUtils.computePR(segments, targetSR, ptFinSegment, repo);

            /*
             Si on n'a toujours pas trouvé le segment sur lequel se trouve le point de début :
             */
            if (segmentDebut == null) {

                // On vérifie si le segment courant contient le point de début
                // 1- Si oui, on le garde en mémoire, ainsi que ses PRs de début et de fin.
                if (((prDebut >= prDebutSegment && prDebut <= prFinSegment)
                        || (prDebut <= prDebutSegment && prDebut >= prFinSegment))) {
                    segmentDebut = segment;
                    prDebutSegmentDebut = prDebutSegment;
                    prFinSegmentDebut = prFinSegment;
                } // 2- Sinon, on incrémente la distance
                else {
                    distanceAlongLinear0 += segment.length;
                }
            }

            // Idem pour le segment sur lequel se trouve le point de fin.
            if (segmentFin == null) {

                if (((prFin >= prDebutSegment && prFin <= prFinSegment)
                        || (prFin <= prDebutSegment && prFin >= prFinSegment))) {
                    segmentFin = segment;
                    prDebutSegmentFin = prDebutSegment;
                    prFinSegmentFin = prFinSegment;
                } else {
                    distanceAlongLinear1 += segment.length;
                }
            }

            /*
             Si on a trouvé les segments sur lesquels se trouvent les points de
             début et de fin de la géométrie à construire, on peut sortir de la
             boucle.
             */
            if (segmentFin != null && segmentDebut != null) {
                break;
            }
        }

        /*
         Si l'un des segments de début ou de fin est null en sortie de boucle,
         c'est que la géométrie à construire n'est pas incluse dans la géométrie
         de référence. On lance donc une exception pour le signaler.
         */
        if (segmentDebut == null || segmentFin == null) {
            throw new IllegalArgumentException("The geometry of the reference positionable must include the geometry that have to be built from the prs of the target positionable.");
        }

        /*
         À la fin de la boucle, on doit avoir les valeurs suivantes pour
         distanceAlongLinear0 et distanceAlongLinear1 :

        *
        *    segment 1   |          segment 2        |  3   | segment 4 |segment 5
        * ===============|====X======================|======|======X====|=========
        *                  prDebut                               prFin
        * -------------->| distanceAlongLinear0
        * ------------------------------------------------->| distanceAlongLinear1
        *
         ________________________________________________________________________

         On doit également a voir les valeurs suivantes pour les PRs de début
         et de fin des segments de début et de fin :

         segmentDebut : segment2
         segmentFin   : segment4


        *
        *                |        segmentDebut       |      | segmentFin|
        * ===============|===========================|======|===========|=========
        *                |                           |      |           |
        *                |                           |      |           |
        *                |                           |      |           x prFinSegmentFin
        *                |                           |      x prDebutSegmentFin
        *                |                           |
        *                |                           x prFinSegmentDebut
        *                x prDebutSegmentDebut
        *
         ________________________________________________________________________

         Il faut maintenant mettre à jour les distancesAlongLinear de manière
        à les ajuster aux points des pr de début et de fin du positionable pour
        lequel on veut construire la géométrie.

        *
        *    segment 1   |          segment 2        |  3   | segment 4 |segment 5
        * ===============|====X======================|======|======X====|=========
        *                  prDebut                               prFin
        * ------------------->| distanceAlongLinear0
        * -------------------------------------------------------->| distanceAlongLinear1
        *

        */



        distanceAlongLinear0 += (prDebut - prDebutSegmentDebut) / (prFinSegmentDebut - prDebutSegmentDebut);
        distanceAlongLinear1 += (prFin - prDebutSegmentFin) / (prFinSegmentFin - prDebutSegmentFin);

        /*
        Enfin, on découpe la géométrie du positionable de référence pour obtenir la nouvelle géométrie.

        *
        *    segment 1   |          segment 2        |  3   | segment 4 |segment 5
        * ===============|====X======================|======|======X====|=========
        *                  prDebut                               prFin
        * ------------------->| distanceAlongLinear0
        * -------------------------------------------------------->| distanceAlongLinear1
        *                     |                                    |
        *                     |                                    |
        *                     |                                    |
        * géométrie résultat: x======================|======|======x

        */
        return cut(refPositionableLineString, distanceAlongLinear0, distanceAlongLinear1);

    }

    /**
     * Create a JTS geometry for the input {@link Positionable}. Generated
     * geometry is a line string along an input geometry, whose beginning and
     * end are defined by geographic begin and end position in the
     * {@link Positionable}. If no valid point can be found, we will use its
     * start and end {@link BorneDigue}.
     *
     * @param tronconGeom The source geometry to follow when creating the new
     * one.
     * @param structure The object to generate a geometry for.
     * @param repo The {@link BorneDigueRepository} to use to retrieve input
     * {@link Positionable} bornes.
     * @return A line string for the given structure. Never null.
     */
    public static LineString buildGeometry(Geometry tronconGeom, Positionable structure, AbstractSIRSRepository<BorneDigue> repo) {
        final LineString tronconLineString = asLineString(tronconGeom);
        return buildGeometry(tronconLineString, buildSegments(tronconLineString), structure, repo);
    }

    /**
     *
     * Create a JTS geometry for the input {@link Positionable}. Generated
     * geometry is a line string along an input geometry, whose beginning and
     * end are defined by geographic begin and end position in the
     * {@link Positionable}. If no valid point can be found, we will use its
     * start and end {@link BorneDigue}.
     *
     * @param refLinear The source linear object to follow when creating the new
     * one.
     * @param segments soure linear object described as a list of segments.
     * @param structure The positionable object to compute geometry for.
     * @param repo A repository to access bornes along source linear object.
     * @return A line string for the given structure. Never null.
     */
    public static LineString buildGeometry(LineString refLinear, SegmentInfo[] segments, Positionable structure, AbstractSIRSRepository<BorneDigue> repo) {

        final Point positionDebut = structure.getPositionDebut();
        final Point positionFin = structure.getPositionFin();

        if (positionDebut != null || positionFin != null) {
            return buildGeometryFromGeo(refLinear, segments, positionDebut, positionFin);
        } else {
            return buildGeometryFromBorne(refLinear, segments, structure, repo);
        }
    }

    /**
     * Create a JTS geometry for the input {@link Positionable}. Generated
     * geometry is a line string along an input geometry, whose beginning and
     * end are defined by given bornes.
     *
     * @param tronconGeom The source geometry to follow when creating the new
     * one.
     * @param structure The object to generate a geometry for.
     * @param repo The {@link BorneDigueRepository} to use to retrieve input
     * @return A line string for the given structure. Never null.
     */
    public static LineString buildGeometryFromBorne(Geometry tronconGeom, Positionable structure, AbstractSIRSRepository<BorneDigue> repo) {
        final LineString tronconLineString = asLineString(tronconGeom);
        return buildGeometryFromBorne(tronconLineString, buildSegments(tronconLineString), structure, repo);
    }

    /**
     *
     * Create a JTS geometry for the input {@link Positionable}. Generated
     * geometry is a line string along an input geometry, whose beginning and
     * end are defined by given bornes.
     *
     * @param refLinear Line string to use as source.
     * @param segments Segments composing given line string.
     * @param structure The object to build a geometry for.
     * @param repo A repository to query needed {@link BorneDigue} from database.
     * @return A line string built for given structure.
     */
    public static LineString buildGeometryFromBorne(LineString refLinear, SegmentInfo[] segments, Positionable structure, AbstractSIRSRepository<BorneDigue> repo) {
        //reconstruction a partir de bornes et de distances
        final BorneDigue borneDebut = (structure.getBorneDebutId() != null) ? repo.get(structure.getBorneDebutId()) : null;
        final BorneDigue borneFin = (structure.getBorneFinId() != null) ? repo.get(structure.getBorneFinId()) : null;
        if (borneDebut == null && borneFin == null) {
            //aucune borne définie, on ne peut pas calculer la géométrie
            return null;
        }

        double distanceDebut = structure.getBorne_debut_distance();
        double distanceFin = structure.getBorne_fin_distance();
        //on considére que les troncons sont numérisé dans le sens amont vers aval.
        if (structure.getBorne_debut_aval()) {
            distanceDebut *= -1.0;
        }
        if (structure.getBorne_fin_aval()) {
            distanceFin *= -1.0;
        }

        //calcul de la distance des bornes. Il peut y avoir qu'une seule borne définie dans le cas d'un ponctuel.
        final Point tronconStart = GO2Utilities.JTS_FACTORY.createPoint(refLinear.getCoordinates()[0]);
        if (borneDebut != null) {
            final Point borneDebutGeom = borneDebut.getGeometry();
            final double borneDebutDistance = computeRelative(segments, new Point[]{tronconStart}, borneDebutGeom).getValue();
            //conversion des distances au borne en distance par rapport au debut du troncon
            distanceDebut += borneDebutDistance;
        }

        if (borneFin != null) {
            final Point borneFinGeom = borneFin.getGeometry();
            final double borneFinDistance = computeRelative(segments, new Point[]{tronconStart}, borneFinGeom).getValue();
            distanceFin += borneFinDistance;
        }

        if (borneDebut == null) {
            distanceDebut = distanceFin;
        } else if (borneFin == null) {
            distanceFin = distanceDebut;
        }
        return cut(refLinear, distanceDebut, distanceFin);
    }

    /**
     * Method used to sort the bornes and coordinates from Amont to Aval when they were recorded on the field from Aval to Amont.
     * It forces the recalculation of the geometry by setting it to null.
     * @param structure Positionable we want to sort the bornes for.
     * @return whether the structure has been updated and must be saved.
     */
    public static boolean ensureAvalToAmont(Positionable structure) {
        if (structure.getPrDebut() > structure.getPrFin()) {
            // store start point info
            final String tmpOldBorneDebutId = structure.getBorneDebutId();
            final boolean tmpOldBorneDebutAval = structure.getBorne_debut_aval();
            final double tmpOldBorneDebutDistance = structure.getBorne_debut_distance();
            final Point tmpPositionDebut = structure.getPositionDebut();
            // update start point info with end point info
            structure.setBorneDebutId(structure.getBorneFinId());
            structure.setBorne_debut_aval(structure.getBorne_fin_aval());
            structure.setBorne_debut_distance(structure.getBorne_fin_distance());
            structure.setPositionDebut(structure.getPositionFin());
            // update end point from stored point info
            structure.setBorneFinId(tmpOldBorneDebutId);
            structure.setBorne_fin_aval(tmpOldBorneDebutAval);
            structure.setBorne_fin_distance(tmpOldBorneDebutDistance);
            structure.setPositionFin(tmpPositionDebut);
            // force recalculation of geometry and PRs
            structure.setGeometry(null);
            ConvertPositionableCoordinates.updateGeometryAndPRs(structure);
            return true;
        }
        return false;
    }

    /**
     *
     * @param segments An array of segments composing a line string.
     * @param distanceAlongLinear Distance to the wanted segment, from the start of the segment array.
     * @return The nearest found segment as a line string, along with distance between segment start and given distance along reference line string.
     */
    public static Entry<LineString, Double> buildSegmentFromDistance(final SegmentInfo[] segments,
            final double distanceAlongLinear) {

        final Entry<SegmentInfo, Double> segmentAndDistance = getSegmentAndDistance(segments, distanceAlongLinear);

        return new HashMap.SimpleEntry<>(segmentAndDistance.getKey().geometry, segmentAndDistance.getValue());
    }

    /**
     * Pour un point sur le linéaire donné par une borne, sa distance à cette
     * borne et leur position relative, retourne le segment sur lequel se trouve
     * ce point, accompagné de la distance entre le début du segment et le point.
     *
     * @param tronconLineString Polyligne de réference.
     * @param borneId L'identifiant de la borne cible.
     * @param borneAval Vrai si la borne est en aval de la position voulue, faux si la borne est en amont.
     * @param borneDistance Distance entre la borne et le point voulu.
     * @param repo Connecteur à la base de données permettant de récupérer les bornes dans la base de données.
     * @return Segment contenant le point décrit, avec la distance entre le point et le debut du segment.
     */
    public static Entry<LineString, Double> buildSegmentFromBorne(final LineString tronconLineString,
            final String borneId, final boolean borneAval, final double borneDistance,
            final AbstractSIRSRepository<BorneDigue> repo) {

        final SegmentInfo[] segments = buildSegments(tronconLineString);

        //reconstruction a partir de bornes et de distances
        final BorneDigue borne = (borneId != null) ? repo.get(borneId) : null;
        if (borne == null) {
            //aucune borne définie, on ne peut pas calculer la géométrie
            return null;
        }

        double distanceDebut = borneDistance;
        //on considére que les troncons sont numérisé dans le sens amont vers aval.
        if (borneAval) {
            distanceDebut *= -1.0;
        }

        //calcul de la distance des bornes. Il peut y avoir qu'une seule borne définie dans le cas d'un ponctuel.
        final Point tronconStart = GO2Utilities.JTS_FACTORY.createPoint(tronconLineString.getCoordinates()[0]);

        // Distance entre la borne et le début du tronçon.
        final double distanceBorneTronconStart = computeRelative(segments, new Point[]{tronconStart}, borne.getGeometry()).getValue();

        // La distance au début du troncon se calcule par ajout de la distance entre la borne et le début du troncon à la distance du point à la borne
        distanceDebut += distanceBorneTronconStart;

        return buildSegmentFromDistance(segments, distanceDebut);
    }

    /**
     * Find nearest segment to given distance.
     *
     * @param segments An array of segments composing a line string.
     * @param distance Distance to the wanted segment, from the start of the segment array.
     * @return Extracted segment for given distance, and distance between segment start and given distance along reference line string.
     * @throws ArrayIndexOutOfBoundsException If given array is empty.
     */
    public static Entry<SegmentInfo, Double> getSegmentAndDistance(SegmentInfo[] segments, final double distance) {
        // First, we check extremums. So we eliminate any edge case.
        final SegmentInfo segment;
        if (segments[0].endDistance >= distance) {
            segment = segments[0];
        } else if (segments[segments.length -1].startDistance <= distance) {
            segment = segments[segments.length -1];
        } else {
            /* Make a binary search based on distances to speed up segment search.
             * Extremums are ignored as they've been fully tested above.
             */
            int lower = 1;
            int upper = segments.length - 2;
            int middle = 0;
            while (upper >= lower) {
                // We've not found a segment matching requirements, but we've only one possibility left.
                if (lower == upper) {
                    middle = lower;
                    break;
                }

                middle = lower + (upper - lower) / 2;
                if (segments[middle].endDistance < distance) {
                    // Checked segment ends before source distance
                    lower = middle + 1;
                } else if (segments[middle].startDistance > distance) {
                    // Checked segment starts after given distance
                    upper = middle - 1;
                } else
                    break;
            }

            segment = segments[middle];
        }

        return new AbstractMap.SimpleEntry<>(segment, distance - segment.startDistance);
    }

    /**
     * Create a JTS geometry for the input {@link Positionable}. Generated
     * geometry is a line string along an input geometry, whose beginning and
     * end are defined by given geographic begin and end position.
     *
     * @param tronconGeom The source geometry to follow when creating the new
     * one.
     * @param positionDebut Point to use as start for output geometry.
     * @param positionFin Point to use as end for output geometry.
     * @return A line string for the given structure. Never null.
     */
    public static LineString buildGeometryFromGeo(Geometry tronconGeom, Point positionDebut, Point positionFin) {
        final LineString linear = asLineString(tronconGeom);
        return buildGeometryFromGeo(linear, buildSegments(linear), positionDebut, positionFin);
    }

    /**
     *
     * @param referenceLinear The source geometry to follow when creating the new
     * one.
     * @param segments Input reference linear represented as a succession of segments.
     * @param positionDebut Start point of the geometry to compute.
     * @param positionFin End point of the geometry to compute.
     * @return A line along given linear structure, between start and end point.
     */
    public static LineString buildGeometryFromGeo(final LineString referenceLinear, final SegmentInfo[] segments, Point positionDebut, Point positionFin) {
        ProjectedPoint refDebut = null, refFin = null;
        if (positionDebut != null) {
            refDebut = projectReference(segments, positionDebut);
        }
        if (positionFin != null) {
            refFin = projectReference(segments, positionFin);
        }
        if (refDebut == null) {
            refDebut = refFin;
        } else if (refFin == null) {
            refFin = refDebut;
        }

        return cut(referenceLinear, refDebut.distanceAlongLinear, refFin.distanceAlongLinear);
    }

    /**
     * Create a line string which begins on input line, from a certain distance
     * after its beginning to another further away.
     *
     * @param linear The input {@link LineString} we want to extract a piece
     * from.
     * @param distanceDebut Distance from the start of input line for the
     * beginning of the new geometry.
     * @param distanceFin Distance from the start of input line for the end of
     * the new geometry.
     * @return A line string, never null.
     */
    public static LineString cut(final LineString linear, double distanceDebut, double distanceFin) {
        //on s"assure de ne pas sortir du troncon
        final double tronconLength = linear.getLength();

        final double tmpDistanceDebut = distanceDebut;
        distanceDebut = XMath.clamp(Math.min(tmpDistanceDebut, distanceFin), 0, tronconLength);
        distanceFin = XMath.clamp(Math.max(tmpDistanceDebut, distanceFin), 0, tronconLength);

        //create du tracé de la structure le long du troncon
        final PathIterator ite = new JTSLineIterator(linear, null);
        final DoublePathWalker walker = new DoublePathWalker(ite);
        walker.walk(distanceDebut);
        double remain = distanceFin - distanceDebut;

        final List<Coordinate> structureCoords = new ArrayList<>();
        Point2D point = walker.getPosition(null);
        structureCoords.add(new Coordinate(point.getX(), point.getY()));

        while (!walker.isFinished() && remain > 0) {
            final double advance = Math.min(walker.getSegmentLengthRemaining(), remain);
            remain -= advance;
            walker.walk(advance);
            point = walker.getPosition(point);
            structureCoords.add(new Coordinate(point.getX(), point.getY()));
        }

        if (structureCoords.size() == 1) {
            //point unique, on le duplique pour obtenir on moins un segment
            structureCoords.add(new Coordinate(structureCoords.get(0)));
        }

        final LineString geom = GO2Utilities.JTS_FACTORY.createLineString(structureCoords.toArray(new Coordinate[structureCoords.size()]));
        JTS.setCRS(geom, InjectorCore.getBean(SessionCore.class).getProjection());

        return geom;
    }


    /**
     * Create a line string which begins on input line, from a certain distance
     * after its beginning to another further away.
     *
     * @param linear The input {@link LineString} we want to extract a piece
     * from.
     * @param indexDebut the coordinate index to start (included)
     * @param indexFin the coordinate index to end (included). Must be greater than indexDebut.
     *
     * @return A line string, never null.
     */
    public static LineString cut(final LineString linear, int indexDebut, double indexFin) {

        final List<Coordinate> structureCoords = new ArrayList<>();
        for (int i=0; i<=(indexFin-indexDebut); i++){
            structureCoords.set(i, linear.getCoordinateN(indexDebut+i));
        }

        final LineString geom = GO2Utilities.JTS_FACTORY.createLineString(structureCoords.toArray(new Coordinate[structureCoords.size()]));
        JTS.setCRS(geom, InjectorCore.getBean(SessionCore.class).getProjection());

        return geom;
    }

    /**
     * Search which bornes of the given SR are enclosing given point. Input SR
     * bornes are projected on given linear for the analysis.
     *
     * @param sourceLinear The set of segments composing reference linear.
     * @param toGetBundaryFor The point for which we want enclosing bornes.
     * @param possibleBornes List of points in which we'll pick bounding bornes.
     */
    public void getBoundingBornes(SegmentInfo[] sourceLinear, final Point toGetBundaryFor, final Point... possibleBornes) {
        ProjectedPoint projectedPoint = projectReference(sourceLinear, toGetBundaryFor);
        if (projectedPoint.segment == null) {
            throw new RuntimeException("Cannot project point on linear."); // TODO : better exception
        }        // We'll try to find bornes on the nearest possible segment.
        if (projectedPoint.segmentIndex < 0) {
            throw new RuntimeException("Cannot project point on linear."); // TODO : better exception
        }
        for (final Point borne : possibleBornes) {
            ProjectedPoint projBorne = projectReference(sourceLinear, borne);
            if (projBorne.segmentIndex < 0) {
                continue;
            }
        }

    }
}
