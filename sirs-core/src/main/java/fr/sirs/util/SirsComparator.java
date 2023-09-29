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

package fr.sirs.util;

import fr.sirs.core.model.AbstractObservation;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.Element;
import java.util.Comparator;

/**
 *
 * @author maximegavens
 */
public class SirsComparator {
    /**
     * Pour le classement des observations de la plus récente à la plus
     * ancienne.
     */
    public static final Comparator<AbstractObservation> OBSERVATION_COMPARATOR = (o1, o2) -> {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null || o2 == null) {
            return (o1 == null) ? -1 : 1;
        } else if (o1.getDate() == null && o2.getDate() == null) {
            return 0;
        } else if (o1.getDate() == null || o2.getDate() == null) {
            return (o1.getDate() == null) ? 1 : -1;
        } else {
            return -o1.getDate().compareTo(o2.getDate());
        }
    };

    /**
     * Pour le classement des photographies de la plus récente à la plus
     * ancienne.
     */
    static final Comparator<AbstractPhoto> PHOTO_COMPARATOR = (p1, p2) -> {
        if (p1 == null && p2 == null) {
            return 0;
        } else if (p1 == null || p2 == null) {
            return (p1 == null) ? -1 : 1;
        } else if (p1.getDate() == null && p2.getDate() == null) {
            return 0;
        } else if (p1.getDate() == null || p2.getDate() == null) {
            return (p1.getDate() == null) ? 1 : -1;
        } else {
            return -p1.getDate().compareTo(p2.getDate());
        }
    };

    /**
     * Pour le classement des éléments par désignation (ordre alphabétique).
     */
    static final Comparator<Element> ELEMENT_COMPARATOR = (p1, p2) -> {
        if (p1 == null && p2 == null) {
            return 0;
        } else if (p1 == null || p2 == null) {
            return (p1 == null) ? -1 : 1;
        } else if (p1.getDesignation() == null && p2.getDesignation() == null) {
            return 0;
        } else if (p1.getDesignation() == null || p2.getDesignation() == null) {
            return (p1.getDesignation() == null) ? 1 : -1;
        } else {
            return p1.getDesignation().compareTo(p2.getDesignation());
        }
    };
}
