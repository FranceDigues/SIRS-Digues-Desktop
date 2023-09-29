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
package fr.sirs.core.model;

import javafx.beans.property.ObjectProperty;

import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * Spécifie une date de fin de validité temporelle.
 *
 * @author Estelle Idée (Geomatys)
 */
public interface AvecFinTemporelle {

    public ObjectProperty<LocalDate> date_finProperty();

    public LocalDate getDate_fin();

    public void setDate_fin(LocalDate date_fin);


    /**
     * Predicate to determine whether an Element is archived.
     *
     * Returns true if the Element is not archived.
     */
    public static final class IsNotArchivedPredicate implements Predicate<AvecFinTemporelle> {

        @Override
        public boolean test(AvecFinTemporelle input) {
            final LocalDate endDate = input.getDate_fin();
            return endDate == null || endDate.isAfter(LocalDate.now());
        }
    }
}
