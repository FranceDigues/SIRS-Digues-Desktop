package fr.sirs.core.model;

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;

import java.util.List;

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
public class HorodatageReference {


    private static String refNonTimeStampedStatus;
    private static String refWaitingStatus;
    private static String refTimeStampedStatus;
    private static final String NON_TIME_STAMPED= "Non horodaté";
    private static final String WAITING = "En attente";
    public static final String TIME_STAMPED = "Horodaté";

    public static String getRefNonTimeStampedStatus() {
        if (refNonTimeStampedStatus == null) {
            checkRefHorodatageStatusAllPresent();
        }
        return refNonTimeStampedStatus;
    }

    public static String getRefWaitingStatus() {
        if (refWaitingStatus == null) {
            checkRefHorodatageStatusAllPresent();
        }
        return refWaitingStatus;
    }

    public static String getRefTimeStampedStatus() {
        if (refTimeStampedStatus == null) {
            checkRefHorodatageStatusAllPresent();
        }
        return refTimeStampedStatus;
    }

    /**
     * Check that all the requested time stamped status are available in SIRS :
     * <ul>
     *     <li>Non horodaté</li>
     *     <li>En attente</li>
     *     <li>Horodaté</li>
     * </ul>
     * @return true if all the requested status are present.
     */
    private static boolean checkRefHorodatageStatusAllPresent() {
        final List<RefHorodatageStatus> allStatus = InjectorCore.getBean(SessionCore.class).getRepositoryForClass(RefHorodatageStatus.class).getAll();
        if (allStatus == null || allStatus.isEmpty())
            return false;


        allStatus.forEach(status -> {
            String libelle = status.getLibelle();
            if (NON_TIME_STAMPED.equalsIgnoreCase(libelle.trim()))
                refNonTimeStampedStatus = status.getId();
            else if (WAITING.equalsIgnoreCase(libelle.trim()))
                refWaitingStatus = status.getId();
            else if (TIME_STAMPED.equalsIgnoreCase(libelle.trim()))
                refTimeStampedStatus = status.getId();
        });
        return refNonTimeStampedStatus != null && refWaitingStatus != null && refTimeStampedStatus != null;
    }
}
